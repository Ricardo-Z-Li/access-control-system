package acs.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * LogEntry 表示一条“访问日志记录”
 *
 * 
 * - 每一次访问（无论成功还是失败）都必须生成一条 LogEntry
 * - 日志是企业级访问控制系统的核心组成部分
 */

@Entity
@Table(name = "access_logs")
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "badge_id", referencedColumnName = "badge_id")
    private Badge badge;

    @ManyToOne
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "resource_id", referencedColumnName = "resource_id")
    private Resource resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false)
    private AccessDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false)
    private ReasonCode reasonCode;

    // 无参构造器（JPA必需）
    public LogEntry() {
        this.decision = AccessDecision.PENDING;
        this.reasonCode = ReasonCode.PENDING;
    }

    // 全参构造器
    public LogEntry(LocalDateTime timestamp, Badge badge, Employee employee, Resource resource, AccessDecision decision, ReasonCode reasonCode) {
        this.timestamp = timestamp;
        this.badge = badge;
        this.employee = employee;
        this.resource = resource;
        this.decision = decision;
        this.reasonCode = reasonCode;
    }

    // Getter和Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Badge getBadge() {
        return badge;
    }

    public void setBadge(Badge badge) {
        this.badge = badge;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public AccessDecision getDecision() {
        return decision;
    }

    public void setDecision(AccessDecision decision) {
        this.decision = decision;
    }

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(ReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }
}