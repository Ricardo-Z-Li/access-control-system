package acs.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import acs.service.LogQueryService;
import acs.service.AccessControlService;
import acs.domain.AccessRequest;
import acs.domain.AccessResult;
import acs.domain.Badge;
import acs.domain.Employee;
import acs.cache.LocalCacheManager;

public class MonitorPanel extends JPanel {
    private final LogQueryService logQueryService;
    private final AccessControlService accessControlService;
    private final LocalCacheManager cacheManager;
    private final SiteMapPanel siteMapPanel;
    private final acs.log.csv.CsvLogExporter csvLogExporter;
    private final acs.service.LogCleanupService logCleanupService;
    private JTabbedPane tabbedPane;
    private JTextArea logArea;
    private JTable realTimeTable;
    private DefaultTableModel realTimeTableModel;
    private JLabel cacheStatusLabel;
    private JLabel dbStatusLabel;
    private JLabel lastUpdateLabel;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        add(UiTheme.createHeader("System Monitor", "Real-time events, log search, and health"), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Real-time", createRealTimePanel());
        tabbedPane.addTab("Logs", createLogSearchPanel());
        tabbedPane.addTab("System Status", createSystemStatusPanel());

        if (siteMapPanel != null) {
            tabbedPane.addTab("Site Map", siteMapPanel);
        }

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createRealTimePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Time", "Badge ID", "Employee", "Resource ID", "Decision", "Reason"};
        realTimeTableModel = new DefaultTableModel(columns, 0);
        realTimeTable = new JTable(realTimeTableModel);
        realTimeTable.setAutoCreateRowSorter(true);
        UiTheme.styleTable(realTimeTable);

        JScrollPane scrollPane = new JScrollPane(realTimeTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton simulateButton = UiTheme.primaryButton("Simulate Access");
        simulateButton.addActionListener(e -> simulateAccess());
        JButton clearButton = UiTheme.secondaryButton("Clear Table");
        clearButton.addActionListener(e -> realTimeTableModel.setRowCount(0));
        controlPanel.add(simulateButton);
        controlPanel.add(clearButton);

        panel.add(UiTheme.wrapContent(controlPanel), BorderLayout.NORTH);

        return panel;
    }

    private JPanel createLogSearchPanel() {
        JPanel filters = new JPanel();
        filters.setOpaque(false);
        filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));

        badgeIdField = new JTextField(16);
        employeeIdField = new JTextField(16);
        resourceIdField = new JTextField(16);
        startTimeField = new JTextField(16);
        endTimeField = new JTextField(16);
        decisionCombo = new JComboBox<>(new String[]{"", "ALLOW", "DENY"});

        filters.add(UiTheme.formRow("Badge ID", badgeIdField));
        filters.add(Box.createVerticalStrut(6));
        filters.add(UiTheme.formRow("Employee ID", employeeIdField));
        filters.add(Box.createVerticalStrut(6));
        filters.add(UiTheme.formRow("Resource ID", resourceIdField));
        filters.add(Box.createVerticalStrut(6));
        filters.add(UiTheme.formRow("Start Time (yyyy-MM-dd HH:mm)", startTimeField));
        filters.add(Box.createVerticalStrut(6));
        filters.add(UiTheme.formRow("End Time (yyyy-MM-dd HH:mm)", endTimeField));
        filters.add(Box.createVerticalStrut(6));
        filters.add(UiTheme.formRow("Decision", decisionCombo));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionPanel.setOpaque(false);
        JButton queryButton = UiTheme.primaryButton("Search");
        queryButton.addActionListener(e -> performAdvancedQuery());
        JButton refreshButton = UiTheme.secondaryButton("Refresh");
        refreshButton.addActionListener(e -> refreshLogs());
        JButton exportButton = UiTheme.secondaryButton("Export CSV");
        exportButton.addActionListener(e -> exportToCsv());
        JButton cleanupButton = UiTheme.secondaryButton("Clean Logs");
        cleanupButton.addActionListener(e -> cleanupLogs());
        actionPanel.add(queryButton);
        actionPanel.add(refreshButton);
        actionPanel.add(exportButton);
        actionPanel.add(cleanupButton);

        filters.add(Box.createVerticalStrut(12));
        filters.add(actionPanel);

        JPanel leftCard = UiTheme.cardPanel();
        leftCard.add(filters, BorderLayout.NORTH);

        logArea = new JTextArea(20, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(UiTheme.surface());
        JPanel rightCard = UiTheme.cardPanel();
        rightCard.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            UiTheme.wrapContent(leftCard),
            UiTheme.wrapContent(rightCard));
        splitPane.setDividerLocation(360);
        splitPane.setResizeWeight(0.33);
        splitPane.setDividerSize(1);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setContinuousLayout(true);

        refreshLogs();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSystemStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel stats = new JPanel();
        stats.setOpaque(false);
        stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));

        cacheStatusLabel = UiTheme.statusPill("Unknown", new Color(226, 232, 240), new Color(51, 65, 85));
        dbStatusLabel = UiTheme.statusPill("Unknown", new Color(226, 232, 240), new Color(51, 65, 85));
        lastUpdateLabel = new JLabel("Unknown");

        stats.add(UiTheme.formRow("Cache", cacheStatusLabel));
        stats.add(Box.createVerticalStrut(8));
        stats.add(UiTheme.formRow("Database", dbStatusLabel));
        stats.add(Box.createVerticalStrut(8));
        stats.add(UiTheme.formRow("Last Update", lastUpdateLabel));

        JButton refreshStatusButton = UiTheme.primaryButton("Refresh Status");
        refreshStatusButton.addActionListener(e -> refreshSystemStatus());

        JPanel card = UiTheme.cardPanel();
        card.add(stats, BorderLayout.CENTER);
        card.add(refreshStatusButton, BorderLayout.SOUTH);

        panel.add(UiTheme.wrapContent(card), BorderLayout.NORTH);

        refreshSystemStatus();
        return panel;
    }

    private String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "N/A";
        }
        return timestamp.format(TIMESTAMP_FORMATTER);
    }

    private void refreshLogs() {
        if (logQueryService == null) {
            logArea.setText("Log query service unavailable.");
            return;
        }

        try {
            List<acs.domain.LogEntry> logs = logQueryService.findAll();
            StringBuilder sb = new StringBuilder();
            sb.append("Recent Logs (up to 100)\n");
            sb.append("========================================\n");

            int count = 0;
            for (acs.domain.LogEntry log : logs) {
                if (count >= 100) break;
                sb.append(String.format("Time: %s | Badge: %s | Resource: %s | Decision: %s | Reason: %s\n",
                    formatTimestamp(log.getTimestamp()),
                    log.getBadge() != null ? log.getBadge().getBadgeId() : "N/A",
                    log.getResource() != null ? log.getResource().getResourceId() : "N/A",
                    log.getDecision() != null ? log.getDecision() : "N/A",
                    log.getReasonCode() != null ? log.getReasonCode() : "N/A"));
                count++;
            }

            sb.append("========================================\n");
            sb.append("Count: ").append(count);
            logArea.setText(sb.toString());
        } catch (Exception ex) {
            logArea.setText("Failed to load logs: " + ex.getMessage());
        }
    }

    private void simulateAccess() {
        if (accessControlService == null) {
            JOptionPane.showMessageDialog(this, "Access control service unavailable.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] badges = {"BADGE001", "BADGE002", "BADGE003", "BADGE004"};
        String[] resources = {"RES001", "RES002", "RES003", "RES004"};

        String badgeId = badges[(int) (Math.random() * badges.length)];
        String resourceId = resources[(int) (Math.random() * resources.length)];
        Instant timestamp = Instant.now();

        String employeeInfo = "Unknown";
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
            }
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
        cacheStatusLabel.setText("OK");
        cacheStatusLabel.setBackground(new Color(220, 244, 231));
        cacheStatusLabel.setForeground(new Color(30, 120, 90));
        dbStatusLabel.setText("OK");
        dbStatusLabel.setBackground(new Color(219, 232, 254));
        dbStatusLabel.setForeground(new Color(30, 72, 160));
        lastUpdateLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    private void startRefreshTimer() {
        Timer timer = new Timer(5000, e -> refreshSystemStatus());
        timer.start();
    }

    private void performAdvancedQuery() {
        if (logQueryService == null) {
            logArea.setText("Log query service unavailable.");
            return;
        }

        try {
            String badgeId = badgeIdField.getText().trim();
            String employeeId = employeeIdField.getText().trim();
            String resourceId = resourceIdField.getText().trim();
            String startTimeStr = startTimeField.getText().trim();
            String endTimeStr = endTimeField.getText().trim();
            String decision = (String) decisionCombo.getSelectedItem();

            Instant from = null;
            Instant to = null;

            if (!startTimeStr.isEmpty()) {
                try {
                    LocalDateTime startLdt = LocalDateTime.parse(startTimeStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    from = startLdt.atZone(java.time.ZoneId.systemDefault()).toInstant();
                } catch (Exception e) {
                    logArea.setText("Invalid time format, use yyyy-MM-dd HH:mm");
                    return;
                }
            }
            if (!endTimeStr.isEmpty()) {
                try {
                    LocalDateTime endLdt = LocalDateTime.parse(endTimeStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    to = endLdt.atZone(java.time.ZoneId.systemDefault()).toInstant();
                } catch (Exception e) {
                    logArea.setText("Invalid time format, use yyyy-MM-dd HH:mm");
                    return;
                }
            }

            List<acs.domain.LogEntry> logs;
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
                    logs = logQueryService.findAll();
                    logs = logs.stream()
                        .filter(log -> log.getDecision() != null && acs.domain.AccessDecision.ALLOW.equals(log.getDecision()))
                        .collect(Collectors.toList());
                }
            } else {
                logs = logQueryService.findAll();
            }

            if ((from != null || to != null) && (badgeId.isEmpty() && employeeId.isEmpty() && resourceId.isEmpty())) {
                final Instant finalFrom = from;
                final Instant finalTo = to;
                logs = logs.stream()
                    .filter(log -> {
                        if (finalFrom == null) return true;
                        Instant logInstant = log.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant();
                        return !logInstant.isBefore(finalFrom);
                    })
                    .filter(log -> {
                        if (finalTo == null) return true;
                        Instant logInstant = log.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant();
                        return !logInstant.isAfter(finalTo);
                    })
                    .collect(Collectors.toList());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Results (").append(logs.size()).append(")\n");
            sb.append("========================================\n");

            for (acs.domain.LogEntry log : logs) {
                sb.append(String.format("Time: %s | Badge: %s | Employee: %s | Resource: %s | Decision: %s | Reason: %s\n",
                    formatTimestamp(log.getTimestamp()),
                    log.getBadge() != null ? log.getBadge().getBadgeId() : "N/A",
                    log.getEmployee() != null ? log.getEmployee().getEmployeeId() : "N/A",
                    log.getResource() != null ? log.getResource().getResourceId() : "N/A",
                    log.getDecision() != null ? log.getDecision() : "N/A",
                    log.getReasonCode() != null ? log.getReasonCode() : "N/A"));
            }

            sb.append("========================================\n");
            logArea.setText(sb.toString());
        } catch (Exception ex) {
            logArea.setText("Query failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void exportToCsv() {
        if (csvLogExporter == null) {
            logArea.setText("CSV export service unavailable.");
            return;
        }

        try {
            List<acs.domain.LogEntry> logs = logQueryService.findAll();

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new java.io.File("access_logs_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv"));
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                java.nio.file.Path path = file.toPath();
                csvLogExporter.exportToFile(logs, path);
                logArea.setText("CSV exported: " + path.toString());
            }
        } catch (Exception ex) {
            logArea.setText("Export failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void cleanupLogs() {
        if (logCleanupService == null) {
            logArea.setText("Log cleanup service unavailable.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Clean expired logs?",
            "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                logCleanupService.cleanExpiredLogs();
                logArea.setText("Log cleanup completed.");
            } catch (Exception ex) {
                logArea.setText("Cleanup failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}
