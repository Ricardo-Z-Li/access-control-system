package acs.simulator;

import acs.domain.Badge;
import acs.domain.BadgeStatus;
import acs.repository.BadgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 徽章代码更新服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
public class BadgeCodeUpdateServiceImplTest {

    @Mock
    private BadgeRepository badgeRepository;

    @InjectMocks
    private BadgeCodeUpdateServiceImpl badgeCodeUpdateService;

    @Captor
    private ArgumentCaptor<Badge> badgeCaptor;

    private Badge activeBadge;
    private Badge expiredBadge;
    private Badge disabledBadge;

    @BeforeEach
    void setUp() {
        // 重置统计信息
        badgeCodeUpdateService.resetStats();

        // 创建测试徽章
        activeBadge = new Badge("BADGE_ACTIVE", BadgeStatus.ACTIVE);
        activeBadge.setExpirationDate(LocalDate.now().plusYears(1));
        activeBadge.setCodeExpirationDate(LocalDate.now().plusYears(1));
        activeBadge.setBadgeCode("OLD_CODE_123");

        expiredBadge = new Badge("BADGE_EXPIRED", BadgeStatus.ACTIVE);
        expiredBadge.setExpirationDate(LocalDate.now().minusDays(1));
        expiredBadge.setCodeExpirationDate(LocalDate.now().minusDays(1));
        expiredBadge.setBadgeCode("OLD_CODE_EXPIRED");

        disabledBadge = new Badge("BADGE_DISABLED", BadgeStatus.DISABLED);
        disabledBadge.setExpirationDate(LocalDate.now().plusYears(1));
        disabledBadge.setCodeExpirationDate(LocalDate.now().plusYears(1));
    }

    @Test
    void checkBadgeNeedsUpdate_activeBadgeFarFromExpiry_shouldReturnFalse() {
        // 给定：有效徽章，距离过期还有很长时间
        when(badgeRepository.findById("BADGE_ACTIVE")).thenReturn(Optional.of(activeBadge));

        // 当：检查是否需要更新
        boolean result = badgeCodeUpdateService.checkBadgeNeedsUpdate("BADGE_ACTIVE");

        // 则：返回false
        assertThat(result).isFalse();
        verify(badgeRepository, times(1)).findById("BADGE_ACTIVE");
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void checkBadgeNeedsUpdate_expiredBadge_shouldReturnTrueAndSetNeedsUpdate() {
        // 给定：过期徽章
        when(badgeRepository.findById("BADGE_EXPIRED")).thenReturn(Optional.of(expiredBadge));
        when(badgeRepository.save(any(Badge.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当：检查是否需要更新
        boolean result = badgeCodeUpdateService.checkBadgeNeedsUpdate("BADGE_EXPIRED");

        // 则：返回true，并设置needsUpdate和updateDueDate
        assertThat(result).isTrue();
        verify(badgeRepository).save(badgeCaptor.capture());
        Badge savedBadge = badgeCaptor.getValue();
        assertThat(savedBadge.isNeedsUpdate()).isTrue();
        assertThat(savedBadge.getUpdateDueDate()).isEqualTo(LocalDate.now().plusDays(14)); // UPDATE_WINDOW_DAYS
    }

    @Test
    void checkBadgeNeedsUpdate_disabledBadge_shouldReturnFalse() {
        // 给定：禁用徽章
        when(badgeRepository.findById("BADGE_DISABLED")).thenReturn(Optional.of(disabledBadge));

        // 当：检查是否需要更新
        boolean result = badgeCodeUpdateService.checkBadgeNeedsUpdate("BADGE_DISABLED");

        // 则：返回false，不保存
        assertThat(result).isFalse();
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void checkBadgeNeedsUpdate_badgeWithUpdateDueDatePassed_shouldDisableBadge() {
        // 给定：需要更新且更新截止日期已过的徽章
        Badge overdueBadge = new Badge("BADGE_OVERDUE", BadgeStatus.ACTIVE);
        overdueBadge.setExpirationDate(LocalDate.now().minusDays(10));
        overdueBadge.setCodeExpirationDate(LocalDate.now().minusDays(10));
        overdueBadge.setNeedsUpdate(true);
        overdueBadge.setUpdateDueDate(LocalDate.now().minusDays(1)); // 已过期

        when(badgeRepository.findById("BADGE_OVERDUE")).thenReturn(Optional.of(overdueBadge));
        when(badgeRepository.save(any(Badge.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当：检查是否需要更新
        boolean result = badgeCodeUpdateService.checkBadgeNeedsUpdate("BADGE_OVERDUE");

        // 则：返回false，徽章被禁用
        assertThat(result).isFalse();
        verify(badgeRepository).save(badgeCaptor.capture());
        Badge savedBadge = badgeCaptor.getValue();
        assertThat(savedBadge.getStatus()).isEqualTo(BadgeStatus.DISABLED);
    }

    @Test
    void updateBadgeCode_validBadgeNeedsUpdate_shouldUpdateCodeAndResetFlags() {
        // 给定：需要更新的徽章
        Badge badgeNeedsUpdate = new Badge("BADGE_NEEDS_UPDATE", BadgeStatus.ACTIVE);
        badgeNeedsUpdate.setExpirationDate(LocalDate.now().minusDays(1));
        badgeNeedsUpdate.setCodeExpirationDate(LocalDate.now().minusDays(1));
        badgeNeedsUpdate.setNeedsUpdate(true);
        badgeNeedsUpdate.setUpdateDueDate(LocalDate.now().plusDays(5));

        when(badgeRepository.findById("BADGE_NEEDS_UPDATE")).thenReturn(Optional.of(badgeNeedsUpdate));
        when(badgeRepository.save(any(Badge.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当：执行更新
        Badge updatedBadge = badgeCodeUpdateService.updateBadgeCode("BADGE_NEEDS_UPDATE");

        // 则：返回更新后的徽章，重置标志
        assertThat(updatedBadge).isNotNull();
        assertThat(updatedBadge.isNeedsUpdate()).isFalse();
        assertThat(updatedBadge.getUpdateDueDate()).isNull();
        assertThat(updatedBadge.getBadgeCode()).startsWith("UPD_");
        assertThat(updatedBadge.getExpirationDate()).isEqualTo(LocalDate.now().plusYears(1));
        assertThat(updatedBadge.getCodeExpirationDate()).isEqualTo(LocalDate.now().plusYears(1));
        assertThat(updatedBadge.getLastCodeUpdate()).isNotNull();
    }

    @Test
    void updateBadgeCode_badgeDoesNotNeedUpdate_shouldReturnNull() {
        // 给定：不需要更新的徽章
        when(badgeRepository.findById("BADGE_ACTIVE")).thenReturn(Optional.of(activeBadge));

        // 当：执行更新
        Badge updatedBadge = badgeCodeUpdateService.updateBadgeCode("BADGE_ACTIVE");

        // 则：返回null
        assertThat(updatedBadge).isNull();
        verify(badgeRepository, never()).save(any());
    }

    @Test
    void generateNewBadgeCode_shouldGenerateValidFormat() {
        // 当：生成新徽章代码
        String newCode = badgeCodeUpdateService.generateNewBadgeCode("ANY_ID");

        // 则：代码格式正确
        assertThat(newCode).startsWith("UPD_");
        assertThat(newCode).contains("_");
        // 总长度至少为前缀+随机部分+时间戳
        assertThat(newCode.length()).isGreaterThan(20);
    }

    @Test
    void checkAllBadgesForUpdate_withMultipleBadges_shouldReturnNeedingUpdateIds() {
        // 给定：多个徽章
        when(badgeRepository.findAll()).thenReturn(java.util.List.of(activeBadge, expiredBadge, disabledBadge));
        // 为每个徽章ID设置stub
        when(badgeRepository.findById("BADGE_ACTIVE")).thenReturn(Optional.of(activeBadge));
        when(badgeRepository.findById("BADGE_EXPIRED")).thenReturn(Optional.of(expiredBadge));
        when(badgeRepository.findById("BADGE_DISABLED")).thenReturn(Optional.of(disabledBadge));
        when(badgeRepository.save(any(Badge.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当：检查所有徽章
        var badgesNeedingUpdate = badgeCodeUpdateService.checkAllBadgesForUpdate();

        // 则：只返回需要更新的徽章ID
        assertThat(badgesNeedingUpdate).containsExactly("BADGE_EXPIRED");
        assertThat(badgesNeedingUpdate).doesNotContain("BADGE_ACTIVE", "BADGE_DISABLED");
    }

    @Test
    void getUpdateStats_afterOperations_shouldReturnValidStats() {
        // 给定：执行一些操作
        when(badgeRepository.findById("BADGE_EXPIRED")).thenReturn(Optional.of(expiredBadge));
        when(badgeRepository.save(any(Badge.class))).thenAnswer(inv -> inv.getArgument(0));

        badgeCodeUpdateService.checkBadgeNeedsUpdate("BADGE_EXPIRED");
        badgeCodeUpdateService.checkBadgeNeedsUpdate("BADGE_EXPIRED");

        // 当：获取统计信息
        String stats = badgeCodeUpdateService.getUpdateStats();

        // 则：统计信息包含预期数据
        assertThat(stats).contains("total checks=2");
        assertThat(stats).contains("updates triggered=0"); // 未触发更新
        assertThat(stats).contains("notifications sent=2"); // 两次检查都发送了通知
    }
}