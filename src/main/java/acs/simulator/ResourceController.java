package acs.simulator;

import acs.domain.ResourceState;

/**
 * 资源控制接口，模拟物理资源（如门锁）的控制操作。
 * 负责执行资源状态变更，并模拟操作延迟（如开门时间、关门时间）。
 */
public interface ResourceController {

    /**
     * 锁定资源（如关门上锁）
     * @param resourceId 资源ID
     * @throws InterruptedException 如果线程在等待延迟时被中断
     */
    void lockResource(String resourceId) throws InterruptedException;

    /**
     * 解锁资源（如开门）
     * @param resourceId 资源ID
     * @throws InterruptedException 如果线程在等待延迟时被中断
     */
    void unlockResource(String resourceId) throws InterruptedException;

    /**
     * 获取资源当前状态
     * @param resourceId 资源ID
     * @return 资源状态
     */
    ResourceState getResourceState(String resourceId);

    /**
     * 模拟资源操作延迟（如开门动作所需时间）
     * @param resourceId 资源ID
     * @param operation 操作类型（"LOCK" 或 "UNLOCK"）
     * @throws InterruptedException 如果线程在等待延迟时被中断
     */
    void simulateOperationDelay(String resourceId, String operation) throws InterruptedException;
}