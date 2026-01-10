package acs.service;

import acs.domain.ResourceType;
import java.util.List;

/**
 * 紧急控制服务接口，用于火灾疏散等紧急情况下批量控制资源状态。
 */
public interface EmergencyControlService {

    /**
     * 一键将所有门资源设置为非受控状态（UNCONTROLLED）。
     * 用于火灾疏散等紧急情况。
     */
    void setAllDoorsUncontrolled();

    /**
     * 将指定类型的所有资源设置为受控或非受控状态。
     * @param resourceType 资源类型（如DOOR、PRINTER等）
     * @param controlled true表示受控，false表示非受控
     */
    void setResourcesControlledByType(ResourceType resourceType, boolean controlled);

    /**
     * 将指定资源ID列表中的资源设置为受控或非受控状态。
     * @param resourceIds 资源ID列表
     * @param controlled true表示受控，false表示非受控
     */
    void setResourcesControlled(List<String> resourceIds, boolean controlled);

    /**
     * 将指定组的所有资源设置为受控或非受控状态。
     * @param groupId 组ID
     * @param controlled true表示受控，false表示非受控
     */
    void setGroupResourcesControlled(String groupId, boolean controlled);

    /**
     * 恢复所有资源为受控状态（恢复正常操作）。
     */
    void restoreAllToControlled();
}