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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
        // 支持PDF示例：2025.July,August.Monday-Friday.8:00-12:00,14:00-17:00
        // 支持EXCEPT逻辑：2026.EXCEPT June,July,August.EXCEPT Sunday.ALL
        // 支持组合逻辑：ALL.ALL.Monday-Friday.EXCEPT 12:00-14:00
        String[] parts = rawRule.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("时间规则格式错误，必须包含4个部分: " + rawRule);
        }

        // 1. 年
        String yearPart = parts[0].trim();
        if (!isAllKeyword(yearPart)) {
            try {
                int year = Integer.parseInt(yearPart);
                timeFilter.setYear(year);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无效的年份: " + yearPart);
            }
        }

        // 2. 月
        String monthPart = parts[1].trim();
        String[] monthIncludedExcluded = parseIncludedExcluded(monthPart);
        String includedMonths = monthIncludedExcluded[0];
        String excludedMonths = monthIncludedExcluded[1];
        if (includedMonths != null) {
            Set<String> monthSet = parseMonthList(includedMonths);
            timeFilter.setMonths(String.join(",", monthSet));
        }
        if (excludedMonths != null) {
            Set<String> excludedSet = parseMonthList(excludedMonths);
            timeFilter.setExcludedMonths(String.join(",", excludedSet));
        }

        // 3. 星期
        String dayOfWeekPart = parts[2].trim();
        String[] dayIncludedExcluded = parseIncludedExcluded(dayOfWeekPart);
        String includedDays = dayIncludedExcluded[0];
        String excludedDays = dayIncludedExcluded[1];
        if (includedDays != null) {
            Set<Integer> daySet = parseDayOfWeekList(includedDays);
            timeFilter.setDaysOfWeek(daySet.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        if (excludedDays != null) {
            Set<Integer> excludedSet = parseDayOfWeekList(excludedDays);
            timeFilter.setExcludedDaysOfWeek(excludedSet.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }

        // 4. 时间范围
        String timePart = parts[3].trim();
        // 检查是否包含EXCEPT
        if (isExceptKeyword(timePart)) {
            // 格式：EXCEPT 12:00-14:00 或 EXCEPT 8:00-12:00,14:00-17:00
            String excludedTimeRanges = timePart.substring(7).trim(); // 去除"EXCEPT "
            List<LocalTime[]> excludedRanges = parseTimeRanges(excludedTimeRanges);
            timeFilter.setExcludedTimeRanges(timeRangesToString(excludedRanges));
            // 设置timeRanges为null（表示全部时间允许，但排除这些区间）
        } else if (isAllKeyword(timePart)) {
            // 全部时间允许，无需设置时间范围
        } else {
            // 包含的时间区间（可能多个）
            List<LocalTime[]> includedRanges = parseTimeRanges(timePart);
            if (!includedRanges.isEmpty()) {
                timeFilter.setTimeRanges(timeRangesToString(includedRanges));
                // 为了向后兼容，设置第一个区间为startTime/endTime
                LocalTime[] firstRange = includedRanges.get(0);
                timeFilter.setStartTime(firstRange[0]);
                timeFilter.setEndTime(firstRange[1]);
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
        String currentMonth = dateTime.getMonth().name();
        if (timeFilter.getMonths() != null && !timeFilter.getMonths().isEmpty()) {
            // 包含列表：必须在此列表中
            Set<String> includedMonths = new HashSet<>(Arrays.asList(timeFilter.getMonths().split(",")));
            if (!includedMonths.contains(currentMonth)) {
                return false;
            }
        }
        if (timeFilter.getExcludedMonths() != null && !timeFilter.getExcludedMonths().isEmpty()) {
            // 排除列表：不能在此列表中
            Set<String> excludedMonths = new HashSet<>();
            for (String month : timeFilter.getExcludedMonths().split(",")) {
                String trimmed = month.trim();
                if (!trimmed.isEmpty()) {
                    excludedMonths.add(trimmed);
                }
            }
            if (excludedMonths.contains(currentMonth)) {
                return false;
            }
        }

        // 检查星期
        int currentDay = dateTime.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday

        if (timeFilter.getDaysOfWeek() != null && !timeFilter.getDaysOfWeek().isEmpty()) {
            Set<String> includedDays = new HashSet<>(Arrays.asList(timeFilter.getDaysOfWeek().split(",")));
            if (!includedDays.contains(String.valueOf(currentDay))) {
                return false;
            }
        }
        if (timeFilter.getExcludedDaysOfWeek() != null && !timeFilter.getExcludedDaysOfWeek().isEmpty()) {
            Set<String> excludedDays = new HashSet<>();
            for (String day : timeFilter.getExcludedDaysOfWeek().split(",")) {
                String trimmed = day.trim();
                if (!trimmed.isEmpty()) {
                    excludedDays.add(trimmed);
                }
            }
            if (excludedDays.contains(String.valueOf(currentDay))) {
                return false;
            }
        }

        // 检查时间范围
        LocalTime currentTime = dateTime.toLocalTime();
        // 首先检查排除时间区间
        if (timeFilter.getExcludedTimeRanges() != null && !timeFilter.getExcludedTimeRanges().isEmpty()) {
            List<LocalTime[]> excludedRanges = parseTimeRanges(timeFilter.getExcludedTimeRanges());
            for (LocalTime[] range : excludedRanges) {
                if (currentTime.compareTo(range[0]) >= 0 && currentTime.compareTo(range[1]) <= 0) {
                    // 当前时间在排除区间内
                    return false;
                }
            }
        }
        // 检查包含的时间区间（多区间）
        if (timeFilter.getTimeRanges() != null && !timeFilter.getTimeRanges().isEmpty()) {
            List<LocalTime[]> includedRanges = parseTimeRanges(timeFilter.getTimeRanges());
            boolean inAnyRange = false;
            for (LocalTime[] range : includedRanges) {
                if (currentTime.compareTo(range[0]) >= 0 && currentTime.compareTo(range[1]) <= 0) {
                    inAnyRange = true;
                    break;
                }
            }
            if (!inAnyRange) {
                return false;
            }
        } else if (timeFilter.getStartTime() != null && timeFilter.getEndTime() != null) {
            // 向后兼容：使用旧的startTime/endTime字段
            if (currentTime.isBefore(timeFilter.getStartTime()) || currentTime.isAfter(timeFilter.getEndTime())) {
                return false;
            }
        }
        // 如果没有设置时间区间，则表示时间不受限制

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

    private boolean isAllKeyword(String part) {
        return part.equals("*") || part.equalsIgnoreCase("ALL");
    }

    private boolean isExceptKeyword(String part) {
        return part.trim().toUpperCase().startsWith("EXCEPT ");
    }

    private String[] parseIncludedExcluded(String part) {
        // 返回数组：[included, excluded]
        // 如果包含EXCEPT，则提取排除部分
        // 例如："EXCEPT June,July,August" -> [null, "June,July,August"]
        // "January,March,May" -> ["January,March,May", null]
        // "ALL" -> [null, null] (表示全部允许)
        // "EXCEPT Sunday" -> [null, "Sunday"]
        String trimmed = part.trim();
        if (isAllKeyword(trimmed)) {
            return new String[]{null, null};
        }
        if (isExceptKeyword(trimmed)) {
            String excluded = trimmed.substring(7).trim(); // 去除"EXCEPT "
            return new String[]{null, excluded};
        }
        // 检查是否同时包含包含和排除部分？可能不需要，因为EXCEPT只出现在开头
        // 简单处理：视为包含列表
        return new String[]{trimmed, null};
    }

    private Set<String> parseMonthList(String monthListStr) {
        Set<String> monthSet = new HashSet<>();
        if (monthListStr == null || monthListStr.isEmpty()) {
            return monthSet;
        }
        String[] items = monthListStr.split(",");
        for (String item : items) {
            item = item.trim();
            try {
                Month month = Month.valueOf(item.toUpperCase());
                monthSet.add(month.name());
            } catch (IllegalArgumentException e) {
                Month month = parseMonthAbbreviation(item);
                monthSet.add(month.name());
            }
        }
        return monthSet;
    }

    private Set<Integer> parseDayOfWeekList(String dayListStr) {
        Set<Integer> daySet = new HashSet<>();
        if (dayListStr == null || dayListStr.isEmpty()) {
            return daySet;
        }
        String[] items = dayListStr.split(",");
        for (String item : items) {
            item = item.trim();
            if (item.contains("-")) {
                String[] range = item.split("-");
                if (range.length != 2) {
                    throw new IllegalArgumentException("无效的星期范围: " + item);
                }
                int start = parseDayOfWeek(range[0]);
                int end = parseDayOfWeek(range[1]);
                for (int i = start; i <= end; i++) {
                    daySet.add(i);
                }
            } else {
                int day = parseDayOfWeek(item);
                daySet.add(day);
            }
        }
        return daySet;
    }

    private List<LocalTime[]> parseTimeRanges(String timeRangesStr) {
        List<LocalTime[]> ranges = new ArrayList<>();
        if (timeRangesStr == null || timeRangesStr.isEmpty() || isAllKeyword(timeRangesStr)) {
            return ranges; // 空列表表示无限制
        }
        // 支持多个时间区间，用逗号分隔
        String[] rangeStrings = timeRangesStr.split(",");
        for (String rangeStr : rangeStrings) {
            rangeStr = rangeStr.trim();
            if (rangeStr.isEmpty()) continue;
            String[] times = rangeStr.split("-");
            if (times.length != 2) {
                throw new IllegalArgumentException("无效的时间区间: " + rangeStr);
            }
            LocalTime start = LocalTime.parse(times[0].trim(), DateTimeFormatter.ofPattern("H:mm"));
            LocalTime end = LocalTime.parse(times[1].trim(), DateTimeFormatter.ofPattern("H:mm"));
            ranges.add(new LocalTime[]{start, end});
        }
        return ranges;
    }

    private String timeRangesToString(List<LocalTime[]> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (LocalTime[] range : ranges) {
            parts.add(range[0].toString() + "-" + range[1].toString());
        }
        return String.join(",", parts);
    }
}