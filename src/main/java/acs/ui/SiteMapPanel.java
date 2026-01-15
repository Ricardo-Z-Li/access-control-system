package acs.ui;

import acs.cache.LocalCacheManager;
import acs.domain.AccessDecision;
import acs.domain.LogEntry;
import acs.domain.Resource;
import acs.domain.ResourceState;
import acs.domain.ResourceType;
import acs.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Site resource map / access visualization panel.
 */
@Component
@Profile("!test")
public class SiteMapPanel extends JPanel {

    private static final int DOT_RADIUS = 6;
    private static final int DOT_HIT_RADIUS = 14;
    private static final int GRID_STEP_X = 46;
    private static final int GRID_STEP_Y = 36;
    private static final int GRID_START_X = 50;
    private static final int GRID_START_Y = 70;
    private static final int MIN_DOT_SPACING = 32;
    private static final long FLASH_WINDOW_MS = 8000;
    private static final long CLICK_PULSE_DURATION_MS = 900;
    private static final long TOAST_DURATION_MS = 2200;
    private static final long HOVER_FADE_MS = 180;
    private static final int PLACEHOLDER_STRIPE_GAP = 36;

    private final ResourceRepository resourceRepository;
    private final LocalCacheManager cacheManager;

    private List<Resource> resources;
    private Timer refreshTimer;
    private Timer flashTimer;
    private Timer placeholderTimer;
    private boolean flashOn = true;
    private float placeholderPhase = 0f;

    private final Map<String, Rectangle> resourceBounds = new HashMap<>();
    private final Map<String, Point> resourceCenters = new HashMap<>();
    private final Map<String, Long> clickTimes = new HashMap<>();
    private String hoveredResourceId;
    private Point hoverPoint;
    private long hoverAt;
    private String toastMessage;
    private long toastAt;

    private BufferedImage officeLayoutImage;
    private LayoutType currentLayout = LayoutType.OFFICE;

    private final Map<String, AccessDecision> recentDecisions = new HashMap<>();
    private final Map<String, Long> decisionTimes = new HashMap<>();
    private MapCanvas mapCanvas;
    private JLabel hintLabel;
    private JLabel layoutLabel;
    private JToggleButton officeButton;

    private static final Color CANVAS_BG = new Color(235, 240, 246);
    private static final Color CANVAS_DEEP = new Color(221, 228, 238);
    private static final Color GRID_COLOR = new Color(148, 163, 184, 48);
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color TEXT_ACCENT = new Color(30, 64, 175);

    private static final Font TITLE_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 20);
    private static final Font SUBTITLE_FONT = new Font("Consolas", Font.PLAIN, 12);
    private static final Font LABEL_FONT = new Font("Microsoft YaHei UI", Font.PLAIN, 12);
    private static final Font MONO_FONT = new Font("Consolas", Font.PLAIN, 11);

    private enum LayoutType {
        OFFICE
    }

    @Autowired
    public SiteMapPanel(ResourceRepository resourceRepository, LocalCacheManager cacheManager) {
        this.resourceRepository = resourceRepository;
        this.cacheManager = cacheManager;
        loadLayoutImages();
        initUI();
        loadResources();
        startRefreshTimer();
        startFlashTimer();
        startPlaceholderTimer();
        applyLayout(currentLayout);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        mapCanvas = new MapCanvas();
        mapCanvas.setBorder(BorderFactory.createEmptyBorder());
        JPanel mapWrapper = new JPanel(new BorderLayout());
        mapWrapper.setBackground(UiTheme.surface());
        mapWrapper.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UiTheme.border(), 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        mapWrapper.add(mapCanvas, BorderLayout.CENTER);
        add(mapWrapper, BorderLayout.CENTER);

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UiTheme.surface());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.border()),
            BorderFactory.createEmptyBorder(14, 18, 10, 18)
        ));

        JLabel titleLabel = new JLabel("Site Resource Map");
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setFont(TITLE_FONT);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        layoutLabel = new JLabel("Office Layout");
        layoutLabel.setForeground(new Color(30, 64, 175));
        layoutLabel.setFont(SUBTITLE_FONT);
        layoutLabel.setOpaque(true);
        layoutLabel.setBackground(UiTheme.accentSoft());
        layoutLabel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(148, 163, 184, 120), 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        headerPanel.add(layoutLabel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(new Color(238, 243, 249));
        controlPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(148, 163, 184, 80)));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        leftActions.setOpaque(false);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadResources());
        styleButton(refreshButton, new Color(223, 232, 244), TEXT_PRIMARY);
        leftActions.add(refreshButton);

        officeButton = new JToggleButton("Office Layout");
        ButtonGroup group = new ButtonGroup();
        group.add(officeButton);
        styleToggleButton(officeButton, new Color(223, 232, 244));
        officeButton.addActionListener(e -> applyLayout(LayoutType.OFFICE));
        leftActions.add(officeButton);
        controlPanel.add(leftActions, BorderLayout.WEST);

        hintLabel = new JLabel("Hover for details - Left click to toggle - Right click for menu - Auto-fit image");
        hintLabel.setForeground(TEXT_ACCENT);
        hintLabel.setFont(SUBTITLE_FONT);
        JPanel rightMeta = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        rightMeta.setOpaque(false);
        rightMeta.add(hintLabel);
        controlPanel.add(rightMeta, BorderLayout.EAST);

        return controlPanel;
    }

    private void styleButton(AbstractButton button, Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(148, 163, 184, 120)),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        button.setOpaque(true);
    }

    private void styleToggleButton(JToggleButton button, Color background) {
        styleButton(button, background, TEXT_PRIMARY);
    }

    private void applyLayout(LayoutType type) {
        currentLayout = LayoutType.OFFICE;
        if (officeButton != null) {
            officeButton.setSelected(true);
        }
        if (layoutLabel != null) {
            layoutLabel.setText("Layout: Office");
        }
        updateCanvasSize();
        repaint();
    }

    private void updateCanvasSize() {
        if (mapCanvas != null) {
            mapCanvas.revalidate();
            mapCanvas.repaint();
        }
    }

    private void loadResources() {
        resources = resourceRepository.findAll();
        updateRecentDecisions();
        repaint();
    }

    private void updateRecentDecisions() {
        recentDecisions.clear();
        decisionTimes.clear();
        if (cacheManager == null || cacheManager.getLogs() == null) {
            return;
        }
        for (LogEntry entry : cacheManager.getLogs()) {
            if (entry.getResource() == null || entry.getDecision() == null || entry.getTimestamp() == null) {
                continue;
            }
            String resourceId = entry.getResource().getResourceId();
            long ts = entry.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            Long existingTs = decisionTimes.get(resourceId);
            if (existingTs == null || ts >= existingTs) {
                decisionTimes.put(resourceId, ts);
                recentDecisions.put(resourceId, entry.getDecision());
            }
        }
    }

    private void drawSiteMap(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (resources == null || resources.isEmpty()) {
            drawEmptyState(g2);
            g2.dispose();
            return;
        }

        int panelWidth = mapCanvas != null ? mapCanvas.getWidth() : getWidth();
        int panelHeight = mapCanvas != null ? mapCanvas.getHeight() : getHeight();

        BufferedImage bg = officeLayoutImage;
        LayoutMetrics metrics = calculateLayoutMetrics(bg, panelWidth, panelHeight);
        if (bg != null) {
            g2.drawImage(bg, metrics.offsetX, metrics.offsetY, metrics.drawWidth, metrics.drawHeight, null);
            paintOverlay(g2, metrics);
        } else {
            drawDynamicPlaceholder(g2, panelWidth, panelHeight);
        }

        int fallbackIndex = 0;
        Map<ResourceType, Integer> typeIndex = new EnumMap<>(ResourceType.class);
        List<Point> placedPoints = new ArrayList<>();
        resourceBounds.clear();
        resourceCenters.clear();
        List<Resource> sortedResources = new ArrayList<>(resources);
        sortedResources.sort(Comparator.comparing(
            Resource::getResourceId,
            Comparator.nullsLast(String::compareToIgnoreCase)
        ));
        for (Resource resource : sortedResources) {
            Point basePoint = resolveBasePoint(resource, bg, typeIndex);
            if (basePoint == null) {
                int col = fallbackIndex % 12;
                int row = fallbackIndex / 12;
                basePoint = new Point(GRID_START_X + col * GRID_STEP_X, GRID_START_Y + row * GRID_STEP_Y);
                fallbackIndex++;
            }

            Point placed = spreadPoint(basePoint, placedPoints, bg);
            placedPoints.add(placed);

            int centerX = metrics.offsetX + (int) Math.round(placed.x * metrics.scale);
            int centerY = metrics.offsetY + (int) Math.round(placed.y * metrics.scale);

            resourceCenters.put(resource.getResourceId(), new Point(centerX, centerY));
            resourceBounds.put(resource.getResourceId(),
                new Rectangle(centerX - metrics.hitRadius, centerY - metrics.hitRadius, metrics.hitRadius * 2, metrics.hitRadius * 2));

            drawResourceDot(g2, resource, centerX, centerY, metrics.dotRadius, metrics.scale);
        }

        drawHoverInfo(g2);
        drawToast(g2);
        g2.dispose();
    }

    private void drawResourceDot(Graphics2D g2, Resource resource, int centerX, int centerY, int dotRadius, double scale) {
        Color stateColor = getResourceColor(resource);

        int glowRadius = Math.max(dotRadius * 2, dotRadius + 4);
        int ringOffset = Math.max(4, (int) Math.round(4 * scale));

        Ellipse2D outerGlow = new Ellipse2D.Float(centerX - glowRadius, centerY - glowRadius, glowRadius * 2, glowRadius * 2);
        g2.setColor(new Color(stateColor.getRed(), stateColor.getGreen(), stateColor.getBlue(), 60));
        g2.fill(outerGlow);

        Ellipse2D dot = new Ellipse2D.Float(centerX - dotRadius, centerY - dotRadius, dotRadius * 2, dotRadius * 2);
        g2.setColor(stateColor);
        g2.fill(dot);

        g2.setColor(new Color(255, 255, 255, 160));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(dot);

        if (resource.getResourceId() != null && resource.getResourceId().equals(hoveredResourceId)) {
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(new Ellipse2D.Float(centerX - dotRadius - ringOffset, centerY - dotRadius - ringOffset,
                (dotRadius + ringOffset) * 2, (dotRadius + ringOffset) * 2));
        }

        if (shouldFlash(resource.getResourceId()) && flashOn) {
            AccessDecision decision = recentDecisions.get(resource.getResourceId());
            Color flashColor = AccessDecision.ALLOW.equals(decision) ? new Color(50, 220, 120) : new Color(235, 80, 80);
            g2.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(), 200));
            g2.setStroke(new BasicStroke(2.2f));
            int flashOffset = Math.max(6, (int) Math.round(6 * scale));
            g2.draw(new Ellipse2D.Float(centerX - dotRadius - flashOffset, centerY - dotRadius - flashOffset,
                (dotRadius + flashOffset) * 2, (dotRadius + flashOffset) * 2));
        }

        Long clickTime = clickTimes.get(resource.getResourceId());
        if (clickTime != null) {
            long elapsed = System.currentTimeMillis() - clickTime;
            if (elapsed <= CLICK_PULSE_DURATION_MS) {
                float progress = elapsed / (float) CLICK_PULSE_DURATION_MS;
                int pulseAlpha = (int) (140 * (1 - progress));
                int expand = (int) Math.round(10 * progress * scale);
                g2.setColor(new Color(255, 255, 255, pulseAlpha));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Ellipse2D.Float(centerX - dotRadius - expand, centerY - dotRadius - expand,
                    (dotRadius + expand) * 2, (dotRadius + expand) * 2));
            }
        }
    }

    private void drawHoverInfo(Graphics2D g2) {
        if (hoveredResourceId == null || hoverPoint == null) {
            return;
        }
        Resource resource = findResourceById(hoveredResourceId);
        if (resource == null) {
            return;
        }
        long age = System.currentTimeMillis() - hoverAt;
        float alpha = Math.min(1f, age / (float) HOVER_FADE_MS);
        float floatOffset = (float) Math.sin(System.currentTimeMillis() / 200.0) * 3f;

        int cardWidth = 240;
        int cardHeight = 84;
        int x = Math.min(hoverPoint.x + 16, mapCanvas.getWidth() - cardWidth - 12);
        int y = hoverPoint.y - cardHeight - 16;
        if (y < 10) {
            y = hoverPoint.y + 16;
        }

        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.92f * alpha));
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillRoundRect(x, y + (int) floatOffset, cardWidth, cardHeight, 14, 14);
        g2.setColor(new Color(148, 163, 184, 120));
        g2.drawRoundRect(x, y + (int) floatOffset, cardWidth, cardHeight, 14, 14);

        g2.setFont(LABEL_FONT);
        g2.setColor(TEXT_PRIMARY);
        String name = safeName(resource.getResourceName(), 16);
        g2.drawString(name, x + 12, y + 20 + (int) floatOffset);

        g2.setFont(MONO_FONT);
        g2.setColor(TEXT_MUTED);
        String idText = "ID: " + (resource.getResourceId() != null ? resource.getResourceId() : "N/A");
        g2.drawString(idText, x + 12, y + 38 + (int) floatOffset);

        ResourceState state = resource.getResourceState();
        ResourceType type = resource.getResourceType();
        String stateText = "State: " + (state != null ? state.name() : "UNKNOWN");
        String typeText = "Type: " + (type != null ? type.name() : "UNKNOWN");
        g2.drawString(stateText, x + 12, y + 56 + (int) floatOffset);
        g2.drawString(typeText, x + 12, y + 72 + (int) floatOffset);

        g2.setComposite(old);
    }

    private void drawEmptyState(Graphics2D g2) {
        int w = mapCanvas != null ? mapCanvas.getWidth() : getWidth();
        int h = mapCanvas != null ? mapCanvas.getHeight() : getHeight();
        drawDynamicPlaceholder(g2, w, h);
        g2.setColor(TEXT_PRIMARY);
        g2.setFont(TITLE_FONT);
        g2.drawString("No resource data", 80, 120);
        g2.setFont(SUBTITLE_FONT);
        g2.setColor(TEXT_MUTED);
        g2.drawString("Refresh or check resource configuration", 80, 150);
    }

    private void drawDynamicPlaceholder(Graphics2D g2, int w, int h) {
        GradientPaint paint = new GradientPaint(0, 0, CANVAS_BG, w, h, CANVAS_DEEP);
        g2.setPaint(paint);
        g2.fillRect(0, 0, w, h);

        g2.setColor(GRID_COLOR);
        for (int x = 0; x < w; x += 48) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += 48) {
            g2.drawLine(0, y, w, y);
        }

        int offset = (int) (placeholderPhase % PLACEHOLDER_STRIPE_GAP);
        g2.setColor(new Color(148, 163, 184, 40));
        g2.setStroke(new BasicStroke(2f));
        for (int x = -h; x < w + h; x += PLACEHOLDER_STRIPE_GAP) {
            g2.drawLine(x + offset, 0, x - h + offset, h);
        }

        g2.setColor(new Color(71, 85, 105, 160));
        g2.setFont(LABEL_FONT);
        g2.drawString("Layout placeholder rendering", 60, 80);
    }

    private void paintOverlay(Graphics2D g2, LayoutMetrics metrics) {
        g2.setColor(new Color(15, 23, 42, 26));
        g2.fillRect(metrics.offsetX, metrics.offsetY, metrics.drawWidth, metrics.drawHeight);
        g2.setColor(GRID_COLOR);
        for (int x = metrics.offsetX; x < metrics.offsetX + metrics.drawWidth; x += 120) {
            g2.drawLine(x, metrics.offsetY, x, metrics.offsetY + metrics.drawHeight);
        }
        for (int y = metrics.offsetY; y < metrics.offsetY + metrics.drawHeight; y += 120) {
            g2.drawLine(metrics.offsetX, y, metrics.offsetX + metrics.drawWidth, y);
        }
    }

    private LayoutMetrics calculateLayoutMetrics(BufferedImage bg, int panelWidth, int panelHeight) {
        LayoutMetrics metrics = new LayoutMetrics();
        if (panelWidth <= 0 || panelHeight <= 0) {
            metrics.scale = 1.0;
            metrics.drawWidth = Math.max(panelWidth, 1);
            metrics.drawHeight = Math.max(panelHeight, 1);
            metrics.offsetX = 0;
            metrics.offsetY = 0;
            metrics.dotRadius = DOT_RADIUS;
            metrics.hitRadius = DOT_HIT_RADIUS;
            return metrics;
        }

        if (bg == null) {
            metrics.scale = 1.0;
            metrics.drawWidth = panelWidth;
            metrics.drawHeight = panelHeight;
            metrics.offsetX = 0;
            metrics.offsetY = 0;
            metrics.dotRadius = DOT_RADIUS;
            metrics.hitRadius = DOT_HIT_RADIUS;
            return metrics;
        }

        double scale = Math.min(panelWidth / (double) bg.getWidth(), panelHeight / (double) bg.getHeight());
        int drawWidth = (int) Math.round(bg.getWidth() * scale);
        int drawHeight = (int) Math.round(bg.getHeight() * scale);
        int offsetX = (panelWidth - drawWidth) / 2;
        int offsetY = (panelHeight - drawHeight) / 2;

        metrics.scale = scale;
        metrics.drawWidth = drawWidth;
        metrics.drawHeight = drawHeight;
        metrics.offsetX = offsetX;
        metrics.offsetY = offsetY;
        metrics.dotRadius = Math.max(4, (int) Math.round(DOT_RADIUS * scale));
        metrics.hitRadius = Math.max(metrics.dotRadius + 6, (int) Math.round(DOT_HIT_RADIUS * scale));
        return metrics;
    }

    private Point resolveBasePoint(Resource resource,
                                   BufferedImage bg,
                                   Map<ResourceType, Integer> typeIndex) {
        if (resource == null) {
            return null;
        }
        Integer coordX = resource.getCoordX();
        Integer coordY = resource.getCoordY();
        if (coordX != null && coordY != null) {
            return new Point(coordX, coordY);
        }
        if (bg == null) {
            return null;
        }
        ResourceType type = resource.getResourceType();
        if (type == null) {
            type = ResourceType.OTHER;
        }
        int index = typeIndex.getOrDefault(type, 0);
        typeIndex.put(type, index + 1);

        Rectangle[] regions = getPlacementRegions(currentLayout, type, bg);
        if (regions.length == 0) {
            return null;
        }
        int regionIndex = index % regions.length;
        Rectangle region = regions[regionIndex];
        int padding = 12;
        int regionWidth = Math.max(region.width - padding * 2, 1);
        int regionHeight = Math.max(region.height - padding * 2, 1);
        int cols = Math.max(1, regionWidth / MIN_DOT_SPACING);
        int rows = Math.max(1, regionHeight / MIN_DOT_SPACING);
        int maxSlots = Math.max(1, cols * rows);
        int slot = (index / regions.length) % maxSlots;
        int col = slot % cols;
        int row = slot / cols;

        int x = region.x + padding + col * MIN_DOT_SPACING;
        int y = region.y + padding + row * MIN_DOT_SPACING;
        return applyJitter(new Point(x, y), resource, region);
    }

    private Rectangle[] getPlacementRegions(LayoutType layout, ResourceType type, BufferedImage bg) {
        int w = bg.getWidth();
        int h = bg.getHeight();
        return switch (type) {
            case DOOR -> new Rectangle[]{
                new Rectangle((int) (w * 0.06), (int) (h * 0.12), (int) (w * 0.26), (int) (h * 0.18)),
                new Rectangle((int) (w * 0.10), (int) (h * 0.70), (int) (w * 0.22), (int) (h * 0.18)),
                new Rectangle((int) (w * 0.78), (int) (h * 0.66), (int) (w * 0.16), (int) (h * 0.20))
            };
            case ROOM -> new Rectangle[]{
                new Rectangle((int) (w * 0.18), (int) (h * 0.28), (int) (w * 0.30), (int) (h * 0.32)),
                new Rectangle((int) (w * 0.50), (int) (h * 0.30), (int) (w * 0.26), (int) (h * 0.30)),
                new Rectangle((int) (w * 0.30), (int) (h * 0.58), (int) (w * 0.30), (int) (h * 0.22))
            };
            case COMPUTER -> new Rectangle[]{
                new Rectangle((int) (w * 0.48), (int) (h * 0.16), (int) (w * 0.30), (int) (h * 0.18)),
                new Rectangle((int) (w * 0.56), (int) (h * 0.56), (int) (w * 0.22), (int) (h * 0.20))
            };
            case PRINTER -> new Rectangle[]{
                new Rectangle((int) (w * 0.40), (int) (h * 0.56), (int) (w * 0.18), (int) (h * 0.18)),
                new Rectangle((int) (w * 0.76), (int) (h * 0.46), (int) (w * 0.12), (int) (h * 0.20))
            };
            case OTHER, PENDING -> new Rectangle[]{
                new Rectangle((int) (w * 0.08), (int) (h * 0.46), (int) (w * 0.18), (int) (h * 0.20)),
                new Rectangle((int) (w * 0.78), (int) (h * 0.14), (int) (w * 0.16), (int) (h * 0.16)),
                new Rectangle((int) (w * 0.64), (int) (h * 0.80), (int) (w * 0.20), (int) (h * 0.14))
            };
        };
    }

    private Point applyJitter(Point base, Resource resource, Rectangle region) {
        if (resource == null || base == null || region == null) {
            return base;
        }
        int hash = Math.abs(resource.getResourceId() != null ? resource.getResourceId().hashCode() : 0);
        int jitter = Math.min(8, Math.max(4, MIN_DOT_SPACING / 6));
        int dx = (hash % (jitter * 2 + 1)) - jitter;
        int dy = ((hash / 31) % (jitter * 2 + 1)) - jitter;
        int x = Math.min(Math.max(base.x + dx, region.x + 8), region.x + region.width - 8);
        int y = Math.min(Math.max(base.y + dy, region.y + 8), region.y + region.height - 8);
        return new Point(x, y);
    }

    private Point spreadPoint(Point base, List<Point> placedPoints, BufferedImage bg) {
        if (base == null) {
            return new Point(GRID_START_X, GRID_START_Y);
        }
        int maxX = bg != null ? bg.getWidth() : Integer.MAX_VALUE;
        int maxY = bg != null ? bg.getHeight() : Integer.MAX_VALUE;
        int marginX = Math.max(10, MIN_DOT_SPACING);
        int marginY = Math.max(10, MIN_DOT_SPACING);

        Point clamped = new Point(
            Math.min(Math.max(base.x, marginX), Math.max(marginX, maxX - marginX)),
            Math.min(Math.max(base.y, marginY), Math.max(marginY, maxY - marginY))
        );
        if (isFarEnough(clamped, placedPoints)) {
            return clamped;
        }

        for (int i = 0; i < 60; i++) {
            double angle = i * 0.65;
            int radius = MIN_DOT_SPACING + (i / 8) * 10;
            int x = (int) Math.round(clamped.x + Math.cos(angle) * radius);
            int y = (int) Math.round(clamped.y + Math.sin(angle) * radius);
            Point candidate = new Point(
                Math.min(Math.max(x, marginX), Math.max(marginX, maxX - marginX)),
                Math.min(Math.max(y, marginY), Math.max(marginY, maxY - marginY))
            );
            if (isFarEnough(candidate, placedPoints)) {
                return candidate;
            }
        }

        return clamped;
    }

    private boolean isFarEnough(Point candidate, List<Point> placedPoints) {
        int minDistance = MIN_DOT_SPACING;
        int minDistanceSq = minDistance * minDistance;
        for (Point existing : placedPoints) {
            int dx = candidate.x - existing.x;
            int dy = candidate.y - existing.y;
            if (dx * dx + dy * dy < minDistanceSq) {
                return false;
            }
        }
        return true;
    }

    private static final class LayoutMetrics {
        private double scale;
        private int drawWidth;
        private int drawHeight;
        private int offsetX;
        private int offsetY;
        private int dotRadius;
        private int hitRadius;
    }

    private void drawToast(Graphics2D g2) {
        if (toastMessage == null) {
            return;
        }
        long age = System.currentTimeMillis() - toastAt;
        if (age > TOAST_DURATION_MS) {
            toastMessage = null;
            return;
        }
        float progress = age / (float) TOAST_DURATION_MS;
        int alpha = (int) (200 * (1 - progress));
        int w = mapCanvas != null ? mapCanvas.getWidth() : getWidth();
        int x = Math.max(20, w - 280);
        int y = 16;
        g2.setColor(new Color(255, 255, 255, alpha));
        g2.fillRoundRect(x, y, 240, 28, 16, 16);
        g2.setColor(new Color(30, 41, 59, alpha));
        g2.setFont(SUBTITLE_FONT);
        g2.drawString(toastMessage, x + 12, y + 18);
    }

    private boolean shouldFlash(String resourceId) {
        if (resourceId == null) {
            return false;
        }
        Long ts = decisionTimes.get(resourceId);
        if (ts == null) {
            return false;
        }
        long age = System.currentTimeMillis() - ts;
        return age >= 0 && age <= FLASH_WINDOW_MS;
    }

    private Color getResourceColor(Resource resource) {
        if (resource.getIsControlled() != null && !resource.getIsControlled()) {
            return new Color(120, 128, 140);
        }

        if (resource.getResourceState() == null) {
            return new Color(110, 114, 122);
        }

        switch (resource.getResourceState()) {
            case AVAILABLE:
                return new Color(44, 186, 120);
            case OCCUPIED:
                return new Color(248, 182, 90);
            case LOCKED:
                return new Color(230, 84, 84);
            case OFFLINE:
                return new Color(74, 84, 98);
            case PENDING:
            default:
                return new Color(122, 128, 140);
        }
    }

    private void loadLayoutImages() {
        officeLayoutImage = loadImageFromResources("office-layout.png");
    }

    private BufferedImage loadImageFromResources(String name) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            if (input == null) {
                return null;
            }
            return ImageIO.read(input);
        } catch (Exception e) {
            return null;
        }
    }

    private void startRefreshTimer() {
        refreshTimer = new Timer(10000, e -> loadResources());
        refreshTimer.start();
    }

    private void startFlashTimer() {
        flashTimer = new Timer(500, e -> {
            flashOn = !flashOn;
            repaint();
        });
        flashTimer.start();
    }

    private void startPlaceholderTimer() {
        placeholderTimer = new Timer(80, e -> {
            placeholderPhase += 2f;
            repaint();
        });
        placeholderTimer.start();
    }

    public void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        if (flashTimer != null) {
            flashTimer.stop();
        }
        if (placeholderTimer != null) {
            placeholderTimer.stop();
        }
    }

    private String safeName(String value, int max) {
        if (value == null || value.isEmpty()) {
            return "Unnamed";
        }
        return value.length() > max ? value.substring(0, max) + "..." : value;
    }

    private Resource findResourceById(String resourceId) {
        if (resources == null || resourceId == null) {
            return null;
        }
        for (Resource resource : resources) {
            if (resourceId.equals(resource.getResourceId())) {
                return resource;
            }
        }
        return null;
    }

    private ResourceState nextState(ResourceState current) {
        if (current == null) {
            return ResourceState.PENDING;
        }
        switch (current) {
            case AVAILABLE:
                return ResourceState.OCCUPIED;
            case OCCUPIED:
                return ResourceState.LOCKED;
            case LOCKED:
                return ResourceState.OFFLINE;
            case OFFLINE:
                return ResourceState.PENDING;
            case PENDING:
            default:
                return ResourceState.AVAILABLE;
        }
    }

    private void updateResourceState(Resource resource, ResourceState targetState) {
        if (resource == null || targetState == null) {
            return;
        }
        ResourceState original = resource.getResourceState();
        if (targetState.equals(original)) {
            toastMessage = "Status already " + targetState.name();
            toastAt = System.currentTimeMillis();
            repaint();
            return;
        }
        resource.setResourceState(targetState);
        try {
            resourceRepository.save(resource);
            toastMessage = "Updated " + resource.getResourceId() + " -> " + targetState.name();
            toastAt = System.currentTimeMillis();
            loadResources();
        } catch (Exception ex) {
            resource.setResourceState(original);
            JOptionPane.showMessageDialog(this,
                "Failed to update resource status: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showStateMenu(Resource resource, Point point) {
        JPopupMenu menu = new JPopupMenu();
        for (ResourceState state : ResourceState.values()) {
            JMenuItem item = new JMenuItem(state.name());
            item.addActionListener(e -> updateResourceState(resource, state));
            menu.add(item);
        }
        menu.show(mapCanvas, point.x, point.y);
    }

    private String findResourceIdAt(Point point) {
        for (Map.Entry<String, Rectangle> entry : resourceBounds.entrySet()) {
            if (entry.getValue().contains(point)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private class MapCanvas extends JPanel {
        MapCanvas() {
            setOpaque(true);
            setBackground(CANVAS_BG);
            setPreferredSize(new Dimension(1024, 720));
            setToolTipText("resource");

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    String resourceId = findResourceIdAt(e.getPoint());
                    if (resourceId == null) {
                        return;
                    }
                    Resource resource = findResourceById(resourceId);
                    if (resource == null) {
                        return;
                    }
                    clickTimes.put(resourceId, System.currentTimeMillis());
                    if (SwingUtilities.isRightMouseButton(e)) {
                        showStateMenu(resource, e.getPoint());
                        repaint();
                        return;
                    }
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        updateResourceState(resource, nextState(resource.getResourceState()));
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoveredResourceId = null;
                    hoverPoint = null;
                    repaint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    String resourceId = findResourceIdAt(e.getPoint());
                    if (resourceId != null && !resourceId.equals(hoveredResourceId)) {
                        hoverAt = System.currentTimeMillis();
                    }
                    hoveredResourceId = resourceId;
                    hoverPoint = e.getPoint();
                    if (resourceId != null) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawSiteMap(g);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            String resourceId = findResourceIdAt(event.getPoint());
            Resource resource = findResourceById(resourceId);
            if (resource == null) {
                return null;
            }
            String state = resource.getResourceState() != null ? resource.getResourceState().name() : "UNKNOWN";
            String name = resource.getResourceName() != null ? resource.getResourceName() : "Unnamed";
            return name + " | " + resource.getResourceId() + " | " + state;
        }
    }
}
