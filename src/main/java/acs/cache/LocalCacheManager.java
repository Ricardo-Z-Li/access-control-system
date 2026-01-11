package acs.cache;

import acs.domain.Badge;
import acs.domain.Employee;
import acs.domain.Group;
import acs.domain.Resource;
import acs.domain.LogEntry;
import acs.repository.BadgeRepository;
import acs.repository.EmployeeRepository;
import acs.repository.GroupRepository;
import acs.repository.ResourceRepository;
import acs.repository.AccessLogRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class LocalCacheManager {

    // 缓存存储结构
    private final Map<String, Badge> badgeCache = new ConcurrentHashMap<>();
    private final Map<String, Employee> employeeCache = new ConcurrentHashMap<>();
    private final Map<String, Group> groupCache = new ConcurrentHashMap<>();
    private final Map<String, Resource> resourceCache = new ConcurrentHashMap<>();
    private final List<LogEntry> logCache = new CopyOnWriteArrayList<>();  // 日志缓存（有序列表）

    // 缓存性能统计
    private final AtomicLong badgeCacheHits = new AtomicLong(0);
    private final AtomicLong badgeCacheMisses = new AtomicLong(0);
    private final AtomicLong employeeCacheHits = new AtomicLong(0);
    private final AtomicLong employeeCacheMisses = new AtomicLong(0);
    private final AtomicLong groupCacheHits = new AtomicLong(0);
    private final AtomicLong groupCacheMisses = new AtomicLong(0);
    private final AtomicLong resourceCacheHits = new AtomicLong(0);
    private final AtomicLong resourceCacheMisses = new AtomicLong(0);

    // 依赖的Repository
    private final BadgeRepository badgeRepository;
    private final EmployeeRepository employeeRepository;
    private final GroupRepository groupRepository;
    private final ResourceRepository resourceRepository;
    // 注入日志Repository
    private final AccessLogRepository accessLogRepository;

    public LocalCacheManager(BadgeRepository badgeRepository,
                            EmployeeRepository employeeRepository,
                            GroupRepository groupRepository,
                            ResourceRepository resourceRepository,
                            AccessLogRepository accessLogRepository) {
        this.badgeRepository = badgeRepository;
        this.employeeRepository = employeeRepository;
        this.groupRepository = groupRepository;
        this.resourceRepository = resourceRepository;
        this.accessLogRepository = accessLogRepository; // 初始化日志Repository
    }

    // 初始化缓存，应用启动时执行
    @PostConstruct
    public void initCache() {
        loadBadges();
        loadEmployees();
        loadGroups();
        loadResources();
        loadLogs(); 
        // 日志输出
        System.out.println("缓存初始化完成 - 徽章数: " + badgeCache.size() 
            + ", 员工数: " + employeeCache.size()
            + ", 组数: " + groupCache.size()
            + ", 资源数: " + resourceCache.size()
            + ", 日志数: " + logCache.size());
    }

    // 从数据库加载所有徽章到缓存
    private void loadBadges() {
        badgeCache.clear();
        badgeRepository.findAll().forEach(badge -> badgeCache.put(badge.getBadgeId(), badge));
    }

    // 从数据库加载所有员工到缓存（包含组和资源关联）
    private void loadEmployees() {
        employeeCache.clear();
        employeeRepository.findAllWithGroupsAndResources().forEach(employee -> employeeCache.put(employee.getEmployeeId(), employee));
    }

    // 从数据库加载所有组到缓存（包含资源关联）
    private void loadGroups() {
        groupCache.clear();
        groupRepository.findAllWithResources().forEach(group -> groupCache.put(group.getGroupId(), group));
    }

    // 从数据库加载所有资源到缓存
    private void loadResources() {
        resourceCache.clear();
        resourceRepository.findAll().forEach(resource -> resourceCache.put(resource.getResourceId(), resource));
    }

    // 从数据库加载所有日志到本地缓存
    private void loadLogs() {
        logCache.clear();
        // 从数据库查询所有日志，按创建时间排序后存入缓存
        List<LogEntry> allLogs = accessLogRepository.findAll();
        List<LogEntry> sortedLogs = allLogs.stream()
                .sorted(Comparator.comparing(LogEntry::getTimestamp))  // 按时间升序（从早到晚）
                .collect(Collectors.toList());
        logCache.addAll(sortedLogs);
    }

    // 缓存操作方法
    public Badge getBadge(String badgeId) {
        Badge badge = badgeCache.get(badgeId);
        if (badge != null) {
            badgeCacheHits.incrementAndGet();
        } else {
            badgeCacheMisses.incrementAndGet();
        }
        return badge;
    }

    public Employee getEmployee(String employeeId) {
        Employee employee = employeeCache.get(employeeId);
        if (employee != null) {
            employeeCacheHits.incrementAndGet();
        } else {
            employeeCacheMisses.incrementAndGet();
        }
        return employee;
    }

    public Group getGroup(String groupId) {
        Group group = groupCache.get(groupId);
        if (group != null) {
            groupCacheHits.incrementAndGet();
        } else {
            groupCacheMisses.incrementAndGet();
        }
        return group;
    }

    public Resource getResource(String resourceId) {
        Resource resource = resourceCache.get(resourceId);
        if (resource != null) {
            resourceCacheHits.incrementAndGet();
        } else {
            resourceCacheMisses.incrementAndGet();
        }
        return resource;
    }

    // 获取有序日志列表（返回不可修改集合，防止外部篡改顺序）
    public List<LogEntry> getLogs() {
        return Collections.unmodifiableList(logCache);
    }

    // 更新缓存中的徽章
    @Transactional
    public void updateBadge(Badge badge) {
        badgeRepository.save(badge);
        badgeCache.put(badge.getBadgeId(), badge);
    }

    // 更新缓存中的员工
    @Transactional
    public void updateEmployee(Employee employee) {
        if (employee.getEmployeeId() == null) {
            System.err.println("错误：尝试更新没有ID的Employee对象");
            System.err.println("Employee对象详情: " + employee);
            System.err.println("Employee名称: " + employee.getEmployeeName());

            System.err.println("关联的Badge: " + (employee.getBadge() != null ? employee.getBadge().getBadgeId() : "null"));
            
            // 打印调用堆栈以确定问题来源
            System.err.println("调用堆栈:");
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                System.err.println("    " + element);
            }
            
            throw new IllegalArgumentException("Employee ID不能为null");
        }
        employeeRepository.save(employee);
        employeeCache.put(employee.getEmployeeId(), employee);
    }

    // 更新缓存中的组
    @Transactional
    public void updateGroup(Group group) {
        groupRepository.save(group);
        groupCache.put(group.getGroupId(), group);
    }

    // 更新缓存中的资源
    @Transactional
    public void updateResource(Resource resource) {
        resourceRepository.save(resource);
        resourceCache.put(resource.getResourceId(), resource);
    }

    // 更新日志缓存（新增或修改日志后重新排序）
    @Transactional
    public void updateLog(LogEntry log) {
        // 先同步到数据库
        accessLogRepository.save(log);
        // 更新缓存：先移除旧记录（若存在）
        logCache.removeIf(existingLog -> existingLog.getId().equals(log.getId()));
        // 添加新记录并重新排序
        logCache.add(log);
        logCache.sort(Comparator.comparing(LogEntry::getTimestamp));  // 保持有序
    }

    // 从缓存中删除徽章
    @Transactional
    public void removeBadge(String badgeId) {
        badgeRepository.deleteById(badgeId);
        badgeCache.remove(badgeId);
    }

    // 从缓存中删除员工
    @Transactional
    public void removeEmployee(String employeeId) {
        employeeRepository.deleteById(employeeId);
        employeeCache.remove(employeeId);
    }

    // 从缓存中删除组
    @Transactional
    public void removeGroup(String groupId) {
        groupRepository.deleteById(groupId);
        groupCache.remove(groupId);
    }

    // 从缓存中删除资源
    @Transactional
    public void removeResource(String resourceId) {
        resourceRepository.deleteById(resourceId);
        resourceCache.remove(resourceId);
    }

    // 从缓存中删除日志
    @Transactional
    public void removeLog(Long logId) {
        accessLogRepository.deleteById(logId);
        logCache.removeIf(log -> log.getId().equals(logId));
    }

    @Transactional 
    // 清理缓存中过期的日志（7天前）
    public int clearExpiredLogs(LocalDateTime sevenDaysAgo) {
        // 收集需要删除的日志ID
        List<Long> idsToDelete = new ArrayList<>();
        for (LogEntry log : logCache) {
            if (log.getTimestamp().isBefore(sevenDaysAgo)) {
                idsToDelete.add(log.getId());
            }
        }
        
        // 如果没有任何日志需要删除，直接返回0
        if (idsToDelete.isEmpty()) {
            return 0;
        }
        
        // 先删除数据库中的过期日志
        accessLogRepository.deleteByTimestampBefore(sevenDaysAgo);
        
        // 从缓存中移除已删除的日志
        logCache.removeIf(log -> idsToDelete.contains(log.getId()));
        
        // 返回实际从缓存中删除的数量
        return idsToDelete.size();
    }

    // 强制刷新所有缓存（从数据库重新加载）
    public void refreshAllCache() {
        loadBadges();
        loadEmployees();
        loadGroups();
        loadResources();
        loadLogs();
    }

    // 获取缓存性能统计信息
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long badgeHits = badgeCacheHits.get();
        long badgeMisses = badgeCacheMisses.get();
        long employeeHits = employeeCacheHits.get();
        long employeeMisses = employeeCacheMisses.get();
        long groupHits = groupCacheHits.get();
        long groupMisses = groupCacheMisses.get();
        long resourceHits = resourceCacheHits.get();
        long resourceMisses = resourceCacheMisses.get();
        
        stats.put("badgeCacheHits", badgeHits);
        stats.put("badgeCacheMisses", badgeMisses);
        stats.put("badgeCacheHitRate", badgeHits + badgeMisses == 0 ? 0.0 : (double) badgeHits / (badgeHits + badgeMisses));
        stats.put("employeeCacheHits", employeeHits);
        stats.put("employeeCacheMisses", employeeMisses);
        stats.put("employeeCacheHitRate", employeeHits + employeeMisses == 0 ? 0.0 : (double) employeeHits / (employeeHits + employeeMisses));
        stats.put("groupCacheHits", groupHits);
        stats.put("groupCacheMisses", groupMisses);
        stats.put("groupCacheHitRate", groupHits + groupMisses == 0 ? 0.0 : (double) groupHits / (groupHits + groupMisses));
        stats.put("resourceCacheHits", resourceHits);
        stats.put("resourceCacheMisses", resourceMisses);
        stats.put("resourceCacheHitRate", resourceHits + resourceMisses == 0 ? 0.0 : (double) resourceHits / (resourceHits + resourceMisses));
        
        stats.put("badgeCacheSize", badgeCache.size());
        stats.put("employeeCacheSize", employeeCache.size());
        stats.put("groupCacheSize", groupCache.size());
        stats.put("resourceCacheSize", resourceCache.size());
        stats.put("logCacheSize", logCache.size());
        
        return stats;
    }
}