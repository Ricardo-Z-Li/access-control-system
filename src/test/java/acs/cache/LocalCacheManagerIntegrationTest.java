package acs.cache;

import acs.domain.*;
import acs.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional; 

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实数据库（MySQL）集成测试
 * 测试全链路：数据库操作 -> 缓存同步 -> 业务逻辑有效性
 */
@SpringBootTest // Spring 自动加载真实测试数据库配置
@ActiveProfiles("test") // 激活 test 环境，加载 application-test.properties
public class LocalCacheManagerIntegrationTest {

    // ************************ 注入真实 Repository（操作测试数据库） ************************
    @Autowired
    private LocalCacheManager cacheManager;

    @Autowired
    private BadgeRepository badgeRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private AccessLogRepository accessLogRepository;

    // 测试数据
    private Badge testBadge;
    private Employee testEmployee;
    private Group testGroup;
    private Resource testResource;
    private LogEntry log1;
    private LogEntry log2;

    // ************************ 每个测试方法执行前：准备测试数据（写入真实测试库） ************************
    @BeforeEach
    @Transactional
    @Rollback(false) // 确保数据被真正提交到数据库（而非回滚）
    void setUp() {
        // 1. 清空测试库中的所有数据（保证测试方法独立性，即使 ddl-auto=create-drop 也建议添加）
        accessLogRepository.deleteAll();
        resourceRepository.deleteAll();
        groupRepository.deleteAll();
        employeeRepository.deleteAll();
        badgeRepository.deleteAll();

        // 2. 初始化测试数据，并保存到真实测试数据库
        testBadge = new Badge();
        testBadge.setBadgeId("BD-" + UUID.randomUUID());
        testBadge.setStatus(BadgeStatus.ACTIVE);
        badgeRepository.save(testBadge); // 写入 MySQL

        testEmployee = new Employee();
        testEmployee.setEmployeeId("EMP-" + UUID.randomUUID());
        testEmployee.setEmployeeName("Test Employee");
        employeeRepository.save(testEmployee);

        testGroup = new Group();
        testGroup.setGroupId("GRP-" + UUID.randomUUID());
        testGroup.setName("Test Group");
        groupRepository.save(testGroup);

        testResource = new Resource();
        testResource.setResourceId("RES-" + UUID.randomUUID());
        testResource.setResourceName("Test Resource");
        testResource.setResourceState(ResourceState.AVAILABLE); // 符合业务逻辑的合法值
        testResource.setResourceType(ResourceType.DOOR); // 符合业务逻辑的合法值
        resourceRepository.save(testResource); 

        List<LogEntry> logEntryList = new ArrayList<>();
        log1 = new LogEntry();
        log1.setTimestamp(LocalDateTime.now().minusHours(2));
        log1.setResource(testResource);
        log1.setDecision(AccessDecision.ALLOW);
        log1.setReasonCode(ReasonCode.ALLOW);
        logEntryList.add(log1);

        log2 = new LogEntry();
        log2.setTimestamp(LocalDateTime.now().minusHours(1));
        log2.setResource(testResource);
        log2.setDecision(AccessDecision.DENY);
        log2.setReasonCode(ReasonCode.RESOURCE_OCCUPIED);
        logEntryList.add(log2);
        accessLogRepository.saveAll(logEntryList);

        // 3. 刷新缓存（从真实测试数据库加载数据，完成数据库 -> 缓存的同步）
        cacheManager.refreshAllCache();
    }

    // ************************ 每个测试方法执行后：额外清理（双重保障，避免数据残留） ************************
    @AfterEach
    @Transactional
    @Rollback(false) 
    void tearDown() {
        cacheManager.clearExpiredLogs(LocalDateTime.now()); // 清理缓存日志
        accessLogRepository.deleteAll(); // 清理数据库日志
    }

    /**
     * 测试 1：缓存初始化 - 从真实数据库加载数据，验证缓存与数据库数据一致
     */
    @Test
    void initCache_ShouldLoadAllDataFromRealDatabase() {
        // 执行缓存初始化（从真实测试库加载）
        cacheManager.initCache();

        // 验证 1：缓存数据量与数据库一致
        assertEquals(badgeRepository.count(), 1);
        assertEquals(1, cacheManager.getBadge(testBadge.getBadgeId()) != null ? 1 : 0);

        assertEquals(employeeRepository.count(), 1);
        assertEquals(1, cacheManager.getEmployee(testEmployee.getEmployeeId()) != null ? 1 : 0);

        assertEquals(accessLogRepository.count(), 2);
        assertEquals(2, cacheManager.getLogs().size());

        // 验证 2：缓存数据与数据库数据内容一致（精准匹配）
        Badge cacheBadge = cacheManager.getBadge(testBadge.getBadgeId());
        Badge dbBadge = badgeRepository.findById(testBadge.getBadgeId()).orElse(null);
        assertNotNull(dbBadge);
        assertEquals(dbBadge.getBadgeId(), cacheBadge.getBadgeId());
        assertEquals(dbBadge.getStatus(), cacheBadge.getStatus());
    }

    /**
     * 测试 2：缓存更新 + 数据库同步 - 验证缓存更新后，数据库数据也同步更新
     */
    @Test
    void updateBadge_ShouldUpdateCacheAndRealDatabase() {
        cacheManager.initCache();
        String badgeId = testBadge.getBadgeId();

        // 步骤 1：构造更新后的徽章，执行缓存更新（业务方法应同步更新数据库）
        Badge updatedBadge = new Badge();
        updatedBadge.setBadgeId(badgeId);
        updatedBadge.setStatus(BadgeStatus.DISABLED);
        cacheManager.updateBadge(updatedBadge);

        // 步骤 2：验证缓存已更新
        Badge cacheBadge = cacheManager.getBadge(badgeId);
        assertNotNull(cacheBadge);
        assertEquals(BadgeStatus.DISABLED,cacheBadge.getStatus());

        // 步骤 3：验证真实数据库已同步更新（核心：真实数据库集成测试的关键验证）
        Badge dbBadge = badgeRepository.findById(badgeId).orElse(null);
        assertNotNull(dbBadge);
        assertEquals(BadgeStatus.DISABLED,dbBadge.getStatus());
    }

    /**
     * 测试 3：过期日志清理 - 验证缓存和真实数据库中的过期日志都被清理
     */
    @Test
    @Transactional
    void clearExpiredLogs_ShouldRemoveOldLogsFromCacheAndRealDatabase() {
        cacheManager.initCache();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 步骤 1：添加一条过期日志到缓存和数据库
        LogEntry expiredLog = new LogEntry();
        expiredLog.setTimestamp(sevenDaysAgo.minusHours(1)); // 超过7天，属于过期
        accessLogRepository.save(expiredLog); // 写入数据库
        cacheManager.updateLog(expiredLog); // 写入缓存
        assertEquals(3, accessLogRepository.count());
        assertEquals(3, cacheManager.getLogs().size());

        // 步骤 2：执行过期日志清理（业务方法应同步清理缓存和数据库）
        int deletedCount = cacheManager.clearExpiredLogs(sevenDaysAgo);

        // 步骤 3：验证缓存中过期日志已被清理
        assertEquals(1, deletedCount);
        assertEquals(2, cacheManager.getLogs().size());

        // 步骤 4：验证真实数据库中过期日志已被清理（核心验证）
        List<LogEntry> remainingDbLogs = accessLogRepository.findAll();
        assertEquals(2, remainingDbLogs.size());
        assertFalse(remainingDbLogs.stream().anyMatch(log -> log.getId() == 3L), "真实数据库中过期日志未被清理");
    }
}