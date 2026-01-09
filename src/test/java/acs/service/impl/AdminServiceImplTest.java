package acs.service.impl;

import acs.cache.LocalCacheManager;
import acs.domain.*;
import acs.repository.BadgeRepository;
import acs.repository.EmployeeRepository;
import acs.repository.GroupRepository;
import acs.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private LocalCacheManager cacheManager;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void registerEmployee_success() {
        String employeeId = "E001";
        when(employeeRepository.existsById(employeeId)).thenReturn(false);

        adminService.registerEmployee(employeeId, "Test Employee");

        verify(employeeRepository).save(any(Employee.class));
        verify(cacheManager).updateEmployee(any(Employee.class));
    }

    @Test
    void registerEmployee_duplicateId_shouldThrow() {
        String employeeId = "E001";
        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> 
            adminService.registerEmployee(employeeId, "Test Employee")
        );
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void issueBadge_success() {
        String employeeId = "E001";
        String badgeId = "B001";
        Employee employee = new Employee(employeeId, "Test");
        
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(badgeRepository.existsById(badgeId)).thenReturn(false);
        when(badgeRepository.findById(badgeId)).thenAnswer(i -> Optional.of(new Badge(badgeId, BadgeStatus.ACTIVE)));

        adminService.issueBadge(employeeId, badgeId);

        verify(employeeRepository).save(employee);
        verify(cacheManager).updateBadge(any(Badge.class));
        verify(cacheManager).updateEmployee(employee);
        assertNotNull(employee.getBadge());
        assertEquals(badgeId, employee.getBadge().getBadgeId());
    }

    @Test
    void setBadgeStatus_success() {
        String badgeId = "B001";
        Badge badge = new Badge(badgeId, BadgeStatus.ACTIVE);
        when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge));

        adminService.setBadgeStatus(badgeId, BadgeStatus.LOST);

        assertEquals(BadgeStatus.LOST, badge.getStatus());
        verify(badgeRepository).save(badge);
        verify(cacheManager).updateBadge(badge);
    }

    @Test
    void createGroup_success() {
        String groupId = "G001";
        when(groupRepository.existsById(groupId)).thenReturn(false);

        adminService.createGroup(groupId, "Admin Group");

        verify(groupRepository).save(any(Group.class));
        verify(cacheManager).updateGroup(any(Group.class));
    }

    @Test
    void assignEmployeeToGroup_success() {
        String employeeId = "E001";
        String groupId = "G001";
        Employee employee = new Employee(employeeId, "Test");
        Group group = new Group(groupId, "Admin");

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        adminService.assignEmployeeToGroup(employeeId, groupId);

        assertTrue(employee.getGroups().contains(group));
        assertTrue(group.getEmployees().contains(employee));
        verify(employeeRepository).save(employee);
        verify(groupRepository).save(group);
        verify(cacheManager).updateEmployee(employee);
        verify(cacheManager).updateGroup(group);
    }

    @Test
    void registerResource_success() {
        String resourceId = "R001";
        when(resourceRepository.existsById(resourceId)).thenReturn(false);

        adminService.registerResource(resourceId, "Door", ResourceType.DOOR);

        verify(resourceRepository).save(any(Resource.class));
        verify(cacheManager).updateResource(any(Resource.class));
    }

    @Test
    void grantGroupAccessToResource_success() {
        String groupId = "G001";
        String resourceId = "R001";
        Group group = new Group(groupId, "Admin");
        Resource resource = new Resource(resourceId, "Door", ResourceType.DOOR, ResourceState.AVAILABLE);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        adminService.grantGroupAccessToResource(groupId, resourceId);

        assertTrue(group.getResources().contains(resource));
        assertTrue(resource.getGroups().contains(group));
        verify(groupRepository).save(group);
        verify(resourceRepository).save(resource);
        verify(cacheManager).updateGroup(group);
        verify(cacheManager).updateResource(resource);
    }
}