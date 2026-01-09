package acs.domain;

/**
 * 访问决策结果
 */
public enum AccessDecision {

    /** 初始化非空类型 */
    PENDING,

    /** 允许访问 */
    ALLOW,

    /** 拒绝访问 */
    DENY
}
