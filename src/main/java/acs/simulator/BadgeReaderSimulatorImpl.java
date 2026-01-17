package acs.simulator;

import acs.domain.*;
import acs.repository.BadgeReaderRepository;
import acs.repository.BadgeRepository;
import acs.service.AccessControlService;
import acs.service.ClockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 读卡器模拟器实现，模拟物理读卡器的完整工作流程：
 * 1. 徽章代码读取（模拟读取延迟）
 * 2. 访问请求发送（模拟网络延迟）
 * 3. 访问控制处理（调用真实服务）
 * 4. 资源控制执行（模拟操作延迟）
 */
@Service
public class BadgeReaderSimulatorImpl implements BadgeReaderSimulator {

    private final AccessControlService accessControlService;
    private final RouterSystem routerSystem;
    private final ResourceController resourceController;
    private final BadgeReaderRepository badgeReaderRepository;
    private final BadgeRepository badgeRepository;
    private final ClockService clockService;
    
    // 模拟延迟配置（毫秒）
    private static final long BADGE_READ_DELAY_MS = 200;      // 读卡延迟200ms
    private static final long NETWORK_DELAY_MS = 50;         // 网络延迟50ms
    private static final long PROCESSING_DELAY_MS = 100;     // 处理延迟100ms
    
    // 读卡器统计信息
    private final ConcurrentHashMap<String, ReaderStats> readerStats = new ConcurrentHashMap<>();
    
    // 最后读取状态（ThreadLocal用于线程安全）
    private final ThreadLocal<String> lastReadStatus = ThreadLocal.withInitial(() -> null);
    
    @Autowired
    public BadgeReaderSimulatorImpl(AccessControlService accessControlService,
                                    RouterSystem routerSystem,
                                    ResourceController resourceController,
                                    BadgeReaderRepository badgeReaderRepository,
                                    BadgeRepository badgeRepository,
                                    ClockService clockService) {
        this.accessControlService = accessControlService;
        this.routerSystem = routerSystem;
        this.resourceController = resourceController;
        this.badgeReaderRepository = badgeReaderRepository;
        this.badgeRepository = badgeRepository;
        this.clockService = clockService;
    }

    @Override
    public AccessResult simulateBadgeSwipe(String readerId, String badgeId) throws InterruptedException {
        return simulateBadgeSwipe(readerId, badgeId, null);
    }
    
    @Override
    public AccessResult simulateBadgeSwipe(String readerId, String badgeId, String eventId) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        ReaderStats stats = readerStats.computeIfAbsent(readerId, id -> new ReaderStats());
        stats.incrementTotalSwipes();
        
        // 生成事件ID（如果未提供）
        String actualEventId = eventId != null ? eventId : "EVENT_" + System.currentTimeMillis();
        
        // 开始执行链跟踪
        ExecutionChainTracker tracker = ExecutionChainTracker.getInstance();
        String chainId = "CHAIN_" + actualEventId;
        tracker.startChain(actualEventId, readerId, badgeId);
        
        // 1. 模拟读卡器读取徽章代码
        tracker.addStep(chainId, ExecutionChainTracker.StepType.BADGE_READ_START, 
                actualEventId, readerId, badgeId, null, null, "Start reading badge code");
        String badgeCode = readBadgeCode(readerId, badgeId);
        if (badgeCode == null) {
            stats.incrementFailedReads();
            tracker.addStep(chainId, ExecutionChainTracker.StepType.BADGE_READ_COMPLETE,
                    actualEventId, readerId, badgeId, null, null, "Badge read failed");
            return createErrorResult("Unable to read badge code");
        }
        tracker.addStep(chainId, ExecutionChainTracker.StepType.BADGE_READ_COMPLETE,
                actualEventId, readerId, badgeId, null, null, "Badge code: " + badgeCode);
        
        // 2. 模拟网络延迟（发送请求）
        Thread.sleep(NETWORK_DELAY_MS);
        tracker.addStep(chainId, ExecutionChainTracker.StepType.REQUEST_TO_ROUTER,
                actualEventId, readerId, badgeId, null, null, "Request sent to router");
        
        // 3. 获取读卡器关联的资源ID
        String resourceId = getResourceForReader(readerId);
        if (resourceId == null) {
            stats.incrementFailedRequests();
            tracker.addStep(chainId, ExecutionChainTracker.StepType.CHAIN_COMPLETE,
                    actualEventId, readerId, badgeId, null, null, "Reader has no resource mapped, chain stopped");
            return createErrorResult("Reader has no resource mapped");
        }
        
        // 4. 创建访问请求
        AccessRequest request = new AccessRequest(badgeId, resourceId, clockService.now());
        
        // 5. 模拟处理延迟
        Thread.sleep(PROCESSING_DELAY_MS);
        
        // 6. 调用访问控制服务（通过路由系统）
        tracker.addStep(chainId, ExecutionChainTracker.StepType.ROUTER_FORWARD_REQUEST,
                actualEventId, readerId, badgeId, resourceId, null, "Router forwarded request to access control");
        AccessResult result = routerSystem.routeRequest(request, actualEventId, chainId, 
                readerId, badgeId, resourceId);
        
        // 读卡器接收响应
        tracker.addStep(chainId, ExecutionChainTracker.StepType.READER_RECEIVE_RESPONSE,
                actualEventId, readerId, badgeId, resourceId, null, 
                "Response received: " + result.getDecision() + ", Reason: " + result.getReasonCode());
        
        // 7. 根据访问结果控制资源
        if (result.getDecision() == AccessDecision.ALLOW) {
            stats.incrementGrants();
            try {
                tracker.addStep(chainId, ExecutionChainTracker.StepType.RESOURCE_CONTROL_START,
                        actualEventId, readerId, badgeId, resourceId, null, "Start unlocking resource");
                resourceController.unlockResource(resourceId);
                tracker.addStep(chainId, ExecutionChainTracker.StepType.RESOURCE_CONTROL_COMPLETE,
                        actualEventId, readerId, badgeId, resourceId, null, "Resource unlocked");
                // 模拟门自动重新锁定（在后台线程中）
                scheduleAutoLock(resourceId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stats.incrementResourceControlFailures();
                tracker.addStep(chainId, ExecutionChainTracker.StepType.RESOURCE_CONTROL_COMPLETE,
                        actualEventId, readerId, badgeId, resourceId, null, "Resource control interrupted");
            }
        } else {
            stats.incrementDenials();
            tracker.addStep(chainId, ExecutionChainTracker.StepType.RESOURCE_CONTROL_COMPLETE,
                    actualEventId, readerId, badgeId, resourceId, null, "Access denied, no resource control");
        }
        
        // 8. 更新读卡器最后通信时间
        updateReaderLastSeen(readerId);
        
        long endTime = System.currentTimeMillis();
        stats.recordProcessingTime(endTime - startTime);
        
        // 标记执行链完成
        tracker.addStep(chainId, ExecutionChainTracker.StepType.CHAIN_COMPLETE,
                actualEventId, readerId, badgeId, resourceId, null, 
                "Chain complete, total time: " + (endTime - startTime) + "ms");
        
        return result;
    }

    @Override
    public String readBadgeCode(String readerId, String badgeId) throws InterruptedException {
        // 模拟读卡延迟
        Thread.sleep(BADGE_READ_DELAY_MS);
        
        // 从数据库查询徽章
        Optional<Badge> badgeOpt = badgeRepository.findById(badgeId);
        if (badgeOpt.isEmpty()) {
            String status = "Badge not found: " + badgeId;
            System.out.println(status);
            lastReadStatus.set(status);
            return null;
        }
        
        Badge badge = badgeOpt.get();
        String badgeCode = badge.getBadgeCode();
        StringBuilder statusBuilder = new StringBuilder();
        
        // 检查徽章状态
        if (badge.getStatus() != BadgeStatus.ACTIVE) {
            String status = "Badge is inactive (disabled or reported lost): " + badgeId;
            System.out.println(status);
            statusBuilder.append(status);
            lastReadStatus.set(statusBuilder.toString());
            return badgeCode;
        }
        
        // 检查徽章过期日期
        if (badge.getExpirationDate() != null && badge.getExpirationDate().isBefore(java.time.LocalDate.now())) {
            String status = "Badge expired: " + badgeId + ", expiration date: " + badge.getExpirationDate();
            System.out.println(status);
            statusBuilder.append(status);
            lastReadStatus.set(statusBuilder.toString());
            return badgeCode;
        }
        
        // 检查徽章代码更新状态
        if (badge.getCodeExpirationDate() != null && badge.getCodeExpirationDate().isBefore(java.time.LocalDate.now())) {
            String status = "Badge code expired: " + badgeId + ", code expiration date: " + badge.getCodeExpirationDate();
            System.out.println(status);
            statusBuilder.append(status);
            lastReadStatus.set(statusBuilder.toString());
            return badgeCode;
        }
        
        if (badge.isNeedsUpdate()) {
            String status = "Badge update required: " + badgeId;
            System.out.println(status);
            statusBuilder.append(status);
            lastReadStatus.set(statusBuilder.toString());
            return badgeCode;
        }
        
        // 徽章代码有效，不需要更新
        String status = "Status: No update needed. Note: Current badge code is valid";
        System.out.println(status);
        lastReadStatus.set(status);
        return badgeCode;
    }

    @Override
    public void updateReaderStatus(String readerId, String status) {
        badgeReaderRepository.findByReaderId(readerId).ifPresent(reader -> {
            reader.setStatus(status);
            reader.setLastSeen(clockService.now());
            badgeReaderRepository.save(reader);
        });
    }

    @Override
    public String getSimulationStats(String readerId) {
        ReaderStats stats = readerStats.get(readerId);
        if (stats == null) {
            return "Reader " + readerId + " has no stats";
        }
        
        return String.format("Reader %s stats: total swipes=%d, grants=%d, denials=%d, avg processing=%.2fms",
                readerId, stats.getTotalSwipes(), stats.getGrants(), stats.getDenials(),
                stats.getAverageProcessingTime());
    }
    
    private String getResourceForReader(String readerId) {
        return badgeReaderRepository.findByReaderId(readerId)
                .map(BadgeReader::getResourceId)
                .orElse(null);
    }
    
    private void updateReaderLastSeen(String readerId) {
        badgeReaderRepository.findByReaderId(readerId).ifPresent(reader -> {
            reader.setLastSeen(clockService.now());
            badgeReaderRepository.save(reader);
        });
    }
    
    private AccessResult createErrorResult(String message) {
        return new AccessResult(AccessDecision.DENY, ReasonCode.SYSTEM_ERROR, message);
    }
    
    private void scheduleAutoLock(String resourceId) {
        new Thread(() -> {
            try {
                // 模拟门保持解锁状态5秒后自动锁定
                Thread.sleep(5000);
                resourceController.lockResource(resourceId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 读卡器统计信息内部类
     */
    private static class ReaderStats {
        private final AtomicInteger totalSwipes = new AtomicInteger(0);
        private final AtomicInteger grants = new AtomicInteger(0);
        private final AtomicInteger denials = new AtomicInteger(0);
        private final AtomicInteger failedReads = new AtomicInteger(0);
        private final AtomicInteger failedRequests = new AtomicInteger(0);
        private final AtomicInteger resourceControlFailures = new AtomicInteger(0);
        private final AtomicInteger totalProcessingTime = new AtomicInteger(0);
        
        public void incrementTotalSwipes() { totalSwipes.incrementAndGet(); }
        public void incrementGrants() { grants.incrementAndGet(); }
        public void incrementDenials() { denials.incrementAndGet(); }
        public void incrementFailedReads() { failedReads.incrementAndGet(); }
        public void incrementFailedRequests() { failedRequests.incrementAndGet(); }
        public void incrementResourceControlFailures() { resourceControlFailures.incrementAndGet(); }
        
        public void recordProcessingTime(long timeMs) {
            totalProcessingTime.addAndGet((int) timeMs);
        }
        
        public int getTotalSwipes() { return totalSwipes.get(); }
        public int getGrants() { return grants.get(); }
        public int getDenials() { return denials.get(); }
        public double getAverageProcessingTime() {
            int total = totalSwipes.get();
            return total > 0 ? (double) totalProcessingTime.get() / total : 0.0;
        }
    }
    
    @Override
    public String getLastReadStatus() {
        String status = lastReadStatus.get();
        lastReadStatus.remove(); // 清除当前线程的状态
        return status;
    }
}
