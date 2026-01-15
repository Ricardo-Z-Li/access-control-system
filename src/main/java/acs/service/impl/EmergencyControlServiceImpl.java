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
        
        System.out.println("Emergency control: set " + doors.size() + " doors to uncontrolled state");
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
        
        System.out.println("Emergency control: set " + resources.size() + " resources of type " + resourceType
            + " to " + (controlled ? "controlled" : "uncontrolled"));
    }

    @Override
    @Transactional
    public void setResourcesControlled(List<String> resourceIds, boolean controlled) {
        for (String resourceId : resourceIds) {
            Resource resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceId));
            resource.setIsControlled(controlled);
            resourceRepository.save(resource);
            cacheManager.updateResource(resource);
        }
        
        System.out.println("Emergency control: set " + resourceIds.size() + " resources to "
            + (controlled ? "controlled" : "uncontrolled"));
    }

    @Override
    @Transactional
    public void setGroupResourcesControlled(String groupId, boolean controlled) {
        acs.domain.Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        
        List<Resource> groupResources = group.getResources().stream()
                .collect(Collectors.toList());
        
        for (Resource resource : groupResources) {
            resource.setIsControlled(controlled);
            resourceRepository.save(resource);
            cacheManager.updateResource(resource);
        }
        
        System.out.println("Emergency control: set group '" + groupId + "' resources (" + groupResources.size()
            + ") to " + (controlled ? "controlled" : "uncontrolled"));
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
        
        System.out.println("Emergency control: restored " + uncontrolledResources.size() + " resources to controlled state");
    }
}
