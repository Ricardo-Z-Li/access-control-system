package acs.domain;

import jakarta.persistence.*;

/**
 * ProfileResourceLimit defines per-resource access limits.
 */
@Entity
@Table(name = "profile_resource_limits")
public class ProfileResourceLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Column(name = "weekly_limit")
    private Integer weeklyLimit;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public ProfileResourceLimit() {
        this.isActive = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Integer getWeeklyLimit() {
        return weeklyLimit;
    }

    public void setWeeklyLimit(Integer weeklyLimit) {
        this.weeklyLimit = weeklyLimit;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
