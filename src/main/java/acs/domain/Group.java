package acs.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Group 表示一类权限角色（例如 Admin / Staff / Visitor）。
 *
 * 我们采用一种简单、易解释的权限模型：
 * - Group 持有自己能访问的 ResourceId 集合（resourceIds）
 * - 判断权限时：只要员工所属任意 group 的 resourceIds 包含该资源，即可访问
 *
 * 优点：
 * - 逻辑清晰，答辩好讲
 * - 数据驱动，配置权限只需改 Group 的授权列表
 */

@Entity
@Table(name = "group_permissions")
public class Group {

    @Id
    @Column(name = "group_id", nullable = false, length = 50)
    private String groupId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToMany(mappedBy = "groups")
    private Set<Employee> employees = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "group_resources",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "resource_id")
    )
    private Set<Resource> resources = new HashSet<>();

    // 无参构造器（JPA必需）
    public Group() {}

    // 全参构造器
    public Group(String groupId, String name) {
        this.groupId = groupId;
        this.name = name;
    }

    // Getter和Setter
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(Set<Employee> employees) {
        this.employees = employees;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }
}