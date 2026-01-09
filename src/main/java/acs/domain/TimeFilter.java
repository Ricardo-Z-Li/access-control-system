package acs.domain;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * TimeFilter 表示时间访问规则，用于限制在特定时间范围内允许访问。
 *
 * 设计原则：
 * - timeFilterId 是时间过滤器的唯一标识符
 * - 支持复杂的年、月、日、星期、时间范围规则
 * - 规则格式示例："2025.July,August.Monday-Friday.8:00-12:00"
 * - 解析后的字段便于快速匹配
 */
@Entity
@Table(name = "time_filters")
public class TimeFilter {

    @Id
    @Column(name = "time_filter_id", nullable = false, length = 50)
    private String timeFilterId;

    @Column(name = "filter_name", nullable = false, length = 100)
    private String filterName;

    @Column(name = "raw_rule", length = 500)
    private String rawRule;

    @Column(name = "year")
    private Integer year;

    @Column(name = "months", length = 100)
    private String months;

    @Column(name = "days_of_month", length = 100)
    private String daysOfMonth;

    @Column(name = "days_of_week", length = 50)
    private String daysOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "is_recurring")
    private Boolean isRecurring;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToMany(mappedBy = "timeFilters")
    private Set<Profile> profiles = new HashSet<>();

    // 无参构造器（JPA必需）
    public TimeFilter() {
        this.isRecurring = false;
    }

    // 全参构造器（简化版）
    public TimeFilter(String timeFilterId, String filterName, String rawRule) {
        this.timeFilterId = timeFilterId;
        this.filterName = filterName;
        this.rawRule = rawRule;
        this.isRecurring = false;
    }

    // Getter和Setter
    public String getTimeFilterId() {
        return timeFilterId;
    }

    public void setTimeFilterId(String timeFilterId) {
        this.timeFilterId = timeFilterId;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getRawRule() {
        return rawRule;
    }

    public void setRawRule(String rawRule) {
        this.rawRule = rawRule;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getMonths() {
        return months;
    }

    public void setMonths(String months) {
        this.months = months;
    }

    public String getDaysOfMonth() {
        return daysOfMonth;
    }

    public void setDaysOfMonth(String daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(String daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean isRecurring) {
        this.isRecurring = isRecurring;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Profile> getProfiles() {
        return profiles;
    }

    public void setProfiles(Set<Profile> profiles) {
        this.profiles = profiles;
    }
}