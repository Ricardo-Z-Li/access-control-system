package acs.domain;

/**
 * Badge 当前状态
 */
public enum BadgeStatus {

    /** 正常可用 */
    ACTIVE,

    /** 被管理员禁用 */
    DISABLED,

    /** 丢失或挂失 */
    LOST
}
