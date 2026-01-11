package acs.service.impl;

import acs.service.ClockService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 系统时钟服务实现，支持模拟时间设置。
 * 默认使用真实系统时间，可设置为固定的模拟时间。
 */
@Service
public class ClockServiceImpl implements ClockService {

    private final AtomicReference<Instant> simulatedTime = new AtomicReference<>(null);
    private final ZoneId defaultZone = ZoneId.systemDefault();

    @Override
    public Instant now() {
        Instant sim = simulatedTime.get();
        return sim != null ? sim : Instant.now();
    }

    @Override
    public LocalDateTime localNow() {
        Instant sim = simulatedTime.get();
        if (sim != null) {
            return LocalDateTime.ofInstant(sim, defaultZone);
        }
        return LocalDateTime.now(defaultZone);
    }

    @Override
    public void setSimulatedTime(Instant simulatedTime) {
        this.simulatedTime.set(simulatedTime);
    }

    @Override
    public void setSimulatedTime(LocalDateTime simulatedDateTime) {
        Instant instant = simulatedDateTime.atZone(defaultZone).toInstant();
        setSimulatedTime(instant);
    }

    @Override
    public Instant getSimulatedTime() {
        return simulatedTime.get();
    }

    @Override
    public boolean isSimulated() {
        return simulatedTime.get() != null;
    }

    @Override
    public void resetToRealTime() {
        simulatedTime.set(null);
    }
}