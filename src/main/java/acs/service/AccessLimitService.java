package acs.service;

import acs.domain.Employee;
import acs.domain.Profile;
import acs.domain.Resource;

import java.time.Instant;

/**
 * 访问次数限制服务接口
 * 负责检查每日/每周访问次数限制
 */
public interface AccessLimitService {
    
    /**
     * 检查员工是否超过每日访问限制
     * @param employee 员工
     * @param profile 配置文件（包含限制规则）
     * @return true 如果未超过限制，false 如果超过限制
     */
    boolean checkDailyLimit(Employee employee, Profile profile);
    
    /**
     * 检查员工是否超过每周访问限制
     * @param employee 员工
     * @param profile 配置文件（包含限制规则）
     * @return true 如果未超过限制，false 如果超过限制
     */
    boolean checkWeeklyLimit(Employee employee, Profile profile);
    
    /**
     * 记录一次访问（用于计数）
     * @param employee 员工
     * @param resource 资源
     * @param timestamp 访问时间
     */
    void recordAccess(Employee employee, Resource resource, Instant timestamp);
    
    /**
     * 获取员工今日已访问次数
     * @param employee 员工
     * @return 今日访问次数
     */
    int getTodayAccessCount(Employee employee);
    
    /**
     * 获取员工本周已访问次数
     * @param employee 员工
     * @return 本周访问次数
     */
    int getWeekAccessCount(Employee employee);
    
    /**
     * 检查员工是否超过其所属配置文件的访问限制
     * @param employee 员工
     * @return true 如果未超过任何限制，false 如果超过任一限制
     */
    boolean checkAllLimits(Employee employee);
    
    /**
     * 检查员工是否超过其所属配置文件的访问限制（基于指定时间戳）
     * @param employee 员工
     * @param timestamp 参考时间戳
     * @return true 如果未超过任何限制，false 如果超过任一限制
     */
    boolean checkAllLimits(Employee employee, Instant timestamp);
}