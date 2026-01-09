package acs.repository;

import acs.domain.TimeFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeFilterRepository extends JpaRepository<TimeFilter, String> {

    Optional<TimeFilter> findByTimeFilterId(String timeFilterId);

    List<TimeFilter> findByFilterNameContaining(String filterName);

    List<TimeFilter> findByIsRecurring(Boolean isRecurring);

    List<TimeFilter> findByStartTimeBeforeAndEndTimeAfter(LocalTime startTime, LocalTime endTime);

    List<TimeFilter> findByYear(Integer year);

}