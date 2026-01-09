package acs.domain;

import jakarta.persistence.*;

/**
 * 资源依赖关系实体
 * 表示访问某个资源前必须先访问另一个资源（空间访问顺序）
 * 例如：必须先进大楼（资源A）才能进办公室（资源B）
 */
@Entity
@Table(name = "resource_dependencies")
public class ResourceDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne
    @JoinColumn(name = "required_resource_id", nullable = false)
    private Resource requiredResource;

    @Column(name = "time_window_minutes")
    private Integer timeWindowMinutes; // 有效时间窗口（分钟），null表示无时间限制

    @Column(name = "description")
    private String description;

    // 无参构造器（JPA必需）
    public ResourceDependency() {
    }

    // 全参构造器
    public ResourceDependency(Resource resource, Resource requiredResource, Integer timeWindowMinutes, String description) {
        this.resource = resource;
        this.requiredResource = requiredResource;
        this.timeWindowMinutes = timeWindowMinutes;
        this.description = description;
    }

    // Getter和Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Resource getRequiredResource() {
        return requiredResource;
    }

    public void setRequiredResource(Resource requiredResource) {
        this.requiredResource = requiredResource;
    }

    public Integer getTimeWindowMinutes() {
        return timeWindowMinutes;
    }

    public void setTimeWindowMinutes(Integer timeWindowMinutes) {
        this.timeWindowMinutes = timeWindowMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}