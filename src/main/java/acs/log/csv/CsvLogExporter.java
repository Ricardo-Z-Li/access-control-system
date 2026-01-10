package acs.log.csv;

import acs.domain.LogEntry;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * CSV日志导出器，用于将一组LogEntry导出为单个CSV文件。
 * 适用于日志搜索结果导出。
 */
@Component
public class CsvLogExporter {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_OF_WEEK_FORMATTER = DateTimeFormatter.ofPattern("E", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * 将日志条目列表导出到指定CSV文件。
     * 文件将包含标题行和所有条目。
     */
    public void exportToFile(List<LogEntry> entries, Path outputFile) throws IOException {
        StringBuilder csvContent = new StringBuilder();
        // 添加标题行
        csvContent.append("Year,Month,Day,DayOfWeek,Time,BadgeId,ReaderId,ResourceId,EmployeeId,EmployeeName,Decision\n");
        for (LogEntry entry : entries) {
            csvContent.append(formatCsvLine(entry)).append("\n");
        }
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, csvContent.toString());
    }
    
    /**
     * 将日志条目列表格式化为CSV字符串（不含标题）。
     */
    public String exportToString(List<LogEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("Year,Month,Day,DayOfWeek,Time,BadgeId,ReaderId,ResourceId,EmployeeId,EmployeeName,Decision\n");
        for (LogEntry entry : entries) {
            sb.append(formatCsvLine(entry)).append("\n");
        }
        return sb.toString();
    }
    
    private String formatCsvLine(LogEntry entry) {
        LocalDateTime timestamp = entry.getTimestamp();
        String year = String.valueOf(timestamp.getYear());
        String month = timestamp.format(MONTH_FORMATTER);
        String day = String.valueOf(timestamp.getDayOfMonth());
        String dayOfWeek = timestamp.format(DAY_OF_WEEK_FORMATTER);
        String time = timestamp.format(TIME_FORMATTER);
        
        String badgeId = entry.getBadge() != null ? entry.getBadge().getBadgeId() : "";
        String readerId = entry.getResource() != null ? entry.getResource().getResourceId() : "";
        String resourceId = entry.getResource() != null ? entry.getResource().getResourceId() : "";
        String employeeId = entry.getEmployee() != null ? entry.getEmployee().getEmployeeId() : "";
        String employeeName = "";
        if (entry.getEmployee() != null && entry.getEmployee().getEmployeeName() != null) {
            String fullName = entry.getEmployee().getEmployeeName().trim();
            employeeName = fullName.replace(' ', ':');
        }
        String decision = entry.getDecision() != null ? entry.getDecision().toString() : "";
        
        return String.join(",",
                year, month, day, dayOfWeek, time,
                badgeId, readerId, resourceId, employeeId, employeeName, decision);
    }
}