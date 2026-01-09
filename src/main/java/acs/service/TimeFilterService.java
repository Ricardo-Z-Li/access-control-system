package acs.service;

import acs.domain.TimeFilter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 时间过滤器服务接口，负责时间规则解析和匹配。
 */
public interface TimeFilterService {

    /**
     * 解析时间规则字符串，创建或更新TimeFilter实体。
     * 规则格式示例："2025.July,August.Monday-Friday.8:00-12:00"
     * @param rawRule 原始规则字符串
     * @return 解析后的TimeFilter实体
     */
    TimeFilter parseTimeRule(String rawRule);

    /**
     * 检查给定时间是否匹配时间过滤器规则。
     * @param timeFilter 时间过滤器
     * @param dateTime 待检查的时间
     * @return 如果匹配返回true
     */
    boolean matches(TimeFilter timeFilter, LocalDateTime dateTime);

    /**
     * 检查给定时间是否匹配一组时间过滤器（任意一个匹配即通过）。
     * @param timeFilters 时间过滤器列表
     * @param dateTime 待检查的时间
     * @return 如果任意一个匹配返回true
     */
    boolean matchesAny(List<TimeFilter> timeFilters, LocalDateTime dateTime);

    /**
     * 验证时间规则字符串格式是否有效。
     * @param rawRule 原始规则字符串
     * @return 验证结果
     */
    boolean validateTimeRule(String rawRule);

}