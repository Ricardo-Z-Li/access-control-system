package acs.service;

import acs.domain.Group;
import acs.domain.Resource;
import java.util.List;
import java.util.Map;

/**
 * 组文件服务接口，用于从外部文件加载组和资源关联配置。
 */
public interface GroupFileService {

    /**
     * 从指定文件路径加载组配置。
     * 文件格式：每行定义一个组，格式为 "groupId:groupName:resourceId1,resourceId2,..."
     * @param filePath 文件路径
     * @return 加载的组列表
     */
    List<Group> loadGroupsFromFile(String filePath);

    /**
     * 从文件加载组和资源的映射关系。
     * @param filePath 文件路径
     * @return 映射表，key为组ID，value为该组可访问的资源ID列表
     */
    Map<String, List<String>> loadGroupResourceMapping(String filePath);

    /**
     * 验证组文件格式是否正确。
     * @param filePath 文件路径
     * @return 验证结果，true表示格式正确
     */
    boolean validateGroupFile(String filePath);

}