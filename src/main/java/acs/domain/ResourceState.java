package acs.domain;

/**
 * 资源当前状态
 */
public enum ResourceState {

    /** 初始化时的非空默认值 */
    PENDING,

    /** 可被访问 */
    AVAILABLE,

    /** 正在被使用 */
    OCCUPIED,

    /** 被系统或管理员锁定 */
    LOCKED,

    /** 资源离线 / 不可用 */
    OFFLINE
}
