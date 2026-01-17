package acs.service.impl;

import acs.cache.LocalCacheManager;
import acs.domain.Resource;
import acs.domain.ResourceType;
import acs.domain.ResourceState;
import acs.domain.Group;
import acs.repository.GroupRepository;
import acs.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmergencyControlServiceImplTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private LocalCacheManager cacheManager;

    @InjectMocks
    private EmergencyControlServiceImpl emergencyControlService;

    @Test
    void setAllDoorsUncontrolled_shouldSetAllDoorsToUncontrolled() {
        // 准备测试数据
        Resource door1 = createResource("D001", "Main Door", ResourceType.DOOR, true);
        Resource door2 = createResource("D002", "Side Door", ResourceType.DOOR, true);
        Resource printer = createResource("P001", "Printer", ResourceType.PRINTER, true);
        
        List<Resource> allDoors = Arrays.asList(door1, door2);
        when(resourceRepository.findByResourceType(ResourceType.DOOR)).thenReturn(allDoors);
        
        // 执行测试
        emergencyControlService.setAllDoorsUncontrolled();
        
        // 验证结果
        assertFalse(door1.getIsControlled());
        assertFalse(door2.getIsControlled());
        assertTrue(printer.getIsControlled()); // 打印机不受影响
        
        verify(resourceRepository, times(2)).save(any(Resource.class));
        verify(cacheManager, times(2)).updateResource(any(Resource.class));
        verify(resourceRepository).findByResourceType(ResourceType.DOOR);
    }

    @Test
    void setResourcesControlledByType_shouldSetAllResourcesOfType() {
        ResourceType targetType = ResourceType.PRINTER;
        Resource printer1 = createResource("P001", "Printer 1", ResourceType.PRINTER, true);
        Resource printer2 = createResource("P002", "Printer 2", ResourceType.PRINTER, true);
        List<Resource> printers = Arrays.asList(printer1, printer2);
        
        when(resourceRepository.findByResourceType(targetType)).thenReturn(printers);
        
        emergencyControlService.setResourcesControlledByType(targetType, false);
        
        assertFalse(printer1.getIsControlled());
        assertFalse(printer2.getIsControlled());
        verify(resourceRepository, times(2)).save(any(Resource.class));
        verify(cacheManager, times(2)).updateResource(any(Resource.class));
    }

    @Test
    void setResourcesControlledByType_shouldSetToControlled() {
        ResourceType targetType = ResourceType.COMPUTER;
        Resource computer1 = createResource("C001", "Computer 1", ResourceType.COMPUTER, false);
        Resource computer2 = createResource("C002", "Computer 2", ResourceType.COMPUTER, false);
        List<Resource> computers = Arrays.asList(computer1, computer2);
        
        when(resourceRepository.findByResourceType(targetType)).thenReturn(computers);
        
        emergencyControlService.setResourcesControlledByType(targetType, true);
        
        assertTrue(computer1.getIsControlled());
        assertTrue(computer2.getIsControlled());
        verify(resourceRepository, times(2)).save(any(Resource.class));
        verify(cacheManager, times(2)).updateResource(any(Resource.class));
    }

    @Test
    void setResourcesControlled_shouldSetSpecificResources() {
        Resource res1 = createResource("R001", "Resource 1", ResourceType.DOOR, true);
        Resource res2 = createResource("R002", "Resource 2", ResourceType.PRINTER, true);
        List<String> resourceIds = Arrays.asList("R001", "R002");
        
        when(resourceRepository.findById("R001")).thenReturn(Optional.of(res1));
        when(resourceRepository.findById("R002")).thenReturn(Optional.of(res2));
        
        emergencyControlService.setResourcesControlled(resourceIds, false);
        
        assertFalse(res1.getIsControlled());
        assertFalse(res2.getIsControlled());
        verify(resourceRepository, times(2)).save(any(Resource.class));
        verify(cacheManager, times(2)).updateResource(any(Resource.class));
    }

    @Test
    void setResourcesControlled_shouldThrowExceptionWhenResourceNotFound() {
        when(resourceRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> emergencyControlService.setResourcesControlled(Arrays.asList("NONEXISTENT"), false));
        
        assertEquals("Resource not found: NONEXISTENT", exception.getMessage());
        verify(resourceRepository, never()).save(any(Resource.class));
        verify(cacheManager, never()).updateResource(any(Resource.class));
    }

    @Test
    void setGroupResourcesControlled_shouldSetAllResourcesInGroup() {
        String groupId = "G001";
        Group group = new Group(groupId, "Test Group");
        Resource res1 = createResource("R001", "Resource 1", ResourceType.DOOR, true);
        Resource res2 = createResource("R002", "Resource 2", ResourceType.PRINTER, true);
        
        group.setResources(new HashSet<>(Arrays.asList(res1, res2)));
        
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        
        emergencyControlService.setGroupResourcesControlled(groupId, false);
        
        assertFalse(res1.getIsControlled());
        assertFalse(res2.getIsControlled());
        verify(resourceRepository, times(2)).save(any(Resource.class));
        verify(cacheManager, times(2)).updateResource(any(Resource.class));
    }

    @Test
    void setGroupResourcesControlled_shouldHandleEmptyGroup() {
        String groupId = "G001";
        Group group = new Group(groupId, "Empty Group");
        group.setResources(new HashSet<>());
        
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        
        emergencyControlService.setGroupResourcesControlled(groupId, false);
        
        verify(resourceRepository, never()).save(any(Resource.class));
        verify(cacheManager, never()).updateResource(any(Resource.class));
    }

    @Test
    void setGroupResourcesControlled_shouldThrowExceptionWhenGroupNotFound() {
        when(groupRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> emergencyControlService.setGroupResourcesControlled("NONEXISTENT", false));
        
        assertEquals("Group not found: NONEXISTENT", exception.getMessage());
        verify(resourceRepository, never()).save(any(Resource.class));
        verify(cacheManager, never()).updateResource(any(Resource.class));
    }

    @Test
    void restoreAllToControlled_shouldOnlyRestoreUncontrolledResources() {
        Resource controlled = createResource("R001", "Controlled", ResourceType.DOOR, true);
        Resource uncontrolled1 = createResource("R002", "Uncontrolled 1", ResourceType.DOOR, false);
        Resource uncontrolled2 = createResource("R003", "Uncontrolled 2", ResourceType.PRINTER, false);
        
        List<Resource> uncontrolledResources = Arrays.asList(uncontrolled1, uncontrolled2);
        when(resourceRepository.findByIsControlled(false)).thenReturn(uncontrolledResources);
        
        emergencyControlService.restoreAllToControlled();
        
        assertTrue(controlled.getIsControlled()); // 保持受控
        assertTrue(uncontrolled1.getIsControlled()); // 恢复为受控
        assertTrue(uncontrolled2.getIsControlled()); // 恢复为受控
        
        verify(resourceRepository, times(2)).save(any(Resource.class));
        verify(cacheManager, times(2)).updateResource(any(Resource.class));
        verify(resourceRepository).findByIsControlled(false);
    }

    @Test
    void restoreAllToControlled_shouldHandleAllAlreadyControlled() {
        when(resourceRepository.findByIsControlled(false)).thenReturn(Arrays.asList());
        
        emergencyControlService.restoreAllToControlled();
        
        verify(resourceRepository, never()).save(any(Resource.class));
        verify(cacheManager, never()).updateResource(any(Resource.class));
    }

    private Resource createResource(String id, String name, ResourceType type, boolean isControlled) {
        Resource resource = new Resource(id, name, type, ResourceState.AVAILABLE);
        resource.setIsControlled(isControlled);
        return resource;
    }
}