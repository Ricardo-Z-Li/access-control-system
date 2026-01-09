package acs.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId;

    @Column(name = "employee_name", nullable = false, length = 100)
    private String employeeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "badge_id", referencedColumnName = "badge_id")
    private Badge badge;

    @ManyToMany
    @JoinTable(
        name = "employee_groups",
        joinColumns = @JoinColumn(name = "employee_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Group> groups = new HashSet<>();

    // 无参构造器（JPA必需）
    public Employee() {
        this.userType = UserType.EMPLOYEE;
    }

    // 全参构造器
    public Employee(String employeeId, String employeeName) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.userType = UserType.EMPLOYEE;
    }

    // 扩展构造器（包含userType）
    public Employee(String employeeId, String employeeName, UserType userType) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.userType = userType;
    }

    // Getter和Setter
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Badge getBadge() {
        return badge;
    }

    public void setBadge(Badge badge) {
        this.badge = badge;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "employeeId='" + employeeId + '\'' +
                ", employeeName='" + employeeName + '\'' +
                ", userType=" + userType +
                ", badge=" + (badge != null ? badge.getBadgeId() : "null") +
                '}';
    }
}