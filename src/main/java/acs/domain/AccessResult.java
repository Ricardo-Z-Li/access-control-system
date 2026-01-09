package acs.domain;

/**
 * AccessResult 表示一次访问请求的“最终判定结果”
 *
 * AccessControlService.processAccess(...) 的返回值。
 *
 * 设计原则：
 * - UI 不需要知道内部判断细节
 * - UI 只根据结果展示信息
 */
public class AccessResult {

    /**
     * 最终访问决策：
     * ALLOW - 允许访问
     * DENY  - 拒绝访问
     */
    private AccessDecision decision;

    /**
     * 机器可读的拒绝 / 允许原因码
     *
     * 用途：
     * - 日志记录
     * - 统计分析
     * - UI 根据 reasonCode 做不同展示
     */
    private ReasonCode reasonCode;

    /**
     * 人类可读的提示信息
     *
     * 例如：
     * - "Access granted"
     * - "Badge is inactive"
     */
    private String message;

    /**
     * 构造访问结果
     *
     * @param decision   访问决策（ALLOW / DENY）
     * @param reasonCode 原因码（统一枚举）
     * @param message    给用户展示的信息
     */
    public AccessResult(AccessDecision decision,
                        ReasonCode reasonCode,
                        String message) {
        this.decision = decision;
        this.reasonCode = reasonCode;
        this.message = message;
    }

    public AccessDecision getDecision() {
        return decision;
    }

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    public String getMessage() {
        return message;
    }
}
