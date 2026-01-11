package acs.service.impl;

import acs.domain.LogEntry;
import acs.domain.AccessDecision;
import acs.service.LogQueryService;
import acs.cache.LocalCacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogQueryServiceImpl implements LogQueryService {

    private final LocalCacheManager cacheManager;

    public LogQueryServiceImpl(LocalCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    // 按徽章查询日志
    @Override
    public List<LogEntry> findByBadge(String badgeId, Instant from, Instant to) {
        LocalDateTime start = (from != null) ? LocalDateTime.ofInstant(from, ZoneId.systemDefault()) : null;
        LocalDateTime end = (to != null) ? LocalDateTime.ofInstant(to, ZoneId.systemDefault()) : null;
        // 从缓存过滤
        return cacheManager.getLogs().stream()
                .filter(log -> log.getBadge() != null && log.getBadge().getBadgeId().equals(badgeId))
                .filter(log -> log.getTimestamp() != null)
                .filter(log -> start == null || !log.getTimestamp().isBefore(start))
                .filter(log -> end == null || !log.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }

    // 按员工查询日志
    @Override
    public List<LogEntry> findByEmployee(String employeeId, Instant from, Instant to) {
        LocalDateTime start = (from != null) ? LocalDateTime.ofInstant(from, ZoneId.systemDefault()) : null;
        LocalDateTime end = (to != null) ? LocalDateTime.ofInstant(to, ZoneId.systemDefault()) : null;
        return cacheManager.getLogs().stream()
                .filter(log -> log.getEmployee() != null && log.getEmployee().getEmployeeId().equals(employeeId))
                .filter(log -> log.getTimestamp() != null)
                .filter(log -> start == null || !log.getTimestamp().isBefore(start))
                .filter(log -> end == null || !log.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }

    // 按资源查询日志
    @Override
    public List<LogEntry> findByResource(String resourceId, Instant from, Instant to) {
        LocalDateTime start = (from != null) ? LocalDateTime.ofInstant(from, ZoneId.systemDefault()) : null;
        LocalDateTime end = (to != null) ? LocalDateTime.ofInstant(to, ZoneId.systemDefault()) : null;
        return cacheManager.getLogs().stream()
                .filter(log -> log.getResource() != null && log.getResource().getResourceId().equals(resourceId))
                .filter(log -> log.getTimestamp() != null)
                .filter(log -> start == null || !log.getTimestamp().isBefore(start))
                .filter(log -> end == null || !log.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }

    // 查询被拒绝的日志
    @Override
    public List<LogEntry> findDenied(Instant from, Instant to) {
        LocalDateTime start = (from != null) ? LocalDateTime.ofInstant(from, ZoneId.systemDefault()) : null;
        LocalDateTime end = (to != null) ? LocalDateTime.ofInstant(to, ZoneId.systemDefault()) : null;
        return cacheManager.getLogs().stream()
                .filter(log -> log.getDecision() != null && log.getDecision() == AccessDecision.DENY)
                .filter(log -> log.getTimestamp() != null)
                .filter(log -> start == null || !log.getTimestamp().isBefore(start))
                .filter(log -> end == null || !log.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }

    // 查询所有日志
    @Override
    public List<LogEntry> findAll() {
        return cacheManager.getLogs();
    }
}