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
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站点资源地图/访问可视化面板
 */
@Component
@Profile("!test")
public class SiteMapPanel extends JPanel {

    private static final int DOT_RADIUS = 6;
    private static final int DOT_HIT_RADIUS = 14;
    private static final int GRID_STEP_X = 38;
    private static final int GRID_STEP_Y = 30;
    private static final int GRID_START_X = 40;
    private static final int GRID_START_Y = 60;
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

    private BufferedImage siteLayoutImage;
    private BufferedImage officeLayoutImage;
    private LayoutType currentLayout = LayoutType.SITE;

    private final Map<String, AccessDecision> recentDecisions = new HashMap<>();
    private final Map<String, Long> decisionTimes = new HashMap<>();
    private MapCanvas mapCanvas;
    private JLabel hintLabel;
    private JLabel layoutLabel;
    private JToggleButton siteButton;
    private JToggleButton officeButton;

    private static final Color CANVAS_BG = new Color(12, 16, 24);
    private static final Color CANVAS_DEEP = new Color(6, 8, 14);
    private static final Color GRID_COLOR = new Color(255, 255, 255, 12);
    private static final Color TEXT_PRIMARY = new Color(245, 247, 250);
    private static final Color TEXT_MUTED = new Color(176, 184, 196);
    private static final Color TEXT_ACCENT = new Color(255, 207, 98);

    private static final Font TITLE_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 20);
    private static final Font SUBTITLE_FONT = new Font("Consolas", Font.PLAIN, 12);
    private static final Font LABEL_FONT = new Font("Microsoft YaHei UI", Font.PLAIN, 12);
    private static final Font MONO_FONT = new Font("Consolas", Font.PLAIN, 11);

    private enum LayoutType {
        SITE,
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
        JScrollPane scrollPane = new JScrollPane(mapCanvas);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CANVAS_BG);
        add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint paint = new GradientPaint(0, 0, new Color(26, 34, 50), getWidth(), getHeight(), new Color(8, 10, 18));
                g2.setPaint(paint);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 12, 18));

        JLabel titleLabel = new JLabel("站点资源态势图");
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setFont(TITLE_FONT);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        layoutLabel = new JLabel("布局: 站点");
        layoutLabel.setForeground(TEXT_MUTED);
        layoutLabel.setFont(SUBTITLE_FONT);
        headerPanel.add(layoutLabel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setBackground(new Color(18, 22, 32));
        controlPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 24)));

        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> loadResources());
        styleButton(refreshButton, new Color(32, 44, 62), TEXT_PRIMARY);
        controlPanel.add(refreshButton);

        ButtonGroup group = new ButtonGroup();
        siteButton = new JToggleButton("站点布局");
        officeButton = new JToggleButton("办公布局");
        group.add(siteButton);
        group.add(officeButton);
        styleToggleButton(siteButton, new Color(36, 44, 62));
        styleToggleButton(officeButton, new Color(36, 44, 62));
        siteButton.addActionListener(e -> applyLayout(LayoutType.SITE));
        officeButton.addActionListener(e -> applyLayout(LayoutType.OFFICE));
        controlPanel.add(siteButton);
        controlPanel.add(officeButton);

        hintLabel = new JLabel("悬停查看详情 · 左键切换状态 · 右键选择状态");
        hintLabel.setForeground(TEXT_ACCENT);
        hintLabel.setFont(SUBTITLE_FONT);
        controlPanel.add(hintLabel);

        return controlPanel;
    }

    private void styleButton(AbstractButton button, Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 40)),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        button.setOpaque(true);
    }

    private void styleToggleButton(JToggleButton button, Color background) {
        styleButton(button, background, TEXT_PRIMARY);
    }

    private void applyLayout(LayoutType type) {
        currentLayout = type;
        if (siteButton != null && officeButton != null) {
            siteButton.setSelected(type == LayoutType.SITE);
            officeButton.setSelected(type == LayoutType.OFFICE);
        }
        if (layoutLabel != null) {
            layoutLabel.setText("布局: " + (type == LayoutType.SITE ? "站点" : "办公"));
        }
        updateCanvasSize();
        repaint();
    }

    private void updateCanvasSize() {
        Dimension size = new Dimension(1024, 720);
        BufferedImage bg = currentLayout == LayoutType.OFFICE ? officeLayoutImage : siteLayoutImage;
        if (bg != null) {
            size = new Dimension(bg.getWidth(), bg.getHeight());
        }
        if (mapCanvas != null) {
            mapCanvas.setPreferredSize(size);
            mapCanvas.revalidate();
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

        BufferedImage bg = currentLayout == LayoutType.OFFICE ? officeLayoutImage : siteLayoutImage;
        if (bg != null) {
            g2.drawImage(bg, 0, 0, null);
            paintOverlay(g2, bg.getWidth(), bg.getHeight());
        } else {
            drawDynamicPlaceholder(g2, panelWidth, panelHeight);
        }

        int fallbackIndex = 0;
        resourceBounds.clear();
        resourceCenters.clear();
        for (Resource resource : resources) {
            Integer coordX = resource.getCoordX();
            Integer coordY = resource.getCoordY();

            int centerX;
            int centerY;
            if (coordX != null && coordY != null) {
                centerX = coordX;
                centerY = coordY;
            } else {
                int col = fallbackIndex % 12;
                int row = fallbackIndex / 12;
                centerX = GRID_START_X + col * GRID_STEP_X;
                centerY = GRID_START_Y + row * GRID_STEP_Y;
                fallbackIndex++;
            }

            resourceCenters.put(resource.getResourceId(), new Point(centerX, centerY));
            resourceBounds.put(resource.getResourceId(),
                new Rectangle(centerX - DOT_HIT_RADIUS, centerY - DOT_HIT_RADIUS, DOT_HIT_RADIUS * 2, DOT_HIT_RADIUS * 2));

            drawResourceDot(g2, resource, centerX, centerY);
        }

        drawHoverInfo(g2);
        drawToast(g2);
        g2.dispose();
    }

    private void drawResourceDot(Graphics2D g2, Resource resource, int centerX, int centerY) {
        Color stateColor = getResourceColor(resource);

        Ellipse2D outerGlow = new Ellipse2D.Float(centerX - DOT_RADIUS * 2, centerY - DOT_RADIUS * 2, DOT_RADIUS * 4, DOT_RADIUS * 4);
        g2.setColor(new Color(stateColor.getRed(), stateColor.getGreen(), stateColor.getBlue(), 60));
        g2.fill(outerGlow);

        Ellipse2D dot = new Ellipse2D.Float(centerX - DOT_RADIUS, centerY - DOT_RADIUS, DOT_RADIUS * 2, DOT_RADIUS * 2);
        g2.setColor(stateColor);
        g2.fill(dot);

        g2.setColor(new Color(255, 255, 255, 160));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(dot);

        if (resource.getResourceId() != null && resource.getResourceId().equals(hoveredResourceId)) {
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(new Ellipse2D.Float(centerX - DOT_RADIUS - 4, centerY - DOT_RADIUS - 4, (DOT_RADIUS + 4) * 2, (DOT_RADIUS + 4) * 2));
        }

        if (shouldFlash(resource.getResourceId()) && flashOn) {
            AccessDecision decision = recentDecisions.get(resource.getResourceId());
            Color flashColor = AccessDecision.ALLOW.equals(decision) ? new Color(50, 220, 120) : new Color(235, 80, 80);
            g2.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(), 200));
            g2.setStroke(new BasicStroke(2.2f));
            g2.draw(new Ellipse2D.Float(centerX - DOT_RADIUS - 6, centerY - DOT_RADIUS - 6, (DOT_RADIUS + 6) * 2, (DOT_RADIUS + 6) * 2));
        }

        Long clickTime = clickTimes.get(resource.getResourceId());
        if (clickTime != null) {
            long elapsed = System.currentTimeMillis() - clickTime;
            if (elapsed <= CLICK_PULSE_DURATION_MS) {
                float progress = elapsed / (float) CLICK_PULSE_DURATION_MS;
                int pulseAlpha = (int) (140 * (1 - progress));
                int expand = (int) (10 * progress);
                g2.setColor(new Color(255, 255, 255, pulseAlpha));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Ellipse2D.Float(centerX - DOT_RADIUS - expand, centerY - DOT_RADIUS - expand,
                    (DOT_RADIUS + expand) * 2, (DOT_RADIUS + expand) * 2));
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
        g2.setColor(new Color(10, 14, 20, 230));
        g2.fillRoundRect(x, y + (int) floatOffset, cardWidth, cardHeight, 14, 14);
        g2.setColor(new Color(255, 255, 255, 40));
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
        String stateText = "状态: " + (state != null ? state.name() : "UNKNOWN");
        String typeText = "类型: " + (type != null ? type.name() : "UNKNOWN");
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
        g2.drawString("暂无资源数据", 80, 120);
        g2.setFont(SUBTITLE_FONT);
        g2.setColor(TEXT_MUTED);
        g2.drawString("请刷新或检查资源配置", 80, 150);
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
        g2.setColor(new Color(255, 255, 255, 28));
        g2.setStroke(new BasicStroke(2f));
        for (int x = -h; x < w + h; x += PLACEHOLDER_STRIPE_GAP) {
            g2.drawLine(x + offset, 0, x - h + offset, h);
        }

        g2.setColor(new Color(255, 255, 255, 120));
        g2.setFont(LABEL_FONT);
        g2.drawString("布局占位图动态展示中", 60, 80);
    }

    private void paintOverlay(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 32));
        g2.fillRect(0, 0, w, h);
        g2.setColor(GRID_COLOR);
        for (int x = 0; x < w; x += 120) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += 120) {
            g2.drawLine(0, y, w, y);
        }
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
        g2.setColor(new Color(0, 0, 0, alpha));
        g2.fillRoundRect(x, y, 240, 28, 16, 16);
        g2.setColor(new Color(255, 255, 255, alpha));
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
        siteLayoutImage = loadImageFromResources("site-layout.png");
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
            return "未命名";
        }
        return value.length() > max ? value.substring(0, max) + "…" : value;
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
            toastMessage = "状态已是 " + targetState.name();
            toastAt = System.currentTimeMillis();
            repaint();
            return;
        }
        resource.setResourceState(targetState);
        try {
            resourceRepository.save(resource);
            toastMessage = "已更新 " + resource.getResourceId() + " → " + targetState.name();
            toastAt = System.currentTimeMillis();
            loadResources();
        } catch (Exception ex) {
            resource.setResourceState(original);
            JOptionPane.showMessageDialog(this,
                "更新资源状态失败: " + ex.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
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
            String name = resource.getResourceName() != null ? resource.getResourceName() : "未命名";
            return name + " · " + resource.getResourceId() + " · " + state;
        }
    }
}