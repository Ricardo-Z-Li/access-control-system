package acs.service.impl;

import acs.cache.LocalCacheManager;
import acs.domain.*;
import acs.log.LogService;
import acs.repository.ProfileRepository;
import acs.repository.ResourceDependencyRepository;
import acs.repository.AccessLogRepository;
import acs.service.TimeFilterService;
import acs.service.AccessLimitService;
import acs.simulator.BadgeCodeUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    private BadgeCodeUpdateService badgeCodeUpdateService;

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

    @BeforeEach
    void setUpDefaults() {
        when(accessLimitService.checkResourceLimits(any(), any(), any())).thenReturn(true);
        when(profileRepository.findByEmployeesContaining(any())).thenReturn(Collections.emptyList());
        when(profileRepository.findByBadgesContaining(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void processAccess_invalidRequest_shouldDeny() {
        // 无效请求（徽章ID为空）
        AccessRequest request = new AccessRequest();
        request.setResourceId("RES001");
        request.setTimestamp(testInstant);

        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.INVALID_REQUEST, result.getReasonCode());
        verify(logService).record(any(LogEntry.class));
    }

    @Test
    void processAccess_badgeNotFound_shouldDeny() {
        AccessRequest request = createAccessRequest("BADGEMP001", "RES001");
        when(cacheManager.getBadge("BADGEMP001")).thenReturn(null); // 徽章不存在

        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.BADGE_NOT_FOUND, result.getReasonCode());
    }

    @Test
    void processAccess_badgeInactive_shouldDeny() {
        Badge badge = new Badge("BADGEMP001", BadgeStatus.DISABLED); // 徽章未激活
        when(cacheManager.getBadge("BADGEMP001")).thenReturn(badge);

        AccessRequest request = createAccessRequest("BADGEMP001", "RES001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.BADGE_INACTIVE, result.getReasonCode());
    }

    @Test
    void processAccess_badgeExpired_shouldDeny() {
        Badge badge = new Badge("BADGEXP001", BadgeStatus.ACTIVE);
        badge.setExpirationDate(LocalDate.of(2024, 4, 30));
        when(cacheManager.getBadge("BADGEXP001")).thenReturn(badge);

        AccessRequest request = createAccessRequest("BADGEXP001", "RES001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.BADGE_EXPIRED, result.getReasonCode());
    }

    @Test
    void processAccess_badgeUpdateRequired_shouldDeny() {
        Badge badge = new Badge("BADGEUPD001", BadgeStatus.ACTIVE);
        badge.setExpirationDate(LocalDate.of(2024, 12, 31));
        when(cacheManager.getBadge("BADGEUPD001")).thenReturn(badge);
        when(badgeCodeUpdateService.evaluateBadgeUpdateStatus(eq("BADGEUPD001"), any(Instant.class)))
                .thenReturn(BadgeUpdateStatus.UPDATE_REQUIRED);

        AccessRequest request = createAccessRequest("BADGEUPD001", "RES001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.BADGE_UPDATE_REQUIRED, result.getReasonCode());
    }

    @Test
    void processAccess_badgeUpdateOverdue_shouldDeny() {
        Badge badge = new Badge("BADGEUPD002", BadgeStatus.ACTIVE);
        badge.setExpirationDate(LocalDate.of(2024, 12, 31));
        when(cacheManager.getBadge("BADGEUPD002")).thenReturn(badge);
        when(badgeCodeUpdateService.evaluateBadgeUpdateStatus(eq("BADGEUPD002"), any(Instant.class)))
                .thenReturn(BadgeUpdateStatus.UPDATE_OVERDUE);

        AccessRequest request = createAccessRequest("BADGEUPD002", "RES001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.BADGE_UPDATE_OVERDUE, result.getReasonCode());
    }


    @Test
    void processAccess_employeeNotFound_shouldDeny() {
        Badge badge = new Badge("BADGEMP001", BadgeStatus.ACTIVE);
        badge.setEmployee(new Employee("EMP001", "Test")); // 员工ID存在但缓存中无数据
        when(cacheManager.getBadge("BADGEMP001")).thenReturn(badge);
        when(cacheManager.getEmployee("EMP001")).thenReturn(null);

        AccessRequest request = createAccessRequest("BADGEMP001", "RES001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.EMPLOYEE_NOT_FOUND, result.getReasonCode());
    }

    @Test
    void processAccess_resourceNotFound_shouldDeny() {
        // 准备测试数据
        Employee employee = new Employee("EMP001", "Test");
        Badge badge = new Badge("BADGEMP001", BadgeStatus.ACTIVE);
        badge.setEmployee(employee);

        when(cacheManager.getBadge("BADGEMP001")).thenReturn(badge);
        when(cacheManager.getEmployee("EMP001")).thenReturn(employee);
        when(cacheManager.getResource("RES001")).thenReturn(null); // 资源不存在

        AccessRequest request = createAccessRequest("BADGEMP001", "RES001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.RESOURCE_NOT_FOUND, result.getReasonCode());
    }

    @Test
    void processAccess_noPermission_shouldDeny() {
        // 准备测试数据（员工组无资源权限）
        Resource resource = new Resource("RES001", "Door", ResourceType.DOOR, ResourceState.AVAILABLE);
        Employee employee = new Employee("EMP001", "Test");
        employee.setGroups(Collections.emptySet()); // 无组 -> 无权限
        Badge badge = new Badge("BADGEMP001", BadgeStatus.ACTIVE);
        badge.setEmployee(employee);

        when(cacheManager.getBadge("BADGEMP001")).thenReturn(badge);
        when(cacheManager.getEmployee("EMP001")).thenReturn(employee);
        when(cacheManager.getResource("RES001")).thenReturn(resource);

        AccessRequest request = createAccessRequest("BADGEMP001", "RES001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.NO_PERMISSION, result.getReasonCode());
    }

    @Test
    void processAccess_resourceLocked_shouldDeny() {
        // 准备测试数据（资源锁定）
        Resource resource = new Resource("RES001", "Door", ResourceType.DOOR, ResourceState.LOCKED);
        Group group = new Group("GROUP001", "Admin");
        group.setResources(Collections.singleton(resource));
        Employee employee = new Employee("EMP001", "Test");
        employee.setGroups(Collections.singleton(group));
        Badge badge = new Badge("BADGEMP001", BadgeStatus.ACTIVE);
        badge.setEmployee(employee);

        when(cacheManager.getBadge("BADGEMP001")).thenReturn(badge);
        when(cacheManager.getEmployee("EMP001")).thenReturn(employee);
        when(cacheManager.getResource("RES001")).thenReturn(resource);

        AccessRequest request = createAccessRequest("BADGEMP001", "RES001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.RESOURCE_LOCKED, result.getReasonCode());
    }

    @Test
    void processAccess_allValid_shouldAllow() {
        // 准备测试数据（所有验证通过）
        Resource resource = new Resource("RES001", "Door", ResourceType.DOOR, ResourceState.AVAILABLE);
        Group group = new Group("GROUP001", "Admin");
        group.setResources(Collections.singleton(resource));
        Employee employee = new Employee("EMP001", "Test");
        employee.setGroups(Collections.singleton(group));
        Badge badge = new Badge("BADGEMP001", BadgeStatus.ACTIVE);
        badge.setEmployee(employee);

        when(cacheManager.getBadge("BADGEMP001")).thenReturn(badge);
        when(cacheManager.getEmployee("EMP001")).thenReturn(employee);
        when(cacheManager.getResource("RES001")).thenReturn(resource);
        // 模拟时间过滤器检查（资源未受控或没有时间过滤器）
        resource.setIsControlled(false); // 确保不检查时间过滤器
        // 模拟访问限制检查通过
        when(accessLimitService.checkAllLimits(eq(employee), any(Instant.class))).thenReturn(true);
        // 模拟资源依赖关系检查通过（无依赖）
        when(resourceDependencyRepository.findByResourceResourceId("RES001")).thenReturn(Collections.emptyList());

        AccessRequest request = createAccessRequest("BADGEMP001", "RES001");
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

    @Test
    void processAccess_timeFilterNotMatch_shouldDeny() {
        // 准备测试数据：资源受控且有时间过滤器，但当前时间不匹配
        Resource resource = new Resource("RES001", "Door", ResourceType.DOOR, ResourceState.AVAILABLE);
        resource.setIsControlled(true); // 资源受时间控制
        Group group = new Group("GROUP001", "Admin");
        group.setResources(Collections.singleton(resource));
        Employee employee = new Employee("EMP001", "Test");
        employee.setGroups(Collections.singleton(group));
        Badge badge = new Badge("BADGEMP001", BadgeStatus.ACTIVE);
        badge.setEmployee(employee);

        // 模拟缓存
        when(cacheManager.getBadge("BADGEMP001")).thenReturn(badge);
        when(cacheManager.getEmployee("EMP001")).thenReturn(employee);
        when(cacheManager.getResource("RES001")).thenReturn(resource);

        // 模拟配置文件：激活的配置文件关联到组，并包含时间过滤器
        Profile profile = new Profile("PROF001", "Test Profile", "Test");
        profile.setIsActive(true);
        profile.setPriorityLevel(1);
        TimeFilter timeFilter = new TimeFilter();
        // 假设时间过滤器规则是"2025.July.Monday-Friday.8:00-12:00"
        timeFilter.setYear(2025);
        timeFilter.setMonths("JULY");
        timeFilter.setDaysOfWeek("1,2,3,4,5");
        timeFilter.setTimeRanges("08:00-12:00");
        profile.setTimeFilters(Collections.singleton(timeFilter));
        profile.setGroups(Collections.singleton(group));

        // 模拟profileRepository返回此配置文件
        when(profileRepository.findByGroupsContaining(group)).thenReturn(Collections.singletonList(profile));

        // 模拟时间过滤器服务：不匹配当前时间（测试时间是2024-05-01T12:00:00Z）
        when(timeFilterService.matchesAny(anyList(), any())).thenReturn(false);

        // 模拟访问限制检查通过
        when(accessLimitService.checkAllLimits(eq(employee), any(Instant.class))).thenReturn(true);
        // 模拟资源依赖关系检查通过
        when(resourceDependencyRepository.findByResourceResourceId("RES001")).thenReturn(Collections.emptyList());

        AccessRequest request = createAccessRequest("BADGEMP001", "RES001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.DENY, result.getDecision());
        assertEquals(ReasonCode.NO_PERMISSION, result.getReasonCode());
        assertTrue(result.getMessage().contains("Access not allowed at this time"));
        // 验证日志记录
        verify(logService).record(any(LogEntry.class));
    }

    @Test
    void processAccess_timeFilterMatch_shouldAllow() {
        // 准备测试数据：资源受控且有时间过滤器，当前时间匹配
        Resource resource = new Resource("RES001", "Door", ResourceType.DOOR, ResourceState.AVAILABLE);
        resource.setIsControlled(true);
        Group group = new Group("GROUP001", "Admin");
        group.setResources(Collections.singleton(resource));
        Employee employee = new Employee("EMP001", "Test");
        employee.setGroups(Collections.singleton(group));
        Badge badge = new Badge("BADGEMP001", BadgeStatus.ACTIVE);
        badge.setEmployee(employee);

        when(cacheManager.getBadge("BADGEMP001")).thenReturn(badge);
        when(cacheManager.getEmployee("EMP001")).thenReturn(employee);
        when(cacheManager.getResource("RES001")).thenReturn(resource);

        Profile profile = new Profile("PROF001", "Test Profile", "Test");
        profile.setIsActive(true);
        profile.setPriorityLevel(1);
        TimeFilter timeFilter = new TimeFilter();
        profile.setTimeFilters(Collections.singleton(timeFilter));
        profile.setGroups(Collections.singleton(group));

        when(profileRepository.findByGroupsContaining(group)).thenReturn(Collections.singletonList(profile));
        // 模拟时间过滤器服务：匹配当前时间
        when(timeFilterService.matchesAny(anyList(), any())).thenReturn(true);

        when(accessLimitService.checkAllLimits(eq(employee), any(Instant.class))).thenReturn(true);
        when(resourceDependencyRepository.findByResourceResourceId("RES001")).thenReturn(Collections.emptyList());

        AccessRequest request = createAccessRequest("BADGEMP001", "RES001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.ALLOW, result.getDecision());
        assertEquals(ReasonCode.ALLOW, result.getReasonCode());
        verify(logService).record(any(LogEntry.class));
    }

    @Test
    void processAccess_resourceNotControlled_shouldIgnoreTimeFilter() {
        // 准备测试数据：资源不受时间控制，即使有时间过滤器也应允许访问
        Resource resource = new Resource("RES001", "Door", ResourceType.DOOR, ResourceState.AVAILABLE);
        resource.setIsControlled(false); // 资源不受时间控制
        Group group = new Group("GROUP001", "Admin");
        group.setResources(Collections.singleton(resource));
        Employee employee = new Employee("EMP001", "Test");
        employee.setGroups(Collections.singleton(group));
        Badge badge = new Badge("BADGEMP001", BadgeStatus.ACTIVE);
        badge.setEmployee(employee);

        when(cacheManager.getBadge("BADGEMP001")).thenReturn(badge);
        when(cacheManager.getEmployee("EMP001")).thenReturn(employee);
        when(cacheManager.getResource("RES001")).thenReturn(resource);

        // 即使存在配置文件和时间过滤器，资源不受控时应跳过时间检查
        Profile profile = new Profile("PROF001", "Test Profile", "Test");
        profile.setIsActive(true);
        profile.setPriorityLevel(1);
        TimeFilter timeFilter = new TimeFilter();
        profile.setTimeFilters(Collections.singleton(timeFilter));
        profile.setGroups(Collections.singleton(group));

        when(profileRepository.findByGroupsContaining(group)).thenReturn(Collections.singletonList(profile));
        // 时间过滤器服务不应被调用，因为资源不受控
        // 但我们仍然模拟它，如果被调用则返回false（确保测试失败如果被调用）
        when(timeFilterService.matchesAny(anyList(), any())).thenReturn(false);

        when(accessLimitService.checkAllLimits(eq(employee), any(Instant.class))).thenReturn(true);
        when(resourceDependencyRepository.findByResourceResourceId("RES001")).thenReturn(Collections.emptyList());

        AccessRequest request = createAccessRequest("BADGEMP001", "RES001");
        AccessResult result = accessControlService.processAccess(request);

        assertEquals(AccessDecision.ALLOW, result.getDecision());
        assertEquals(ReasonCode.ALLOW, result.getReasonCode());
        // 验证时间过滤器服务未被调用（因为资源不受控）
        verify(timeFilterService, never()).matchesAny(anyList(), any());
        verify(logService).record(any(LogEntry.class));
    }
}
