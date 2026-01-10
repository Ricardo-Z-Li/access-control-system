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
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] 开始每日徽章更新检查...");
        
        long startTime = System.currentTimeMillis();
        var badgesNeedingUpdate = badgeCodeUpdateService.checkAllBadgesForUpdate();
        long endTime = System.currentTimeMillis();
        
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] 每日徽章更新检查完成，耗时 " + (endTime - startTime) + " ms");
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] 需要更新的徽章数量: " + badgesNeedingUpdate.size());
        
        if (!badgesNeedingUpdate.isEmpty()) {
            System.out.println("[" + LocalDateTime.now().format(formatter) + "] 需要更新的徽章ID: " + badgesNeedingUpdate);
        }
        
        // 打印统计信息
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] " + badgeCodeUpdateService.getUpdateStats());
    }

    /**
     * 每小时检查一次即将过期的徽章（用于实时通知）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyUpdateNotificationCheck() {
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] 每小时徽章更新通知检查...");
        
        // 这里可以添加更精细的通知逻辑
        // 目前依赖checkAllBadgesForUpdate中的通知机制
        badgeCodeUpdateService.checkAllBadgesForUpdate();
    }

    /**
     * 每30分钟检查一次更新窗口过期情况（更频繁的监控）
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void monitorUpdateWindow() {
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] 检查更新窗口过期情况...");
        
        // 依赖checkAllBadgesForUpdate中的过期禁用逻辑
        badgeCodeUpdateService.checkAllBadgesForUpdate();
    }
}