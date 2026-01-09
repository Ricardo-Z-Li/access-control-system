package acs.service.impl;

import acs.cache.LocalCacheManager;
import acs.domain.LogEntry;
import acs.domain.Badge;
import acs.domain.Employee;
import acs.domain.Resource;
import acs.domain.ResourceState;
import acs.domain.ResourceType;
import acs.domain.AccessDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LogQueryServiceImplTest {

    @Mock
    private LocalCacheManager cacheManager;

    @InjectMocks
    private LogQueryServiceImpl logQueryService;

    private final ZoneId zoneId = ZoneId.systemDefault();
    private final Instant baseInstant = Instant.parse("2024-05-01T12:00:00Z");
    private final LocalDateTime baseTime = LocalDateTime.ofInstant(baseInstant, zoneId);

    // 构建测试用日志条目
    private LogEntry createLogEntry(String badgeId, String employeeId, String resourceId, 
                                   AccessDecision decision, LocalDateTime timestamp) {
        Badge badge = badgeId != null ? new Badge(badgeId, null) : null;
        Employee employee = employeeId != null ? new Employee(employeeId, null) : null;
        Resource resource = resourceId != null ? new Resource(resourceId, "TestResource", ResourceType.OTHER, ResourceState.AVAILABLE) : null;
        return new LogEntry(timestamp, badge, employee, resource, decision, null);

    }

    @Test
    void findByBadge_shouldReturnMatchingLogs() {
        // 准备测试数据
        LogEntry log1 = createLogEntry("B001", "E001", "R001", AccessDecision.ALLOW, baseTime);
        LogEntry log2 = createLogEntry("B001", "E001", "R002", AccessDecision.DENY, baseTime.plus(1, ChronoUnit.HOURS));
        LogEntry log3 = createLogEntry("B002", "E002", "R001", AccessDecision.ALLOW, baseTime); // 不同徽章
        LogEntry log4 = createLogEntry("B001", "E001", "R003", AccessDecision.ALLOW, baseTime.minus(1, ChronoUnit.HOURS)); // 时间范围外

        List<LogEntry> allLogs = Arrays.asList(log1, log2, log3, log4);
        when(cacheManager.getLogs()).thenReturn(allLogs);

        // 执行测试
        Instant from = baseInstant;
        Instant to = baseInstant.plus(2,ChronoUnit.HOURS);
        List<LogEntry> result = logQueryService.findByBadge("B001", from, to);

        // 验证结果
        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList(log1, log2)));
        assertFalse(result.contains(log3));
        assertFalse(result.contains(log4));
    }

    @Test
    void findByEmployee_shouldReturnMatchingLogs() {
        // 准备测试数据
        LogEntry log1 = createLogEntry("B001", "E001", "R001", AccessDecision.ALLOW, baseTime);
        LogEntry log2 = createLogEntry("B002", "E001", "R002", AccessDecision.DENY, baseTime.plus(1, ChronoUnit.HOURS));
        LogEntry log3 = createLogEntry("B001", "E002", "R001", AccessDecision.ALLOW, baseTime); // 不同员工

        List<LogEntry> allLogs = Arrays.asList(log1, log2, log3);
        when(cacheManager.getLogs()).thenReturn(allLogs);

        // 执行测试
        Instant from = baseInstant;
        Instant to = baseInstant.plus(2, ChronoUnit.HOURS);
        List<LogEntry> result = logQueryService.findByEmployee("E001", from, to);

        // 验证结果
        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList(log1, log2)));
    }

    @Test
    void findByResource_shouldReturnMatchingLogs() {
        // 准备测试数据
        LogEntry log1 = createLogEntry("B001", "E001", "R001", AccessDecision.ALLOW, baseTime);
        LogEntry log2 = createLogEntry("B002", "E002", "R001", AccessDecision.DENY, baseTime.plus(1, ChronoUnit.HOURS));
        LogEntry log3 = createLogEntry("B001", "E001", "R002", AccessDecision.ALLOW, baseTime); // 不同资源

        List<LogEntry> allLogs = Arrays.asList(log1, log2, log3);
        when(cacheManager.getLogs()).thenReturn(allLogs);

        // 执行测试
        Instant from = baseInstant;
        Instant to = baseInstant.plus(2, ChronoUnit.HOURS);
        List<LogEntry> result = logQueryService.findByResource("R001", from, to);

        // 验证结果
        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList(log1, log2)));
    }

    @Test
    void findDenied_shouldReturnDeniedLogs() {
        // 准备测试数据
        LogEntry log1 = createLogEntry("B001", "E001", "R001", AccessDecision.DENY, baseTime);
        LogEntry log2 = createLogEntry("B002", "E002", "R002", AccessDecision.DENY, baseTime.plus(1, ChronoUnit.HOURS));
        LogEntry log3 = createLogEntry("B001", "E001", "R003", AccessDecision.ALLOW, baseTime); // 允许访问

        List<LogEntry> allLogs = Arrays.asList(log1, log2, log3);
        when(cacheManager.getLogs()).thenReturn(allLogs);

        // 执行测试
        Instant from = baseInstant;
        Instant to = baseInstant.plus(2, ChronoUnit.HOURS);
        List<LogEntry> result = logQueryService.findDenied(from, to);

        // 验证结果
        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList(log1, log2)));
    }
}