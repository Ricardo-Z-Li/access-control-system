package acs.repository;

import acs.domain.Profile;
import acs.domain.ProfileResourceLimit;
import acs.domain.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileResourceLimitRepository extends JpaRepository<ProfileResourceLimit, Long> {

    List<ProfileResourceLimit> findByProfileAndIsActiveTrue(Profile profile);

    Optional<ProfileResourceLimit> findByProfileAndResource(Profile profile, Resource resource);
}

