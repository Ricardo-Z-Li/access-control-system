package acs.service.impl;

import acs.service.ClockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClockServiceImplTest {

    private ClockService clockService;

    @BeforeEach
    void setUp() {
        clockService = new ClockServiceImpl();
    }

    @Test
    void now_withoutSimulation_shouldReturnCurrentTime() {
        Instant before = Instant.now();
        Instant result = clockService.now();
        Instant after = Instant.now();
        
        assertNotNull(result);
        assertTrue(result.compareTo(before) >= 0);
        assertTrue(result.compareTo(after) <= 0);
    }

    @Test
    void localNow_withoutSimulation_shouldReturnCurrentLocalDateTime() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime result = clockService.localNow();
        LocalDateTime after = LocalDateTime.now();
        
        assertNotNull(result);
        assertTrue(result.compareTo(before) >= 0);
        assertTrue(result.compareTo(after) <= 0);
    }

    @Test
    void setSimulatedTime_withInstant_shouldSetSimulatedTime() {
        Instant simulatedTime = Instant.parse("2025-07-15T10:30:00Z");
        clockService.setSimulatedTime(simulatedTime);
        
        assertTrue(clockService.isSimulated());
        assertEquals(simulatedTime, clockService.getSimulatedTime());
        assertEquals(simulatedTime, clockService.now());
        
        LocalDateTime expectedLocal = LocalDateTime.ofInstant(simulatedTime, ZoneId.systemDefault());
        assertEquals(expectedLocal, clockService.localNow());
    }

    @Test
    void setSimulatedTime_withLocalDateTime_shouldSetSimulatedTime() {
        LocalDateTime simulatedDateTime = LocalDateTime.of(2025, 7, 15, 10, 30, 0);
        clockService.setSimulatedTime(simulatedDateTime);
        
        assertTrue(clockService.isSimulated());
        assertNotNull(clockService.getSimulatedTime());
        
        Instant expectedInstant = simulatedDateTime.atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(expectedInstant, clockService.now());
        assertEquals(simulatedDateTime, clockService.localNow());
    }

    @Test
    void resetToRealTime_shouldClearSimulation() {
        clockService.setSimulatedTime(Instant.parse("2025-07-15T10:30:00Z"));
        assertTrue(clockService.isSimulated());
        
        clockService.resetToRealTime();
        
        assertFalse(clockService.isSimulated());
        assertNull(clockService.getSimulatedTime());
        
        Instant now = Instant.now();
        Instant result = clockService.now();
        assertTrue(result.compareTo(now.minusSeconds(1)) >= 0);
        assertTrue(result.compareTo(now.plusSeconds(1)) <= 0);
    }

    @Test
    void isSimulated_whenNotSet_shouldReturnFalse() {
        assertFalse(clockService.isSimulated());
        assertNull(clockService.getSimulatedTime());
    }

    @Test
    void getSimulatedTime_whenNotSet_shouldReturnNull() {
        assertNull(clockService.getSimulatedTime());
    }
}