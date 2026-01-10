package acs.log.csv;

import acs.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CsvLogWriter单元测试。
 */
class CsvLogWriterTest {

    @TempDir
    Path tempDir;
    
    @Test
    void write_shouldCreateCsvFileWithCorrectFormat() throws Exception {
        // 准备测试数据
        LocalDateTime timestamp = LocalDateTime.of(2025, 12, 24, 14, 36, 49);
        Badge badge = new Badge("BX76Z541", BadgeStatus.ACTIVE);
        Employee employee = new Employee("83746028", "John Doe");
        Resource resource = new Resource("R7U39PL2", "Test Resource", ResourceType.DOOR, ResourceState.AVAILABLE);
        LogEntry entry = new LogEntry(timestamp, badge, employee, resource, AccessDecision.ALLOW, ReasonCode.ALLOW);
        
        // 使用临时目录
        CsvLogWriter writer = new CsvLogWriter(tempDir);
        writer.write(entry);
        
        // 验证文件路径
        Path expectedFile = tempDir.resolve("2025").resolve("Dec").resolve("24.csv");
        assertTrue(Files.exists(expectedFile), "CSV文件应被创建: " + expectedFile);
        
        // 读取文件内容
        List<String> lines = Files.readAllLines(expectedFile);
        assertEquals(1, lines.size());
        String csvLine = lines.get(0);
        
        // 验证格式
        // 期望: 2025,Dec,24,Wed,14:36:49,BX76Z541,R7U39PL2,R7U39PL2,83746028,John:Doe,ALLOW
        // 注意：读卡器ID使用资源ID占位
        String expected = "2025,Dec,24,Wed,14:36:49,BX76Z541,R7U39PL2,R7U39PL2,83746028,John:Doe,ALLOW";
        assertEquals(expected, csvLine, "CSV行格式不正确");
    }
    
    @Test
    void write_withNullFields_shouldHandleGracefully() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
        LogEntry entry = new LogEntry(timestamp, null, null, null, AccessDecision.DENY, ReasonCode.BADGE_NOT_FOUND);
        
        CsvLogWriter writer = new CsvLogWriter(tempDir);
        writer.write(entry);
        
        Path expectedFile = tempDir.resolve("2026").resolve("Jan").resolve("1.csv");
        assertTrue(Files.exists(expectedFile));
        
        List<String> lines = Files.readAllLines(expectedFile);
        String csvLine = lines.get(0);
        // 空字段应为空字符串
        assertTrue(csvLine.startsWith("2026,Jan,1,Thu,10:00:00,,,,"));
    }
}