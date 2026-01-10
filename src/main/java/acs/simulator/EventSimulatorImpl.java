package acs.simulator;

import acs.domain.AccessResult;
import acs.repository.BadgeReaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    private static final int DEFAULT_EVENT_DELAY_MS = 1000; // 默认事件间隔1秒
    
    @Autowired
    public EventSimulatorImpl(BadgeReaderSimulator badgeReaderSimulator,
                              BadgeReaderRepository badgeReaderRepository) {
        this.badgeReaderSimulator = badgeReaderSimulator;
        this.badgeReaderRepository = badgeReaderRepository;
        this.completionLatch = new CountDownLatch(0); // 初始化为0
    }

    @Override
    public void startSimulation(int numEvents, int concurrencyLevel) {
        if (status == SimulationStatus.RUNNING) {
            throw new IllegalStateException("模拟器已在运行中");
        }
        
        setSimulationStatus(SimulationStatus.RUNNING);
        resetSimulationStats();
        
        // 创建虚拟线程执行器
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        
        // 获取可用的读卡器列表
        List<String> readerIds = getAvailableReaderIds();
        if (readerIds.isEmpty()) {
            throw new IllegalStateException("没有可用的读卡器进行模拟");
        }
        
        // 获取可用的徽章列表（模拟数据）
        List<String> badgeIds = generateSimulatedBadgeIds();
        
        // 计算每个线程的事件数
        int eventsPerThread = numEvents / concurrencyLevel;
        int remainingEvents = numEvents % concurrencyLevel;
        
        completionLatch = new CountDownLatch(concurrencyLevel);
        
        // 提交任务到线程池
        for (int i = 0; i < concurrencyLevel; i++) {
            int eventsForThisThread = eventsPerThread + (i < remainingEvents ? 1 : 0);
            if (eventsForThisThread > 0) {
                futures.add(executorService.submit(new SimulationTask(
                        eventsForThisThread, readerIds, badgeIds, completionLatch)));
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
            throw new IllegalArgumentException("时间加速因子必须大于0");
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
            badgeIds.add("SIM_BADGE_" + i);
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
                System.out.println("模拟完成: " + getPerformanceMetrics());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                setSimulationStatus(SimulationStatus.ERROR);
            }
        }).start();
    }
    
    /**
     * 模拟任务类，负责执行单个线程的模拟事件
     */
    private class SimulationTask implements Runnable {
        private final int numEvents;
        private final List<String> readerIds;
        private final List<String> badgeIds;
        private final CountDownLatch latch;
        
        public SimulationTask(int numEvents, List<String> readerIds, 
                              List<String> badgeIds, CountDownLatch latch) {
            this.numEvents = numEvents;
            this.readerIds = readerIds;
            this.badgeIds = badgeIds;
            this.latch = latch;
        }
        
        @Override
        public void run() {
            try {
                Random random = new Random();
                
                for (int i = 0; i < numEvents; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    
                    // 随机选择读卡器和徽章
                    String readerId = readerIds.get(random.nextInt(readerIds.size()));
                    String badgeId = badgeIds.get(random.nextInt(badgeIds.size()));
                    String eventId = "EVENT_" + Instant.now().toEpochMilli() + "_" + i;
                    
                    // 通知事件开始
                    listeners.forEach(l -> l.onSimulationEventStarted(eventId, readerId, badgeId));
                    totalEvents.incrementAndGet();
                    
                    long startTime = System.currentTimeMillis();
                    
                    try {
                        // 模拟刷卡事件
                        AccessResult result = badgeReaderSimulator.simulateBadgeSwipe(readerId, badgeId);
                        
                        long endTime = System.currentTimeMillis();
                        long processingTime = endTime - startTime;
                        
                        // 更新统计信息
                        completedEvents.incrementAndGet();
                        totalProcessingTime.addAndGet(processingTime);
                        
                        if (result.getDecision().toString().equals("ALLOW")) {
                            grantedAccess.incrementAndGet();
                        } else {
                            deniedAccess.incrementAndGet();
                        }
                        
                        // 通知事件完成
                        listeners.forEach(l -> l.onSimulationEventCompleted(eventId, result, processingTime));
                        
                    } catch (Exception e) {
                        failedEvents.incrementAndGet();
                        listeners.forEach(l -> l.onSimulationError(eventId, e.getMessage()));
                    }
                    
                    // 模拟事件间隔（应用时间加速因子）
                    if (i < numEvents - 1) { // 最后一个事件后不等待
                        int delay = (int) (DEFAULT_EVENT_DELAY_MS / timeAccelerationFactor);
                        Thread.sleep(Math.max(1, delay)); // 至少1毫秒
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