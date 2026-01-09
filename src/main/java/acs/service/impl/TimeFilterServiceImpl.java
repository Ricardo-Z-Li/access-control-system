package acs.service.impl;

import acs.service.TimeFilterService;
import acs.domain.TimeFilter;
import acs.repository.TimeFilterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TimeFilterServiceImpl implements TimeFilterService {

    private final TimeFilterRepository timeFilterRepository;

    @Autowired
    public TimeFilterServiceImpl(TimeFilterRepository timeFilterRepository) {
        this.timeFilterRepository = timeFilterRepository;
    }

    @Override
    public TimeFilter parseTimeRule(String rawRule) {
        TimeFilter timeFilter = new TimeFilter();
        timeFilter.setRawRule(rawRule);

        // 解析格式：年.月.星期.时间范围
        // 示例：2025.July,August.Monday-Friday.8:00-12:00
        String[] parts = rawRule.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("时间规则格式错误，必须包含4个部分: " + rawRule);
        }

        // 1. 年
        String yearPart = parts[0].trim();
        if (!yearPart.equals("*")) {
            try {
                int year = Integer.parseInt(yearPart);
                timeFilter.setYear(year);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的年份: " + yearPart);
            }
        }

        // 2. 月
        String monthPart = parts[1].trim();
        if (!monthPart.equals("*")) {
            Set<String> monthSet = new HashSet<>();
            String[] monthItems = monthPart.split(",");
            for (String monthItem : monthItems) {
                monthItem = monthItem.trim();
                // 处理月份缩写或全名
                try {
                    Month month = Month.valueOf(monthItem.toUpperCase());
                    monthSet.add(month.name());
                } catch (IllegalArgumentException e) {
                    // 尝试缩写转换
                    Month month = parseMonthAbbreviation(monthItem);
                    monthSet.add(month.name());
                }
            }
            timeFilter.setMonths(String.join(",", monthSet));
        }

        // 3. 星期
        String dayOfWeekPart = parts[2].trim();
        if (!dayOfWeekPart.equals("*")) {
            // 支持 Monday-Friday 或 Monday,Wednesday
            Set<String> daySet = new HashSet<>();
            String[] dayItems = dayOfWeekPart.split(",");
            for (String dayItem : dayItems) {
                dayItem = dayItem.trim();
                if (dayItem.contains("-")) {
                    // 范围
                    String[] range = dayItem.split("-");
                    if (range.length != 2) {
                        throw new IllegalArgumentException("无效的星期范围: " + dayItem);
                    }
                    int start = parseDayOfWeek(range[0]);
                    int end = parseDayOfWeek(range[1]);
                    for (int i = start; i <= end; i++) {
                        daySet.add(String.valueOf(i));
                    }
                } else {
                    int day = parseDayOfWeek(dayItem);
                    daySet.add(String.valueOf(day));
                }
            }
            timeFilter.setDaysOfWeek(String.join(",", daySet));
        }

        // 4. 时间范围
        String timeRangePart = parts[3].trim();
        if (!timeRangePart.equals("*")) {
            String[] timeRange = timeRangePart.split("-");
            if (timeRange.length != 2) {
                throw new IllegalArgumentException("无效的时间范围: " + timeRangePart);
            }
            try {
                LocalTime startTime = LocalTime.parse(timeRange[0].trim(), DateTimeFormatter.ofPattern("H:mm"));
                LocalTime endTime = LocalTime.parse(timeRange[1].trim(), DateTimeFormatter.ofPattern("H:mm"));
                timeFilter.setStartTime(startTime);
                timeFilter.setEndTime(endTime);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("时间格式错误，应为HH:mm: " + timeRangePart);
            }
        }

        // 生成一个ID（基于原始规则的哈希）
        String filterId = "TF_" + Math.abs(rawRule.hashCode());
        timeFilter.setTimeFilterId(filterId);
        timeFilter.setFilterName("Rule: " + rawRule);

        // 保存到仓库（可选）
        timeFilterRepository.save(timeFilter);
        return timeFilter;
    }

    @Override
    public boolean matches(TimeFilter timeFilter, LocalDateTime dateTime) {
        // 检查年份
        if (timeFilter.getYear() != null && timeFilter.getYear() != dateTime.getYear()) {
            return false;
        }

        // 检查月份
        if (timeFilter.getMonths() != null && !timeFilter.getMonths().isEmpty()) {
            String currentMonth = dateTime.getMonth().name();
            if (!Arrays.asList(timeFilter.getMonths().split(",")).contains(currentMonth)) {
                return false;
            }
        }

        // 检查星期
        if (timeFilter.getDaysOfWeek() != null && !timeFilter.getDaysOfWeek().isEmpty()) {
            int currentDay = dateTime.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
            Set<String> allowedDays = new HashSet<>(Arrays.asList(timeFilter.getDaysOfWeek().split(",")));
            if (!allowedDays.contains(String.valueOf(currentDay))) {
                return false;
            }
        }

        // 检查时间范围
        if (timeFilter.getStartTime() != null && timeFilter.getEndTime() != null) {
            LocalTime currentTime = dateTime.toLocalTime();
            if (currentTime.isBefore(timeFilter.getStartTime()) || currentTime.isAfter(timeFilter.getEndTime())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean matchesAny(List<TimeFilter> timeFilters, LocalDateTime dateTime) {
        for (TimeFilter filter : timeFilters) {
            if (matches(filter, dateTime)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean validateTimeRule(String rawRule) {
        try {
            parseTimeRule(rawRule);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Month parseMonthAbbreviation(String abbreviation) {
        String lower = abbreviation.toLowerCase();
        switch (lower) {
            case "jan": return Month.JANUARY;
            case "feb": return Month.FEBRUARY;
            case "mar": return Month.MARCH;
            case "apr": return Month.APRIL;
            case "may": return Month.MAY;
            case "jun": return Month.JUNE;
            case "jul": return Month.JULY;
            case "aug": return Month.AUGUST;
            case "sep": return Month.SEPTEMBER;
            case "oct": return Month.OCTOBER;
            case "nov": return Month.NOVEMBER;
            case "dec": return Month.DECEMBER;
            default: throw new IllegalArgumentException("无效的月份缩写: " + abbreviation);
        }
    }

    private int parseDayOfWeek(String day) {
        String lower = day.toLowerCase();
        switch (lower) {
            case "monday": case "mon": return 1;
            case "tuesday": case "tue": return 2;
            case "wednesday": case "wed": return 3;
            case "thursday": case "thu": return 4;
            case "friday": case "fri": return 5;
            case "saturday": case "sat": return 6;
            case "sunday": case "sun": return 7;
            default: throw new IllegalArgumentException("无效的星期: " + day);
        }
    }
}