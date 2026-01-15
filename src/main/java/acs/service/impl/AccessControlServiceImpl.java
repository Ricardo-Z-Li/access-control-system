package acs.service.impl;

import acs.domain.AccessRequest;
import acs.domain.AccessResult;
import acs.domain.ResourceDependency;
import acs.cache.LocalCacheManager;
import acs.domain.AccessDecision;
import acs.domain.Badge;
import acs.domain.BadgeStatus;
import acs.domain.BadgeUpdateStatus;
import acs.domain.Employee;
import acs.domain.LogEntry;
import acs.domain.ReasonCode;
import acs.domain.Resource;
import acs.domain.ResourceState;
import acs.domain.Group;
import acs.domain.Profile;
import acs.domain.TimeFilter;
import acs.log.LogService;
import acs.service.AccessControlService;
import acs.service.TimeFilterService;
import acs.service.AccessLimitService;
import acs.repository.ProfileRepository;
import acs.repository.ResourceDependencyRepository;
import acs.repository.AccessLogRepository;
import acs.simulator.BadgeCodeUpdateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

@Service
public class AccessControlServiceImpl implements AccessControlService {

    private final LogService logService;
    // 在类中注入LocalCacheManager
    private final LocalCacheManager cacheManager;
    private final ProfileRepository profileRepository;
    private final TimeFilterService timeFilterService;
    private final AccessLimitService accessLimitService;
    private final ResourceDependencyRepository resourceDependencyRepository;
    private final AccessLogRepository accessLogRepository;
    private final BadgeCodeUpdateService badgeCodeUpdateService;

    public AccessControlServiceImpl(
                                LogService logService,
                                LocalCacheManager cacheManager,
                                ProfileRepository profileRepository,
                                TimeFilterService timeFilterService,
                                AccessLimitService accessLimitService,
                                ResourceDependencyRepository resourceDependencyRepository,
                                AccessLogRepository accessLogRepository,
                                BadgeCodeUpdateService badgeCodeUpdateService) {
        this.logService = logService;
        this.cacheManager = cacheManager;
        this.profileRepository = profileRepository;
        this.timeFilterService = timeFilterService;
        this.accessLimitService = accessLimitService;
        this.resourceDependencyRepository = resourceDependencyRepository;
        this.accessLogRepository = accessLogRepository;
        this.badgeCodeUpdateService = badgeCodeUpdateService;
    }

    // 修改processAccess方法中的数据访问部分，使用缓存
    @Override
    @Transactional
    public AccessResult processAccess(AccessRequest request) {
        // 1. 验证请求参数
        if (request.getBadgeId() == null || request.getBadgeId().trim().isEmpty() ||
                request.getResourceId() == null || request.getResourceId().trim().isEmpty() ||
                request.getTimestamp() == null) {
            AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.INVALID_REQUEST, "Invalid access request parameters");
            recordLog(null, null, null, result, request);
            return result;
        }

        try {
            // 2. 验证徽章存在性 - 从缓存获取
            Badge badge = cacheManager.getBadge(request.getBadgeId());
            if (badge == null) {
                AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.BADGE_NOT_FOUND, "Badge not found");
                recordLog(null, null, null, result, request);
                return result;
            }

            // 3. 验证徽章状态
            if (badge.getStatus() != BadgeStatus.ACTIVE) {
                AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.BADGE_INACTIVE, "Badge is inactive (disabled or reported lost)");
                recordLog(badge, null, null, result, request);
                return result;
            }

            // 3.1 Badge expiration check
            LocalDate requestDate = LocalDate.ofInstant(request.getTimestamp(), ZoneId.systemDefault());
            if (badge.getExpirationDate() != null && requestDate.isAfter(badge.getExpirationDate())) {
                AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.BADGE_EXPIRED, "Badge expired");
                recordLog(badge, null, null, result, request);
                return result;
            }

            // 3.2 Badge code update status check
            BadgeUpdateStatus updateStatus = badgeCodeUpdateService.evaluateBadgeUpdateStatus(
                badge.getBadgeId(), request.getTimestamp());
            if (updateStatus == null) {
                updateStatus = BadgeUpdateStatus.OK;
            }
            if (updateStatus == BadgeUpdateStatus.UPDATE_OVERDUE) {
                AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.BADGE_UPDATE_OVERDUE, "Badge update window overdue");
                recordLog(badge, null, null, result, request);
                return result;
            }
            if (updateStatus == BadgeUpdateStatus.UPDATE_REQUIRED) {
                AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.BADGE_UPDATE_REQUIRED, "Badge update required");
                recordLog(badge, null, null, result, request);
                return result;
            }


            // 4. 验证员工存在性 - 从缓存获取
            Employee employee = badge.getEmployee() != null ? 
                cacheManager.getEmployee(badge.getEmployee().getEmployeeId()) : null;
            if (employee == null) {
                AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.EMPLOYEE_NOT_FOUND, "Badge not linked to a valid employee");
                recordLog(badge, null, null, result, request);
                return result;
            }

            // 5. 验证资源存在性 - 从缓存获取
            Resource resource = cacheManager.getResource(request.getResourceId());
            if (resource == null) {
                AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.RESOURCE_NOT_FOUND, "Resource not found");
                recordLog(badge, employee, null, result, request);
                return result;
            }

            // 6. 验证权限（员工所属组是否有权限访问该资源）- 仅当资源受控时检查
            if (resource.getIsControlled() != null && resource.getIsControlled()) {
                if (employee.getGroups() == null || employee.getGroups().isEmpty()) {
                    AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.NO_PERMISSION, "Employee has no assigned groups");
                    recordLog(badge, employee, resource, result, request);
                    return result;
                }
                boolean hasPermission = employee.getGroups().stream()
                        .filter(group -> group.getResources() != null)
                        .flatMap(group -> group.getResources().stream())
                        .anyMatch(r -> r.getResourceId().equals(resource.getResourceId()));

                if (!hasPermission) {
                    AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.NO_PERMISSION, "No permission to access this resource");
                    recordLog(badge, employee, resource, result, request);
                    return result;
                }
            }

            // 7. 验证资源状态（仅当资源受控时检查）
            if (resource.getIsControlled() != null && resource.getIsControlled()) {
                if (resource.getResourceState() == ResourceState.LOCKED) {
                    AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.RESOURCE_LOCKED, "Resource is locked");
                    recordLog(badge, employee, resource, result, request);
                    return result;
                }
                if (resource.getResourceState() == ResourceState.OCCUPIED) {
                    AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.RESOURCE_OCCUPIED, "Resource is occupied");
                    recordLog(badge, employee, resource, result, request);
                    return result;
                }
                if (resource.getResourceState() == ResourceState.OFFLINE || resource.getResourceState() == ResourceState.PENDING) {
                    AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.RESOURCE_LOCKED, "Resource is unavailable");
                    recordLog(badge, employee, resource, result, request);
                    return result;
                }
            }

            // 8. 时间过滤器验证（仅当资源受控时）
            if (resource.getIsControlled() != null && resource.getIsControlled()) {
                // 获取员工所属组关联的配置文件的时间过滤器
                // 收集所有激活的配置文件，按优先级排序（priorityLevel越小优先级越高）
                List<Profile> activeProfiles = getActiveProfiles(employee, badge);
                if (!activeProfiles.isEmpty()) {
                    Profile highestPriorityProfile = activeProfiles.get(0);
                    Set<TimeFilter> timeFilters = highestPriorityProfile.getTimeFilters();
                    // 如果有时间过滤器规则，则检查当前时间是否匹配
                    if (timeFilters != null && !timeFilters.isEmpty()) {
                        LocalDateTime accessTime = LocalDateTime.ofInstant(request.getTimestamp(), ZoneId.systemDefault());
                        if (!timeFilterService.matchesAny(new ArrayList<>(timeFilters), accessTime)) {
                            AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.NO_PERMISSION, "Access not allowed at this time");
                            recordLog(badge, employee, resource, result, request);
                            return result;
                        }
                    }
                    // 如果没有时间过滤器，则时间不受限制
                }
                // 如果没有激活的配置文件，则跳过时间检查
            }

            // 9. 访问次数限制检查（仅当资源受控时检查）
            if (resource.getIsControlled() != null && resource.getIsControlled()) {
                if (!accessLimitService.checkAllLimits(employee, request.getTimestamp())) {
                    AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.NO_PERMISSION, "Daily/weekly access limit exceeded");
                    recordLog(badge, employee, resource, result, request);
                    return result;
                }
                if (!accessLimitService.checkResourceLimits(employee, resource, request.getTimestamp())) {
                    AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.NO_PERMISSION, "Resource access count limit exceeded");
                    recordLog(badge, employee, resource, result, request);
                    return result;
                }

            }

            // 10. 优先级规则检查（资源依赖关系）- 仅当资源受控时检查
            if (resource.getIsControlled() != null && resource.getIsControlled()) {
                if (!checkPriorityRules(employee, resource, request.getTimestamp())) {
                    AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.NO_PERMISSION, "Prerequisite access not satisfied");
                    recordLog(badge, employee, resource, result, request);
                    return result;
                }
            }

            // 11. 所有验证通过，允许访问
            AccessResult result = new AccessResult(AccessDecision.ALLOW, ReasonCode.ALLOW, "Access allowed");
            recordLog(badge, employee, resource, result, request);
            return result;

        } catch (Exception e) {
            // 处理系统异常
            e.printStackTrace();
            AccessResult result = new AccessResult(AccessDecision.DENY, ReasonCode.SYSTEM_ERROR, "Internal system error");
            recordLog(null, null, null, result, request);
            return result;
        }
    }

    // 记录访问日志
    private void recordLog(Badge badge, Employee employee, Resource resource, AccessResult result, AccessRequest request) {
        LogEntry logEntry = new LogEntry(
                LocalDateTime.ofInstant(request.getTimestamp(), ZoneId.systemDefault()),
                badge,
                employee,
                resource,
                result.getDecision(),
                result.getReasonCode()
        );
        logService.record(logEntry);
    }
    
    // 检查优先级规则（资源依赖关系）
    private boolean checkPriorityRules(Employee employee, Resource resource, Instant timestamp) {
        // 获取该资源的所有依赖关系
        List<ResourceDependency> dependencies = resourceDependencyRepository.findByResourceResourceId(resource.getResourceId());
        if (dependencies.isEmpty()) {
            return true; // 无依赖关系，允许访问
        }
        
        LocalDateTime accessTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        
        for (ResourceDependency dependency : dependencies) {
            Resource requiredResource = dependency.getRequiredResource();
            Integer timeWindow = dependency.getTimeWindowMinutes();
            
            // 查询员工是否在指定时间窗口内访问过必需资源
            LocalDateTime startTime = timeWindow != null ? 
                accessTime.minusMinutes(timeWindow) : accessTime.minusYears(100); // 如果无时间限制，检查很长时间范围
            
            List<LogEntry> accessLogs = accessLogRepository.findByEmployeeEmployeeIdAndResourceResourceIdAndTimestampBetween(
                employee.getEmployeeId(), requiredResource.getResourceId(), startTime, accessTime);
            
            // 只计算允许的访问
            boolean hasAccess = accessLogs.stream()
                .anyMatch(log -> AccessDecision.ALLOW.equals(log.getDecision()));
                
            if (!hasAccess) {
                return false; // 缺少必需的先决访问
            }
        }
        
        return true; // 所有依赖关系满足
    }


    private List<Profile> getActiveProfiles(Employee employee, Badge badge) {
        List<Profile> activeProfiles = new ArrayList<>();
        if (employee != null && employee.getGroups() != null) {
            for (Group group : employee.getGroups()) {
                List<Profile> profiles = profileRepository.findByGroupsContaining(group);
                for (Profile profile : profiles) {
                    if (profile.getIsActive() != null && profile.getIsActive() && !activeProfiles.contains(profile)) {
                        activeProfiles.add(profile);
                    }
                }
            }
        }
        if (employee != null) {
            List<Profile> profiles = profileRepository.findByEmployeesContaining(employee);
            for (Profile profile : profiles) {
                if (profile.getIsActive() != null && profile.getIsActive() && !activeProfiles.contains(profile)) {
                    activeProfiles.add(profile);
                }
            }
        }
        if (badge != null) {
            List<Profile> profiles = profileRepository.findByBadgesContaining(badge);
            for (Profile profile : profiles) {
                if (profile.getIsActive() != null && profile.getIsActive() && !activeProfiles.contains(profile)) {
                    activeProfiles.add(profile);
                }
            }
        }
        // sort by priority (lowest number first)
        activeProfiles.sort((p1, p2) -> {
            Integer p1Level = p1.getPriorityLevel();
            Integer p2Level = p2.getPriorityLevel();
            if (p1Level == null && p2Level == null) return 0;
            if (p1Level == null) return 1;
            if (p2Level == null) return -1;
            return Integer.compare(p1Level, p2Level);
        });
        return activeProfiles;
    }
}
