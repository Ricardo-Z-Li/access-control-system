package acs.simulator;

import acs.domain.Badge;
import acs.repository.BadgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 徽章代码更新服务实现，模拟徽章更新工作流程。
 * 包含过期检查、新代码生成、更新通知等功能。
 */
@Service
public class BadgeCodeUpdateServiceImpl implements BadgeCodeUpdateService {

    private final BadgeRepository badgeRepository;
    private final Random random = new Random();
    
    // 更新统计信息
    private final AtomicInteger totalChecks = new AtomicInteger(0);
    private final AtomicInteger updatesTriggered = new AtomicInteger(0);
    private final AtomicInteger notificationsSent = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Integer> updateAttempts = new ConcurrentHashMap<>();
    
    // 配置参数
    private static final int DAYS_BEFORE_EXPIRY_TO_NOTIFY = 30; // 过期前30天开始通知
    private static final int UPDATE_WINDOW_DAYS = 14; // 更新窗口14天
    private static final int BADGE_CODE_LENGTH = 16; // 新徽章代码长度
    
    @Autowired
    public BadgeCodeUpdateServiceImpl(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    @Override
    public boolean checkBadgeNeedsUpdate(String badgeId) {
        totalChecks.incrementAndGet();
        
        return badgeRepository.findById(badgeId)
                .map(badge -> {
                    // 如果徽章已禁用，不需要更新
                    if (badge.getStatus() == acs.domain.BadgeStatus.DISABLED) {
                        return false;
                    }
                    
                    // 确定代码过期日期（优先使用codeExpirationDate）
                    LocalDate codeExpiryDate = badge.getCodeExpirationDate();
                    if (codeExpiryDate == null) {
                        codeExpiryDate = badge.getExpirationDate();
                    }
                    if (codeExpiryDate == null) {
                        return false; // 无过期日期，不需要更新
                    }
                    
                    long daysUntilCodeExpiry = ChronoUnit.DAYS.between(LocalDate.now(), codeExpiryDate);
                    
                    // 检查是否已超过更新截止日期
                    LocalDate updateDue = badge.getUpdateDueDate();
                    if (updateDue != null && LocalDate.now().isAfter(updateDue)) {
                        // 更新窗口已过，禁用徽章
                        badge.setStatus(acs.domain.BadgeStatus.DISABLED);
                        badgeRepository.save(badge);
                        simulateUpdateNotification(badgeId, (int) daysUntilCodeExpiry);
                        return false; // 已禁用，不需要更新
                    }
                    
                    // 如果代码已过期或即将过期（30天内），则需要更新
                    boolean needsUpdate = daysUntilCodeExpiry <= DAYS_BEFORE_EXPIRY_TO_NOTIFY;
                    
                    if (needsUpdate) {
                        // 设置needsUpdate标志
                        badge.setNeedsUpdate(true);
                        // 如果尚未设置更新截止日期，则设置
                        if (updateDue == null) {
                            LocalDate dueDate = LocalDate.now().plusDays(UPDATE_WINDOW_DAYS);
                            badge.setUpdateDueDate(dueDate);
                        }
                        badgeRepository.save(badge);
                        simulateUpdateNotification(badgeId, (int) daysUntilCodeExpiry);
                    } else {
                        // 不需要更新，确保标志为false
                        if (badge.isNeedsUpdate()) {
                            badge.setNeedsUpdate(false);
                            badgeRepository.save(badge);
                        }
                    }
                    
                    return needsUpdate;
                })
                .orElse(false);
    }

    @Override
    public String generateNewBadgeCode(String badgeId) {
        // 生成随机徽章代码（模拟真实系统）
        StringBuilder code = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        
        for (int i = 0; i < BADGE_CODE_LENGTH; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // 添加前缀和后缀以增强唯一性
        return "UPD_" + code.toString() + "_" + System.currentTimeMillis();
    }

    @Override
    public Badge updateBadgeCode(String badgeId) {
        updateAttempts.merge(badgeId, 1, Integer::sum);
        
        return badgeRepository.findById(badgeId)
                .map(badge -> {
                    // 检查是否真的需要更新
                    if (!checkBadgeNeedsUpdate(badgeId)) {
                        return null; // 不需要更新
                    }
                    
                    // 生成新徽章代码
                    String newCode = generateNewBadgeCode(badgeId);
                    
                    // 更新徽章
                    badge.setBadgeCode(newCode);
                    badge.setLastUpdated(java.time.Instant.now());
                    badge.setLastCodeUpdate(java.time.Instant.now());
                    
                    // 延长过期日期（模拟：延长一年）
                    LocalDate newExpiryDate = LocalDate.now().plusYears(1);
                    badge.setExpirationDate(newExpiryDate);
                    // 同时延长代码过期日期
                    badge.setCodeExpirationDate(newExpiryDate);
                    
                    // 重置更新标志和截止日期
                    badge.setNeedsUpdate(false);
                    badge.setUpdateDueDate(null);
                    
                    // 保存更新
                    Badge updatedBadge = badgeRepository.save(badge);
                    updatesTriggered.incrementAndGet();
                    
                    return updatedBadge;
                })
                .orElse(null);
    }

    @Override
    public List<String> checkAllBadgesForUpdate() {
        List<String> badgesNeedingUpdate = new ArrayList<>();
        
        badgeRepository.findAll().forEach(badge -> {
            if (checkBadgeNeedsUpdate(badge.getBadgeId())) {
                badgesNeedingUpdate.add(badge.getBadgeId());
            }
        });
        
        return badgesNeedingUpdate;
    }

    @Override
    public void simulateUpdateNotification(String badgeId, int daysUntilExpiry) {
        // 模拟发送通知（在实际系统中可能是邮件、短信或UI通知）
        String message;
        if (daysUntilExpiry > 0) {
            message = String.format("徽章 %s 将在 %d 天后过期，请及时更新。", badgeId, daysUntilExpiry);
        } else if (daysUntilExpiry == 0) {
            message = String.format("徽章 %s 今天过期，请立即更新。", badgeId);
        } else {
            message = String.format("徽章 %s 已过期 %d 天，请立即更新。", badgeId, -daysUntilExpiry);
        }
        
        // 在实际系统中，这里会调用通知服务
        // 现在只是记录日志
        System.out.println("[徽章更新通知] " + message);
        notificationsSent.incrementAndGet();
    }

    @Override
    public String getUpdateStats() {
        return String.format("徽章更新统计: 总检查次数=%d, 触发更新=%d, 发送通知=%d, 尝试更新徽章数=%d",
                totalChecks.get(), updatesTriggered.get(), notificationsSent.get(), updateAttempts.size());
    }
    
    /**
     * 重置统计信息（用于测试）
     */
    public void resetStats() {
        totalChecks.set(0);
        updatesTriggered.set(0);
        notificationsSent.set(0);
        updateAttempts.clear();
    }
    
    /**
     * 获取特定徽章的更新尝试次数
     */
    public int getUpdateAttempts(String badgeId) {
        return updateAttempts.getOrDefault(badgeId, 0);
    }
}