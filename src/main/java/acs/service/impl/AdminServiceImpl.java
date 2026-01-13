package acs.service.impl;

import acs.cache.LocalCacheManager;
import acs.domain.Badge;
import acs.domain.BadgeStatus;
import acs.domain.Employee;
import acs.domain.Group;
import acs.domain.Profile;
import acs.domain.Resource;
import acs.domain.ResourceState;
import acs.domain.ResourceType;
import acs.repository.BadgeRepository;
import acs.repository.EmployeeRepository;
import acs.repository.GroupRepository;
import acs.repository.ProfileRepository;
import acs.repository.ResourceRepository;
import acs.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminServiceImpl implements AdminService {

    // 注入LocalCacheManager
    private final LocalCacheManager cacheManager;
    private final EmployeeRepository employeeRepository;
    private final BadgeRepository badgeRepository;
    private final GroupRepository groupRepository;
    private final ResourceRepository resourceRepository;
    private final ProfileRepository profileRepository;

    public AdminServiceImpl(EmployeeRepository employeeRepository,
                            BadgeRepository badgeRepository,
                            GroupRepository groupRepository,
                            ResourceRepository resourceRepository,
                            ProfileRepository profileRepository,
                            LocalCacheManager cacheManager) {
        this.employeeRepository = employeeRepository;
        this.badgeRepository = badgeRepository;
        this.groupRepository = groupRepository;
        this.resourceRepository = resourceRepository;
        this.profileRepository = profileRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    public void registerEmployee(String employeeId, String name) {
        if (employeeRepository.existsById(employeeId)) {
            throw new IllegalStateException("员工ID已存在: " + employeeId);
        }
        Employee employee = new Employee(employeeId, name);
        employeeRepository.save(employee);
        // 同步缓存
        cacheManager.updateEmployee(employee);
    }

    @Override
    @Transactional
    public void issueBadge(String employeeId, String badgeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new IllegalArgumentException("员工ID不能为空");
        }
        if (badgeId == null || badgeId.trim().isEmpty()) {
            throw new IllegalArgumentException("徽章ID不能为空");
        }
        
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("员工不存在: " + employeeId));
        
        // 验证employee对象是否有有效的ID
        if (employee.getEmployeeId() == null) {
            throw new IllegalStateException("从数据库获取的员工对象ID为null: " + employeeId);
        }
        
        if (badgeRepository.existsById(badgeId)) {
            throw new IllegalStateException("徽章ID已存在: " + badgeId);
        }
        
        // 创建Badge并建立双向关系
        Badge badge = new Badge(badgeId, BadgeStatus.ACTIVE);
        
        // 设置双向关联（Employee是拥有方，有@JoinColumn）
        badge.setEmployee(employee);
        employee.setBadge(badge);
        
        // 保存Employee，级联操作会自动保存Badge
        employeeRepository.save(employee);
        
        // 重新从数据库加载以保证关联正确建立
        Badge persistedBadge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new IllegalStateException("保存后无法找到徽章: " + badgeId));
        Employee persistedEmployee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalStateException("保存后无法找到员工: " + employeeId));
        
        // 同步缓存
        cacheManager.updateBadge(persistedBadge);
        cacheManager.updateEmployee(persistedEmployee);
    }

    @Override
    @Transactional
    public void setBadgeStatus(String badgeId, BadgeStatus status) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new IllegalArgumentException("徽章不存在: " + badgeId));
        badge.setStatus(status);
        badgeRepository.save(badge);
        // 同步缓存
        cacheManager.updateBadge(badge);
    }

    @Override
    @Transactional
    public void createGroup(String groupId, String groupName) {
        if (groupRepository.existsById(groupId)) {
            throw new IllegalStateException("组ID已存在: " + groupId);
        }
        Group group = new Group(groupId, groupName);
        groupRepository.save(group);
        // 同步缓存
        cacheManager.updateGroup(group);
    }

    @Override
    @Transactional
    public void assignEmployeeToGroup(String employeeId, String groupId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("员工不存在: " + employeeId));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("组不存在: " + groupId));
        
        employee.getGroups().add(group);
        group.getEmployees().add(employee);
        
        employeeRepository.save(employee);
        groupRepository.save(group);
        // 同步缓存
        cacheManager.updateEmployee(employee);
        cacheManager.updateGroup(group);
    }

    @Override
    @Transactional
    public void removeEmployeeFromGroup(String employeeId, String groupId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("员工不存在: " + employeeId));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("组不存在: " + groupId));
        
        employee.getGroups().remove(group);
        group.getEmployees().remove(employee);
        
        employeeRepository.save(employee);
        groupRepository.save(group);
        // 同步缓存
        cacheManager.updateEmployee(employee);
        cacheManager.updateGroup(group);
    }

    @Override
    @Transactional
    public void registerResource(String resourceId, String name, ResourceType type) {
        if (resourceRepository.existsById(resourceId)) {
            throw new IllegalStateException("资源ID已存在: " + resourceId);
        }
        // 新资源默认状态为可用
        Resource resource = new Resource(resourceId, name, type, ResourceState.AVAILABLE);
        resourceRepository.save(resource);
        // 同步缓存
        cacheManager.updateResource(resource);
    }

    @Override
    @Transactional
    public void setResourceState(String resourceId, ResourceState state) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("资源不存在: " + resourceId));
        resource.setResourceState(state);
        resourceRepository.save(resource);
        // 同步缓存
        cacheManager.updateResource(resource);
    }
    @Override
    @Transactional
    public void updateResourceLocation(String resourceId, String building, String floor,
                                       Integer coordX, Integer coordY, String location) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceId));
        resource.setBuilding(building);
        resource.setFloor(floor);
        resource.setCoordX(coordX);
        resource.setCoordY(coordY);
        resource.setLocation(location);
        resourceRepository.save(resource);
        cacheManager.updateResource(resource);
    }


    @Override
    @Transactional
    public void grantGroupAccessToResource(String groupId, String resourceId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("组不存在: " + groupId));
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("资源不存在: " + resourceId));
        
        group.getResources().add(resource);
        resource.getGroups().add(group);
        
        groupRepository.save(group);
        resourceRepository.save(resource);
        // 同步缓存
        cacheManager.updateGroup(group);
        cacheManager.updateResource(resource);
    }

    @Override
    @Transactional
    public void revokeGroupAccessToResource(String groupId, String resourceId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("组不存在: " + groupId));
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("资源不存在: " + resourceId));
        
        group.getResources().remove(resource);
        resource.getGroups().remove(group);
        
        groupRepository.save(group);
        resourceRepository.save(resource);
        // 同步缓存
        cacheManager.updateGroup(group);
        cacheManager.updateResource(resource);
    }


    @Override
    @Transactional
    public void assignProfileToEmployee(String profileId, String employeeId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        profile.getEmployees().add(employee);
        profileRepository.save(profile);
        cacheManager.refreshAllCache();
    }

    @Override
    @Transactional
    public void removeProfileFromEmployee(String profileId, String employeeId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        profile.getEmployees().remove(employee);
        profileRepository.save(profile);
        cacheManager.refreshAllCache();
    }

    @Override
    @Transactional
    public void assignProfileToBadge(String profileId, String badgeId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new IllegalArgumentException("Badge not found: " + badgeId));

        profile.getBadges().add(badge);
        profileRepository.save(profile);
        cacheManager.refreshAllCache();
    }

    @Override
    @Transactional
    public void removeProfileFromBadge(String profileId, String badgeId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new IllegalArgumentException("Badge not found: " + badgeId));

        profile.getBadges().remove(badge);
        profileRepository.save(profile);
        cacheManager.refreshAllCache();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Profile> getProfilesForEmployee(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        return profileRepository.findByEmployeesContaining(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Profile> getProfilesForBadge(String badgeId) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new IllegalArgumentException("Badge not found: " + badgeId));
        return profileRepository.findByBadgesContaining(badge);
    }
}
