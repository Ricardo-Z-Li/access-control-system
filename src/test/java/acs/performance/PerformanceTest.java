package acs.performance;

import acs.cache.LocalCacheManager;
import acs.domain.*;
import acs.repository.*;
import acs.simulator.EventSimulator;
import acs.simulator.SimulationStatus;
import acs.service.AccessControlService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 大规模性能测试类，模拟300徽章/400读卡器的并发访问场景。
 * 测试系统在高并发下的性能表现，包括响应时间、吞吐量和错误率。
 * 使用真实数据库集成测试，确保性能测试的准确性。
 */
@SpringBootTest
@ActiveProfiles("test")
public class PerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceTest.class);

    // 配置参数
    private static final int NUM_BADGES = 300;
    private static final int NUM_READERS = 400;
    private static final int NUM_EVENTS = 1000; // 测试事件总数
    private static final int CONCURRENCY_LEVEL = 100; // 并发级别
    private static final int THREAD_POOL_SIZE = 50; // 直接并发测试的线程池大小

    @Autowired
    private EventSimulator eventSimulator;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private LocalCacheManager cacheManager;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private BadgeReaderRepository badgeReaderRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private GroupRepository groupRepository;

    // 测试数据集合
    private List<Badge> testBadges = new ArrayList<>();
    private List<BadgeReader> testReaders = new ArrayList<>();
    private List<Employee> testEmployees = new ArrayList<>();
    private List<Resource> testResources = new ArrayList<>();
    private List<Group> testGroups = new ArrayList<>();

    /**
     * 准备大规模测试数据：300徽章、400读卡器、员工、资源、组等。
     */
    @BeforeEach
    @Transactional
    @Rollback(false)
    void setUp() {
        logger.info("开始准备大规模性能测试数据...");
        
        // 清空现有数据
        badgeReaderRepository.deleteAll();
        badgeRepository.deleteAll();
        employeeRepository.deleteAll();
        resourceRepository.deleteAll();
        groupRepository.deleteAll();

        // 1. 创建300个徽章和对应的员工
        for (int i = 1; i <= NUM_BADGES; i++) {
            Badge badge = new Badge();
            badge.setBadgeId("PERF_BADGE_" + i);
            badge.setStatus(BadgeStatus.ACTIVE);
            badge.setBadgeCode("CODE_" + UUID.randomUUID().toString().substring(0, 8));
            badgeRepository.save(badge);
            testBadges.add(badge);

            Employee employee = new Employee();
            employee.setEmployeeId("PERF_EMP_" + i);
            employee.setEmployeeName("员工 " + i);
            employee.setBadge(badge);
            employeeRepository.save(employee);
            testEmployees.add(employee);
        }

        // 2. 创建400个读卡器和对应的资源
        for (int i = 1; i <= NUM_READERS; i++) {
            Resource resource = new Resource();
            resource.setResourceId("PERF_RES_" + i);
            resource.setResourceName("资源 " + i);
            resource.setResourceType(ResourceType.DOOR);
            resource.setResourceState(ResourceState.AVAILABLE);
            resource.setIsControlled(true);
            resourceRepository.save(resource);
            testResources.add(resource);

            BadgeReader reader = new BadgeReader();
            reader.setReaderId("PERF_READER_" + i);
            reader.setReaderName("读卡器 " + i);
            reader.setLocation("位置 " + i);
            reader.setStatus("ONLINE");
            reader.setResourceId(resource.getResourceId());
            badgeReaderRepository.save(reader);
            testReaders.add(reader);
        }

        // 3. 创建测试组并关联员工和资源
        Group group = new Group();
        group.setGroupId("PERF_GROUP_1");
        group.setName("性能测试组");
        // 关联员工（前50个员工）
        group.setEmployees(new HashSet<>(testEmployees.subList(0, Math.min(50, testEmployees.size()))));
        // 关联资源（前100个资源）
        group.setResources(new HashSet<>(testResources.subList(0, Math.min(100, testResources.size()))));
        groupRepository.save(group);
        testGroups.add(group);

        // 4. 刷新缓存
        cacheManager.refreshAllCache();

        logger.info("大规模测试数据准备完成: {} 徽章, {} 读卡器, {} 员工, {} 资源, {} 组",
                testBadges.size(), testReaders.size(), testEmployees.size(), testResources.size(), testGroups.size());
    }

    @AfterEach
    @Transactional
    @Rollback(false)
    void tearDown() {
        // 清理测试数据
        badgeReaderRepository.deleteAll();
        badgeRepository.deleteAll();
        employeeRepository.deleteAll();
        resourceRepository.deleteAll();
        groupRepository.deleteAll();
        logger.info("测试数据清理完成");
    }

    /**
     * 测试1：使用EventSimulator进行大规模并发事件模拟
     * 模拟PDF要求的300徽章/400读卡器并发访问场景。
     */
    @Test
    void eventSimulator_largeScaleConcurrentEvents_shouldMeetPerformanceRequirements() throws InterruptedException {
        logger.info("开始大规模并发事件模拟测试...");
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 启动模拟：1000个事件，100并发级别
        eventSimulator.startSimulation(NUM_EVENTS, CONCURRENCY_LEVEL);

        // 等待模拟完成（最多5分钟）
        int maxWaitSeconds = 300;
        int waitIntervalMs = 1000;
        int waitedSeconds = 0;

        while (eventSimulator.getSimulationStatus() == SimulationStatus.RUNNING && waitedSeconds < maxWaitSeconds) {
            Thread.sleep(waitIntervalMs);
            waitedSeconds++;

            // 每10秒输出一次进度
            if (waitedSeconds % 10 == 0) {
                Map<String, Object> metrics = eventSimulator.getPerformanceMetrics();
                logger.info("模拟进度: {}/{} 事件完成, 已等待 {} 秒",
                        metrics.get("completedEvents"), metrics.get("totalEvents"), waitedSeconds);
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTimeMs = endTime - startTime;

        // 获取最终性能指标
        Map<String, Object> finalMetrics = eventSimulator.getPerformanceMetrics();
        
        // 输出性能结果
        logger.info("大规模并发模拟完成，耗时: {} 毫秒", totalTimeMs);
        logger.info("性能指标:");
        finalMetrics.forEach((key, value) -> logger.info("  {}: {}", key, value));

        // 验证性能要求
        int completedEvents = (int) finalMetrics.get("completedEvents");
        double successRate = (double) finalMetrics.get("successRate");
        double avgProcessingTimeMs = (double) finalMetrics.get("averageProcessingTimeMs");

        // 基本验证
        assertTrue(completedEvents > 0, "应至少完成一些事件");
        assertTrue(successRate >= 0.9, "成功率应达到90%以上");
        assertTrue(avgProcessingTimeMs < 1000, "平均处理时间应小于1000毫秒");

        logger.info("大规模并发事件模拟测试通过");
    }

    /**
     * 测试2：直接并发调用AccessControlService处理访问请求
     * 模拟真实场景下的并发访问压力。
     */
    @Test
    void accessControlService_directConcurrentCalls_shouldHandleConcurrentRequests() throws InterruptedException {
        logger.info("开始直接并发访问控制测试...");

        // 准备测试数据：选择一些徽章和读卡器
        List<String> badgeIds = testBadges.stream()
                .map(Badge::getBadgeId)
                .limit(50)
                .collect(Collectors.toList());
        
        List<String> readerIds = testReaders.stream()
                .map(BadgeReader::getReaderId)
                .limit(100)
                .collect(Collectors.toList());
        
        // 创建读卡器到资源的映射
        Map<String, String> readerToResourceMap = testReaders.stream()
                .limit(100)
                .collect(Collectors.toMap(
                    BadgeReader::getReaderId,
                    BadgeReader::getResourceId
                ));

        // 性能统计
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> processingTimes = Collections.synchronizedList(new ArrayList<>());

        // 创建虚拟线程执行器
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(NUM_EVENTS);

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 提交并发任务
        Random random = new Random();
        for (int i = 0; i < NUM_EVENTS; i++) {
            executorService.submit(() -> {
                try {
                    // 随机选择徽章和读卡器
                    String badgeId = badgeIds.get(random.nextInt(badgeIds.size()));
                    String readerId = readerIds.get(random.nextInt(readerIds.size()));
                    String resourceId = readerToResourceMap.get(readerId);

                    // 如果找不到资源，跳过此请求
                    if (resourceId == null) {
                        errorCount.incrementAndGet();
                        return;
                    }

                    long requestStartTime = System.currentTimeMillis();
                    
                    // 执行访问控制
                    AccessRequest request = new AccessRequest();
                    request.setBadgeId(badgeId);
                    request.setResourceId(resourceId);
                    request.setTimestamp(Instant.now());
                    
                    AccessResult result = accessControlService.processAccess(request);
                    
                    long requestEndTime = System.currentTimeMillis();
                    long processingTime = requestEndTime - requestStartTime;
                    
                    processingTimes.add(processingTime);
                    successCount.incrementAndGet();

                    // 记录慢请求
                    if (processingTime > 500) {
                        logger.warn("慢请求检测: 处理时间 {} 毫秒, 徽章: {}, 读卡器: {}, 资源: {}, 结果: {}",
                                processingTime, badgeId, readerId, resourceId, result.getDecision());
                    }

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    logger.error("访问控制请求失败", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有任务完成
        latch.await(5, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();

        // 关闭线程池
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        // 计算性能指标
        long totalTimeMs = endTime - startTime;
        double throughput = (double) successCount.get() / (totalTimeMs / 1000.0); // 请求/秒
        
        // 计算平均、P95、P99处理时间
        List<Long> sortedTimes = new ArrayList<>(processingTimes);
        Collections.sort(sortedTimes);
        
        double avgTime = sortedTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95Time = sortedTimes.isEmpty() ? 0 : sortedTimes.get((int) (sortedTimes.size() * 0.95));
        long p99Time = sortedTimes.isEmpty() ? 0 : sortedTimes.get((int) (sortedTimes.size() * 0.99));

        // 输出性能结果
        logger.info("直接并发访问控制测试完成");
        logger.info("总耗时: {} 毫秒", totalTimeMs);
        logger.info("成功请求: {}, 失败请求: {}", successCount.get(), errorCount.get());
        logger.info("吞吐量: {}/秒", String.format("%.2f", throughput));
        logger.info("平均处理时间: {} 毫秒", String.format("%.2f", avgTime));
        logger.info("P95处理时间: {} 毫秒", p95Time);
        logger.info("P99处理时间: {} 毫秒", p99Time);

        // 验证性能要求
        assertTrue(successCount.get() > 0, "应至少成功处理一些请求");
        assertTrue(errorCount.get() < successCount.get() * 0.1, "错误率应低于10%");
        assertTrue(avgTime < 500, "平均处理时间应小于500毫秒");
        assertTrue(throughput > 10, "吞吐量应大于10请求/秒");

        logger.info("直接并发访问控制测试通过");
    }

    /**
     * 测试3：缓存性能测试 - 验证缓存命中率和响应时间
     */
    @Test
    void cachePerformance_shouldProvideFastResponseTimes() {
        logger.info("开始缓存性能测试...");

        int iterations = 1000;
        List<Long> cacheTimes = new ArrayList<>();
        List<Long> dbTimes = new ArrayList<>();

        Random random = new Random();

        // 测试缓存访问性能
        for (int i = 0; i < iterations; i++) {
            String badgeId = testBadges.get(random.nextInt(testBadges.size())).getBadgeId();
            
            // 缓存访问
            long cacheStart = System.nanoTime();
            Badge cachedBadge = cacheManager.getBadge(badgeId);
            long cacheEnd = System.nanoTime();
            cacheTimes.add(cacheEnd - cacheStart);

            // 数据库访问（直接通过Repository）
            long dbStart = System.nanoTime();
            Badge dbBadge = badgeRepository.findById(badgeId).orElse(null);
            long dbEnd = System.nanoTime();
            dbTimes.add(dbEnd - dbStart);

            assertNotNull(cachedBadge);
            assertNotNull(dbBadge);
            assertEquals(cachedBadge.getBadgeId(), dbBadge.getBadgeId());
        }

        // 计算统计信息
        double avgCacheTimeNs = cacheTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgDbTimeNs = dbTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        
        double cacheSpeedup = avgDbTimeNs / avgCacheTimeNs;

        logger.info("缓存性能测试完成");
        logger.info("平均缓存访问时间: {} 纳秒", String.format("%.2f", avgCacheTimeNs));
        logger.info("平均数据库访问时间: {} 纳秒", String.format("%.2f", avgDbTimeNs));
        logger.info("缓存加速比: {} 倍", String.format("%.2f", cacheSpeedup));

        // 验证缓存性能优势
        assertTrue(avgCacheTimeNs < avgDbTimeNs, "缓存访问时间应小于数据库访问时间");
        assertTrue(cacheSpeedup > 5, "缓存应提供至少5倍的性能加速");

        logger.info("缓存性能测试通过");
    }

    /**
     * 测试4：系统资源监控 - 验证高并发下的内存和线程使用情况
     */
    @Test
    void systemResources_shouldRemainStableUnderLoad() throws InterruptedException {
        logger.info("开始系统资源监控测试...");

        // 记录初始资源状态
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        int initialThreadCount = Thread.activeCount();

        // 执行并发测试
        eventSimulator.startSimulation(500, 50);

        // 等待模拟进行一段时间
        Thread.sleep(5000);

        // 记录运行中资源状态
        long runningMemory = runtime.totalMemory() - runtime.freeMemory();
        int runningThreadCount = Thread.activeCount();

        // 停止模拟
        eventSimulator.stopSimulation();
        Thread.sleep(1000); // 等待清理

        // 记录最终资源状态
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        int finalThreadCount = Thread.activeCount();

        // 输出资源使用情况
        logger.info("系统资源监控结果:");
        logger.info("内存使用 - 初始: {} MB, 运行中: {} MB, 最终: {} MB",
                initialMemory / (1024 * 1024),
                runningMemory / (1024 * 1024),
                finalMemory / (1024 * 1024));
        logger.info("线程数 - 初始: {}, 运行中: {}, 最终: {}",
                initialThreadCount, runningThreadCount, finalThreadCount);

        // 验证资源稳定性
        long memoryIncrease = runningMemory - initialMemory;
        assertTrue(memoryIncrease < 200 * 1024 * 1024, "内存增加应小于200MB");
        assertTrue(finalThreadCount <= initialThreadCount + 10, "线程数应基本恢复到初始水平");

        logger.info("系统资源监控测试通过");
    }
}