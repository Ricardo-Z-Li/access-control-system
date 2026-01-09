package acs.repository;

import acs.domain.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccessLogRepository extends JpaRepository<LogEntry, Long> {

    // 按徽章ID和时间范围查询
    List<LogEntry> findByBadgeBadgeIdAndTimestampBetween(String badgeId, LocalDateTime start, LocalDateTime end);

    // 按员工ID和时间范围查询
    List<LogEntry> findByEmployeeEmployeeIdAndTimestampBetween(String employeeId, LocalDateTime start, LocalDateTime end);

    // 按员工ID、资源ID和时间范围查询
    List<LogEntry> findByEmployeeEmployeeIdAndResourceResourceIdAndTimestampBetween(String employeeId, String resourceId, LocalDateTime start, LocalDateTime end);

    // 按资源ID和时间范围查询
    List<LogEntry> findByResourceResourceIdAndTimestampBetween(String resourceId, LocalDateTime start, LocalDateTime end);

    // 按决策（DENY）和时间范围查询
    List<LogEntry> findByDecisionAndTimestampBetween(String decision, LocalDateTime start, LocalDateTime end);

    // 删除指定时间之前的日志
    long deleteByTimestampBefore(LocalDateTime timestamp);
}