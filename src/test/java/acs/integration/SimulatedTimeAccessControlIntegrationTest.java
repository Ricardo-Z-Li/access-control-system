package acs.integration;

import acs.domain.*;
import acs.repository.*;
import acs.service.AccessControlService;
import acs.service.ClockService;
import acs.service.TimeFilterService;
import acs.cache.LocalCacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模拟时间系统集成测试
 * 验证设置模拟时间后，时间限制过滤能否正确进行逻辑判定
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SimulatedTimeAccessControlIntegrationTest {

    @Autowired
    private ClockService clockService;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private TimeFilterService timeFilterService;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private TimeFilterRepository timeFilterRepository;

    @Autowired
    private LocalCacheManager cacheManager;

    // 测试数据
    private Badge testBadge;
    private Employee testEmployee;
    private Group testGroup;
    private Resource testResource;
    private Profile testProfile;
    private TimeFilter timeFilter;

    @BeforeEach
    @Rollback(false)
    void setUp() {
        // 确保使用真实系统时间开始测试
        clockService.resetToRealTime();

        // 清理可能存在的旧数据
        profileRepository.deleteAll();
        timeFilterRepository.deleteAll();
        groupRepository.deleteAll();
        resourceRepository.deleteAll();
        employeeRepository.deleteAll();
        badgeRepository.deleteAll();

        // 创建测试徽章
        testBadge = new Badge();
        testBadge.setBadgeId("TEST-BADGE-" + UUID.randomUUID());
        testBadge.setStatus(BadgeStatus.ACTIVE);
        badgeRepository.save(testBadge);

        // 创建测试员工
        testEmployee = new Employee();
        testEmployee.setEmployeeId("TEST-EMP-" + UUID.randomUUID());
        testEmployee.setEmployeeName("Test Employee");
        testEmployee.setBadge(testBadge);
        employeeRepository.save(testEmployee);

        // 更新徽章关联员工
        testBadge.setEmployee(testEmployee);
        badgeRepository.save(testBadge);

        // 创建测试组
        testGroup = new Group();
        testGroup.setGroupId("TEST-GROUP-" + UUID.randomUUID());
        testGroup.setName("Test Group");
        groupRepository.save(testGroup);

        // 将员工添加到组
        testEmployee.setGroups(Collections.singleton(testGroup));
        employeeRepository.save(testEmployee);

        // 创建测试资源（受时间控制）
        testResource = new Resource();
        testResource.setResourceId("TEST-RES-" + UUID.randomUUID());
        testResource.setResourceName("Test Resource");
        testResource.setResourceType(ResourceType.DOOR);
        testResource.setResourceState(ResourceState.AVAILABLE);
        testResource.setIsControlled(true); // 关键：启用时间控制
        resourceRepository.save(testResource);

        // 将资源分配给组
        testGroup.setResources(Collections.singleton(testResource));
        groupRepository.save(testGroup);
        
        // 刷新缓存以确保新数据被加载
        cacheManager.refreshAllCache();
    }

    @AfterEach
    void tearDown() {
        // 每次测试后重置模拟时间
        clockService.resetToRealTime();
    }

    /**
     * 测试场景1：设置模拟时间在时间过滤器允许范围内，访问应被允许
     */
    @Test
    void processAccess_withSimulatedTimeMatchingFilter_shouldAllow() {
        // 1. 创建时间过滤器：允许2025年7月，周一至周五，8:00-12:00
        TimeFilter filter = new TimeFilter();
        filter.setTimeFilterId("TF-TEST-1");
        filter.setFilterName("Test Filter");
        filter.setYear(2025);
        filter.setMonths("JULY");
        filter.setDaysOfWeek("1,2,3,4,5"); // Monday-Friday
        filter.setTimeRanges("08:00-12:00");
        timeFilterRepository.save(filter);

        // 2. 创建配置文件并关联时间过滤器
        Profile profile = new Profile();
        profile.setProfileId("TEST-PROFILE-1");
        profile.setProfileName("Test Profile");
        profile.setDescription("Test profile with time filter");
        profile.setIsActive(true);
        profile.setPriorityLevel(1);
        profile.setTimeFilters(Collections.singleton(filter));
        profile.setGroups(Collections.singleton(testGroup));
        profileRepository.save(profile);

        // 3. 设置模拟时间：2025-07-01 星期二 09:00（在允许范围内）
        LocalDateTime simulatedTime = LocalDateTime.of(2025, Month.JULY, 1, 9, 0);
        clockService.setSimulatedTime(simulatedTime);

        // 4. 创建访问请求，使用模拟时间作为时间戳
        AccessRequest request = new AccessRequest();
        request.setBadgeId(testBadge.getBadgeId());
        request.setResourceId(testResource.getResourceId());
        // 使用ClockService的当前时间（模拟时间）
        request.setTimestamp(clockService.now());

        // 5. 处理访问请求
        AccessResult result = accessControlService.processAccess(request);

        // 6. 验证：应允许访问
        assertEquals(AccessDecision.ALLOW, result.getDecision(), 
            "当模拟时间匹配时间过滤器时，应允许访问。实际结果: " + result.getDecision());
        assertEquals(ReasonCode.ALLOW, result.getReasonCode());
    }

    /**
     * 测试场景2：设置模拟时间在时间过滤器范围外，访问应被拒绝
     */
    @Test
    void processAccess_withSimulatedTimeNotMatchingFilter_shouldDeny() {
        // 1. 创建相同的时间过滤器
        TimeFilter filter = new TimeFilter();
        filter.setTimeFilterId("TF-TEST-2");
        filter.setFilterName("Test Filter");
        filter.setYear(2025);
        filter.setMonths("JULY");
        filter.setDaysOfWeek("1,2,3,4,5");
        filter.setTimeRanges("08:00-12:00");
        timeFilterRepository.save(filter);

        // 2. 创建配置文件
        Profile profile = new Profile();
        profile.setProfileId("TEST-PROFILE-2");
        profile.setProfileName("Test Profile");
        profile.setDescription("Test profile with time filter");
        profile.setIsActive(true);
        profile.setPriorityLevel(1);
        profile.setTimeFilters(Collections.singleton(filter));
        profile.setGroups(Collections.singleton(testGroup));
        profileRepository.save(profile);

        // 3. 设置模拟时间：2025-07-01 星期二 13:00（在允许时间范围外）
        LocalDateTime simulatedTime = LocalDateTime.of(2025, Month.JULY, 1, 13, 0);
        clockService.setSimulatedTime(simulatedTime);

        // 4. 创建访问请求
        AccessRequest request = new AccessRequest();
        request.setBadgeId(testBadge.getBadgeId());
        request.setResourceId(testResource.getResourceId());
        request.setTimestamp(clockService.now());

        // 5. 处理访问请求
        AccessResult result = accessControlService.processAccess(request);

        // 6. 验证：应拒绝访问
        assertEquals(AccessDecision.DENY, result.getDecision(),
            "当模拟时间不匹配时间过滤器时，应拒绝访问。实际结果: " + result.getDecision());
        assertEquals(ReasonCode.NO_PERMISSION, result.getReasonCode());
        assertTrue(result.getMessage().contains("当前时间不允许访问"),
            "拒绝消息应包含'当前时间不允许访问'。实际消息: " + result.getMessage());
    }

    /**
     * 测试场景3：使用真实系统时间（非模拟时间），验证时间过滤器仍然工作
     */
    @Test
    void processAccess_withRealTime_shouldWorkNormally() {
        // 1. 创建时间过滤器：允许未来时间（确保当前真实时间不匹配）
        TimeFilter filter = new TimeFilter();
        filter.setTimeFilterId("TF-TEST-3");
        filter.setFilterName("Test Filter");
        filter.setYear(2030); // 未来年份
        filter.setMonths("JANUARY");
        filter.setDaysOfWeek("1");
        filter.setTimeRanges("08:00-12:00");
        timeFilterRepository.save(filter);

        // 2. 创建配置文件
        Profile profile = new Profile();
        profile.setProfileId("TEST-PROFILE-3");
        profile.setProfileName("Test Profile");
        profile.setDescription("Test profile with future time filter");
        profile.setIsActive(true);
        profile.setPriorityLevel(1);
        profile.setTimeFilters(Collections.singleton(filter));
        profile.setGroups(Collections.singleton(testGroup));
        profileRepository.save(profile);

        // 3. 确保使用真实系统时间（不设置模拟时间）
        clockService.resetToRealTime();
        assertFalse(clockService.isSimulated(), "测试应使用真实系统时间");

        // 4. 创建访问请求，使用当前真实时间
        AccessRequest request = new AccessRequest();
        request.setBadgeId(testBadge.getBadgeId());
        request.setResourceId(testResource.getResourceId());
        request.setTimestamp(clockService.now());

        // 5. 处理访问请求
        AccessResult result = accessControlService.processAccess(request);

        // 6. 验证：应拒绝访问（因为当前时间不是2030年）
        assertEquals(AccessDecision.DENY, result.getDecision(),
            "当真实时间不匹配时间过滤器时，应拒绝访问");
        assertEquals(ReasonCode.NO_PERMISSION, result.getReasonCode());
    }

    /**
     * 测试场景4：资源不受时间控制时，即使有时间过滤器也应允许访问
     */
    @Test
    void processAccess_resourceNotControlled_shouldIgnoreTimeFilter() {
        // 1. 修改资源为不受时间控制
        testResource.setIsControlled(false);
        resourceRepository.save(testResource);

        // 2. 创建时间过滤器（但资源不受控，应被忽略）
        TimeFilter filter = new TimeFilter();
        filter.setTimeFilterId("TF-TEST-4");
        filter.setFilterName("Test Filter");
        filter.setYear(2025);
        filter.setMonths("JULY");
        filter.setDaysOfWeek("1,2,3,4,5");
        filter.setTimeRanges("08:00-12:00");
        timeFilterRepository.save(filter);

        // 3. 创建配置文件
        Profile profile = new Profile();
        profile.setProfileId("TEST-PROFILE-4");
        profile.setProfileName("Test Profile");
        profile.setDescription("Test profile");
        profile.setIsActive(true);
        profile.setPriorityLevel(1);
        profile.setTimeFilters(Collections.singleton(filter));
        profile.setGroups(Collections.singleton(testGroup));
        profileRepository.save(profile);

        // 4. 设置模拟时间在过滤器范围外（13:00）
        LocalDateTime simulatedTime = LocalDateTime.of(2025, Month.JULY, 1, 13, 0);
        clockService.setSimulatedTime(simulatedTime);

        // 5. 创建访问请求
        AccessRequest request = new AccessRequest();
        request.setBadgeId(testBadge.getBadgeId());
        request.setResourceId(testResource.getResourceId());
        request.setTimestamp(clockService.now());

        // 6. 处理访问请求
        AccessResult result = accessControlService.processAccess(request);

        // 7. 验证：应允许访问（资源不受时间控制，忽略过滤器）
        assertEquals(AccessDecision.ALLOW, result.getDecision(),
            "当资源不受时间控制时，应忽略时间过滤器并允许访问");
        assertEquals(ReasonCode.ALLOW, result.getReasonCode());
    }
}