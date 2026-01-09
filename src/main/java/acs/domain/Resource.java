package acs.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Resource 表示受控资源（门、房间、设备等）。
 *
 * 设计原则：
 * - ResourceType 描述资源类型（DOOR/PRINTER/...）
 * - ResourceState 描述资源状态（AVAILABLE/OCCUPIED/LOCKED/OFFLINE）
 * - 权限不直接写在 Resource 中（我们选择由 Group 管理授权列表）
 */ 

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @Column(name = "resource_id", nullable = false, length = 50)
    private String resourceId;

    @Column(name = "resource_name", nullable = false, length = 100)
    private String resourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_state", nullable = false)
    private ResourceState resourceState;

    @Column(name = "is_controlled")
    private Boolean isControlled;

    @ManyToMany(mappedBy = "resources")
    private Set<Group> groups = new HashSet<>();

    // 无参构造器（JPA必需）
    public Resource() {
        this.resourceState = ResourceState.PENDING; //设置默认值
        this.resourceType = ResourceType.PENDING;
        this.isControlled = true;
    }

    // 全参构造器
    public Resource(String resourceId, String resourceName, ResourceType resourceType, ResourceState resourceState) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.resourceState = resourceState;
        this.isControlled = true;
    }

    // 扩展构造器（包含isControlled）
    public Resource(String resourceId, String resourceName, ResourceType resourceType, ResourceState resourceState, Boolean isControlled) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.resourceState = resourceState;
        this.isControlled = isControlled;
    }

    // Getter和Setter
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public ResourceState getResourceState() {
        return resourceState;
    }

    public void setResourceState(ResourceState resourceState) {
        this.resourceState = resourceState;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    public Boolean getIsControlled() {
        return isControlled;
    }

    public void setIsControlled(Boolean isControlled) {
        this.isControlled = isControlled;
    }
}