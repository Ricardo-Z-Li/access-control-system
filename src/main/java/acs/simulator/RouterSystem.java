package acs.simulator;

import acs.domain.AccessRequest;
import acs.domain.AccessResult;

import java.util.List;

/**
 * 路由系统接口，模拟分布式事件处理中的请求路由机制。
 * 负责请求分发、负载均衡和故障恢复模拟。
 */
public interface RouterSystem {

    /**
     * 路由访问请求
     * @param request 访问请求
     * @return 访问结果
     */
    AccessResult routeRequest(AccessRequest request);

    /**
     * 获取所有可用服务节点
     * @return 服务节点ID列表
     */
    List<String> getAvailableNodes();

    /**
     * 标记节点为故障
     * @param nodeId 节点ID
     */
    void markNodeAsFailed(String nodeId);

    /**
     * 恢复故障节点
     * @param nodeId 节点ID
     */
    void recoverNode(String nodeId);

    /**
     * 获取负载均衡统计信息
     * @return 统计信息映射
     */
    LoadBalanceStats getLoadBalanceStats();

    /**
     * 设置负载均衡策略
     * @param strategy 策略名称（"ROUND_ROBIN", "RANDOM", "LEAST_CONNECTIONS"）
     */
    void setLoadBalanceStrategy(String strategy);

    /**
     * 模拟网络延迟
     * @param sourceNode 源节点
     * @param targetNode 目标节点
     * @throws InterruptedException 如果线程在等待延迟时被中断
     */
    void simulateNetworkDelay(String sourceNode, String targetNode) throws InterruptedException;

    /**
     * 获取系统健康状态
     * @return 健康状态描述
     */
    SystemHealth getSystemHealth();
}