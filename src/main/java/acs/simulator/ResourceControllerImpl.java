package acs.simulator;

import acs.domain.ResourceState;
import acs.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源控制接口的实现，模拟物理资源的操作延迟和状态管理。
 * 为每个资源维护模拟状态，并模拟开门/关门等操作的延迟。
 */
@Service
public class ResourceControllerImpl implements ResourceController {

    private final ResourceRepository resourceRepository;
    
    // 模拟每个资源的当前状态（可能比数据库状态更实时）
    private final Map<String, ResourceState> simulatedStates = new ConcurrentHashMap<>();
    
    // 操作延迟配置（毫秒）
    private static final long DEFAULT_UNLOCK_DELAY_MS = 1000; // 开门延迟1秒
    private static final long DEFAULT_LOCK_DELAY_MS = 500;    // 关门延迟0.5秒
    
    @Autowired
    public ResourceControllerImpl(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    public void lockResource(String resourceId) throws InterruptedException {
        simulateOperationDelay(resourceId, "LOCK");
        updateResourceState(resourceId, ResourceState.LOCKED);
    }

    @Override
    public void unlockResource(String resourceId) throws InterruptedException {
        simulateOperationDelay(resourceId, "UNLOCK");
        updateResourceState(resourceId, ResourceState.AVAILABLE);
    }

    @Override
    public ResourceState getResourceState(String resourceId) {
        // 优先返回模拟状态，如果不存在则查询数据库
        return simulatedStates.getOrDefault(resourceId, 
                resourceRepository.findById(resourceId)
                        .map(resource -> resource.getResourceState())
                        .orElse(ResourceState.OFFLINE));
    }

    @Override
    public void simulateOperationDelay(String resourceId, String operation) throws InterruptedException {
        long delay = "LOCK".equals(operation) ? DEFAULT_LOCK_DELAY_MS : DEFAULT_UNLOCK_DELAY_MS;
        
        // 模拟操作延迟
        Thread.sleep(delay);
        
        // 可以在这里添加日志记录，但为了性能，模拟器中可能不记录
    }

    /**
     * 更新资源状态（同时更新模拟状态和数据库状态）
     */
    private void updateResourceState(String resourceId, ResourceState newState) {
        simulatedStates.put(resourceId, newState);
        
        // 更新数据库状态
        resourceRepository.findById(resourceId).ifPresent(resource -> {
            resource.setResourceState(newState);
            resourceRepository.save(resource);
        });
    }
    
    /**
     * 重置模拟状态（用于测试或重新开始模拟）
     */
    public void resetSimulatedStates() {
        simulatedStates.clear();
    }
    
    /**
     * 获取模拟状态映射（用于监控）
     */
    public Map<String, ResourceState> getSimulatedStates() {
        return new HashMap<>(simulatedStates);
    }
}