package acs.repository;

import acs.domain.Employee;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    Optional<Employee> findByEmployeeNameContaining(String name);
    
    @Query("SELECT DISTINCT e FROM Employee e " +
           "LEFT JOIN FETCH e.groups g " +
           "LEFT JOIN FETCH g.resources")
    List<Employee> findAllWithGroupsAndResources();
    
    @Query("SELECT DISTINCT e FROM Employee e " +
           "LEFT JOIN FETCH e.groups")
    List<Employee> findAllWithGroups();
    
    @Query("SELECT DISTINCT e FROM Employee e " +
           "LEFT JOIN FETCH e.groups " +
           "WHERE e.employeeId = :employeeId")
    Optional<Employee> findByIdWithGroups(String employeeId);
}
