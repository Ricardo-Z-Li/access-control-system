package acs.service.impl;

import acs.domain.TimeFilter;
import acs.repository.TimeFilterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 测试时间过滤器服务实现，验证PDF示例规则。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimeFilterServiceImplTest {

    @Mock
    private TimeFilterRepository timeFilterRepository;

    @InjectMocks
    private TimeFilterServiceImpl timeFilterService;

    @BeforeEach
    void setUp() {
        // 模拟保存操作
        when(timeFilterRepository.save(any(TimeFilter.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void parseTimeRule_multiTimeRanges_shouldParseCorrectly() {
        // PDF示例: "2025.July,August.Monday-Friday.8:00-12:00,14:00-17:00"
        String rule = "2025.July,August.Monday-Friday.8:00-12:00,14:00-17:00";
        TimeFilter filter = timeFilterService.parseTimeRule(rule);

        assertNotNull(filter);
        assertEquals(2025, filter.getYear());
        assertNotNull(filter.getMonths());
        assertTrue(filter.getMonths().contains("JULY"));
        assertTrue(filter.getMonths().contains("AUGUST"));
        assertNotNull(filter.getDaysOfWeek());
        // Monday=1, Friday=5
        assertTrue(filter.getDaysOfWeek().contains("1"));
        assertTrue(filter.getDaysOfWeek().contains("2"));
        assertTrue(filter.getDaysOfWeek().contains("3"));
        assertTrue(filter.getDaysOfWeek().contains("4"));
        assertTrue(filter.getDaysOfWeek().contains("5"));
        // 时间区间
        assertNotNull(filter.getTimeRanges());
        assertTrue(filter.getTimeRanges().contains("08:00-12:00"));
        assertTrue(filter.getTimeRanges().contains("14:00-17:00"));
        // 向后兼容的startTime/endTime设置为第一个区间
        assertNotNull(filter.getStartTime());
        assertNotNull(filter.getEndTime());
        assertEquals("08:00", filter.getStartTime().toString());
        assertEquals("12:00", filter.getEndTime().toString());
    }

    @Test
    void parseTimeRule_exceptMonthsAndDays_shouldParseCorrectly() {
        // PDF示例: "2026.EXCEPT June,July,August.EXCEPT Sunday.ALL"
        String rule = "2026.EXCEPT June,July,August.EXCEPT Sunday.ALL";
        TimeFilter filter = timeFilterService.parseTimeRule(rule);

        assertNotNull(filter);
        assertEquals(2026, filter.getYear());
        // 月份：排除June,July,August
        assertNull(filter.getMonths());
        assertNotNull(filter.getExcludedMonths());
        assertTrue(filter.getExcludedMonths().contains("JUNE"));
        assertTrue(filter.getExcludedMonths().contains("JULY"));
        assertTrue(filter.getExcludedMonths().contains("AUGUST"));
        // 星期：排除Sunday
        assertNull(filter.getDaysOfWeek());
        assertNotNull(filter.getExcludedDaysOfWeek());
        assertEquals("7", filter.getExcludedDaysOfWeek());
        // 时间：ALL
        assertNull(filter.getTimeRanges());
        assertNull(filter.getExcludedTimeRanges());
        assertNull(filter.getStartTime());
        assertNull(filter.getEndTime());
    }

    @Test
    void parseTimeRule_combinedExceptTime_shouldParseCorrectly() {
        // PDF示例: "ALL.ALL.Monday-Friday.EXCEPT 12:00-14:00"
        String rule = "ALL.ALL.Monday-Friday.EXCEPT 12:00-14:00";
        TimeFilter filter = timeFilterService.parseTimeRule(rule);

        assertNotNull(filter);
        assertNull(filter.getYear());
        assertNull(filter.getMonths());
        assertNull(filter.getExcludedMonths());
        // 星期：Monday-Friday
        assertNotNull(filter.getDaysOfWeek());
        assertTrue(filter.getDaysOfWeek().contains("1"));
        assertTrue(filter.getDaysOfWeek().contains("5"));
        assertNull(filter.getExcludedDaysOfWeek());
        // 时间：排除12:00-14:00
        assertNull(filter.getTimeRanges());
        assertNotNull(filter.getExcludedTimeRanges());
        assertEquals("12:00-14:00", filter.getExcludedTimeRanges());
    }

    @Test
    void parseTimeRule_specificMonthsAndDays_shouldParseCorrectly() {
        // PDF示例: "2026.January,March,May.Monday,Wednesday,Friday.16:00-18:00"
        String rule = "2026.January,March,May.Monday,Wednesday,Friday.16:00-18:00";
        TimeFilter filter = timeFilterService.parseTimeRule(rule);

        assertNotNull(filter);
        assertEquals(2026, filter.getYear());
        assertNotNull(filter.getMonths());
        assertTrue(filter.getMonths().contains("JANUARY"));
        assertTrue(filter.getMonths().contains("MARCH"));
        assertTrue(filter.getMonths().contains("MAY"));
        assertNotNull(filter.getDaysOfWeek());
        assertTrue(filter.getDaysOfWeek().contains("1")); // Monday
        assertTrue(filter.getDaysOfWeek().contains("3")); // Wednesday
        assertTrue(filter.getDaysOfWeek().contains("5")); // Friday
        // 时间区间
        assertNotNull(filter.getTimeRanges());
        assertEquals("16:00-18:00", filter.getTimeRanges());
        assertEquals("16:00", filter.getStartTime().toString());
        assertEquals("18:00", filter.getEndTime().toString());
    }

    @Test
    void matches_multiTimeRanges_shouldMatchCorrectly() {
        TimeFilter filter = timeFilterService.parseTimeRule("2025.July,August.Monday-Friday.8:00-12:00,14:00-17:00");
        
        // 匹配的日期时间：2025-07-01 星期二 09:00 (在第一个区间内)
        LocalDateTime matching1 = LocalDateTime.of(2025, Month.JULY, 1, 9, 0);
        assertTrue(timeFilterService.matches(filter, matching1));
        
        // 匹配的日期时间：2025-08-15 星期五 15:00 (在第二个区间内)
        LocalDateTime matching2 = LocalDateTime.of(2025, Month.AUGUST, 15, 15, 0);
        assertTrue(timeFilterService.matches(filter, matching2));
        
        // 不匹配的日期时间：2025-07-01 星期二 13:00 (不在任何区间内)
        LocalDateTime notMatching1 = LocalDateTime.of(2025, Month.JULY, 1, 13, 0);
        assertFalse(timeFilterService.matches(filter, notMatching1));
        
        // 不匹配的日期时间：2025-09-01 星期二 10:00 (错误的月份)
        LocalDateTime notMatching2 = LocalDateTime.of(2025, Month.SEPTEMBER, 1, 10, 0);
        assertFalse(timeFilterService.matches(filter, notMatching2));
        
        // 不匹配的日期时间：2025-07-05 星期六 10:00 (错误的星期)
        LocalDateTime notMatching3 = LocalDateTime.of(2025, Month.JULY, 5, 10, 0);
        assertFalse(timeFilterService.matches(filter, notMatching3));
    }

    @Test
    void matches_exceptMonthsAndDays_shouldMatchCorrectly() {
        TimeFilter filter = timeFilterService.parseTimeRule("2026.EXCEPT June,July,August.EXCEPT Sunday.ALL");
        
        // 调试信息
        System.out.println("DEBUG: Excluded months: '" + filter.getExcludedMonths() + "'");
        System.out.println("DEBUG: Excluded days: '" + filter.getExcludedDaysOfWeek() + "'");
        System.out.println("DEBUG: Months: '" + filter.getMonths() + "'");
        
        // 匹配的日期时间：2026-01-15 星期一 12:00 (非排除月份，非排除星期)
        LocalDateTime matching1 = LocalDateTime.of(2026, Month.JANUARY, 15, 12, 0);
        assertTrue(timeFilterService.matches(filter, matching1));
        
        // 不匹配的日期时间：2026-07-15 星期三 12:00 (排除月份)
        LocalDateTime notMatching1 = LocalDateTime.of(2026, Month.JULY, 15, 12, 0);
        boolean result1 = timeFilterService.matches(filter, notMatching1);
        System.out.println("TEST DEBUG month exclude: matches result for JULY 12:00 = " + result1);
        assertFalse(result1);
        
        // 不匹配的日期时间：2026-02-01 星期日 12:00 (排除星期)
        LocalDateTime notMatching2 = LocalDateTime.of(2026, Month.FEBRUARY, 1, 12, 0);
        assertFalse(timeFilterService.matches(filter, notMatching2));
        
        // 不匹配的日期时间：2025-01-15 星期一 12:00 (错误年份)
        LocalDateTime notMatching3 = LocalDateTime.of(2025, Month.JANUARY, 15, 12, 0);
        assertFalse(timeFilterService.matches(filter, notMatching3));
    }

    @Test
    void matches_combinedExceptTime_shouldMatchCorrectly() {
        TimeFilter filter = timeFilterService.parseTimeRule("ALL.ALL.Monday-Friday.EXCEPT 12:00-14:00");
        
        // 调试信息
        System.out.println("DEBUG: Days of week: '" + filter.getDaysOfWeek() + "'");
        System.out.println("DEBUG: Excluded time ranges: '" + filter.getExcludedTimeRanges() + "'");
        System.out.println("DEBUG: Time ranges: '" + filter.getTimeRanges() + "'");
        
        // 匹配的日期时间：2025-01-01 星期三 10:00 (星期一至星期五，不在排除时间)
        LocalDateTime matching1 = LocalDateTime.of(2025, Month.JANUARY, 1, 10, 0);
        assertTrue(timeFilterService.matches(filter, matching1));
        
        // 不匹配的日期时间：2025-01-01 星期三 13:00 (在排除时间区间内)
        LocalDateTime notMatching1 = LocalDateTime.of(2025, Month.JANUARY, 1, 13, 0);
        boolean result1 = timeFilterService.matches(filter, notMatching1);
        System.out.println("TEST DEBUG: matches result for 13:00 = " + result1);
        assertFalse(result1);
        
        // 不匹配的日期时间：2025-01-04 星期六 10:00 (错误的星期)
        LocalDateTime notMatching2 = LocalDateTime.of(2025, Month.JANUARY, 4, 10, 0);
        assertFalse(timeFilterService.matches(filter, notMatching2));
        
        // 匹配的日期时间：2025-01-01 星期三 15:00 (星期一至星期五，不在排除时间)
        LocalDateTime matching2 = LocalDateTime.of(2025, Month.JANUARY, 1, 15, 0);
        assertTrue(timeFilterService.matches(filter, matching2));
    }

    @Test
    void matches_specificMonthsAndDays_shouldMatchCorrectly() {
        TimeFilter filter = timeFilterService.parseTimeRule("2026.January,March,May.Monday,Wednesday,Friday.16:00-18:00");
        
        // 匹配的日期时间：2026-01-05 星期一 17:00 (正确的月份、星期、时间)
        LocalDateTime matching1 = LocalDateTime.of(2026, Month.JANUARY, 5, 17, 0);
        assertTrue(timeFilterService.matches(filter, matching1));
        
        // 不匹配的日期时间：2026-02-01 星期一 17:00 (错误月份)
        LocalDateTime notMatching1 = LocalDateTime.of(2026, Month.FEBRUARY, 1, 17, 0);
        assertFalse(timeFilterService.matches(filter, notMatching1));
        
        // 不匹配的日期时间：2026-01-06 星期二 17:00 (错误星期)
        LocalDateTime notMatching2 = LocalDateTime.of(2026, Month.JANUARY, 6, 17, 0);
        assertFalse(timeFilterService.matches(filter, notMatching2));
        
        // 不匹配的日期时间：2026-01-05 星期一 15:00 (错误时间)
        LocalDateTime notMatching3 = LocalDateTime.of(2026, Month.JANUARY, 5, 15, 0);
        assertFalse(timeFilterService.matches(filter, notMatching3));
    }

    @Test
    void validateTimeRule_validRules_shouldReturnTrue() {
        assertTrue(timeFilterService.validateTimeRule("2025.July,August.Monday-Friday.8:00-12:00,14:00-17:00"));
        assertTrue(timeFilterService.validateTimeRule("2026.EXCEPT June,July,August.EXCEPT Sunday.ALL"));
        assertTrue(timeFilterService.validateTimeRule("ALL.ALL.Monday-Friday.EXCEPT 12:00-14:00"));
        assertTrue(timeFilterService.validateTimeRule("2026.January,March,May.Monday,Wednesday,Friday.16:00-18:00"));
        assertTrue(timeFilterService.validateTimeRule("*.*.*.*"));
        assertTrue(timeFilterService.validateTimeRule("ALL.ALL.ALL.ALL"));
    }

    @Test
    void validateTimeRule_invalidRules_shouldReturnFalse() {
        assertFalse(timeFilterService.validateTimeRule("invalid"));
        assertFalse(timeFilterService.validateTimeRule("2025.July,August.Monday-Friday"));
        assertFalse(timeFilterService.validateTimeRule("2025.July,August.Monday-Friday.8:00-12:00,14:00"));
        assertFalse(timeFilterService.validateTimeRule("2025.July,August.Monday-Friday.8:00-12:00-14:00"));
    }
}