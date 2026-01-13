package acs.ui;

import acs.cache.LocalCacheManager;
import acs.domain.AccessDecision;
import acs.domain.LogEntry;
import acs.domain.Resource;
import acs.domain.ResourceState;
import acs.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
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

    private static final int CELL_WIDTH = 70;
    private static final int CELL_HEIGHT = 50;
    private static final int GRID_GAP_X = 12;
    private static final int GRID_GAP_Y = 12;
    private static final long FLASH_WINDOW_MS = 8000;

    private final ResourceRepository resourceRepository;
    private final LocalCacheManager cacheManager;

    private List<Resource> resources;
    private Timer refreshTimer;
    private Timer flashTimer;
    private boolean flashOn = true;

    private BufferedImage siteLayoutImage;
    private BufferedImage officeLayoutImage;
    private LayoutType currentLayout = LayoutType.SITE;

    private final Map<String, AccessDecision> recentDecisions = new HashMap<>();
    private final Map<String, Long> decisionTimes = new HashMap<>();

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
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("站点地图", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        JPanel mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawSiteMap(g);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(900, 650);
            }
        };
        mapPanel.setBackground(Color.WHITE);
        mapPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JScrollPane scrollPane = new JScrollPane(mapPanel);
        add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> loadResources());
        controlPanel.add(refreshButton);

        JButton switchLayoutButton = new JButton("切换布局");
        switchLayoutButton.addActionListener(e -> {
            currentLayout = currentLayout == LayoutType.SITE ? LayoutType.OFFICE : LayoutType.SITE;
            repaint();
        });
        controlPanel.add(switchLayoutButton);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private void loadResources() {
        resources = resourceRepository.findAll();
        updateRecentDecisions();
        repaint();
    }

    private void updateRecentDecisions() {
        recentDecisions.clear();
        decisionTimes.clear();
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
        if (resources == null || resources.isEmpty()) {
            g.setColor(Color.BLACK);
            g.drawString("暂无资源", 100, 100);
            return;
        }

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        BufferedImage bg = currentLayout == LayoutType.OFFICE ? officeLayoutImage : siteLayoutImage;
        if (bg != null) {
            g.drawImage(bg, 0, 0, panelWidth, panelHeight, null);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, panelWidth, panelHeight);
        }

        int fallbackIndex = 0;
        for (Resource resource : resources) {
            Integer coordX = resource.getCoordX();
            Integer coordY = resource.getCoordY();

            int rectX;
            int rectY;
            if (coordX != null && coordY != null) {
                rectX = coordX;
                rectY = coordY;
            } else {
                int col = fallbackIndex % 8;
                int row = fallbackIndex / 8;
                rectX = 30 + col * (CELL_WIDTH + GRID_GAP_X);
                rectY = 40 + row * (CELL_HEIGHT + GRID_GAP_Y);
                fallbackIndex++;
            }

            Color fillColor = getResourceColor(resource);
            if (shouldFlash(resource.getResourceId())) {
                AccessDecision decision = recentDecisions.get(resource.getResourceId());
                if (flashOn) {
                    fillColor = AccessDecision.ALLOW.equals(decision) ? new Color(0, 180, 0) : new Color(200, 0, 0);
                }
            }

            g.setColor(fillColor);
            g.fillRect(rectX, rectY, CELL_WIDTH, CELL_HEIGHT);

            g.setColor(Color.BLACK);
            g.drawRect(rectX, rectY, CELL_WIDTH, CELL_HEIGHT);

            g.setFont(new Font("Arial", Font.PLAIN, 10));
            String displayName = resource.getResourceName().length() > 10
                    ? resource.getResourceName().substring(0, 10) + "..."
                    : resource.getResourceName();
            g.drawString(displayName, rectX + 4, rectY + 14);

            g.setFont(new Font("Arial", Font.PLAIN, 9));
            g.drawString(resource.getResourceId(), rectX + 4, rectY + 28);
            g.drawString(resource.getResourceState().name(), rectX + 4, rectY + 42);
        }
    }

    private boolean shouldFlash(String resourceId) {
        Long ts = decisionTimes.get(resourceId);
        if (ts == null) {
            return false;
        }
        long age = System.currentTimeMillis() - ts;
        return age >= 0 && age <= FLASH_WINDOW_MS;
    }

    private Color getResourceColor(Resource resource) {
        if (resource.getIsControlled() != null && !resource.getIsControlled()) {
            return Color.LIGHT_GRAY;
        }

        if (resource.getResourceState() == null) {
            return Color.GRAY;
        }

        switch (resource.getResourceState()) {
            case AVAILABLE:
                return Color.GREEN;
            case OCCUPIED:
                return Color.YELLOW;
            case LOCKED:
                return Color.RED;
            case OFFLINE:
                return Color.DARK_GRAY;
            case PENDING:
            default:
                return Color.GRAY;
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

    public void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        if (flashTimer != null) {
            flashTimer.stop();
        }
    }
}
