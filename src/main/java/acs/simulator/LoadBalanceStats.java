package acs.simulator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载均衡统计信息
 */
public class LoadBalanceStats {
    
    private final Map<String, AtomicInteger> requestsPerNode = new ConcurrentHashMap<>();
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicInteger reroutedRequests = new AtomicInteger(0);
    
    public void incrementRequests(String nodeId) {
        requestsPerNode.computeIfAbsent(nodeId, id -> new AtomicInteger()).incrementAndGet();
        totalRequests.incrementAndGet();
    }
    
    public void incrementFailedRequests() {
        failedRequests.incrementAndGet();
    }
    
    public void incrementReroutedRequests() {
        reroutedRequests.incrementAndGet();
    }
    
    public int getRequestsForNode(String nodeId) {
        AtomicInteger counter = requestsPerNode.get(nodeId);
        return counter != null ? counter.get() : 0;
    }
    
    public int getTotalRequests() {
        return totalRequests.get();
    }
    
    public int getFailedRequests() {
        return failedRequests.get();
    }
    
    public int getReroutedRequests() {
        return reroutedRequests.get();
    }
    
    public Map<String, Integer> getRequestsDistribution() {
        Map<String, Integer> distribution = new ConcurrentHashMap<>();
        requestsPerNode.forEach((nodeId, counter) -> distribution.put(nodeId, counter.get()));
        return distribution;
    }
    
    public double getFailureRate() {
        int total = totalRequests.get();
        return total > 0 ? (double) failedRequests.get() / total : 0.0;
    }
    
    public void reset() {
        requestsPerNode.clear();
        totalRequests.set(0);
        failedRequests.set(0);
        reroutedRequests.set(0);
    }
}