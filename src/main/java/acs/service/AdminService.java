package acs.service;

import acs.domain.BadgeStatus;
import acs.domain.ResourceState;
import acs.domain.ResourceType;

/**
 * AdminService 提供系统管理功能
 *
 * 主要用于：
 * - 管理员工
 * - 分配 Badge
 * - 配置权限关系
 *
 * 通常由“管理员界面”调用
 */
public interface AdminService {

    /** 注册一个新员工 */
    void registerEmployee(String employeeId, String name);

    /** 给员工发放 Badge */
    void issueBadge(String employeeId, String badgeId);

    /** 设置 Badge 状态（启用 / 禁用 / 挂失） */
    void setBadgeStatus(String badgeId, BadgeStatus status);

    /** 创建一个权限组 */
    void createGroup(String groupId, String groupName);

    /** 将员工加入某个组 */
    void assignEmployeeToGroup(String employeeId, String groupId);

    /** 将员工从某个组移除 */
    void removeEmployeeFromGroup(String employeeId, String groupId);

    /** 注册一个新资源（门 / 设备等） */
    void registerResource(String resourceId, String name, ResourceType type);

    /** 设置资源当前状态 */
    void setResourceState(String resourceId, ResourceState state);

    /** 授权某个组访问某个资源 */
    void grantGroupAccessToResource(String groupId, String resourceId);

    /** 撤销某个组对资源的访问权限 */
    void revokeGroupAccessToResource(String groupId, String resourceId);
}
