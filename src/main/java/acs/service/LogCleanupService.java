package acs.service;

import acs.cache.LocalCacheManager;
import acs.repository.AccessLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LogCleanupService {

    private final AccessLogRepository accessLogRepository;
    private final LocalCacheManager cacheManager;

    // 注入依赖
    public LogCleanupService(AccessLogRepository accessLogRepository, LocalCacheManager cacheManager) {
        this.accessLogRepository = accessLogRepository;
        this.cacheManager = cacheManager;
    }
    @Scheduled(cron = "0 0 0 */7 * ?")
    @Transactional
    public void cleanExpiredLogs() {
        // 计算7天前的时间（超过7天的日志将被清理）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 1. 清理数据库中的过期日志
        long deletedDbCount = accessLogRepository.deleteByTimestampBefore(sevenDaysAgo);
        System.out.println("清理数据库日志数量：" + deletedDbCount);

        // 2. 清理缓存中的过期日志
        int deletedCacheCount = cacheManager.clearExpiredLogs(sevenDaysAgo);
        System.out.println("清理缓存日志数量：" + deletedCacheCount);
    }
}