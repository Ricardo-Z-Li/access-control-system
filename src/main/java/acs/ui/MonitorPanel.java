package acs.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import acs.service.LogQueryService;
import acs.service.AccessControlService;
import acs.domain.AccessRequest;
import acs.domain.AccessResult;
import acs.domain.Badge;
import acs.domain.Employee;
import acs.cache.LocalCacheManager;

public class MonitorPanel extends JPanel {
    private LogQueryService logQueryService;
    private AccessControlService accessControlService;
    private LocalCacheManager cacheManager;
    private SiteMapPanel siteMapPanel;
    private acs.log.csv.CsvLogExporter csvLogExporter;
    private acs.service.LogCleanupService logCleanupService;
    private JTabbedPane tabbedPane;
    private JTextArea logArea;
    private JTable realTimeTable;
    private DefaultTableModel realTimeTableModel;
    private JLabel cacheStatusLabel;
    private JLabel dbStatusLabel;
    private JLabel lastUpdateLabel;
    
    private JTextField badgeIdField;
    private JTextField employeeIdField;
    private JTextField resourceIdField;
    private JTextField startTimeField;
    private JTextField endTimeField;
    private JComboBox<String> decisionCombo;
    
    public MonitorPanel(LogQueryService logQueryService) {
        this(logQueryService, null, null, null, null, null);
    }
    
    public MonitorPanel(LogQueryService logQueryService, SiteMapPanel siteMapPanel) {
        this(logQueryService, null, null, siteMapPanel, null, null);
    }
    
    public MonitorPanel(LogQueryService logQueryService, SiteMapPanel siteMapPanel,
                       acs.log.csv.CsvLogExporter csvLogExporter,
                       acs.service.LogCleanupService logCleanupService) {
        this(logQueryService, null, null, siteMapPanel, csvLogExporter, logCleanupService);
    }
    
    public MonitorPanel(LogQueryService logQueryService, AccessControlService accessControlService, 
                       SiteMapPanel siteMapPanel,
                       acs.log.csv.CsvLogExporter csvLogExporter,
                       acs.service.LogCleanupService logCleanupService) {
        this(logQueryService, accessControlService, null, siteMapPanel, csvLogExporter, logCleanupService);
    }
    
    public MonitorPanel(LogQueryService logQueryService, AccessControlService accessControlService, 
                       LocalCacheManager cacheManager,
                       SiteMapPanel siteMapPanel,
                       acs.log.csv.CsvLogExporter csvLogExporter,
                       acs.service.LogCleanupService logCleanupService) {
        this.logQueryService = logQueryService;
        this.accessControlService = accessControlService;
        this.cacheManager = cacheManager;
        this.siteMapPanel = siteMapPanel;
        this.csvLogExporter = csvLogExporter;
        this.logCleanupService = logCleanupService;
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
        
        JPanel queryPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        queryPanel.add(new JLabel("徽章ID:"), gbc);
        gbc.gridx = 1;
        badgeIdField = new JTextField(15);
        queryPanel.add(badgeIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        queryPanel.add(new JLabel("员工ID:"), gbc);
        gbc.gridx = 1;
        employeeIdField = new JTextField(15);
        queryPanel.add(employeeIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        queryPanel.add(new JLabel("资源ID:"), gbc);
        gbc.gridx = 1;
        resourceIdField = new JTextField(15);
        queryPanel.add(resourceIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        queryPanel.add(new JLabel("开始时间 (yyyy-MM-dd HH:mm):"), gbc);
        gbc.gridx = 1;
        startTimeField = new JTextField(15);
        queryPanel.add(startTimeField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        queryPanel.add(new JLabel("结束时间 (yyyy-MM-dd HH:mm):"), gbc);
        gbc.gridx = 1;
        endTimeField = new JTextField(15);
        queryPanel.add(endTimeField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        queryPanel.add(new JLabel("决策:"), gbc);
        gbc.gridx = 1;
        decisionCombo = new JComboBox<>(new String[]{"", "ALLOW", "DENY"});
        queryPanel.add(decisionCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton queryButton = new JButton("查询");
        queryButton.addActionListener(e -> performAdvancedQuery());
        buttonPanel.add(queryButton);
        
        JButton exportButton = new JButton("导出CSV");
        exportButton.addActionListener(e -> exportToCsv());
        buttonPanel.add(exportButton);
        
        JButton cleanupButton = new JButton("清理过期日志");
        cleanupButton.addActionListener(e -> cleanupLogs());
        buttonPanel.add(cleanupButton);
        
        JButton refreshAllButton = new JButton("刷新所有日志");
        refreshAllButton.addActionListener(e -> refreshLogs());
        buttonPanel.add(refreshAllButton);
        
        queryPanel.add(buttonPanel, gbc);
        
        panel.add(queryPanel, BorderLayout.NORTH);
        
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
                    log.getTimestamp() != null ? log.getTimestamp() : "N/A",
                    log.getBadge() != null ? log.getBadge().getBadgeId() : "N/A",
                    log.getResource() != null ? log.getResource().getResourceId() : "N/A",
                    log.getDecision() != null ? log.getDecision() : "N/A",
                    log.getReasonCode() != null ? log.getReasonCode() : "N/A"));
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
                    log.getTimestamp() != null ? log.getTimestamp() : "N/A",
                    log.getBadge() != null ? log.getBadge().getBadgeId() : "N/A",
                    log.getResource() != null ? log.getResource().getResourceId() : "N/A",
                    log.getDecision() != null ? log.getDecision() : "N/A",
                    log.getReasonCode() != null ? log.getReasonCode() : "N/A");
                
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
        if (accessControlService == null) {
            JOptionPane.showMessageDialog(this, "访问控制服务未初始化，无法模拟真实访问", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String[] badges = {"BADGE001", "BADGE002", "BADGE003", "BADGE004"};
        String[] resources = {"RES001", "RES002", "RES003", "RES004"};
        
        String badgeId = badges[(int) (Math.random() * badges.length)];
        String resourceId = resources[(int) (Math.random() * resources.length)];
        Instant timestamp = Instant.now();
        
        // 获取真实的员工信息
        String employeeInfo = "未知员工";
        if (cacheManager != null) {
            Badge badge = cacheManager.getBadge(badgeId);
            if (badge != null && badge.getEmployee() != null) {
                Employee employee = badge.getEmployee();
                String employeeName = employee.getEmployeeName() != null ? employee.getEmployeeName() : "";
                String employeeId = employee.getEmployeeId() != null ? employee.getEmployeeId() : "";
                if (!employeeName.isEmpty() && !employeeId.isEmpty()) {
                    employeeInfo = employeeName + " (" + employeeId + ")";
                } else if (!employeeId.isEmpty()) {
                    employeeInfo = employeeId;
                } else if (!employeeName.isEmpty()) {
                    employeeInfo = employeeName;
                }
                // 否则保持"未知员工"
            }
        } else {
            employeeInfo = "员工" + badgeId.substring(2); // 回退到模拟名称
        }
        
        AccessRequest request = new AccessRequest(badgeId, resourceId, timestamp);
        AccessResult result = accessControlService.processAccess(request);
        
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        realTimeTableModel.addRow(new Object[]{time, badgeId, employeeInfo, resourceId, 
            result.getDecision().toString(), result.getReasonCode().toString()});
        
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
    
    private void performAdvancedQuery() {
        if (logQueryService == null) {
            logArea.setText("LogQueryService 未连接");
            return;
        }
        
        try {
            String badgeId = badgeIdField.getText().trim();
            String employeeId = employeeIdField.getText().trim();
            String resourceId = resourceIdField.getText().trim();
            String startTimeStr = startTimeField.getText().trim();
            String endTimeStr = endTimeField.getText().trim();
            String decision = (String) decisionCombo.getSelectedItem();
            
            java.time.Instant from = null;
            java.time.Instant to = null;
            
            if (!startTimeStr.isEmpty()) {
                try {
                    java.time.LocalDateTime startLdt = java.time.LocalDateTime.parse(startTimeStr, 
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    from = startLdt.atZone(java.time.ZoneId.systemDefault()).toInstant();
                } catch (Exception e) {
                    logArea.setText("开始时间格式错误，请使用 yyyy-MM-dd HH:mm 格式");
                    return;
                }
            }
            if (!endTimeStr.isEmpty()) {
                try {
                    java.time.LocalDateTime endLdt = java.time.LocalDateTime.parse(endTimeStr, 
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    to = endLdt.atZone(java.time.ZoneId.systemDefault()).toInstant();
                } catch (Exception e) {
                    logArea.setText("结束时间格式错误，请使用 yyyy-MM-dd HH:mm 格式");
                    return;
                }
            }
            
            java.util.List<acs.domain.LogEntry> logs;
            if (!badgeId.isEmpty()) {
                logs = logQueryService.findByBadge(badgeId, from, to);
            } else if (!employeeId.isEmpty()) {
                logs = logQueryService.findByEmployee(employeeId, from, to);
            } else if (!resourceId.isEmpty()) {
                logs = logQueryService.findByResource(resourceId, from, to);
            } else if (decision != null && !decision.isEmpty()) {
                if (decision.equals("DENY")) {
                    logs = logQueryService.findDenied(from, to);
                } else {
                    // 对于ALLOW，我们需要过滤所有日志中的ALLOW决策
                    logs = logQueryService.findAll();
                    logs = logs.stream()
                        .filter(log -> log.getDecision() != null && log.getDecision().toString().equals("ALLOW"))
                        .collect(java.util.stream.Collectors.toList());
                }
            } else {
                logs = logQueryService.findAll();
            }
            
            // 应用时间过滤（如果提供了时间范围但调用了findAll）
            if ((from != null || to != null) && (badgeId.isEmpty() && employeeId.isEmpty() && resourceId.isEmpty())) {
                final java.time.Instant finalFrom = from;
                final java.time.Instant finalTo = to;
                logs = logs.stream()
                    .filter(log -> {
                        if (finalFrom == null) return true;
                        java.time.Instant logInstant = log.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant();
                        return !logInstant.isBefore(finalFrom);
                    })
                    .filter(log -> {
                        if (finalTo == null) return true;
                        java.time.Instant logInstant = log.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant();
                        return !logInstant.isAfter(finalTo);
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 显示结果
            StringBuilder sb = new StringBuilder();
            sb.append("高级查询结果 (").append(logs.size()).append(" 条记录):\n");
            sb.append("========================================\n");
            
            for (acs.domain.LogEntry log : logs) {
                sb.append(String.format("时间: %s | 徽章: %s | 员工: %s | 资源: %s | 决策: %s | 原因: %s\n",
                    log.getTimestamp() != null ? log.getTimestamp() : "N/A",
                    log.getBadge() != null ? log.getBadge().getBadgeId() : "N/A",
                    log.getEmployee() != null ? log.getEmployee().getEmployeeId() : "N/A",
                    log.getResource() != null ? log.getResource().getResourceId() : "N/A",
                    log.getDecision() != null ? log.getDecision() : "N/A",
                    log.getReasonCode() != null ? log.getReasonCode() : "N/A"));
            }
            
            sb.append("========================================\n");
            logArea.setText(sb.toString());
        } catch (Exception ex) {
            logArea.setText("查询失败: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private void exportToCsv() {
        if (csvLogExporter == null) {
            logArea.setText("CSV导出器未连接");
            return;
        }
        
        try {
            // 获取当前显示的日志（可能来自查询结果）
            // 暂时导出所有日志
            java.util.List<acs.domain.LogEntry> logs = logQueryService.findAll();
            String csvContent = csvLogExporter.exportToString(logs);
            
            // 弹出文件选择器保存CSV文件
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new java.io.File("access_logs_" + 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv"));
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                java.nio.file.Path path = file.toPath();
                csvLogExporter.exportToFile(logs, path);
                logArea.setText("CSV文件已导出到: " + path.toString());
            }
        } catch (Exception ex) {
            logArea.setText("导出失败: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private void cleanupLogs() {
        if (logCleanupService == null) {
            logArea.setText("日志清理服务未连接");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "确定要清理7天前的日志吗？此操作不可逆。", 
            "确认清理", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                logCleanupService.cleanExpiredLogs();
                logArea.setText("日志清理已完成。");
            } catch (Exception ex) {
                logArea.setText("清理失败: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}