package acs.repository;

import acs.domain.ResourceDependency;
import acs.domain.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 资源依赖关系仓库
 */
@Repository
public interface ResourceDependencyRepository extends JpaRepository<ResourceDependency, Long> {

    // 查找某个资源的所有依赖关系
    List<ResourceDependency> findByResource(Resource resource);
    
    // 查找某个资源的所有依赖关系（通过资源ID）
    List<ResourceDependency> findByResourceResourceId(String resourceId);
    
    // 查找某个资源作为前置依赖的所有关系
    List<ResourceDependency> findByRequiredResource(Resource requiredResource);
    
    // 查找某个资源作为前置依赖的所有关系（通过资源ID）
    List<ResourceDependency> findByRequiredResourceResourceId(String requiredResourceId);
}