package acs.service.impl;

import acs.cache.LocalCacheManager;
import acs.domain.*;
import acs.log.LogService;
import acs.repository.ProfileRepository;
import acs.repository.ResourceDependencyRepository;
import acs.repository.AccessLogRepository;
import acs.service.TimeFilterService;
import acs.service.AccessLimitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccessControlServiceImplTest {

    @Mock
    private LogService logService;

    @Mock
    private LocalCacheManager cacheManager;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private TimeFilterService timeFilterService;

    @Mock
    private AccessLimitService accessLimitService;

    @Mock
    private ResourceDependencyRepository resourceDependencyRepository;

    @Mock
    private AccessLogRepository accessLogRepository;

    @InjectMocks
    private AccessControlServiceImpl accessControlService;

    private final Instant testInstant = Instant.parse("2024-05-01T12:00:00Z");

    // 构建测试用访问请求
    private AccessRequest createAccessRequest(String badgeId, String resourceId) {
        AccessRequest request = new AccessRequest();
        request.setBadgeId(badgeId);
        request.setResourceId(resourceId);
        request.setTimestamp(testInstant);
        return request;
    }

    @Test
    void processAccess_invalidRequest_shouldDeny() {
        // 无效请求（徽章ID为空）
        AccessRequest request = new AccessRequest();
        request.setResourceId("R001");
        request.setTimestamp(testInstant);

        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.INVALID_REQUEST, result.getReasonCode());
        verify(logService).record(any(LogEntry.class));
    }

    @Test
    void processAccess_badgeNotFound_shouldDeny() {
        AccessRequest request = createAccessRequest("B001", "R001");
        when(cacheManager.getBadge("B001")).thenReturn(null); // 徽章不存在

        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.BADGE_NOT_FOUND, result.getReasonCode());
    }

    @Test
    void processAccess_badgeInactive_shouldDeny() {
        Badge badge = new Badge("B001", BadgeStatus.DISABLED); // 徽章未激活
        when(cacheManager.getBadge("B001")).thenReturn(badge);

        AccessRequest request = createAccessRequest("B001", "R001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.BADGE_INACTIVE, result.getReasonCode());
    }

    @Test
    void processAccess_employeeNotFound_shouldDeny() {
        Badge badge = new Badge("B001", BadgeStatus.ACTIVE);
        badge.setEmployee(new Employee("E001", "Test")); // 员工ID存在但缓存中无数据
        when(cacheManager.getBadge("B001")).thenReturn(badge);
        when(cacheManager.getEmployee("E001")).thenReturn(null);

        AccessRequest request = createAccessRequest("B001", "R001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.EMPLOYEE_NOT_FOUND, result.getReasonCode());
    }

    @Test
    void processAccess_resourceNotFound_shouldDeny() {
        // 准备测试数据
        Employee employee = new Employee("E001", "Test");
        Badge badge = new Badge("B001", BadgeStatus.ACTIVE);
        badge.setEmployee(employee);

        when(cacheManager.getBadge("B001")).thenReturn(badge);
        when(cacheManager.getEmployee("E001")).thenReturn(employee);
        when(cacheManager.getResource("R001")).thenReturn(null); // 资源不存在

        AccessRequest request = createAccessRequest("B001", "R001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.RESOURCE_NOT_FOUND, result.getReasonCode());
    }

    @Test
    void processAccess_noPermission_shouldDeny() {
        // 准备测试数据（员工组无资源权限）
        Resource resource = new Resource("R001", "Door", ResourceType.DOOR, ResourceState.AVAILABLE);
        Employee employee = new Employee("E001", "Test");
        employee.setGroups(Collections.emptySet()); // 无组 -> 无权限
        Badge badge = new Badge("B001", BadgeStatus.ACTIVE);
        badge.setEmployee(employee);

        when(cacheManager.getBadge("B001")).thenReturn(badge);
        when(cacheManager.getEmployee("E001")).thenReturn(employee);
        when(cacheManager.getResource("R001")).thenReturn(resource);

        AccessRequest request = createAccessRequest("B001", "R001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.NO_PERMISSION, result.getReasonCode());
    }

    @Test
    void processAccess_resourceLocked_shouldDeny() {
        // 准备测试数据（资源锁定）
        Resource resource = new Resource("R001", "Door", ResourceType.DOOR, ResourceState.LOCKED);
        Group group = new Group("G001", "Admin");
        group.setResources(Collections.singleton(resource));
        Employee employee = new Employee("E001", "Test");
        employee.setGroups(Collections.singleton(group));
        Badge badge = new Badge("B001", BadgeStatus.ACTIVE);
        badge.setEmployee(employee);

        when(cacheManager.getBadge("B001")).thenReturn(badge);
        when(cacheManager.getEmployee("E001")).thenReturn(employee);
        when(cacheManager.getResource("R001")).thenReturn(resource);

        AccessRequest request = createAccessRequest("B001", "R001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.RESOURCE_LOCKED, result.getReasonCode());
    }

    @Test
    void processAccess_allValid_shouldAllow() {
        // 准备测试数据（所有验证通过）
        Resource resource = new Resource("R001", "Door", ResourceType.DOOR, ResourceState.AVAILABLE);
        Group group = new Group("G001", "Admin");
        group.setResources(Collections.singleton(resource));
        Employee employee = new Employee("E001", "Test");
        employee.setGroups(Collections.singleton(group));
        Badge badge = new Badge("B001", BadgeStatus.ACTIVE);
        badge.setEmployee(employee);

        when(cacheManager.getBadge("B001")).thenReturn(badge);
        when(cacheManager.getEmployee("E001")).thenReturn(employee);
        when(cacheManager.getResource("R001")).thenReturn(resource);
        // 模拟时间过滤器检查（资源未受控或没有时间过滤器）
        resource.setIsControlled(false); // 确保不检查时间过滤器
        // 模拟访问限制检查通过
        when(accessLimitService.checkAllLimits(eq(employee), any(Instant.class))).thenReturn(true);
        // 模拟资源依赖关系检查通过（无依赖）
        when(resourceDependencyRepository.findByResourceResourceId("R001")).thenReturn(Collections.emptyList());

        AccessRequest request = createAccessRequest("B001", "R001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.ALLOW, result.getDecision());
        assertEquals(ReasonCode.ALLOW, result.getReasonCode());

        // 验证日志记录
        ArgumentCaptor<LogEntry> logCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logService).record(logCaptor.capture());
        LogEntry recordedLog = logCaptor.getValue();
        assertEquals(AccessDecision.ALLOW, recordedLog.getDecision());
        assertEquals(badge, recordedLog.getBadge());
    }
}