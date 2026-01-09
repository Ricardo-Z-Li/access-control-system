package acs.simulator;

import java.util.Map;

/**
 * 事件模拟器接口，负责生成并发访问事件、模拟时间加速和收集性能指标。
 * 模拟PDF中描述的300徽章/400读卡器并发访问场景。
 */
public interface EventSimulator {

    /**
     * 启动事件模拟
     * @param numEvents 要生成的事件总数
     * @param concurrencyLevel 并发级别（同时活动的读卡器数量）
     */
    void startSimulation(int numEvents, int concurrencyLevel);

    /**
     * 停止事件模拟
     */
    void stopSimulation();

    /**
     * 设置时间加速因子
     * @param factor 加速因子（1.0=实时，10.0=10倍速度）
     */
    void setTimeAcceleration(double factor);

    /**
     * 获取当前性能指标
     * @return 性能指标映射
     */
    Map<String, Object> getPerformanceMetrics();

    /**
     * 获取模拟状态
     * @return 状态字符串
     */
    SimulationStatus getSimulationStatus();

    /**
     * 重置模拟器和所有统计信息
     */
    void resetSimulation();

    /**
     * 添加模拟事件监听器
     * @param listener 监听器
     */
    void addSimulationListener(SimulationListener listener);

    /**
     * 移除模拟事件监听器
     * @param listener 监听器
     */
    void removeSimulationListener(SimulationListener listener);
}