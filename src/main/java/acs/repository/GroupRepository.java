package acs.repository;

import acs.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, String> {
    // 默认的findById、save、delete等CRUD方法由JpaRepository提供，无需手动声明
    // 如需自定义查询（如按组名查询），可在此添加规范的方法签名（如Optional<Group> findByGroupName(String groupName)）
    
    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.resources")
    List<Group> findAllWithResources();
    
    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.employees LEFT JOIN FETCH g.resources")
    List<Group> findAllWithEmployeesAndResources();
    
    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.employees LEFT JOIN FETCH g.resources WHERE g.groupId = :groupId")
    Optional<Group> findByIdWithEmployeesAndResources(String groupId);
}