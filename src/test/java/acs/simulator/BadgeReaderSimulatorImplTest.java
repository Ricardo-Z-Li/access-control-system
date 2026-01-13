package acs.simulator;

import acs.domain.*;
import acs.repository.BadgeReaderRepository;
import acs.service.AccessControlService;
import acs.service.ClockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BadgeReaderSimulatorImplTest {

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private RouterSystem routerSystem;

    @Mock
    private ResourceController resourceController;

    @Mock
    private BadgeReaderRepository badgeReaderRepository;

    @Mock
    private ClockService clockService;

    @InjectMocks
    private BadgeReaderSimulatorImpl badgeReaderSimulator;

    @BeforeEach
    void setUp() {
        when(clockService.now()).thenReturn(java.time.Instant.parse("2026-01-12T23:15:00Z"));
    }


    @Test
    public void simulateBadgeSwipe_validInput_shouldReturnAccessResult() throws InterruptedException {
        String readerId = "READER001";
        String badgeId = "BADGE001";
        String resourceId = "RES001";
        Instant now = Instant.parse("2026-01-12T23:15:00Z");

        BadgeReader badgeReader = new BadgeReader(readerId, "Test Reader", "Location", "ONLINE", resourceId);
        AccessResult expectedResult = new AccessResult(AccessDecision.ALLOW, ReasonCode.ALLOW, "Access granted");

        when(badgeReaderRepository.findByReaderId(readerId)).thenReturn(Optional.of(badgeReader));
        when(clockService.now()).thenReturn(now);
        when(routerSystem.routeRequest(any(AccessRequest.class), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(expectedResult);

        AccessResult actualResult = badgeReaderSimulator.simulateBadgeSwipe(readerId, badgeId);

        assertNotNull(actualResult);
        assertEquals(AccessDecision.ALLOW, actualResult.getDecision());
        assertEquals(ReasonCode.ALLOW, actualResult.getReasonCode());

        verify(badgeReaderRepository, atLeastOnce()).findByReaderId(readerId);
        verify(routerSystem, times(1)).routeRequest(any(AccessRequest.class), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(resourceController, times(1)).unlockResource(resourceId);
    }

    @Test
    public void simulateBadgeSwipe_readerNotFound_shouldReturnDenyResult() throws InterruptedException {
        String readerId = "NON_EXISTENT_READER";
        String badgeId = "BADGE001";

        when(badgeReaderRepository.findByReaderId(readerId)).thenReturn(Optional.empty());

        AccessResult actualResult = badgeReaderSimulator.simulateBadgeSwipe(readerId, badgeId);

        assertNotNull(actualResult);
        assertEquals(AccessDecision.DENY, actualResult.getDecision());
        assertEquals(ReasonCode.SYSTEM_ERROR, actualResult.getReasonCode());

        verify(badgeReaderRepository, times(1)).findByReaderId(readerId);
        verify(routerSystem, never()).routeRequest(any(), any(), any(), any(), any(), any());
        verify(resourceController, never()).unlockResource(anyString());
    }

    @Test
    public void simulateBadgeSwipe_accessDenied_shouldNotUnlockResource() throws InterruptedException {
        String readerId = "READER001";
        String badgeId = "BADGE001";
        String resourceId = "RES001";
        Instant now = Instant.parse("2026-01-12T23:15:00Z");

        BadgeReader badgeReader = new BadgeReader(readerId, "Test Reader", "Location", "ONLINE", resourceId);
        AccessResult expectedResult = new AccessResult(AccessDecision.DENY, ReasonCode.NO_PERMISSION, "Access denied");

        when(badgeReaderRepository.findByReaderId(readerId)).thenReturn(Optional.of(badgeReader));
        when(clockService.now()).thenReturn(now);
        when(routerSystem.routeRequest(any(AccessRequest.class), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(expectedResult);

        AccessResult actualResult = badgeReaderSimulator.simulateBadgeSwipe(readerId, badgeId);

        assertNotNull(actualResult);
        assertEquals(AccessDecision.DENY, actualResult.getDecision());
        assertEquals(ReasonCode.NO_PERMISSION, actualResult.getReasonCode());

        verify(resourceController, never()).unlockResource(anyString());
    }

    @Test
    public void readBadgeCode_validInput_shouldReturnCode() throws InterruptedException {
        String readerId = "READER001";
        String badgeId = "BADGE001";
        when(clockService.now()).thenReturn(Instant.parse("2026-01-12T23:15:00Z"));

        String badgeCode = badgeReaderSimulator.readBadgeCode(readerId, badgeId);

        assertNotNull(badgeCode);
        assertTrue(badgeCode.startsWith("SIM_"));
        assertTrue(badgeCode.contains(badgeId));
    }

    @Test
    public void updateReaderStatus_existingReader_shouldUpdateStatus() {
        String readerId = "READER001";
        String newStatus = "MAINTENANCE";

        BadgeReader badgeReader = new BadgeReader(readerId, "Test Reader", "Location", "ONLINE", "RES001");

        when(badgeReaderRepository.findByReaderId(readerId)).thenReturn(Optional.of(badgeReader));
        when(badgeReaderRepository.save(any(BadgeReader.class))).thenReturn(badgeReader);
        when(clockService.now()).thenReturn(Instant.parse("2026-01-12T23:15:00Z"));

        badgeReaderSimulator.updateReaderStatus(readerId, newStatus);

        verify(badgeReaderRepository, times(1)).findByReaderId(readerId);
        verify(badgeReaderRepository, times(1)).save(badgeReader);
        assertEquals(newStatus, badgeReader.getStatus());
    }
}
