package acs.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import acs.service.LogQueryService;
import acs.cache.LocalCacheManager;

public class MonitorPanel extends JPanel {
    private LogQueryService logQueryService;
    private LocalCacheManager cacheManager;
    private SiteMapPanel siteMapPanel;
    private JTabbedPane tabbedPane;
    private JTextArea logArea;
    private JTable realTimeTable;
    private DefaultTableModel realTimeTableModel;
    private JLabel cacheStatusLabel;
    private JLabel dbStatusLabel;
    private JLabel lastUpdateLabel;
    
    public MonitorPanel(LogQueryService logQueryService) {
        this(logQueryService, null);
    }
    
    public MonitorPanel(LogQueryService logQueryService, SiteMapPanel siteMapPanel) {
        this.logQueryService = logQueryService;
        this.siteMapPanel = siteMapPanel;
        initUI();
        startRefreshTimer();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("系统监控面板", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("实时访问", createRealTimePanel());
        tabbedPane.addTab("日志搜索", createLogSearchPanel());
        tabbedPane.addTab("系统状态", createSystemStatusPanel());
        
        // 如果提供了站点平面图组件，则添加标签页
        if (siteMapPanel != null) {
            tabbedPane.addTab("站点平面图", siteMapPanel);
        }
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createRealTimePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"时间", "徽章ID", "员工", "资源ID", "决策", "原因"};
        realTimeTableModel = new DefaultTableModel(columns, 0);
        realTimeTable = new JTable(realTimeTableModel);
        realTimeTable.setAutoCreateRowSorter(true);
        
        JScrollPane scrollPane = new JScrollPane(realTimeTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearButton = new JButton("清空表格");
        clearButton.addActionListener(e -> realTimeTableModel.setRowCount(0));
        controlPanel.add(clearButton);
        
        JButton simulateButton = new JButton("模拟访问");
        simulateButton.addActionListener(e -> simulateAccess());
        controlPanel.add(simulateButton);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    private JPanel createLogSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton refreshButton = new JButton("刷新日志");
        refreshButton.addActionListener(e -> refreshLogs());
        controlPanel.add(refreshButton);
        
        JButton clearButton = new JButton("清空显示");
        clearButton.addActionListener(e -> logArea.setText(""));
        controlPanel.add(clearButton);
        
        JLabel filterLabel = new JLabel("过滤:");
        controlPanel.add(filterLabel);
        
        JTextField filterField = new JTextField(20);
        controlPanel.add(filterField);
        
        JButton filterButton = new JButton("应用过滤");
        filterButton.addActionListener(e -> applyFilter(filterField.getText()));
        controlPanel.add(filterButton);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        logArea = new JTextArea(20, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("宋体", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        refreshLogs();
        
        return panel;
    }
    
    private JPanel createSystemStatusPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        panel.add(new JLabel("系统状态监控", SwingConstants.CENTER), gbc);
        
        gbc.gridy++;
        panel.add(new JSeparator(), gbc);
        
        gbc.gridy++;
        panel.add(new JLabel("缓存状态:"), gbc);
        gbc.gridx = 1;
        cacheStatusLabel = new JLabel("未知");
        panel.add(cacheStatusLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("数据库状态:"), gbc);
        gbc.gridx = 1;
        dbStatusLabel = new JLabel("未知");
        panel.add(dbStatusLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("最后更新时间:"), gbc);
        gbc.gridx = 1;
        lastUpdateLabel = new JLabel("未知");
        panel.add(lastUpdateLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JSeparator(), gbc);
        
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton refreshStatusButton = new JButton("刷新状态");
        refreshStatusButton.addActionListener(e -> refreshSystemStatus());
        panel.add(refreshStatusButton, gbc);
        
        refreshSystemStatus();
        
        return panel;
    }
    
    private void refreshLogs() {
        if (logQueryService == null) {
            logArea.setText("LogQueryService 未连接");
            return;
        }
        
        try {
            java.util.List<acs.domain.LogEntry> logs = logQueryService.findAll();
            StringBuilder sb = new StringBuilder();
            sb.append("最近日志记录 (最多100条):\n");
            sb.append("========================================\n");
            
            int count = 0;
            for (acs.domain.LogEntry log : logs) {
                if (count >= 100) break;
                sb.append(String.format("时间: %s | 徽章: %s | 资源: %s | 决策: %s | 原因: %s\n",
                    log.getTimestamp(),
                    log.getBadge() != null ? log.getBadge().getBadgeId() : "N/A",
                    log.getResource() != null ? log.getResource().getResourceId() : "N/A",
                    log.getDecision(),
                    log.getReasonCode()));
                count++;
            }
            
            sb.append("========================================\n");
            sb.append("总计: ").append(count).append(" 条记录");
            logArea.setText(sb.toString());
        } catch (Exception ex) {
            logArea.setText("加载日志时出错: " + ex.getMessage());
        }
    }
    
    private void applyFilter(String filter) {
        if (logQueryService == null) {
            return;
        }
        
        try {
            java.util.List<acs.domain.LogEntry> logs = logQueryService.findAll();
            StringBuilder sb = new StringBuilder();
            sb.append("过滤结果 (关键词: ").append(filter).append("):\n");
            sb.append("========================================\n");
            
            int count = 0;
            for (acs.domain.LogEntry log : logs) {
                String logStr = String.format("时间: %s | 徽章: %s | 资源: %s | 决策: %s | 原因: %s",
                    log.getTimestamp(),
                    log.getBadge() != null ? log.getBadge().getBadgeId() : "N/A",
                    log.getResource() != null ? log.getResource().getResourceId() : "N/A",
                    log.getDecision(),
                    log.getReasonCode());
                
                if (filter.isEmpty() || logStr.toLowerCase().contains(filter.toLowerCase())) {
                    sb.append(logStr).append("\n");
                    count++;
                }
            }
            
            sb.append("========================================\n");
            sb.append("匹配: ").append(count).append(" 条记录");
            logArea.setText(sb.toString());
        } catch (Exception ex) {
            logArea.setText("过滤日志时出错: " + ex.getMessage());
        }
    }
    
    private void simulateAccess() {
        String[] badges = {"B-10001", "B-10002", "B-10003", "B-10004"};
        String[] resources = {"D-1F-101", "D-2F-201", "D-3F-301", "D-1F-102"};
        String[] decisions = {"ALLOW", "DENY"};
        String[] reasons = {"SUCCESS", "BADGE_NOT_FOUND", "UNAUTHORIZED"};
        
        String badge = badges[(int) (Math.random() * badges.length)];
        String resource = resources[(int) (Math.random() * resources.length)];
        String decision = decisions[(int) (Math.random() * decisions.length)];
        String reason = reasons[(int) (Math.random() * reasons.length)];
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        realTimeTableModel.addRow(new Object[]{time, badge, "员工" + badge.substring(2), resource, decision, reason});
        
        if (realTimeTableModel.getRowCount() > 50) {
            realTimeTableModel.removeRow(0);
        }
    }
    
    private void refreshSystemStatus() {
        cacheStatusLabel.setText("运行中");
        dbStatusLabel.setText("连接正常");
        lastUpdateLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
    
    private void startRefreshTimer() {
        Timer timer = new Timer(5000, e -> {
            refreshSystemStatus();
        });
        timer.start();
    }
}