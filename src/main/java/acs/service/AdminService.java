package acs.service;

import acs.domain.BadgeStatus;
import acs.domain.Profile;
import acs.domain.ResourceState;
import acs.domain.ResourceType;
import java.util.List;

public interface AdminService {

    void registerEmployee(String employeeId, String name);

    void issueBadge(String employeeId, String badgeId);

    void setBadgeStatus(String badgeId, BadgeStatus status);

    void createGroup(String groupId, String groupName);

    void assignEmployeeToGroup(String employeeId, String groupId);

    void removeEmployeeFromGroup(String employeeId, String groupId);

    void registerResource(String resourceId, String name, ResourceType type);

    void setResourceState(String resourceId, ResourceState state);

    void updateResourceLocation(String resourceId,
                                String building,
                                String floor,
                                Integer coordX,
                                Integer coordY,
                                String location);

    void grantGroupAccessToResource(String groupId, String resourceId);

    void revokeGroupAccessToResource(String groupId, String resourceId);

    void assignProfileToEmployee(String profileId, String employeeId);

    void removeProfileFromEmployee(String profileId, String employeeId);

    void assignProfileToBadge(String profileId, String badgeId);

    void removeProfileFromBadge(String profileId, String badgeId);

    List<Profile> getProfilesForEmployee(String employeeId);

    List<Profile> getProfilesForBadge(String badgeId);
}
