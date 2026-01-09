package acs.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Badge 表示员工使用的“徽章/卡片/凭证”。
 *
 * 设计原则：
 * - Badge 是逻辑凭证（logical credential），不是物理芯片建模
 * - status 用于启用/禁用/挂失
 * - employeeId 用于绑定员工（可为空表示未分配）
 */

@Entity
@Table(name = "badges")
public class Badge {

    @Id
    @Column(name = "badge_id", nullable = false, length = 50)
    private String badgeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BadgeStatus status;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "badge_code", length = 100)
    private String badgeCode;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @OneToOne(mappedBy = "badge")
    private Employee employee;

    // 无参构造器（JPA必需）
    public Badge() {}

    // 全参构造器
    public Badge(String badgeId, BadgeStatus status) {
        this.badgeId = badgeId;
        this.status = status;
        this.lastUpdated = Instant.now();
    }

    // 扩展构造器（包含新字段）
    public Badge(String badgeId, BadgeStatus status, LocalDate expirationDate, String badgeCode) {
        this.badgeId = badgeId;
        this.status = status;
        this.expirationDate = expirationDate;
        this.badgeCode = badgeCode;
        this.lastUpdated = Instant.now();
    }

    // Getter和Setter
    public String getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

    public BadgeStatus getStatus() {
        return status;
    }

    public void setStatus(BadgeStatus status) {
        this.status = status;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getBadgeCode() {
        return badgeCode;
    }

    public void setBadgeCode(String badgeCode) {
        this.badgeCode = badgeCode;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}