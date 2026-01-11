package acs.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 系统时钟服务，支持获取当前时间和设置模拟时间。
 * 用于测试日期/时间相关的访问限制。
 */
public interface ClockService {

    /**
     * 获取当前时间作为Instant。
     * @return 当前时间（可能是模拟时间）
     */
    Instant now();

    /**
     * 获取当前时间作为LocalDateTime（使用系统默认时区）。
     * @return 当前日期时间
     */
    LocalDateTime localNow();

    /**
     * 设置模拟时间（覆盖系统时钟）。
     * @param simulatedTime 要设置的模拟时间，为null时恢复为真实系统时间
     */
    void setSimulatedTime(Instant simulatedTime);

    /**
     * 设置模拟时间（使用LocalDateTime）。
     * @param simulatedDateTime 要设置的模拟日期时间
     */
    void setSimulatedTime(LocalDateTime simulatedDateTime);

    /**
     * 获取当前模拟时间（如果设置了模拟时间），否则返回null。
     * @return 当前模拟时间或null
     */
    Instant getSimulatedTime();

    /**
     * 检查当前是否使用模拟时间。
     * @return 如果正在使用模拟时间返回true
     */
    boolean isSimulated();

    /**
     * 重置为真实系统时间。
     */
    void resetToRealTime();
}