package acs.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * AccessRequest 表示一次“访问请求”
 *
 * 由 UI 或 Badge Reader 创建，传递给 AccessControlService 进行权限判断。
 *
 * 一个访问请求至少需要：
 * 1. 使用哪个 Badge（badgeId）
 * 2. 访问哪个 Resource（resourceId）
 * 3. 在什么时间发生（timestamp）
 *
 * 
 * - 这是一个纯 DTO（Data Transfer Object）
 * - 不包含任何业务逻辑
 */
public class AccessRequest {

    /**
     * 唯一标识一个 Badge
     * 例如：B-10001
     */
    private String badgeId;

    /**
     * 被访问的资源 ID
     * 例如：D-3F-201（三楼 201 室的门）
     */
    private String resourceId;

    /**
     * 访问发生的时间
     * 用于：
     * - 记录日志
     * - 后续扩展时间权限控制
     */
    private Instant timestamp;

    /**
     * 构造一个访问请求
     *
     * @param badgeId    发起访问的徽章 ID
     * @param resourceId 被访问的资源 ID
     * @param timestamp  访问时间
     */

    //无参构造
    public AccessRequest() {
    }

    //Instant构造器
    public AccessRequest(String badgeId, String resourceId, Instant timestamp) {
        this.badgeId = badgeId;
        this.resourceId = resourceId;
        this.timestamp = timestamp;
    }

    // localDateTime构造器
    public AccessRequest(String badgeId, String resourceId, LocalDateTime localDateTime) {
        this.badgeId = badgeId;
        this.resourceId = resourceId;
        // Convert LocalDateTime to Instant using a time zone (e.g., UTC)
        this.timestamp = localDateTime.atZone(ZoneId.of("UTC")).toInstant();
    }

    public String getBadgeId() {
        return badgeId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setBadgeId(String badgeId) {
        this.badgeId = badgeId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

}
