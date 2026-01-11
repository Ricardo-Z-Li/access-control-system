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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
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
    @Transactional(readOnly = true)
    public int getTodayAccessCount(Employee employee) {
        return getTodayAccessCount(employee, Instant.now());
    }


    @Override
    @Transactional(readOnly = true)
    public int getWeekAccessCount(Employee employee) {
        return getWeekAccessCount(employee, Instant.now());
    }

    /**
     * 根据指定时间戳获取员工今日已访问次数
     */
    private int getTodayAccessCount(Employee employee, Instant timestamp) {
        if (employee == null || employee.getEmployeeId() == null || timestamp == null) {
            return 0;
        }
        
        LocalDateTime accessTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        LocalDateTime startOfDay = accessTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        
        List<LogEntry> logs = accessLogRepository.findByEmployeeEmployeeIdAndTimestampBetween(
            employee.getEmployeeId(), startOfDay, endOfDay);
        
        return (int) logs.stream()
            .filter(log -> AccessDecision.ALLOW.equals(log.getDecision()))
            .count();
    }

    /**
     * 根据指定时间戳获取员工本周已访问次数
     */
    private int getWeekAccessCount(Employee employee, Instant timestamp) {
        if (employee == null || employee.getEmployeeId() == null || timestamp == null) {
            return 0;
        }
        
        LocalDateTime accessTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        LocalDateTime startOfWeek = accessTime.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
            .toLocalDate().atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusDays(7).minusNanos(1);
        
        List<LogEntry> logs = accessLogRepository.findByEmployeeEmployeeIdAndTimestampBetween(
            employee.getEmployeeId(), startOfWeek, endOfWeek);
        
        return (int) logs.stream()
            .filter(log -> AccessDecision.ALLOW.equals(log.getDecision()))
            .count();
    }

    /**
     * 检查员工是否超过其所属配置文件的访问限制
     * @param employee 员工
     * @return true 如果未超过任何限制，false 如果超过任一限制
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public boolean checkAllLimits(Employee employee, Instant timestamp) {
        if (employee == null || employee.getGroups() == null || timestamp == null) {
            return true;
        }
        
        // 收集员工所属组关联的所有激活配置文件（去重）
        List<Profile> activeProfiles = new ArrayList<>();
        for (acs.domain.Group group : employee.getGroups()) {
            List<Profile> profiles = profileRepository.findByGroupsContaining(group);
            for (Profile profile : profiles) {
                if (profile.getIsActive() != null && profile.getIsActive()) {
                    // 去重
                    if (!activeProfiles.contains(profile)) {
                        activeProfiles.add(profile);
                    }
                }
            }
        }
        
        if (activeProfiles.isEmpty()) {
            return true;
        }
        
        // 按优先级排序（priorityLevel越小优先级越高）
        activeProfiles.sort((p1, p2) -> {
            Integer p1Level = p1.getPriorityLevel();
            Integer p2Level = p2.getPriorityLevel();
            if (p1Level == null && p2Level == null) return 0;
            if (p1Level == null) return 1; // null排后面
            if (p2Level == null) return -1;
            return Integer.compare(p1Level, p2Level);
        });
        
        // 选择最高优先级的配置文件（可能有多个相同优先级）
        Profile highestPriorityProfile = activeProfiles.get(0);
        // 检查限制（仅检查最高优先级配置文件）
        if (highestPriorityProfile.getMaxDailyAccess() != null && highestPriorityProfile.getMaxDailyAccess() > 0) {
            int todayCount = getTodayAccessCount(employee, timestamp);
            if (todayCount >= highestPriorityProfile.getMaxDailyAccess()) {
                return false;
            }
        }
        if (highestPriorityProfile.getMaxWeeklyAccess() != null && highestPriorityProfile.getMaxWeeklyAccess() > 0) {
            int weekCount = getWeekAccessCount(employee, timestamp);
            if (weekCount >= highestPriorityProfile.getMaxWeeklyAccess()) {
                return false;
            }
        }
        
        return true;
    }
}