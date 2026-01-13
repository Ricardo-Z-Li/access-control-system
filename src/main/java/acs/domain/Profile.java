package acs.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Profile 表示一组权限规则配置，用于定义复杂的访问控制策略。
 *
 * 设计原则：
 * - profileId 是配置文件的唯一标识符
 * - 一个配置文件可以关联多个时间过滤器（TimeFilter）
 * - 配置文件可以关联多个资源组（Group）
 * - 配置文件可以定义访问次数限制等高级规则
 */
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "profile_id", nullable = false, length = 50)
    private String profileId;

    @Column(name = "profile_name", nullable = false, length = 100)
    private String profileName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "max_daily_access")
    private Integer maxDailyAccess;

    @Column(name = "max_weekly_access")
    private Integer maxWeeklyAccess;

    @Column(name = "priority_level")
    private Integer priorityLevel;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToMany
    @JoinTable(
        name = "profile_time_filters",
        joinColumns = @JoinColumn(name = "profile_id"),
        inverseJoinColumns = @JoinColumn(name = "time_filter_id")
    )
    private Set<TimeFilter> timeFilters = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "profile_groups",
        joinColumns = @JoinColumn(name = "profile_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Group> groups = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "profile_employees",
        joinColumns = @JoinColumn(name = "profile_id"),
        inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    private Set<Employee> employees = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "profile_badges",
        joinColumns = @JoinColumn(name = "profile_id"),
        inverseJoinColumns = @JoinColumn(name = "badge_id")
    )
    private Set<Badge> badges = new HashSet<>();

    // 无参构造器（JPA必需）
    public Profile() {
        this.isActive = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // 全参构造器（简化版）
    public Profile(String profileId, String profileName, String description) {
        this.profileId = profileId;
        this.profileName = profileName;
        this.description = description;
        this.isActive = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getter和Setter
    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxDailyAccess() {
        return maxDailyAccess;
    }

    public void setMaxDailyAccess(Integer maxDailyAccess) {
        this.maxDailyAccess = maxDailyAccess;
    }

    public Integer getMaxWeeklyAccess() {
        return maxWeeklyAccess;
    }

    public void setMaxWeeklyAccess(Integer maxWeeklyAccess) {
        this.maxWeeklyAccess = maxWeeklyAccess;
    }

    public Integer getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<TimeFilter> getTimeFilters() {
        return timeFilters;
    }

    public void setTimeFilters(Set<TimeFilter> timeFilters) {
        this.timeFilters = timeFilters;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    public Set<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(Set<Employee> employees) {
        this.employees = employees;
    }

    public Set<Badge> getBadges() {
        return badges;
    }

    public void setBadges(Set<Badge> badges) {
        this.badges = badges;
    }
}
