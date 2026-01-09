package acs.service.impl;

import acs.service.AccessLimitService;
import acs.domain.Employee;
import acs.domain.Profile;
import acs.domain.Resource;
import acs.domain.LogEntry;
import acs.domain.AccessDecision;
import acs.repository.AccessLogRepository;
import acs.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

/**
 * 访问次数限制服务实现
 * 基于数据库日志统计每日/每周访问次数
 */
@Service
public class AccessLimitServiceImpl implements AccessLimitService {

    private final AccessLogRepository accessLogRepository;
    private final ProfileRepository profileRepository;

    @Autowired
    public AccessLimitServiceImpl(AccessLogRepository accessLogRepository, ProfileRepository profileRepository) {
        this.accessLogRepository = accessLogRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    public boolean checkDailyLimit(Employee employee, Profile profile) {
        if (profile == null || profile.getMaxDailyAccess() == null || profile.getMaxDailyAccess() <= 0) {
            // 无限制
            return true;
        }
        
        int todayCount = getTodayAccessCount(employee);
        return todayCount < profile.getMaxDailyAccess();
    }

    @Override
    public boolean checkWeeklyLimit(Employee employee, Profile profile) {
        if (profile == null || profile.getMaxWeeklyAccess() == null || profile.getMaxWeeklyAccess() <= 0) {
            // 无限制
            return true;
        }
        
        int weekCount = getWeekAccessCount(employee);
        return weekCount < profile.getMaxWeeklyAccess();
    }

    @Override
    public void recordAccess(Employee employee, Resource resource, Instant timestamp) {
        // 访问记录已由日志服务记录，此处无需额外操作
        // 此方法保留用于未来扩展（如缓存计数）
    }

    @Override
    public int getTodayAccessCount(Employee employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        
        // 统计员工今日的允许访问日志
        List<LogEntry> logs = accessLogRepository.findByEmployeeEmployeeIdAndTimestampBetween(
            employee.getEmployeeId(), startOfDay, endOfDay);
        
        // 只计算决策为ALLOW的访问
        return (int) logs.stream()
            .filter(log -> AccessDecision.ALLOW.name().equals(log.getDecision()))
            .count();
    }

    @Override
    public int getWeekAccessCount(Employee employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        // 获取本周的第一天（周一）
        LocalDateTime startOfWeek = now.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
            .toLocalDate().atStartOfDay();
        // 获取本周的最后一天（周日）
        LocalDateTime endOfWeek = startOfWeek.plusDays(7).minusNanos(1);
        
        List<LogEntry> logs = accessLogRepository.findByEmployeeEmployeeIdAndTimestampBetween(
            employee.getEmployeeId(), startOfWeek, endOfWeek);
        
        // 只计算决策为ALLOW的访问
        return (int) logs.stream()
            .filter(log -> AccessDecision.ALLOW.name().equals(log.getDecision()))
            .count();
    }
    
    /**
     * 检查员工是否超过其所属配置文件的访问限制
     * @param employee 员工
     * @return true 如果未超过任何限制，false 如果超过任一限制
     */
    public boolean checkAllLimits(Employee employee) {
        if (employee == null || employee.getGroups() == null) {
            return true;
        }
        
        // 收集员工所属组关联的所有配置文件（去重）
        Set<Profile> profileSet = new HashSet<>();
        for (acs.domain.Group group : employee.getGroups()) {
            List<Profile> profiles = profileRepository.findByGroupsContaining(group);
            for (Profile profile : profiles) {
                if (profile.getIsActive() != null && profile.getIsActive()) {
                    profileSet.add(profile);
                }
            }
        }
        
        if (profileSet.isEmpty()) {
            return true;
        }
        
        // 检查每个配置文件的限制，取最严格的限制
        for (Profile profile : profileSet) {
            if (!checkDailyLimit(employee, profile)) {
                return false;
            }
            if (!checkWeeklyLimit(employee, profile)) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public boolean checkAllLimits(Employee employee, Instant timestamp) {
        // 暂时委托给无时间戳版本（保持现有行为）
        return checkAllLimits(employee);
    }
}