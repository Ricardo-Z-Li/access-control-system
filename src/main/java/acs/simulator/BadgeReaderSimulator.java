package acs.simulator;

import acs.domain.AccessResult;

/**
 * 读卡器模拟器接口，模拟物理读卡器的行为：
 * 1. 读取徽章代码
 * 2. 发送访问请求
 * 3. 根据响应控制资源
 * 4. 模拟读卡器状态和网络延迟
 */
public interface BadgeReaderSimulator {

    /**
     * 模拟在读卡器上刷卡的操作
     * @param readerId 读卡器ID
     * @param badgeId 徽章ID
     * @return 访问结果
     * @throws InterruptedException 如果模拟过程中线程被中断
     */
    AccessResult simulateBadgeSwipe(String readerId, String badgeId) throws InterruptedException;
    
    /**
     * 模拟在读卡器上刷卡的操作（带事件ID，用于执行链跟踪）
     * @param readerId 读卡器ID
     * @param badgeId 徽章ID
     * @param eventId 事件ID（可为null）
     * @return 访问结果
     * @throws InterruptedException 如果模拟过程中线程被中断
     */
    AccessResult simulateBadgeSwipe(String readerId, String badgeId, String eventId) throws InterruptedException;

    /**
     * 模拟读卡器读取徽章代码（包含读取延迟）
     * @param readerId 读卡器ID
     * @param badgeId 徽章ID
     * @return 读取到的徽章代码，如果读取失败返回null
     * @throws InterruptedException 如果线程在等待延迟时被中断
     */
    String readBadgeCode(String readerId, String badgeId) throws InterruptedException;

    /**
     * 更新读卡器状态（如在线、离线、维护）
     * @param readerId 读卡器ID
     * @param status 新状态
     */
    void updateReaderStatus(String readerId, String status);

    /**
     * 获取读卡器模拟统计信息
     * @param readerId 读卡器ID
     * @return 统计信息字符串
     */
    String getSimulationStats(String readerId);
    
    /**
     * 获取最后一次读取徽章的状态信息
     * @return 状态信息字符串，如果没有读取记录返回null
     */
    String getLastReadStatus();
}