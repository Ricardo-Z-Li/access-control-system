package acs.repository;

import acs.domain.Resource;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, String> {
    // 默认的findById、save、delete等CRUD方法由JpaRepository提供，无需手动声明
    // 如需自定义查询（如按资源名查询），可在此添加规范的方法签名（如Optional<Resource> findByResourceName(String resourceName)）
}
