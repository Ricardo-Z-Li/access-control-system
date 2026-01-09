package acs.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * BadgeReader 表示物理读卡器设备，用于读取徽章并控制资源访问。
 *
 * 读卡器与资源关联：一个读卡器可以控制一个或多个资源。
 * 设计原则：
 * - readerId 是读卡器的唯一标识符
 * - resourceId 指向读卡器控制的资源（可以为空，表示未分配）
 * - status 表示读卡器状态（ONLINE/OFFLINE/MAINTENANCE）
 * - lastSeen 记录最后一次通信时间
 */
@Entity
@Table(name = "badge_readers")
public class BadgeReader {

    @Id
    @Column(name = "reader_id", nullable = false, length = 50)
    private String readerId;

    @Column(name = "reader_name", nullable = false, length = 100)
    private String readerName;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "resource_id", length = 50)
    private String resourceId;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "badge_code_length")
    private Integer badgeCodeLength;

    // 无参构造器（JPA必需）
    public BadgeReader() {}

    // 全参构造器
    public BadgeReader(String readerId, String readerName, String location, String status, String resourceId) {
        this.readerId = readerId;
        this.readerName = readerName;
        this.location = location;
        this.status = status;
        this.resourceId = resourceId;
        this.lastSeen = Instant.now();
    }

    // Getter和Setter
    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }

    public String getReaderName() {
        return readerName;
    }

    public void setReaderName(String readerName) {
        this.readerName = readerName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Integer getBadgeCodeLength() {
        return badgeCodeLength;
    }

    public void setBadgeCodeLength(Integer badgeCodeLength) {
        this.badgeCodeLength = badgeCodeLength;
    }
}