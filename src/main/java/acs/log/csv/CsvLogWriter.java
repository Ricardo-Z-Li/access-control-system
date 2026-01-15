package acs.log.csv;

import acs.domain.LogEntry;
import acs.domain.BadgeReader;
import acs.repository.BadgeReaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * CSV日志写入器，负责按日/月/年目录结构写入CSV格式的日志行。
 * 文件路径示例：./logs/2025/Dec/24.csv
 * CSV格式示例：2025,Dec,24,Wed,14:36:49,BX76Z541,BR59KA87,R7U39PL2,83746028,John:Doe,GRANTED
 * 字段顺序：年,月,日,星期几,时间,徽章ID,读卡器ID,资源ID,员工ID,员工姓名,访问决策
 * 
 * 注意：当前LogEntry未包含读卡器ID，此处通过资源ID查找对应的读卡器ID。
 */
@Component
public class CsvLogWriter {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_OF_WEEK_FORMATTER = DateTimeFormatter.ofPattern("E", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final Path baseLogDir;
    private final BadgeReaderRepository badgeReaderRepository;
    
    @Autowired
    public CsvLogWriter(BadgeReaderRepository badgeReaderRepository) {
        this(Paths.get("./logs"), badgeReaderRepository);
    }
    
    public CsvLogWriter() {
        this(Paths.get("./logs"), null);
    }
    
    public CsvLogWriter(Path baseLogDir) {
        this(baseLogDir, null);
    }
    
    public CsvLogWriter(Path baseLogDir, BadgeReaderRepository badgeReaderRepository) {
        this.baseLogDir = baseLogDir;
        this.badgeReaderRepository = badgeReaderRepository;
        try {
            Files.createDirectories(baseLogDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory: " + baseLogDir, e);
        }
    }
    
    /**
     * 将LogEntry写入CSV文件。
     * 线程安全：使用同步方法确保并发写入时文件不会损坏。
     */
    public synchronized void write(LogEntry entry) {
        LocalDateTime timestamp = entry.getTimestamp();
        Path filePath = getFilePath(timestamp);
        
        try {
            Files.createDirectories(filePath.getParent());
            String csvLine = formatCsvLine(entry, timestamp);
            Files.writeString(filePath, csvLine + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
            // 在生产环境中应使用日志框架记录错误
        }
    }
    
    /**
     * 根据时间戳确定CSV文件路径。
     * 格式：{baseLogDir}/{year}/{month}/{day}.csv
     * 月份使用英文三字母缩写（Jan, Feb, ...）。
     */
    private Path getFilePath(LocalDateTime timestamp) {
        int year = timestamp.getYear();
        String month = timestamp.format(MONTH_FORMATTER);
        int day = timestamp.getDayOfMonth();
        return baseLogDir.resolve(String.valueOf(year))
                .resolve(month)
                .resolve(day + ".csv");
    }
    
    /**
     * 根据资源ID查找读卡器ID。
     * 如果找不到读卡器，返回空字符串；如果找到多个，返回第一个读卡器ID。
     */
    private String findReaderIdByResourceId(String resourceId) {
        if (badgeReaderRepository == null || resourceId == null || resourceId.isEmpty()) {
            return "";
        }
        List<BadgeReader> readers = badgeReaderRepository.findByResourceId(resourceId);
        if (readers.isEmpty()) {
            return "";
        }
        return readers.get(0).getReaderId();
    }
    
    /**
     * 将LogEntry格式化为CSV行。
     * 字段顺序：年,月,日,星期几,时间,徽章ID,读卡器ID,资源ID,员工ID,员工姓名,访问决策
     */
    private String formatCsvLine(LogEntry entry, LocalDateTime timestamp) {
        String year = String.valueOf(timestamp.getYear());
        String month = timestamp.format(MONTH_FORMATTER);
        String day = String.valueOf(timestamp.getDayOfMonth());
        String dayOfWeek = timestamp.format(DAY_OF_WEEK_FORMATTER);
        String time = timestamp.format(TIME_FORMATTER);
        
        String badgeId = entry.getBadge() != null ? entry.getBadge().getBadgeId() : "";
        String resourceId = entry.getResource() != null ? entry.getResource().getResourceId() : "";
        // 读卡器ID：通过资源ID查找，找不到则使用资源ID占位
        String readerId = "";
        if (resourceId != null && !resourceId.isEmpty()) {
            readerId = findReaderIdByResourceId(resourceId);
        }
        if (readerId.isEmpty()) {
            readerId = resourceId; // 回退到资源ID占位
        }
        String employeeId = entry.getEmployee() != null ? entry.getEmployee().getEmployeeId() : "";
        String employeeName = "";
        if (entry.getEmployee() != null && entry.getEmployee().getEmployeeName() != null) {
            String fullName = entry.getEmployee().getEmployeeName().trim();
            // 将空格替换为冒号，模拟"FirstName:LastName"格式
            employeeName = fullName.replace(' ', ':');
        }
        String decision = entry.getDecision() != null ? entry.getDecision().toString() : "";
        
        return String.join(",",
                year, month, day, dayOfWeek, time,
                badgeId, readerId, resourceId, employeeId, employeeName, decision);
    }
    
    /**
     * 获取基础日志目录（主要用于测试）。
     */
    public Path getBaseLogDir() {
        return baseLogDir;
    }
}
