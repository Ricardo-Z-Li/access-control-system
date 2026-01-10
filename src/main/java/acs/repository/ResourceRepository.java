package acs.repository;

import acs.domain.Resource;
import acs.domain.ResourceType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, String> {
    
    /**
     * 根据资源类型查找资源
     * @param resourceType 资源类型
     * @return 资源列表
     */
    List<Resource> findByResourceType(ResourceType resourceType);
    
    /**
     * 根据资源类型和受控状态查找资源
     * @param resourceType 资源类型
     * @param isControlled 受控状态
     * @return 资源列表
     */
    List<Resource> findByResourceTypeAndIsControlled(ResourceType resourceType, Boolean isControlled);
    
    /**
     * 根据受控状态查找资源
     * @param isControlled 受控状态
     * @return 资源列表
     */
    List<Resource> findByIsControlled(Boolean isControlled);
}
