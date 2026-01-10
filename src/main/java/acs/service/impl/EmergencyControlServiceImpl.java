package acs.service.impl;

import acs.cache.LocalCacheManager;
import acs.domain.Resource;
import acs.domain.ResourceType;
import acs.repository.GroupRepository;
import acs.repository.ResourceRepository;
import acs.service.EmergencyControlService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmergencyControlServiceImpl implements EmergencyControlService {

    private final ResourceRepository resourceRepository;
    private final GroupRepository groupRepository;
    private final LocalCacheManager cacheManager;

    public EmergencyControlServiceImpl(ResourceRepository resourceRepository,
                                      GroupRepository groupRepository,
                                      LocalCacheManager cacheManager) {
        this.resourceRepository = resourceRepository;
        this.groupRepository = groupRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    public void setAllDoorsUncontrolled() {
        // 查找所有类型为DOOR的资源
        List<Resource> doors = resourceRepository.findByResourceType(ResourceType.DOOR);
        
        for (Resource door : doors) {
            door.setIsControlled(false);
            resourceRepository.save(door);
            cacheManager.updateResource(door);
        }
        
        System.out.println("紧急控制: 已将 " + doors.size() + " 个门设置为非受控状态");
    }

    @Override
    @Transactional
    public void setResourcesControlledByType(ResourceType resourceType, boolean controlled) {
        List<Resource> resources = resourceRepository.findByResourceType(resourceType);
        
        for (Resource resource : resources) {
            resource.setIsControlled(controlled);
            resourceRepository.save(resource);
            cacheManager.updateResource(resource);
        }
        
        System.out.println("紧急控制: 已将 " + resources.size() + " 个类型为 " + resourceType + " 的资源设置为 " + 
                         (controlled ? "受控" : "非受控") + " 状态");
    }

    @Override
    @Transactional
    public void setResourcesControlled(List<String> resourceIds, boolean controlled) {
        for (String resourceId : resourceIds) {
            Resource resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("资源不存在: " + resourceId));
            resource.setIsControlled(controlled);
            resourceRepository.save(resource);
            cacheManager.updateResource(resource);
        }
        
        System.out.println("紧急控制: 已将 " + resourceIds.size() + " 个资源设置为 " + 
                         (controlled ? "受控" : "非受控") + " 状态");
    }

    @Override
    @Transactional
    public void setGroupResourcesControlled(String groupId, boolean controlled) {
        acs.domain.Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("组不存在: " + groupId));
        
        List<Resource> groupResources = group.getResources().stream()
                .collect(Collectors.toList());
        
        for (Resource resource : groupResources) {
            resource.setIsControlled(controlled);
            resourceRepository.save(resource);
            cacheManager.updateResource(resource);
        }
        
        System.out.println("紧急控制: 已将组 '" + groupId + "' 的 " + groupResources.size() + 
                         " 个资源设置为 " + (controlled ? "受控" : "非受控") + " 状态");
    }

    @Override
    @Transactional
    public void restoreAllToControlled() {
        List<Resource> uncontrolledResources = resourceRepository.findByIsControlled(false);
        
        for (Resource resource : uncontrolledResources) {
            resource.setIsControlled(true);
            resourceRepository.save(resource);
            cacheManager.updateResource(resource);
        }
        
        System.out.println("紧急控制: 已将 " + uncontrolledResources.size() + " 个资源恢复为受控状态");
    }
}