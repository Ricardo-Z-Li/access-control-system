package acs.log.impl;

import acs.domain.LogEntry;
import acs.log.LogService;
import acs.cache.LocalCacheManager;
import acs.repository.AccessLogRepository;
import org.springframework.stereotype.Service;

@Service
public class LogServiceImpl implements LogService {

    private final AccessLogRepository accessLogRepository;
    private final LocalCacheManager cacheManager;

    public LogServiceImpl(AccessLogRepository accessLogRepository, LocalCacheManager cacheManager) {
        this.accessLogRepository = accessLogRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    public void record(LogEntry entry) {
        // 1. 保存到数据库
        LogEntry savedEntry = accessLogRepository.save(entry);
        // 2. 同步到本地缓存（确保缓存与数据库一致）
        cacheManager.updateLog(savedEntry);
    }
}