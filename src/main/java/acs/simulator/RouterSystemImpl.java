package acs.simulator;

import acs.domain.AccessRequest;
import acs.domain.AccessResult;
import acs.service.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 路由系统实现，模拟分布式事件处理中的请求路由机制。
 * 支持请求分发、负载均衡和故障恢复。
 */
@Service
public class RouterSystemImpl implements RouterSystem {

    private final AccessControlService accessControlService;
    
    // 服务节点管理
    private final List<String> nodeIds = new ArrayList<>();
    private final Set<String> failedNodes = ConcurrentHashMap.newKeySet();
    private final Map<String, NodeStats> nodeStats = new ConcurrentHashMap<>();
    
    // 负载均衡
    private String loadBalanceStrategy = "ROUND_ROBIN";
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    
    // 统计信息
    private final LoadBalanceStats loadBalanceStats = new LoadBalanceStats();
    
    // 配置参数
    private static final int NETWORK_DELAY_MS = 10; // 基础网络延迟10ms
    private static final int MAX_RETRY_ATTEMPTS = 3; // 最大重试次数
    
    @Autowired
    public RouterSystemImpl(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
        
        // 初始化模拟节点（在实际系统中可能从配置读取）
        initializeNodes();
    }

    @Override
    public AccessResult routeRequest(AccessRequest request) {
        return routeRequest(request, null, null, null, null, null);
    }
    
    @Override
    public AccessResult routeRequest(AccessRequest request, String eventId, String chainId, 
                                   String readerId, String badgeId, String resourceId) {
        String selectedNode = selectNode();
        if (selectedNode == null) {
            loadBalanceStats.incrementFailedRequests();
            // 记录执行链步骤
            if (chainId != null && eventId != null) {
                ExecutionChainTracker.getInstance().addStep(chainId, 
                        ExecutionChainTracker.StepType.ROUTER_SELECT_NODE,
                        eventId, readerId, badgeId, resourceId, null, 
                        "No available service nodes");
            }
            return createErrorResult("No available service nodes");
        }
        
        // 记录执行链步骤：路由选择节点
        if (chainId != null && eventId != null) {
            ExecutionChainTracker.getInstance().addStep(chainId, 
                    ExecutionChainTracker.StepType.ROUTER_SELECT_NODE,
                    eventId, readerId, badgeId, resourceId, selectedNode, 
                    "Selected node: " + selectedNode);
        }
        
        int attempt = 0;
        AccessResult result = null;
        
        while (attempt < MAX_RETRY_ATTEMPTS) {
            attempt++;
            selectedNode = selectNode(); // 每次重试可能选择不同节点
            
            if (selectedNode == null) {
                loadBalanceStats.incrementFailedRequests();
                if (chainId != null && eventId != null) {
                    ExecutionChainTracker.getInstance().addStep(chainId, 
                            ExecutionChainTracker.StepType.ROUTER_SELECT_NODE,
                            eventId, readerId, badgeId, resourceId, null, 
                            "No available service nodes during retry");
                }
                return createErrorResult("No available service nodes during retry");
            }
            
            try {
                // 模拟网络延迟
                simulateNetworkDelay("CLIENT", selectedNode);
                
                // 记录请求分发
                loadBalanceStats.incrementRequests(selectedNode);
                nodeStats.computeIfAbsent(selectedNode, id -> new NodeStats()).incrementRequests();
                
                // 记录执行链步骤：路由转发请求
                if (chainId != null && eventId != null) {
                    ExecutionChainTracker.getInstance().addStep(chainId, 
                            ExecutionChainTracker.StepType.ROUTER_FORWARD_REQUEST,
                            eventId, readerId, badgeId, resourceId, selectedNode, 
                            "Forwarded request to node: " + selectedNode + " (attempt: " + attempt + ")");
                }
                
                // 记录执行链步骤：访问控制处理中
                if (chainId != null && eventId != null) {
                    ExecutionChainTracker.getInstance().addStep(chainId, 
                            ExecutionChainTracker.StepType.ACCESS_CONTROL_PROCESSING,
                            eventId, readerId, badgeId, resourceId, selectedNode, 
                            "Processing access request");
                }
                
                // 调用真实服务（所有节点共享同一个服务实例，但模拟分布式环境）
                result = accessControlService.processAccess(request);
                
                // 记录成功
                nodeStats.get(selectedNode).incrementSuccesses();
                
                // 记录执行链步骤：访问控制决策
                if (chainId != null && eventId != null && result != null) {
                    ExecutionChainTracker.getInstance().addStep(chainId, 
                            ExecutionChainTracker.StepType.ACCESS_CONTROL_DECISION,
                            eventId, readerId, badgeId, resourceId, selectedNode, 
                            "Decision: " + result.getDecision() + ", Reason: " + result.getReasonCode());
                    
                    // 记录执行链步骤：路由返回响应
                    ExecutionChainTracker.getInstance().addStep(chainId, 
                            ExecutionChainTracker.StepType.ROUTER_RETURN_RESPONSE,
                            eventId, readerId, badgeId, resourceId, selectedNode, 
                            "Returned response to reader");
                }
                
                // 如果节点之前故障，现在成功，可以考虑恢复
                if (failedNodes.contains(selectedNode) && isResultSuccessful(result)) {
                    recoverNode(selectedNode);
                }
                
                break; // 成功，退出重试循环
                
            } catch (Exception e) {
                // 模拟节点故障
                nodeStats.get(selectedNode).incrementFailures();
                
                // 记录执行链步骤：节点故障
                if (chainId != null && eventId != null) {
                    ExecutionChainTracker.getInstance().addStep(chainId, 
                            ExecutionChainTracker.StepType.ROUTER_FORWARD_REQUEST,
                            eventId, readerId, badgeId, resourceId, selectedNode, 
                            "Node failure: " + e.getMessage());
                }
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    loadBalanceStats.incrementReroutedRequests();
                    markNodeAsFailed(selectedNode);
                    selectedNode = null; // 强制选择新节点
                } else {
                    loadBalanceStats.incrementFailedRequests();
                    if (chainId != null && eventId != null) {
                        ExecutionChainTracker.getInstance().addStep(chainId, 
                                ExecutionChainTracker.StepType.ROUTER_RETURN_RESPONSE,
                                eventId, readerId, badgeId, resourceId, null, 
                                "All retry attempts failed");
                    }
                    return createErrorResult("All retry attempts failed: " + e.getMessage());
                }
            }
        }
        
        return result != null ? result : createErrorResult("Unknown error");
    }

    @Override
    public List<String> getAvailableNodes() {
        List<String> available = new ArrayList<>();
        for (String nodeId : nodeIds) {
            if (!failedNodes.contains(nodeId)) {
                available.add(nodeId);
            }
        }
        return available;
    }

    @Override
    public void markNodeAsFailed(String nodeId) {
        failedNodes.add(nodeId);
        nodeStats.computeIfAbsent(nodeId, id -> new NodeStats()).markAsFailed();
    }

    @Override
    public void recoverNode(String nodeId) {
        failedNodes.remove(nodeId);
        nodeStats.computeIfAbsent(nodeId, id -> new NodeStats()).markAsRecovered();
    }

    @Override
    public LoadBalanceStats getLoadBalanceStats() {
        return loadBalanceStats;
    }

    @Override
    public void setLoadBalanceStrategy(String strategy) {
        if (strategy.equals("ROUND_ROBIN") || strategy.equals("RANDOM") || strategy.equals("LEAST_CONNECTIONS")) {
            this.loadBalanceStrategy = strategy;
        } else {
            throw new IllegalArgumentException("Unsupported load balance strategy: " + strategy);
        }
    }

    @Override
    public void simulateNetworkDelay(String sourceNode, String targetNode) throws InterruptedException {
        // 基础网络延迟 + 随机抖动
        int delay = NETWORK_DELAY_MS + new Random().nextInt(20);
        Thread.sleep(delay);
    }

    @Override
    public SystemHealth getSystemHealth() {
        int totalNodes = nodeIds.size();
        int failedNodesCount = failedNodes.size();
        
        if (failedNodesCount == 0) {
            return SystemHealth.HEALTHY;
        } else if (failedNodesCount < totalNodes / 2) {
            return SystemHealth.DEGRADED;
        } else if (failedNodesCount < totalNodes) {
            return SystemHealth.CRITICAL;
        } else {
            return SystemHealth.FAILED;
        }
    }
    
    private void initializeNodes() {
        // 初始化5个模拟节点
        for (int i = 1; i <= 5; i++) {
            String nodeId = "NODE_" + i;
            nodeIds.add(nodeId);
            nodeStats.put(nodeId, new NodeStats());
        }
    }
    
    private String selectNode() {
        List<String> availableNodes = getAvailableNodes();
        if (availableNodes.isEmpty()) {
            return null;
        }
        
        switch (loadBalanceStrategy) {
            case "ROUND_ROBIN":
                int index = roundRobinIndex.getAndUpdate(i -> (i + 1) % availableNodes.size());
                return availableNodes.get(index);
                
            case "RANDOM":
                return availableNodes.get(new Random().nextInt(availableNodes.size()));
                
            case "LEAST_CONNECTIONS":
                return selectLeastConnectionsNode(availableNodes);
                
            default:
                return availableNodes.get(0);
        }
    }
    
    private String selectLeastConnectionsNode(List<String> availableNodes) {
        String selectedNode = availableNodes.get(0);
        int minConnections = Integer.MAX_VALUE;
        
        for (String nodeId : availableNodes) {
            NodeStats stats = nodeStats.get(nodeId);
            int connections = stats.getActiveConnections();
            if (connections < minConnections) {
                minConnections = connections;
                selectedNode = nodeId;
            }
        }
        
        return selectedNode;
    }
    
    private AccessResult createErrorResult(String message) {
        return new AccessResult(acs.domain.AccessDecision.DENY, acs.domain.ReasonCode.SYSTEM_ERROR, message);
    }
    
    private boolean isResultSuccessful(AccessResult result) {
        return result != null && result.getDecision().toString().equals("ALLOW");
    }
    
    /**
     * 节点统计信息内部类
     */
    private static class NodeStats {
        private final AtomicInteger totalRequests = new AtomicInteger(0);
        private final AtomicInteger successfulRequests = new AtomicInteger(0);
        private final AtomicInteger failedRequests = new AtomicInteger(0);
        private final AtomicInteger activeConnections = new AtomicInteger(0);
        private volatile boolean isFailed = false;
        private volatile long lastFailureTime = 0;
        
        public void incrementRequests() {
            totalRequests.incrementAndGet();
            activeConnections.incrementAndGet();
        }
        
        public void incrementSuccesses() {
            successfulRequests.incrementAndGet();
            activeConnections.decrementAndGet();
        }
        
        public void incrementFailures() {
            failedRequests.incrementAndGet();
            activeConnections.decrementAndGet();
        }
        
        public void markAsFailed() {
            isFailed = true;
            lastFailureTime = System.currentTimeMillis();
        }
        
        public void markAsRecovered() {
            isFailed = false;
        }
        
        public int getTotalRequests() { return totalRequests.get(); }
        public int getSuccessfulRequests() { return successfulRequests.get(); }
        public int getFailedRequests() { return failedRequests.get(); }
        public int getActiveConnections() { return activeConnections.get(); }
        public boolean isFailed() { return isFailed; }
        public long getLastFailureTime() { return lastFailureTime; }
        
        public double getSuccessRate() {
            int total = totalRequests.get();
            return total > 0 ? (double) successfulRequests.get() / total : 0.0;
        }
    }
    
    /**
     * 获取所有节点的详细统计信息
     */
    public Map<String, NodeStats> getAllNodeStats() {
        return new HashMap<>(nodeStats);
    }
    
    /**
     * 重置所有统计信息
     */
    public void resetStats() {
        loadBalanceStats.reset();
        nodeStats.clear();
        failedNodes.clear();
        initializeNodes();
        roundRobinIndex.set(0);
    }
}
