package acs.simulator;

import acs.domain.AccessResult;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 执行链跟踪器，用于跟踪访问控制请求的完整执行链。
 * 记录从读卡器刷卡到资源控制执行的完整过程。
 */
public class ExecutionChainTracker {
    
    public enum StepType {
        BADGE_READ_START("读卡器开始读取徽章"),
        BADGE_READ_COMPLETE("读卡器读取徽章完成"),
        REQUEST_TO_ROUTER("请求发送到路由系统"),
        ROUTER_SELECT_NODE("路由系统选择节点"),
        ROUTER_FORWARD_REQUEST("路由转发请求到访问控制系统"),
        ACCESS_CONTROL_PROCESSING("访问控制系统处理请求"),
        ACCESS_CONTROL_DECISION("访问控制系统做出决策"),
        ROUTER_RETURN_RESPONSE("路由系统返回响应"),
        READER_RECEIVE_RESPONSE("读卡器接收响应"),
        RESOURCE_CONTROL_START("资源控制开始执行"),
        RESOURCE_CONTROL_COMPLETE("资源控制执行完成"),
        CHAIN_COMPLETE("执行链完成");
        
        private final String description;
        
        StepType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 执行链步骤
     */
    public static class ChainStep {
        private final StepType stepType;
        private final String eventId;
        private final String readerId;
        private final String badgeId;
        private final String resourceId;
        private final String nodeId;
        private final String additionalInfo;
        private final long timestamp;
        
        public ChainStep(StepType stepType, String eventId, String readerId, 
                        String badgeId, String resourceId, String nodeId, 
                        String additionalInfo) {
            this.stepType = stepType;
            this.eventId = eventId;
            this.readerId = readerId;
            this.badgeId = badgeId;
            this.resourceId = resourceId;
            this.nodeId = nodeId;
            this.additionalInfo = additionalInfo;
            this.timestamp = System.currentTimeMillis();
        }
        
        public StepType getStepType() {
            return stepType;
        }
        
        public String getEventId() {
            return eventId;
        }
        
        public String getReaderId() {
            return readerId;
        }
        
        public String getBadgeId() {
            return badgeId;
        }
        
        public String getResourceId() {
            return resourceId;
        }
        
        public String getNodeId() {
            return nodeId;
        }
        
        public String getAdditionalInfo() {
            return additionalInfo;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getFormattedTimestamp() {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), 
                    java.time.ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s | 读卡器: %s | 徽章: %s | 资源: %s | 节点: %s | %s",
                    getFormattedTimestamp(),
                    stepType.getDescription(),
                    readerId != null ? readerId : "N/A",
                    badgeId != null ? badgeId : "N/A",
                    resourceId != null ? resourceId : "N/A",
                    nodeId != null ? nodeId : "N/A",
                    additionalInfo != null ? additionalInfo : "");
        }
    }
    
    /**
     * 执行链
     */
    public static class ExecutionChain {
        private final String chainId;
        private final String eventId;
        private final List<ChainStep> steps = new CopyOnWriteArrayList<>();
        private volatile boolean completed = false;
        
        public ExecutionChain(String chainId, String eventId) {
            this.chainId = chainId;
            this.eventId = eventId;
        }
        
        public String getChainId() {
            return chainId;
        }
        
        public String getEventId() {
            return eventId;
        }
        
        public List<ChainStep> getSteps() {
            return new ArrayList<>(steps);
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public void addStep(ChainStep step) {
            steps.add(step);
        }
        
        public void markCompleted() {
            this.completed = true;
        }
        
        public String getSummary() {
            ChainStep firstStep = steps.isEmpty() ? null : steps.get(0);
            ChainStep lastStep = steps.isEmpty() ? null : steps.get(steps.size() - 1);
            
            return String.format("执行链 %s (事件: %s) - 步骤数: %d, 状态: %s, 开始: %s, 结束: %s",
                    chainId, eventId, steps.size(),
                    completed ? "完成" : "进行中",
                    firstStep != null ? firstStep.getFormattedTimestamp() : "N/A",
                    lastStep != null ? lastStep.getFormattedTimestamp() : "N/A");
        }
    }
    
    // 单例实例
    private static ExecutionChainTracker instance;
    
    private final List<ExecutionChain> chains = new CopyOnWriteArrayList<>();
    private final List<ExecutionChainListener> listeners = new CopyOnWriteArrayList<>();
    
    private ExecutionChainTracker() {}
    
    public static synchronized ExecutionChainTracker getInstance() {
        if (instance == null) {
            instance = new ExecutionChainTracker();
        }
        return instance;
    }
    
    /**
     * 开始新的执行链
     */
    public ExecutionChain startChain(String eventId, String readerId, String badgeId) {
        String chainId = "CHAIN_" + System.currentTimeMillis() + "_" + eventId;
        ExecutionChain chain = new ExecutionChain(chainId, eventId);
        
        ChainStep firstStep = new ChainStep(StepType.BADGE_READ_START, eventId, 
                readerId, badgeId, null, null, "开始执行链");
        chain.addStep(firstStep);
        
        chains.add(chain);
        
        // 通知监听器
        for (ExecutionChainListener listener : listeners) {
            listener.onChainStarted(chain);
        }
        
        return chain;
    }
    
    /**
     * 添加步骤到执行链
     */
    public void addStep(String chainId, StepType stepType, String eventId, 
                       String readerId, String badgeId, String resourceId, 
                       String nodeId, String additionalInfo) {
        for (ExecutionChain chain : chains) {
            if (chain.getChainId().equals(chainId)) {
                ChainStep step = new ChainStep(stepType, eventId, readerId, 
                        badgeId, resourceId, nodeId, additionalInfo);
                chain.addStep(step);
                
                // 通知监听器
                for (ExecutionChainListener listener : listeners) {
                    listener.onStepAdded(chain, step);
                }
                
                // 如果是完成步骤，标记链为完成
                if (stepType == StepType.CHAIN_COMPLETE) {
                    chain.markCompleted();
                    for (ExecutionChainListener listener : listeners) {
                        listener.onChainCompleted(chain);
                    }
                }
                break;
            }
        }
    }
    
    /**
     * 根据事件ID查找执行链
     */
    public ExecutionChain findChainByEventId(String eventId) {
        for (ExecutionChain chain : chains) {
            if (chain.getEventId().equals(eventId)) {
                return chain;
            }
        }
        return null;
    }
    
    /**
     * 获取所有执行链
     */
    public List<ExecutionChain> getAllChains() {
        return new ArrayList<>(chains);
    }
    
    /**
     * 清除所有执行链
     */
    public void clearAllChains() {
        chains.clear();
    }
    
    /**
     * 添加监听器
     */
    public void addListener(ExecutionChainListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除监听器
     */
    public void removeListener(ExecutionChainListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 执行链监听器接口
     */
    public interface ExecutionChainListener {
        void onChainStarted(ExecutionChain chain);
        void onStepAdded(ExecutionChain chain, ChainStep step);
        void onChainCompleted(ExecutionChain chain);
    }
}