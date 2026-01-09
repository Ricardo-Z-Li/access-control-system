package acs.simulator;

import acs.domain.AccessResult;

/**
 * 模拟事件监听器接口，用于接收模拟过程中的事件通知。
 */
public interface SimulationListener {

    /**
     * 当模拟事件开始时调用
     * @param eventId 事件ID
     * @param readerId 读卡器ID
     * @param badgeId 徽章ID
     */
    void onSimulationEventStarted(String eventId, String readerId, String badgeId);

    /**
     * 当模拟事件完成时调用
     * @param eventId 事件ID
     * @param result 访问结果
     * @param processingTimeMs 处理时间（毫秒）
     */
    void onSimulationEventCompleted(String eventId, AccessResult result, long processingTimeMs);

    /**
     * 当模拟状态发生变化时调用
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    void onSimulationStatusChanged(SimulationStatus oldStatus, SimulationStatus newStatus);

    /**
     * 当模拟过程中发生错误时调用
     * @param eventId 事件ID（如果可用）
     * @param error 错误信息
     */
    void onSimulationError(String eventId, String error);
}