package acs.simulator;

/**
 * 系统健康状态枚举
 */
public enum SystemHealth {
    /** 所有节点健康，系统运行正常 */
    HEALTHY,
    
    /** 部分节点故障，但系统仍可运行 */
    DEGRADED,
    
    /** 多数节点故障，系统性能严重下降 */
    CRITICAL,
    
    /** 系统完全不可用 */
    FAILED
}