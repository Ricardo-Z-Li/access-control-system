package acs.repository;

import acs.domain.Badge;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 泛型参数：第一个是对应实体类Badge，第二个是Badge主键的类型（需和Badge实体的主键类型一致）
@Repository
public interface BadgeRepository extends JpaRepository<Badge, String> {

    Optional<Badge> findById(String badgeId);
    Optional<Badge> findByEmployeeEmployeeId(String employeeId);

}
