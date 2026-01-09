package acs.repository;

import acs.domain.BadgeReader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeReaderRepository extends JpaRepository<BadgeReader, String> {

    Optional<BadgeReader> findByReaderId(String readerId);

    List<BadgeReader> findByResourceId(String resourceId);

    List<BadgeReader> findByStatus(String status);

    List<BadgeReader> findByLocationContaining(String location);

}