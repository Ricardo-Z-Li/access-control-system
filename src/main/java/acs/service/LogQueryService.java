package acs.service;

import acs.domain.LogEntry;

import java.time.Instant;
import java.util.List;

/**
 * LogQueryService 提供访问日志的查询能力
 *
 * 用于：
 * - 管理员审计
 * - UI 展示访问历史
 */
public interface LogQueryService {

    List<LogEntry> findByBadge(String badgeId, Instant from, Instant to);

    List<LogEntry> findByEmployee(String employeeId, Instant from, Instant to);

    List<LogEntry> findByResource(String resourceId, Instant from, Instant to);

    List<LogEntry> findDenied(Instant from, Instant to);

    List<LogEntry> findAll();
}
