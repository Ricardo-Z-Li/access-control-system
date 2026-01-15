package acs.simulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 徽章更新调度任务，定期检查徽章更新状态并发送通知。
 * 模拟自动更新周期和更新窗口监控。
 */
@Component
public class BadgeUpdateScheduler {

    private final BadgeCodeUpdateService badgeCodeUpdateService;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public BadgeUpdateScheduler(BadgeCodeUpdateService badgeCodeUpdateService) {
        this.badgeCodeUpdateService = badgeCodeUpdateService;
    }

    /**
     * 每天凌晨2点检查所有徽章更新状态
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyBadgeUpdateCheck() {
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] Starting daily badge update check...");
        
        long startTime = System.currentTimeMillis();
        var badgesNeedingUpdate = badgeCodeUpdateService.checkAllBadgesForUpdate();
        long endTime = System.currentTimeMillis();
        
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] Daily badge update check completed, elapsed " + (endTime - startTime) + " ms");
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] Badges needing update: " + badgesNeedingUpdate.size());
        
        if (!badgesNeedingUpdate.isEmpty()) {
            System.out.println("[" + LocalDateTime.now().format(formatter) + "] Badge IDs needing update: " + badgesNeedingUpdate);
        }
        
        // 打印统计信息
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] " + badgeCodeUpdateService.getUpdateStats());
    }

    /**
     * 每小时检查一次即将过期的徽章（用于实时通知）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyUpdateNotificationCheck() {
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] Hourly badge update notification check...");
        
        // 这里可以添加更精细的通知逻辑
        // 目前依赖checkAllBadgesForUpdate中的通知机制
        badgeCodeUpdateService.checkAllBadgesForUpdate();
    }

    /**
     * 每30分钟检查一次更新窗口过期情况（更频繁的监控）
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void monitorUpdateWindow() {
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] Checking update window expiry...");
        
        // 依赖checkAllBadgesForUpdate中的过期禁用逻辑
        badgeCodeUpdateService.checkAllBadgesForUpdate();
    }
}
