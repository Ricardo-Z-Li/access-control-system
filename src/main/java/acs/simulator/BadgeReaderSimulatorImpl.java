package acs.simulator;

import acs.domain.*;
import acs.repository.BadgeReaderRepository;
import acs.service.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    private final ResourceController resourceController;
    private final BadgeReaderRepository badgeReaderRepository;
    
    // 模拟延迟配置（毫秒）
    private static final long BADGE_READ_DELAY_MS = 200;      // 读卡延迟200ms
    private static final long NETWORK_DELAY_MS = 50;         // 网络延迟50ms
    private static final long PROCESSING_DELAY_MS = 100;     // 处理延迟100ms
    
    // 读卡器统计信息
    private final ConcurrentHashMap<String, ReaderStats> readerStats = new ConcurrentHashMap<>();
    
    @Autowired
    public BadgeReaderSimulatorImpl(AccessControlService accessControlService,
                                    ResourceController resourceController,
                                    BadgeReaderRepository badgeReaderRepository) {
        this.accessControlService = accessControlService;
        this.resourceController = resourceController;
        this.badgeReaderRepository = badgeReaderRepository;
    }

    @Override
    public AccessResult simulateBadgeSwipe(String readerId, String badgeId) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        ReaderStats stats = readerStats.computeIfAbsent(readerId, id -> new ReaderStats());
        stats.incrementTotalSwipes();
        
        // 1. 模拟读卡器读取徽章代码
        String badgeCode = readBadgeCode(readerId, badgeId);
        if (badgeCode == null) {
            stats.incrementFailedReads();
            return createErrorResult("无法读取徽章代码");
        }
        
        // 2. 模拟网络延迟（发送请求）
        Thread.sleep(NETWORK_DELAY_MS);
        
        // 3. 获取读卡器关联的资源ID
        String resourceId = getResourceForReader(readerId);
        if (resourceId == null) {
            stats.incrementFailedRequests();
            return createErrorResult("读卡器未关联资源");
        }
        
        // 4. 创建访问请求
        AccessRequest request = new AccessRequest(badgeId, resourceId, Instant.now());
        
        // 5. 模拟处理延迟
        Thread.sleep(PROCESSING_DELAY_MS);
        
        // 6. 调用访问控制服务
        AccessResult result = accessControlService.processAccess(request);
        
        // 7. 根据访问结果控制资源
        if (result.getDecision() == AccessDecision.ALLOW) {
            stats.incrementGrants();
            try {
                resourceController.unlockResource(resourceId);
                // 模拟门自动重新锁定（在后台线程中）
                scheduleAutoLock(resourceId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stats.incrementResourceControlFailures();
            }
        } else {
            stats.incrementDenials();
        }
        
        // 8. 更新读卡器最后通信时间
        updateReaderLastSeen(readerId);
        
        long endTime = System.currentTimeMillis();
        stats.recordProcessingTime(endTime - startTime);
        
        return result;
    }

    @Override
    public String readBadgeCode(String readerId, String badgeId) throws InterruptedException {
        // 模拟读卡延迟
        Thread.sleep(BADGE_READ_DELAY_MS);
        
        // 在实际系统中，这里会从硬件读取徽章代码
        // 我们模拟返回一个合成的徽章代码
        return "SIM_" + badgeId + "_" + System.currentTimeMillis();
    }

    @Override
    public void updateReaderStatus(String readerId, String status) {
        badgeReaderRepository.findByReaderId(readerId).ifPresent(reader -> {
            reader.setStatus(status);
            reader.setLastSeen(Instant.now());
            badgeReaderRepository.save(reader);
        });
    }

    @Override
    public String getSimulationStats(String readerId) {
        ReaderStats stats = readerStats.get(readerId);
        if (stats == null) {
            return "读卡器 " + readerId + " 暂无统计信息";
        }
        
        return String.format("读卡器 %s 统计: 总刷卡次数=%d, 授权=%d, 拒绝=%d, 平均处理时间=%.2fms",
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
            reader.setLastSeen(Instant.now());
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
}