package acs.repository;

import acs.domain.Profile;
import acs.domain.Group;
import acs.domain.Employee;
import acs.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, String> {

    Optional<Profile> findByProfileId(String profileId);

    List<Profile> findByProfileNameContaining(String profileName);

    List<Profile> findByIsActive(Boolean isActive);

    List<Profile> findByPriorityLevelGreaterThanEqual(Integer priorityLevel);

    List<Profile> findByGroupsContaining(Group group);

    List<Profile> findByEmployeesContaining(Employee employee);

    List<Profile> findByBadgesContaining(Badge badge);

}
