package acs.service;

import acs.domain.Profile;
import acs.domain.TimeFilter;
import java.util.List;

/**
 * 配置文件服务接口，用于从JSON文件加载配置文件和时间过滤器配置。
 */
public interface ProfileFileService {

    /**
     * 从JSON文件加载配置文件。
     * @param filePath JSON文件路径
     * @return 加载的配置文件列表
     */
    List<Profile> loadProfilesFromJson(String filePath);

    /**
     * 从JSON文件加载时间过滤器。
     * @param filePath JSON文件路径
     * @return 加载的时间过滤器列表
     */
    List<TimeFilter> loadTimeFiltersFromJson(String filePath);

    /**
     * 验证JSON配置文件格式。
     * @param filePath 文件路径
     * @return 验证结果
     */
    boolean validateJsonFile(String filePath);

}