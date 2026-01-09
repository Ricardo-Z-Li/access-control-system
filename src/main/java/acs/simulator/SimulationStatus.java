package acs.simulator;

/**
 * 模拟器状态枚举
 */
public enum SimulationStatus {
    /** 模拟器已创建但未启动 */
    IDLE,
    
    /** 模拟器正在运行 */
    RUNNING,
    
    /** 模拟器已暂停 */
    PAUSED,
    
    /** 模拟器已停止 */
    STOPPED,
    
    /** 模拟器发生错误 */
    ERROR
}