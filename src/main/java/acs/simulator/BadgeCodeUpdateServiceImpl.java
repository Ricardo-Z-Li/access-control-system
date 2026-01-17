package acs.simulator;

import acs.domain.Badge;
import acs.domain.BadgeUpdateStatus;
import acs.repository.BadgeRepository;
import acs.service.ClockService;
import acs.cache.LocalCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Badge code update service implementation for simulating update workflows.
 * Includes expiry checks, new code generation, and update notifications.
 */
@Service
@Transactional
public class BadgeCodeUpdateServiceImpl implements BadgeCodeUpdateService {

    private final BadgeRepository badgeRepository;
    private final ClockService clockService;
    private final LocalCacheManager cacheManager;
    private final Random random = new Random();
    
    // Update statistics
    private final AtomicInteger totalChecks = new AtomicInteger(0);
    private final AtomicInteger updatesTriggered = new AtomicInteger(0);
    private final AtomicInteger notificationsSent = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Integer> updateAttempts = new ConcurrentHashMap<>();
    
    // Configuration parameters
    private static final int DAYS_BEFORE_EXPIRY_TO_NOTIFY = 30; // Start notifying 30 days before expiry
    private static final int UPDATE_WINDOW_DAYS = 14; // Update window in days
    private static final int BADGE_CODE_LENGTH = 9; // New badge code length (matches existing format e.g., ABC123XYZ)
    
    @Autowired
    public BadgeCodeUpdateServiceImpl(BadgeRepository badgeRepository, ClockService clockService, LocalCacheManager cacheManager) {
        this.badgeRepository = badgeRepository;
        this.clockService = clockService;
        this.cacheManager = cacheManager;
    }

    @Override
    public boolean checkBadgeNeedsUpdate(String badgeId) {
        totalChecks.incrementAndGet();

        return badgeRepository.findById(badgeId)
                .map(badge -> evaluateBadgeUpdateStatusInternal(badge, getCurrentLocalDate(), true)
                        == BadgeUpdateStatus.UPDATE_REQUIRED)
                .orElse(false);
    }

    @Override
    public BadgeUpdateStatus evaluateBadgeUpdateStatus(String badgeId, Instant timestamp) {
        if (timestamp == null) {
            return BadgeUpdateStatus.OK;
        }
        LocalDate today = LocalDate.ofInstant(timestamp, ZoneId.systemDefault());
        return badgeRepository.findById(badgeId)
                .map(badge -> evaluateBadgeUpdateStatusInternal(badge, today, false))
                .orElse(BadgeUpdateStatus.OK);
    }

    @Override
    public String generateNewBadgeCode(String badgeId) {
        // Generate a random badge code matching existing format (9 uppercase alphanumeric chars)
        StringBuilder code = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        
        for (int i = 0; i < BADGE_CODE_LENGTH; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return code.toString();
    }

    @Override
    public Badge updateBadgeCode(String badgeId) {
        updateAttempts.merge(badgeId, 1, Integer::sum);
        
        return badgeRepository.findById(badgeId)
                .map(badge -> {
                    // Check whether an update is actually needed
                    if (!checkBadgeNeedsUpdate(badgeId)) {
                        return null; // No update needed
                    }
                    
                    // Generate a new badge code
                    String newCode = generateNewBadgeCode(badgeId);
                    
                    // Update badge
                    badge.setBadgeCode(newCode);
                    badge.setLastUpdated(getCurrentInstant());
                    badge.setLastCodeUpdate(getCurrentInstant());
                    
                     // Extend code expiry date (simulate: +1 year)
                     LocalDate newExpiryDate = getCurrentLocalDate().plusYears(1);
                     badge.setCodeExpirationDate(newExpiryDate);
                    
                    // Reset update flag and due date
                    badge.setNeedsUpdate(false);
                    badge.setUpdateDueDate(null);
                    
                    // Save update via cache manager (ensures cache consistency)
                    cacheManager.updateBadge(badge);
                    updatesTriggered.incrementAndGet();
                    
                    return badge;
                })
                .orElse(null);
    }

    @Override
    public List<String> checkAllBadgesForUpdate() {
        List<String> badgesNeedingUpdate = new ArrayList<>();
        
        badgeRepository.findAll().forEach(badge -> {
            if (checkBadgeNeedsUpdate(badge.getBadgeId())) {
                badgesNeedingUpdate.add(badge.getBadgeId());
            }
        });
        
        return badgesNeedingUpdate;
    }

    @Override
    public void simulateUpdateNotification(String badgeId, int daysUntilExpiry) {
        // Simulate sending a notification (email, SMS, or UI in real systems)
        String message;
        if (daysUntilExpiry > 0) {
            message = String.format("Badge %s will expire in %d days. Please update.", badgeId, daysUntilExpiry);
        } else if (daysUntilExpiry == 0) {
            message = String.format("Badge %s expires today. Please update now.", badgeId);
        } else {
            message = String.format("Badge %s expired %d days ago. Please update now.", badgeId, -daysUntilExpiry);
        }
        
        // In a real system, this would call a notification service.
        // For now, log the message.
        System.out.println("[Badge Update Notification] " + message);
        notificationsSent.incrementAndGet();
    }

    @Override
    public String getUpdateStats() {
        return String.format("Badge update stats: total checks=%d, updates triggered=%d, notifications sent=%d, badges attempted=%d",
                totalChecks.get(), updatesTriggered.get(), notificationsSent.get(), updateAttempts.size());
    }
    

    private BadgeUpdateStatus evaluateBadgeUpdateStatusInternal(Badge badge, LocalDate today, boolean notify) {
        if (badge == null || today == null) {
            return BadgeUpdateStatus.OK;
        }

        if (badge.getStatus() != acs.domain.BadgeStatus.ACTIVE) {
            return BadgeUpdateStatus.OK;
        }

         LocalDate codeExpiryDate = badge.getCodeExpirationDate();
         if (codeExpiryDate == null) {
             return BadgeUpdateStatus.OK;
         }

        long daysUntilCodeExpiry = ChronoUnit.DAYS.between(today, codeExpiryDate);
        LocalDate updateDue = badge.getUpdateDueDate();

        if (updateDue != null && today.isAfter(updateDue)) {
            badge.setStatus(acs.domain.BadgeStatus.DISABLED);
            cacheManager.updateBadge(badge);
            if (notify) {
                simulateUpdateNotification(badge.getBadgeId(), (int) daysUntilCodeExpiry);
            }
            return BadgeUpdateStatus.UPDATE_OVERDUE;
        }

        if (today.isAfter(codeExpiryDate)) {
            badge.setNeedsUpdate(true);
            if (updateDue == null || updateDue.isBefore(codeExpiryDate.plusDays(UPDATE_WINDOW_DAYS))) {
                badge.setUpdateDueDate(codeExpiryDate.plusDays(UPDATE_WINDOW_DAYS));
            }
            cacheManager.updateBadge(badge);
            if (notify) {
                simulateUpdateNotification(badge.getBadgeId(), (int) daysUntilCodeExpiry);
            }
            return BadgeUpdateStatus.UPDATE_REQUIRED;
        }

        if (daysUntilCodeExpiry <= DAYS_BEFORE_EXPIRY_TO_NOTIFY) {
            if (notify) {
                simulateUpdateNotification(badge.getBadgeId(), (int) daysUntilCodeExpiry);
            }
        }

        if (badge.isNeedsUpdate()) {
            return BadgeUpdateStatus.UPDATE_REQUIRED;
        }

        return BadgeUpdateStatus.OK;
    }

    /**
     * Reset statistics (for testing).
     */
    public void resetStats() {
        totalChecks.set(0);
        updatesTriggered.set(0);
        notificationsSent.set(0);
        updateAttempts.clear();
    }
    
    private LocalDate getCurrentLocalDate() {
        return clockService.localNow().toLocalDate();
    }
    
    private Instant getCurrentInstant() {
        return clockService.now();
    }
    
    /**
     * Get update attempt count for a badge.
     */
    public int getUpdateAttempts(String badgeId) {
        return updateAttempts.getOrDefault(badgeId, 0);
    }

    @Override
    public Badge getBadge(String badgeId) {
        return badgeRepository.findById(badgeId).orElse(null);
    }
}
