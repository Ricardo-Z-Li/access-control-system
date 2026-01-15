package acs.simulator;

import acs.domain.AccessResult;
import acs.domain.BadgeReader;
import acs.repository.BadgeReaderRepository;
import acs.service.ClockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件模拟器实现，负责生成并发访问事件、模拟时间加速和收集性能指标。
 * 支持300徽章/400读卡器的并发访问场景模拟。
 */
@Service
public class EventSimulatorImpl implements EventSimulator {

    private final BadgeReaderSimulator badgeReaderSimulator;
    private final BadgeReaderRepository badgeReaderRepository;
    private final ClockService clockService;
    
    // 模拟状态
    private SimulationStatus status = SimulationStatus.IDLE;
    private double timeAccelerationFactor = 1.0; // 时间加速因子（1.0=实时）
    
    // 并发控制
    private ExecutorService executorService;
    private final List<Future<?>> futures = new ArrayList<>();
    private CountDownLatch completionLatch;
    
    // 性能指标
    private final AtomicInteger totalEvents = new AtomicInteger(0);
    private final AtomicInteger completedEvents = new AtomicInteger(0);
    private final AtomicInteger failedEvents = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicInteger grantedAccess = new AtomicInteger(0);
    private final AtomicInteger deniedAccess = new AtomicInteger(0);
    
    // 事件监听器
    private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();
    
    // 配置参数
    private static final int DEFAULT_EVENT_DELAY_MS = 1000; // default event delay in ms
    private static final String SCENARIO_CONFIG_PATH = "simulator/scenarios.json";

    private final ObjectMapper objectMapper;
    private final Map<String, PathAssignment> pathAssignments = new ConcurrentHashMap<>();
    
    @Autowired
    public EventSimulatorImpl(BadgeReaderSimulator badgeReaderSimulator,
                              BadgeReaderRepository badgeReaderRepository,
                              ClockService clockService) {
        this.badgeReaderSimulator = badgeReaderSimulator;
        this.badgeReaderRepository = badgeReaderRepository;
        this.objectMapper = new ObjectMapper();
        this.clockService = clockService;
        this.completionLatch = new CountDownLatch(0); // 初始化为0
    }

    @Override
    public void startSimulation(int numEvents, int concurrencyLevel) {
        if (status == SimulationStatus.RUNNING) {
            throw new IllegalStateException("Simulator is already running");
        }
        
        setSimulationStatus(SimulationStatus.RUNNING);
        resetSimulationStats();
        
        // 创建虚拟线程执行器
        executorService = Executors.newFixedThreadPool(Math.max(1, concurrencyLevel));
        
        // 获取可用的读卡器列表
        List<String> readerIds = getAvailableReaderIds();
        if (readerIds.isEmpty()) {
            throw new IllegalStateException("No available readers for simulation");
        }
        
        // 获取可用的徽章列表（模拟数据）
        List<String> badgeIds = generateSimulatedBadgeIds();

        SimulationScenarioConfig scenarioConfig = loadScenarioConfig();
        boolean scenarioEnabled = isScenarioEnabled(scenarioConfig);
        Integer scenarioStepDelayMs = scenarioConfig != null ? scenarioConfig.getStepDelayMs() : null;
        Map<String, List<String>> resourceReaderMap = buildResourceReaderMap();
        pathAssignments.clear();
        
        // 计算每个线程的事件数
        int eventsPerThread = numEvents / concurrencyLevel;
        int remainingEvents = numEvents % concurrencyLevel;
        
        completionLatch = new CountDownLatch(concurrencyLevel);
        
        // 提交任务到线程池
        for (int i = 0; i < concurrencyLevel; i++) {
            int eventsForThisThread = eventsPerThread + (i < remainingEvents ? 1 : 0);
            if (eventsForThisThread > 0) {
                futures.add(executorService.submit(new SimulationTask(
                        eventsForThisThread, readerIds, badgeIds, completionLatch,
                        scenarioEnabled, scenarioConfig, scenarioStepDelayMs, resourceReaderMap)));
            } else {
                completionLatch.countDown();
            }
        }
        
        // 启动监控线程
        startMonitoringThread();
    }

    @Override
    public void stopSimulation() {
        if (status != SimulationStatus.RUNNING && status != SimulationStatus.PAUSED) {
            return;
        }
        
        setSimulationStatus(SimulationStatus.STOPPED);
        
        // 取消所有任务
        futures.forEach(future -> future.cancel(true));
        
        // 关闭线程池
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        futures.clear();
    }

    @Override
    public void setTimeAcceleration(double factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("Time acceleration factor must be > 0");
        }
        this.timeAccelerationFactor = factor;
    }

    @Override
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        int completed = completedEvents.get();
        int total = totalEvents.get();
        int failed = failedEvents.get();
        
        metrics.put("totalEvents", total);
        metrics.put("completedEvents", completed);
        metrics.put("failedEvents", failed);
        metrics.put("grantedAccess", grantedAccess.get());
        metrics.put("deniedAccess", deniedAccess.get());
        
        if (completed > 0) {
            metrics.put("averageProcessingTimeMs", (double) totalProcessingTime.get() / completed);
            metrics.put("successRate", (double) completed / total);
            metrics.put("grantRate", (double) grantedAccess.get() / completed);
        } else {
            metrics.put("averageProcessingTimeMs", 0.0);
            metrics.put("successRate", 0.0);
            metrics.put("grantRate", 0.0);
        }
        
        metrics.put("timeAccelerationFactor", timeAccelerationFactor);
        metrics.put("simulationStatus", status.toString());
        
        return metrics;
    }

    @Override
    public SimulationStatus getSimulationStatus() {
        return status;
    }

    @Override
    public void resetSimulation() {
        stopSimulation();
        resetSimulationStats();
        setSimulationStatus(SimulationStatus.IDLE);
    }

    @Override
    public void addSimulationListener(SimulationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeSimulationListener(SimulationListener listener) {
        listeners.remove(listener);
    }
    
    private List<String> getAvailableReaderIds() {
        // 获取状态为ONLINE的读卡器
        return badgeReaderRepository.findByStatus("ONLINE").stream()
                .map(reader -> reader.getReaderId())
                .toList();
    }
    
    private List<String> generateSimulatedBadgeIds() {
        // 生成模拟徽章ID（在实际系统中应从数据库获取）
        List<String> badgeIds = new ArrayList<>();
        for (int i = 1; i <= 300; i++) {
            badgeIds.add("BADGE00" + i);
        }
        return badgeIds;
    }
    
    private void setSimulationStatus(SimulationStatus newStatus) {
        SimulationStatus oldStatus = this.status;
        this.status = newStatus;
        
        // 通知监听器
        listeners.forEach(listener -> listener.onSimulationStatusChanged(oldStatus, newStatus));
    }
    
    private void resetSimulationStats() {
        totalEvents.set(0);
        completedEvents.set(0);
        failedEvents.set(0);
        totalProcessingTime.set(0);
        grantedAccess.set(0);
        deniedAccess.set(0);
    }
    
    private void startMonitoringThread() {
        new Thread(() -> {
            try {
                completionLatch.await();
                setSimulationStatus(SimulationStatus.STOPPED);
                
                // 所有任务完成，关闭线程池
                if (executorService != null) {
                    executorService.shutdown();
                }
                
                // 输出最终统计信息
                System.out.println("Simulation completed: " + getPerformanceMetrics());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                setSimulationStatus(SimulationStatus.ERROR);
            }
        }).start();
    }
    
    /**
     * 模拟任务类，负责执行单个线程的模拟事件
     */
    private SimulationScenarioConfig loadScenarioConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(SCENARIO_CONFIG_PATH)) {
            if (input == null) {
                return null;
            }
            return objectMapper.readValue(input, SimulationScenarioConfig.class);
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isScenarioEnabled(SimulationScenarioConfig config) {
        return config != null && config.isEnabled()
                && config.getPaths() != null && !config.getPaths().isEmpty();
    }

    private Map<String, List<String>> buildResourceReaderMap() {
        Map<String, List<String>> map = new HashMap<>();
        for (BadgeReader reader : badgeReaderRepository.findAll()) {
            if (reader.getResourceId() == null) {
                continue;
            }
            map.computeIfAbsent(reader.getResourceId(), key -> new ArrayList<>())
                    .add(reader.getReaderId());
        }
        return map;
    }

    private SimulationPath selectRandomPath(SimulationScenarioConfig config, Random random) {
        List<SimulationPath> paths = config.getPaths();
        return paths.get(random.nextInt(paths.size()));
    }

    private String getScenarioResourceId(String badgeId, Random random, SimulationScenarioConfig config) {
        if (!isScenarioEnabled(config)) {
            return null;
        }
        PathAssignment assignment = pathAssignments.computeIfAbsent(badgeId,
                id -> new PathAssignment(selectRandomPath(config, random)));
        SimulationPath path = assignment.getPath();
        if (path == null || path.getResourceIds() == null || path.getResourceIds().isEmpty()) {
            return null;
        }
        int index = assignment.nextIndex(path.getResourceIds().size());
        return path.getResourceIds().get(index);
    }

    private String selectReaderId(String resourceId,
                                  Map<String, List<String>> resourceReaderMap,
                                  List<String> fallbackReaderIds,
                                  Random random) {
        if (resourceId != null) {
            List<String> readers = resourceReaderMap.get(resourceId);
            if (readers != null && !readers.isEmpty()) {
                return readers.get(random.nextInt(readers.size()));
            }
        }
        if (fallbackReaderIds == null || fallbackReaderIds.isEmpty()) {
            return null;
        }
        return fallbackReaderIds.get(random.nextInt(fallbackReaderIds.size()));
    }

    private static class PathAssignment {
        private final SimulationPath path;
        private final AtomicInteger index = new AtomicInteger(0);

        private PathAssignment(SimulationPath path) {
            this.path = path;
        }

        public SimulationPath getPath() {
            return path;
        }

        public int nextIndex(int size) {
            if (size <= 0) {
                return 0;
            }
            int current = index.getAndIncrement();
            int value = current % size;
            return value < 0 ? value + size : value;
        }
    }

    private class SimulationTask implements Runnable {
        private final int numEvents;
        private final List<String> readerIds;
        private final List<String> badgeIds;
        private final CountDownLatch latch;
        private final boolean scenarioEnabled;
        private final SimulationScenarioConfig scenarioConfig;
        private final Integer scenarioStepDelayMs;
        private final Map<String, List<String>> resourceReaderMap;

        public SimulationTask(int numEvents, List<String> readerIds,
                              List<String> badgeIds, CountDownLatch latch,
                              boolean scenarioEnabled,
                              SimulationScenarioConfig scenarioConfig,
                              Integer scenarioStepDelayMs,
                              Map<String, List<String>> resourceReaderMap) {
            this.numEvents = numEvents;
            this.readerIds = readerIds;
            this.badgeIds = badgeIds;
            this.latch = latch;
            this.scenarioEnabled = scenarioEnabled;
            this.scenarioConfig = scenarioConfig;
            this.scenarioStepDelayMs = scenarioStepDelayMs;
            this.resourceReaderMap = resourceReaderMap;
        }

        @Override
        public void run() {
            try {
                Random random = new Random();

                for (int i = 0; i < numEvents; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }

                    String badgeId = badgeIds.get(random.nextInt(badgeIds.size()));
                    String resourceId = scenarioEnabled
                            ? getScenarioResourceId(badgeId, random, scenarioConfig)
                            : null;
                    String readerId = scenarioEnabled
                            ? selectReaderId(resourceId, resourceReaderMap, readerIds, random)
                            : readerIds.get(random.nextInt(readerIds.size()));
                    if (readerId == null) {
                        readerId = readerIds.get(random.nextInt(readerIds.size()));
                    }
                    String eventId = "EVENT_" + clockService.now().toEpochMilli() + "_" + i;

                    final String finalReaderId = readerId;
                    final String finalBadgeId = badgeId;
                    final String finalEventId = eventId;

                    listeners.forEach(l -> l.onSimulationEventStarted(finalEventId, finalReaderId, finalBadgeId));
                    totalEvents.incrementAndGet();

                    long startTime = System.currentTimeMillis();

                    try {
                        AccessResult result = badgeReaderSimulator.simulateBadgeSwipe(readerId, badgeId, eventId);

                        long endTime = System.currentTimeMillis();
                        long processingTime = endTime - startTime;

                        completedEvents.incrementAndGet();
                        totalProcessingTime.addAndGet(processingTime);

                        if (result.getDecision().toString().equals("ALLOW")) {
                            grantedAccess.incrementAndGet();
                        } else {
                            deniedAccess.incrementAndGet();
                        }

                        listeners.forEach(l -> l.onSimulationEventCompleted(finalEventId, result, processingTime));

                    } catch (Exception e) {
                        failedEvents.incrementAndGet();
                        listeners.forEach(l -> l.onSimulationError(finalEventId, e.getMessage()));
                    }

                    if (i < numEvents - 1) {
                        int baseDelayMs = DEFAULT_EVENT_DELAY_MS;
                        if (scenarioEnabled && scenarioStepDelayMs != null && scenarioStepDelayMs > 0) {
                            baseDelayMs = scenarioStepDelayMs;
                        }
                        int delay = (int) (baseDelayMs / timeAccelerationFactor);
                        Thread.sleep(Math.max(1, delay));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        }
    }
}
