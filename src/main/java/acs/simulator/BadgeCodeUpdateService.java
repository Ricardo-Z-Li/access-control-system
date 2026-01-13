package acs.simulator;

import acs.domain.Badge;
import acs.domain.BadgeUpdateStatus;

import java.time.Instant;
import java.util.List;

/**
 * 徽章代码更新服务，模拟徽章更新工作流程。
 * 负责检查徽章过期状态、生成新徽章代码、触发更新通知。
 */
public interface BadgeCodeUpdateService {

    /**
     * 检查徽章是否需要更新（基于过期日期）
     * @param badgeId 徽章ID
     * @return 如果需要更新返回true
     */
    boolean checkBadgeNeedsUpdate(String badgeId);

    /**
     * Get badge update status for access decision.
     * @param badgeId badge id
     * @param timestamp reference time
     * @return update status
     */
    BadgeUpdateStatus evaluateBadgeUpdateStatus(String badgeId, Instant timestamp);

    /**
     * 为徽章生成新的徽章代码
     * @param badgeId 徽章ID
     * @return 新的徽章代码，如果更新失败返回null
     */
    String generateNewBadgeCode(String badgeId);

    /**
     * 执行徽章更新流程
     * @param badgeId 徽章ID
     * @return 更新后的徽章对象，如果更新失败返回null
     */
    Badge updateBadgeCode(String badgeId);

    /**
     * 批量检查所有徽章的更新状态
     * @return 需要更新的徽章ID列表
     */
    List<String> checkAllBadgesForUpdate();

    /**
     * 模拟徽章更新通知（如发送邮件或显示提示）
     * @param badgeId 徽章ID
     * @param daysUntilExpiry 距离过期的天数（负值表示已过期）
     */
    void simulateUpdateNotification(String badgeId, int daysUntilExpiry);

    /**
     * 获取徽章更新统计信息
     * @return 统计信息字符串
     */
    String getUpdateStats();
}
