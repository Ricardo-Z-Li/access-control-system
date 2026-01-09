package acs.simulator;

import acs.domain.*;
import acs.repository.BadgeReaderRepository;
import acs.service.AccessControlService;
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

/**
 * BadgeReaderSimulatorImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class BadgeReaderSimulatorImplTest {

    @Mock
    private AccessControlService accessControlService;
    
    @Mock
    private ResourceController resourceController;
    
    @Mock
    private BadgeReaderRepository badgeReaderRepository;
    
    @InjectMocks
    private BadgeReaderSimulatorImpl badgeReaderSimulator;

    @Test
    public void simulateBadgeSwipe_validInput_shouldReturnAccessResult() throws InterruptedException {
        // 准备测试数据
        String readerId = "READER_001";
        String badgeId = "BADGE_001";
        String resourceId = "RESOURCE_001";
        
        BadgeReader badgeReader = new BadgeReader(readerId, "Test Reader", "Location", "ONLINE", resourceId);
        AccessRequest expectedRequest = new AccessRequest(badgeId, resourceId, Instant.now());
        AccessResult expectedResult = new AccessResult(AccessDecision.ALLOW, ReasonCode.ALLOW, "Access granted");
        
        // 模拟依赖行为
        when(badgeReaderRepository.findByReaderId(readerId)).thenReturn(Optional.of(badgeReader));
        when(accessControlService.processAccess(any(AccessRequest.class))).thenReturn(expectedResult);
        
        // 执行测试
        AccessResult actualResult = badgeReaderSimulator.simulateBadgeSwipe(readerId, badgeId);
        
        // 验证结果
        assertNotNull(actualResult);
        assertEquals(AccessDecision.ALLOW, actualResult.getDecision());
        assertEquals(ReasonCode.ALLOW, actualResult.getReasonCode());
        
        // 验证交互
        verify(badgeReaderRepository, atLeastOnce()).findByReaderId(readerId);
        verify(accessControlService, times(1)).processAccess(any(AccessRequest.class));
        verify(resourceController, times(1)).unlockResource(resourceId);
    }

    @Test
    public void simulateBadgeSwipe_readerNotFound_shouldReturnDenyResult() throws InterruptedException {
        // 准备测试数据
        String readerId = "NON_EXISTENT_READER";
        String badgeId = "BADGE_001";
        
        // 模拟依赖行为
        when(badgeReaderRepository.findByReaderId(readerId)).thenReturn(Optional.empty());
        
        // 执行测试
        AccessResult actualResult = badgeReaderSimulator.simulateBadgeSwipe(readerId, badgeId);
        
        // 验证结果
        assertNotNull(actualResult);
        assertEquals(AccessDecision.DENY, actualResult.getDecision());
        assertEquals(ReasonCode.SYSTEM_ERROR, actualResult.getReasonCode());
        
        // 验证交互
        verify(badgeReaderRepository, times(1)).findByReaderId(readerId);
        verify(accessControlService, never()).processAccess(any());
        verify(resourceController, never()).unlockResource(anyString());
    }

    @Test
    public void simulateBadgeSwipe_accessDenied_shouldNotUnlockResource() throws InterruptedException {
        // 准备测试数据
        String readerId = "READER_001";
        String badgeId = "BADGE_001";
        String resourceId = "RESOURCE_001";
        
        BadgeReader badgeReader = new BadgeReader(readerId, "Test Reader", "Location", "ONLINE", resourceId);
        AccessResult expectedResult = new AccessResult(AccessDecision.DENY, ReasonCode.NO_PERMISSION, "Access denied");
        
        // 模拟依赖行为
        when(badgeReaderRepository.findByReaderId(readerId)).thenReturn(Optional.of(badgeReader));
        when(accessControlService.processAccess(any(AccessRequest.class))).thenReturn(expectedResult);
        
        // 执行测试
        AccessResult actualResult = badgeReaderSimulator.simulateBadgeSwipe(readerId, badgeId);
        
        // 验证结果
        assertNotNull(actualResult);
        assertEquals(AccessDecision.DENY, actualResult.getDecision());
        assertEquals(ReasonCode.NO_PERMISSION, actualResult.getReasonCode());
        
        // 验证交互
        verify(resourceController, never()).unlockResource(anyString());
    }

    @Test
    public void readBadgeCode_validInput_shouldReturnCode() throws InterruptedException {
        // 准备测试数据
        String readerId = "READER_001";
        String badgeId = "BADGE_001";
        
        // 执行测试
        String badgeCode = badgeReaderSimulator.readBadgeCode(readerId, badgeId);
        
        // 验证结果
        assertNotNull(badgeCode);
        assertTrue(badgeCode.startsWith("SIM_"));
        assertTrue(badgeCode.contains(badgeId));
    }

    @Test
    public void updateReaderStatus_existingReader_shouldUpdateStatus() {
        // 准备测试数据
        String readerId = "READER_001";
        String newStatus = "MAINTENANCE";
        
        BadgeReader badgeReader = new BadgeReader(readerId, "Test Reader", "Location", "ONLINE", "RESOURCE_001");
        
        // 模拟依赖行为
        when(badgeReaderRepository.findByReaderId(readerId)).thenReturn(Optional.of(badgeReader));
        when(badgeReaderRepository.save(any(BadgeReader.class))).thenReturn(badgeReader);
        
        // 执行测试
        badgeReaderSimulator.updateReaderStatus(readerId, newStatus);
        
        // 验证交互
        verify(badgeReaderRepository, times(1)).findByReaderId(readerId);
        verify(badgeReaderRepository, times(1)).save(badgeReader);
        assertEquals(newStatus, badgeReader.getStatus());
    }
}