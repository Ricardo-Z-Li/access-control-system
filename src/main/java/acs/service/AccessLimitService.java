package acs.service;

import acs.domain.Employee;
import acs.domain.Profile;
import acs.domain.Resource;

import java.time.Instant;

/**
 * ??????????
 */
public interface AccessLimitService {

    boolean checkDailyLimit(Employee employee, Profile profile);

    boolean checkWeeklyLimit(Employee employee, Profile profile);

    void recordAccess(Employee employee, Resource resource, Instant timestamp);

    int getTodayAccessCount(Employee employee);

    int getWeekAccessCount(Employee employee);

    /**
     * ????????????????
     */
    int getTodayAccessCount(Employee employee, Resource resource);

    /**
     * ????????????????
     */
    int getWeekAccessCount(Employee employee, Resource resource);

    boolean checkAllLimits(Employee employee);

    boolean checkAllLimits(Employee employee, Instant timestamp);

    boolean checkResourceLimits(Employee employee, Resource resource, Instant timestamp);
}
