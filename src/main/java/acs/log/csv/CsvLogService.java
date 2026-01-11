package acs.log.csv;

import acs.domain.LogEntry;
import acs.log.LogService;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * CSV日志服务装饰器，包装原有的LogService实现，在记录日志时同时写入CSV文件。
 * 确保日志既保存到数据库，又按PDF要求的格式输出到CSV文件。
 */
@Service
@Primary
public class CsvLogService implements LogService {

    private final LogService delegate;
    private final CsvLogWriter csvLogWriter;
    
    public CsvLogService(@Qualifier("logServiceImpl") LogService delegate, CsvLogWriter csvLogWriter) {
        this.delegate = delegate;
        this.csvLogWriter = csvLogWriter;
    }
    
    @Override
    public void record(LogEntry entry) {
        // 1. 委托给原始LogService保存到数据库（及缓存）
        delegate.record(entry);
        
        // 2. 异步写入CSV文件（实际同步，但捕获异常不影响主流程）
        try {
            csvLogWriter.write(entry);
        } catch (Exception e) {
            // CSV写入失败不应影响业务逻辑，仅记录错误
            e.printStackTrace();
            // 生产环境应使用日志框架
        }
    }
}