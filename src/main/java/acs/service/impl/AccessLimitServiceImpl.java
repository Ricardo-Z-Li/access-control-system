package acs.service.impl;

import acs.service.AccessLimitService;
import acs.domain.Employee;
import acs.domain.Profile;
import acs.domain.ProfileResourceLimit;
import acs.domain.Resource;
import acs.domain.LogEntry;
import acs.domain.AccessDecision;
import acs.repository.AccessLogRepository;
import acs.repository.ProfileRepository;
import acs.repository.ProfileResourceLimitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 访问次数限制服务实现
 * 基于数据库日志统计每日/每周访问次数
 */
@Service
public class AccessLimitServiceImpl implements AccessLimitService {

    private final AccessLogRepository accessLogRepository;
    private final ProfileRepository profileRepository;
    private final ProfileResourceLimitRepository profileResourceLimitRepository;

    @Autowired
    public AccessLimitServiceImpl(AccessLogRepository accessLogRepository,
                                  ProfileRepository profileRepository,
                                  ProfileResourceLimitRepository profileResourceLimitRepository) {
        this.accessLogRepository = accessLogRepository;
        this.profileRepository = profileRepository;
        this.profileResourceLimitRepository = profileResourceLimitRepository;
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

    @Override
    @Transactional(readOnly = true)
    public int getTodayAccessCount(Employee employee, Resource resource) {
        return getTodayAccessCount(employee, resource, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public int getWeekAccessCount(Employee employee, Resource resource) {
        return getWeekAccessCount(employee, resource, Instant.now());
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
        return checkAllLimits(employee, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkAllLimits(Employee employee, Instant timestamp) {
        if (employee == null || timestamp == null) {
            return true;
        }
        
        // 收集员工所属组关联的所有激活配置文件（去重）
        List<Profile> activeProfiles = getActiveProfiles(employee);
        
        if (activeProfiles.isEmpty()) {
            return true;
        }
        
        Profile highestPriorityProfile = getHighestPriorityProfile(activeProfiles);
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


    @Override
    @Transactional(readOnly = true)
    public boolean checkResourceLimits(Employee employee, Resource resource, Instant timestamp) {
        if (employee == null || resource == null || timestamp == null) {
            return true;
        }
        List<Profile> activeProfiles = getActiveProfiles(employee);
        if (activeProfiles.isEmpty()) {
            return true;
        }

        Profile highestPriorityProfile = getHighestPriorityProfile(activeProfiles);
        if (highestPriorityProfile == null) {
            return true;
        }

        List<ProfileResourceLimit> limits = profileResourceLimitRepository
                .findByProfileAndIsActiveTrue(highestPriorityProfile);
        if (limits.isEmpty()) {
            return true;
        }

        int todayCount = -1;
        int weekCount = -1;

        for (ProfileResourceLimit limit : limits) {
            if (limit == null || limit.getResource() == null) {
                continue;
            }
            if (!resource.getResourceId().equals(limit.getResource().getResourceId())) {
                continue;
            }

            Integer dailyLimit = limit.getDailyLimit();
            if (dailyLimit != null && dailyLimit > 0) {
                if (todayCount < 0) {
                    todayCount = getTodayAccessCount(employee, resource, timestamp);
                }
                if (todayCount >= dailyLimit) {
                    return false;
                }
            }

            Integer weeklyLimit = limit.getWeeklyLimit();
            if (weeklyLimit != null && weeklyLimit > 0) {
                if (weekCount < 0) {
                    weekCount = getWeekAccessCount(employee, resource, timestamp);
                }
                if (weekCount >= weeklyLimit) {
                    return false;
                }
            }
        }

        return true;
    }

    private List<Profile> getActiveProfiles(Employee employee) {
        List<Profile> activeProfiles = new ArrayList<>();
        if (employee == null) {
            return activeProfiles;
        }
        if (employee.getGroups() != null) {
            for (acs.domain.Group group : employee.getGroups()) {
                List<Profile> profiles = profileRepository.findByGroupsContaining(group);
                for (Profile profile : profiles) {
                    if (profile.getIsActive() != null && profile.getIsActive()) {
                        if (!activeProfiles.contains(profile)) {
                            activeProfiles.add(profile);
                        }
                    }
                }
            }
        }
        List<Profile> employeeProfiles = profileRepository.findByEmployeesContaining(employee);
        for (Profile profile : employeeProfiles) {
            if (profile.getIsActive() != null && profile.getIsActive() && !activeProfiles.contains(profile)) {
                activeProfiles.add(profile);
            }
        }
        if (employee.getBadge() != null) {
            List<Profile> badgeProfiles = profileRepository.findByBadgesContaining(employee.getBadge());
            for (Profile profile : badgeProfiles) {
                if (profile.getIsActive() != null && profile.getIsActive() && !activeProfiles.contains(profile)) {
                    activeProfiles.add(profile);
                }
            }
        }
        return activeProfiles;
    }

    private Profile getHighestPriorityProfile(List<Profile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return null;
        }
        profiles.sort((p1, p2) -> {
            Integer p1Level = p1.getPriorityLevel();
            Integer p2Level = p2.getPriorityLevel();
            if (p1Level == null && p2Level == null) return 0;
            if (p1Level == null) return 1;
            if (p2Level == null) return -1;
            return Integer.compare(p1Level, p2Level);
        });
        return profiles.get(0);
    }

    private int getTodayAccessCount(Employee employee, Resource resource, Instant timestamp) {
        if (employee == null || resource == null || timestamp == null) {
            return 0;
        }
        LocalDateTime accessTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        LocalDateTime startOfDay = accessTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        List<LogEntry> logs = accessLogRepository.findByEmployeeEmployeeIdAndResourceResourceIdAndTimestampBetween(
                employee.getEmployeeId(), resource.getResourceId(), startOfDay, endOfDay);

        return (int) logs.stream()
                .filter(log -> AccessDecision.ALLOW.equals(log.getDecision()))
                .count();
    }

    private int getWeekAccessCount(Employee employee, Resource resource, Instant timestamp) {
        if (employee == null || resource == null || timestamp == null) {
            return 0;
        }

        LocalDateTime accessTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        LocalDateTime startOfWeek = accessTime.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                .toLocalDate().atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusDays(7).minusNanos(1);

        List<LogEntry> logs = accessLogRepository.findByEmployeeEmployeeIdAndResourceResourceIdAndTimestampBetween(
                employee.getEmployeeId(), resource.getResourceId(), startOfWeek, endOfWeek);

        return (int) logs.stream()
                .filter(log -> AccessDecision.ALLOW.equals(log.getDecision()))
                .count();
    }

    

    
}
