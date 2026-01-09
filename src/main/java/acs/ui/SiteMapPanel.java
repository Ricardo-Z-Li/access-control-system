package acs.ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import acs.domain.Resource;
import acs.domain.ResourceState;
import acs.domain.ResourceType;
import acs.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 站点平面图可视化面板
 * 显示资源布局和状态
 */
@Component
@Profile("!test")
public class SiteMapPanel extends JPanel {
    
    private final ResourceRepository resourceRepository;
    private List<Resource> resources;
    private Timer refreshTimer;
    
    @Autowired
    public SiteMapPanel(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
        initUI();
        loadResources();
        startRefreshTimer();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("站点平面图", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        JPanel mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawSiteMap(g);
            }
            
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(800, 600);
            }
        };
        mapPanel.setBackground(Color.WHITE);
        mapPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JScrollPane scrollPane = new JScrollPane(mapPanel);
        add(scrollPane, BorderLayout.CENTER);
        
        // 控制面板
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> loadResources());
        controlPanel.add(refreshButton);
        
        JButton zoomInButton = new JButton("放大");
        zoomInButton.addActionListener(e -> {
            // 简化实现
            JOptionPane.showMessageDialog(this, "放大功能待实现");
        });
        controlPanel.add(zoomInButton);
        
        JButton zoomOutButton = new JButton("缩小");
        zoomOutButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "缩小功能待实现");
        });
        controlPanel.add(zoomOutButton);
        
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    private void loadResources() {
        resources = resourceRepository.findAll();
        repaint();
    }
    
    private void drawSiteMap(Graphics g) {
        if (resources == null || resources.isEmpty()) {
            g.setColor(Color.BLACK);
            g.drawString("没有资源数据", 100, 100);
            return;
        }
        
        // 简单布局：按楼层分组
        int x = 50;
        int y = 50;
        int cellWidth = 80;
        int cellHeight = 60;
        int horizontalSpacing = 20;
        int verticalSpacing = 20;
        
        // 按楼层分组资源（假设资源ID包含楼层信息，如 D-1F-101）
        java.util.Map<String, java.util.List<Resource>> floorMap = new java.util.HashMap<>();
        for (Resource resource : resources) {
            String resourceId = resource.getResourceId();
            String floor = "未知";
            if (resourceId.contains("-")) {
                String[] parts = resourceId.split("-");
                if (parts.length >= 2) {
                    floor = parts[1]; // 例如 "1F"
                }
            }
            floorMap.computeIfAbsent(floor, k -> new java.util.ArrayList<>()).add(resource);
        }
        
        // 绘制每个楼层的资源
        int floorIndex = 0;
        for (java.util.Map.Entry<String, java.util.List<Resource>> entry : floorMap.entrySet()) {
            String floor = entry.getKey();
            List<Resource> floorResources = entry.getValue();
            
            // 绘制楼层标签
            g.setColor(Color.BLACK);
            g.setFont(new Font("宋体", Font.BOLD, 14));
            g.drawString("楼层 " + floor, x, y - 10);
            
            // 绘制该楼层的资源
            int col = 0;
            for (Resource resource : floorResources) {
                int rectX = x + col * (cellWidth + horizontalSpacing);
                int rectY = y;
                
                // 根据资源状态设置颜色
                Color fillColor = getResourceColor(resource);
                g.setColor(fillColor);
                g.fillRect(rectX, rectY, cellWidth, cellHeight);
                
                // 边框
                g.setColor(Color.BLACK);
                g.drawRect(rectX, rectY, cellWidth, cellHeight);
                
                // 资源名称
                g.setFont(new Font("宋体", Font.PLAIN, 10));
                String displayName = resource.getResourceName().length() > 10 ? 
                    resource.getResourceName().substring(0, 10) + "..." : resource.getResourceName();
                g.drawString(displayName, rectX + 5, rectY + 15);
                
                // 资源ID
                g.drawString(resource.getResourceId(), rectX + 5, rectY + 30);
                
                // 资源状态
                g.setFont(new Font("宋体", Font.PLAIN, 9));
                g.drawString(resource.getResourceState().name(), rectX + 5, rectY + 45);
                
                col++;
                // 每行最多4个资源，然后换行
                if (col >= 4) {
                    col = 0;
                    y += cellHeight + verticalSpacing;
                }
            }
            
            // 下一个楼层位置
            y += cellHeight + verticalSpacing + 40;
            floorIndex++;
            
            // 如果超过最大高度，重置x位置并新列
            if (y > 500 && floorIndex < floorMap.size()) {
                x += 4 * (cellWidth + horizontalSpacing) + 50;
                y = 50;
            }
        }
    }
    
    private Color getResourceColor(Resource resource) {
        if (resource.getIsControlled() != null && !resource.getIsControlled()) {
            return Color.LIGHT_GRAY; // 非受控资源
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
    
    private void startRefreshTimer() {
        refreshTimer = new Timer(10000, e -> loadResources()); // 每10秒刷新
        refreshTimer.start();
    }
    
    public void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
}