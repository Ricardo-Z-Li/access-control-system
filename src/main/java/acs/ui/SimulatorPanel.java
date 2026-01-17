package acs.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import acs.simulator.BadgeReaderSimulator;
import acs.simulator.EventSimulator;
import acs.simulator.RouterSystem;
import acs.simulator.SimulationStatus;
import acs.simulator.SystemHealth;
import acs.simulator.*;
import acs.service.ClockService;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;
import acs.simulator.SimulationScenarioConfig;
import acs.simulator.SimulationPath;

public class SimulatorPanel extends JPanel {
    private BadgeReaderSimulator badgeReaderSimulator;
    private EventSimulator eventSimulator;
    private RouterSystem routerSystem;
    private ClockService clockService;
    
    private JTabbedPane tabbedPane;
    private JTextField readerIdField;
    private JTextField badgeIdField;
    private JTextPane simulatorLogArea;
    private JTable eventTable;
    private DefaultTableModel eventTableModel;
    private JLabel simulationStatusLabel;
    private JLabel systemHealthLabel;
    private JLabel loadBalanceLabel;
    private JTextField absoluteTimeField;
    private JButton setTimeButton;
    private JButton resetTimeButton;
    private JLabel currentTimeLabel;
    
    // Execution chain tracking
    private JTextPane executionChainArea;

    private JTextField scenarioStepDelayField;
    private JCheckBox scenarioEnabledCheck;
    private JTable scenarioTable;
    private DefaultTableModel scenarioTableModel;
    private JTextField scenarioNameField;
    private JTextField scenarioResourcesField;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public SimulatorPanel(BadgeReaderSimulator badgeReaderSimulator, 
                         EventSimulator eventSimulator,
                         RouterSystem routerSystem,
                         ClockService clockService) {
        this.badgeReaderSimulator = badgeReaderSimulator;
        this.eventSimulator = eventSimulator;
        this.routerSystem = routerSystem;
        this.clockService = clockService;
        initUI();
        startStatusTimer();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        add(UiTheme.createHeader("Simulator Control", "Simulate readers, events, routing, and execution chains"), BorderLayout.NORTH);
        
        tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("Badge Reader", createBadgeReaderPanel());
        tabbedPane.addTab("Event Simulator", createEventSimulatorPanel());
        tabbedPane.addTab("Router System", createRouterSystemPanel());
        tabbedPane.addTab("System Monitor", createSystemMonitorPanel());
        tabbedPane.addTab("Execution Chain", createExecutionChainPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        JLabel badgeStatus = new JLabel("Badge Reader: " + (badgeReaderSimulator != null ? "Available" : "Unavailable"));
        JLabel eventStatus = new JLabel("Event Simulator: " + (eventSimulator != null ? "Available" : "Unavailable"));
        JLabel routerStatus = new JLabel("Router System: " + (routerSystem != null ? "Available" : "Unavailable"));
        add(UiTheme.footerBar(badgeStatus, eventStatus, routerStatus), BorderLayout.SOUTH);
    }
    
    private JPanel createBadgeReaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Reader ID:"), gbc);
        
        gbc.gridx = 1;
        readerIdField = new JTextField(20);
        readerIdField.setText("READER001");
        inputPanel.add(readerIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Badge ID:"), gbc);
        
        gbc.gridx = 1;
        badgeIdField = new JTextField(20);
        badgeIdField.setText("BADGE001");
        inputPanel.add(badgeIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton swipeButton = new JButton("Simulate Swipe");
        swipeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulateBadgeSwipe();
            }
        });
        buttonPanel.add(swipeButton);
        
        JButton readButton = new JButton("Simulate Read");
        readButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulateBadgeRead();
            }
        });
        buttonPanel.add(readButton);
        
        JButton statusButton = new JButton("Get Stats");
        statusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getReaderStats();
            }
        });
        buttonPanel.add(statusButton);
        
        inputPanel.add(buttonPanel, gbc);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        simulatorLogArea = UiTheme.createLogPane(true);
        panel.add(new JScrollPane(simulatorLogArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createEventSimulatorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        controlPanel.add(new JLabel("Total Events:"), gbc);
        
        gbc.gridx = 1;
        JTextField numEventsField = new JTextField(10);
        numEventsField.setText("100");
        controlPanel.add(numEventsField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        controlPanel.add(new JLabel("Concurrency:"), gbc);
        
        gbc.gridx = 1;
        JTextField concurrencyField = new JTextField(10);
        concurrencyField.setText("10");
        controlPanel.add(concurrencyField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        controlPanel.add(new JLabel("Time Acceleration:"), gbc);
        
        gbc.gridx = 1;
        JTextField timeAccelField = new JTextField(10);
        timeAccelField.setText("5.0");
        controlPanel.add(timeAccelField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        controlPanel.add(new JLabel("Absolute Time (yyyy-MM-dd HH:mm):"), gbc);
        
        gbc.gridx = 1;
        absoluteTimeField = new JTextField(20);
        absoluteTimeField.setText(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        controlPanel.add(absoluteTimeField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel timeButtonPanel = new JPanel(new FlowLayout());
        
        setTimeButton = new JButton("Set Sim Time");
        setTimeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSimulatedTime();
            }
        });
        timeButtonPanel.add(setTimeButton);
        
        resetTimeButton = new JButton("Reset to Real Time");
        resetTimeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetSimulatedTime();
            }
        });
        timeButtonPanel.add(resetTimeButton);
        
        currentTimeLabel = new JLabel("Current Time: " + clockService.localNow().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeButtonPanel.add(currentTimeLabel);
        
        controlPanel.add(timeButtonPanel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton startButton = new JButton("Start Simulation");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int numEvents = Integer.parseInt(numEventsField.getText());
                    int concurrency = Integer.parseInt(concurrencyField.getText());
                    double acceleration = Double.parseDouble(timeAccelField.getText());
                    startEventSimulation(numEvents, concurrency, acceleration);
                } catch (NumberFormatException ex) {
                    logMessage("Error: enter valid numbers");
                }
            }
        });
        buttonPanel.add(startButton);
        
        JButton stopButton = new JButton("Stop Simulation");
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopEventSimulation();
            }
        });
        buttonPanel.add(stopButton);
        
        JButton resetButton = new JButton("Reset Simulation");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetEventSimulation();
            }
        });
        buttonPanel.add(resetButton);
        
        JButton metricsButton = new JButton("Get Metrics");
        metricsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getPerformanceMetrics();
            }
        });
        buttonPanel.add(metricsButton);
        
        controlPanel.add(buttonPanel, gbc);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        String[] columns = {"Time", "Event", "Reader", "Badge", "Resource", "Result"};
        eventTableModel = new DefaultTableModel(columns, 0);
        eventTable = new JTable(eventTableModel);
        eventTable.setAutoCreateRowSorter(true);
        
        JScrollPane scrollPane = new JScrollPane(eventTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        simulationStatusLabel = new JLabel("Status: Not Started");
        statusPanel.add(simulationStatusLabel);
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createRouterSystemPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        controlPanel.add(new JLabel("Node ID:"), gbc);
        
        gbc.gridx = 1;
        JTextField nodeIdField = new JTextField(15);
        nodeIdField.setText("NODE_1");
        controlPanel.add(nodeIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton failButton = new JButton("Mark Node Failed");
        failButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                markNodeAsFailed(nodeIdField.getText());
            }
        });
        buttonPanel.add(failButton);
        
        JButton recoverButton = new JButton("Recover Node");
        recoverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recoverNode(nodeIdField.getText());
            }
        });
        buttonPanel.add(recoverButton);
        
        JButton statsButton = new JButton("Get Load Stats");
        statsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLoadBalanceStats();
            }
        });
        buttonPanel.add(statsButton);
        
        JComboBox<String> strategyCombo = new JComboBox<>(new String[]{"ROUND_ROBIN", "RANDOM", "LEAST_CONNECTIONS"});
        JButton setStrategyButton = new JButton("Set Strategy");
        setStrategyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setLoadBalanceStrategy((String) strategyCombo.getSelectedItem());
            }
        });
        buttonPanel.add(new JLabel("Strategy:"));
        buttonPanel.add(strategyCombo);
        buttonPanel.add(setStrategyButton);
        
        controlPanel.add(buttonPanel, gbc);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        JTextPane routerInfoArea = UiTheme.createLogPane(true);
        
        updateRouterInfo();
        
        panel.add(new JScrollPane(routerInfoArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createSystemMonitorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("System Monitor", SwingConstants.CENTER), gbc);
        
        gbc.gridy++;
        panel.add(new JSeparator(), gbc);
        
        gbc.gridy++;
        panel.add(new JLabel("System Health:"), gbc);
        gbc.gridx = 1;
        systemHealthLabel = new JLabel("Unknown");
        panel.add(systemHealthLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Load Balance:"), gbc);
        gbc.gridx = 1;
        loadBalanceLabel = new JLabel("Unknown");
        panel.add(loadBalanceLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Available Nodes:"), gbc);
        gbc.gridx = 1;
        JLabel nodesLabel = new JLabel("Unknown");
        panel.add(nodesLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JSeparator(), gbc);
        
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton refreshButton = new JButton("Refresh Monitor Data");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSystemMonitor();
            }
        });
        panel.add(refreshButton, gbc);
        
        return panel;
    }
    
    private void simulateBadgeSwipe() {
        String readerId = readerIdField.getText().trim();
        String badgeId = badgeIdField.getText().trim();
        
        if (readerId.isEmpty() || badgeId.isEmpty()) {
            logMessage("Error: enter reader ID and badge ID");
            return;
        }

        if (badgeReaderSimulator == null) {
            logMessage("Error: badge reader simulator unavailable");
            return;
        }
        
        new Thread(() -> {
            try {
                logMessage("Starting swipe simulation: reader=" + readerId + ", badge=" + badgeId);
                var result = badgeReaderSimulator.simulateBadgeSwipe(readerId, badgeId);
                logMessage("Swipe result: decision=" + result.getDecision() + ", reason=" + result.getReasonCode());
                logMessage("Message: " + result.getMessage());
            } catch (InterruptedException ex) {
                logMessage("Simulation interrupted: " + ex.getMessage());
            } catch (Exception ex) {
                logMessage("Simulation failed: " + ex.getMessage());
            }
        }).start();
    }
    
    private void simulateBadgeRead() {
        String readerId = readerIdField.getText().trim();
        String badgeId = badgeIdField.getText().trim();
        
        if (readerId.isEmpty() || badgeId.isEmpty()) {
            logMessage("Error: enter reader ID and badge ID");
            return;
        }

        if (badgeReaderSimulator == null) {
            logMessage("Error: badge reader simulator unavailable");
            return;
        }
        
        new Thread(() -> {
            try {
                logMessage("Starting read simulation: reader=" + readerId + ", badge=" + badgeId);
                var code = badgeReaderSimulator.readBadgeCode(readerId, badgeId);
                logMessage("Read result: badge code=" + (code != null ? code : "Read failed"));
                
                // 获取并显示徽章读取状态信息
                String status = badgeReaderSimulator.getLastReadStatus();
                if (status != null) {
                    logMessage("Badge status: " + status);
                }
            } catch (InterruptedException ex) {
                logMessage("Simulation interrupted: " + ex.getMessage());
            } catch (Exception ex) {
                logMessage("Simulation failed: " + ex.getMessage());
            }
        }).start();
    }
    
    private void getReaderStats() {
        String readerId = readerIdField.getText().trim();
        
        if (readerId.isEmpty()) {
            logMessage("Error: enter reader ID");
            return;
        }

        if (badgeReaderSimulator == null) {
            logMessage("Error: badge reader simulator unavailable");
            return;
        }
        
        try {
            String stats = badgeReaderSimulator.getSimulationStats(readerId);
            logMessage("Reader stats: " + stats);
        } catch (Exception ex) {
            logMessage("Failed to get stats: " + ex.getMessage());
        }
    }
    
    private void startEventSimulation(int numEvents, int concurrency, double acceleration) {
        if (eventSimulator == null) {
            logMessage("Error: event simulator unavailable");
            return;
        }
        
        new Thread(() -> {
            try {
                eventSimulator.setTimeAcceleration(acceleration);
                eventSimulator.startSimulation(numEvents, concurrency);
                logMessage("Event simulation started: events=" + numEvents + ", concurrency=" + concurrency + ", acceleration=" + acceleration);
            } catch (Exception ex) {
                logMessage("Failed to start simulation: " + ex.getMessage());
            }
        }).start();
    }
    
    private void stopEventSimulation() {
        if (eventSimulator == null) {
            logMessage("Error: event simulator unavailable");
            return;
        }
        
        try {
            eventSimulator.stopSimulation();
            logMessage("Event simulation stopped");
        } catch (Exception ex) {
            logMessage("Failed to stop simulation: " + ex.getMessage());
        }
    }
    
    private void resetEventSimulation() {
        if (eventSimulator == null) {
            logMessage("Error: event simulator unavailable");
            return;
        }
        
        try {
            eventSimulator.resetSimulation();
            logMessage("Event simulation reset");
        } catch (Exception ex) {
            logMessage("Failed to reset simulation: " + ex.getMessage());
        }
    }
    
    private void getPerformanceMetrics() {
        if (eventSimulator == null) {
            logMessage("Error: event simulator unavailable");
            return;
        }
        
        try {
            Map<String, Object> metrics = eventSimulator.getPerformanceMetrics();
            StringBuilder sb = new StringBuilder();
            sb.append("Performance Metrics:\n");
            for (Map.Entry<String, Object> entry : metrics.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            logMessage(sb.toString());
        } catch (Exception ex) {
            logMessage("Failed to get metrics: " + ex.getMessage());
        }
    }
    
    private void markNodeAsFailed(String nodeId) {
        if (routerSystem == null) {
            logMessage("Error: router system unavailable");
            return;
        }
        
        try {
            routerSystem.markNodeAsFailed(nodeId);
            logMessage("Node marked failed: " + nodeId);
            updateRouterInfo();
        } catch (Exception ex) {
            logMessage("Failed to mark node failed: " + ex.getMessage());
        }
    }
    
    private void recoverNode(String nodeId) {
        if (routerSystem == null) {
            logMessage("Error: router system unavailable");
            return;
        }
        
        try {
            routerSystem.recoverNode(nodeId);
            logMessage("Node recovered: " + nodeId);
            updateRouterInfo();
        } catch (Exception ex) {
            logMessage("Failed to recover node: " + ex.getMessage());
        }
    }
    
    private void getLoadBalanceStats() {
        if (routerSystem == null) {
            logMessage("Error: router system unavailable");
            return;
        }
        
        try {
                LoadBalanceStats stats = routerSystem.getLoadBalanceStats();
                StringBuilder sb = new StringBuilder();
                sb.append("Load Balance Stats:\n");
                sb.append("  Total Requests: ").append(stats.getTotalRequests()).append("\n");
                sb.append("  Failed Requests: ").append(stats.getFailedRequests()).append("\n");
                sb.append("  Rerouted Requests: ").append(stats.getReroutedRequests()).append("\n");
                sb.append("  Failure Rate: ").append(String.format("%.2f%%", stats.getFailureRate() * 100)).append("\n");
                sb.append("  Request Distribution: ").append(stats.getRequestsDistribution()).append("\n");
            logMessage(sb.toString());
        } catch (Exception ex) {
                logMessage("Failed to get load stats: " + ex.getMessage());
        }
    }
    
    private void setLoadBalanceStrategy(String strategy) {
        if (routerSystem == null) {
            logMessage("Error: router system unavailable");
            return;
        }
        
        try {
            routerSystem.setLoadBalanceStrategy(strategy);
            logMessage("Load balance strategy set to: " + strategy);
        } catch (Exception ex) {
            logMessage("Failed to set strategy: " + ex.getMessage());
        }
    }
    
    private void updateRouterInfo() {
        // This method needs routerSystem to read the current nodes.
        // routerInfoArea is not in scope here, so we log the info instead.
        if (routerSystem != null) {
            try {
                var nodes = routerSystem.getAvailableNodes();
                logMessage("Available nodes: " + String.join(", ", nodes));
            } catch (Exception ex) {
                logMessage("Failed to get node info: " + ex.getMessage());
            }
        }
    }
    
    private void refreshSystemMonitor() {
        if (routerSystem != null) {
            try {
                SystemHealth health = routerSystem.getSystemHealth();
                systemHealthLabel.setText(health.toString() + " - Failure Rate: " + String.format("%.1f%%", routerSystem.getLoadBalanceStats().getFailureRate() * 100));
                
                LoadBalanceStats stats = routerSystem.getLoadBalanceStats();
                loadBalanceLabel.setText("Total: " + stats.getTotalRequests() + ", Failed: " + stats.getFailedRequests());
                
                var nodes = routerSystem.getAvailableNodes();
                // Update node count label
                Component[] components = ((Container) tabbedPane.getComponentAt(3)).getComponents();
                for (Component comp : components) {
                    if (comp instanceof JPanel) {
                        Component[] subComps = ((Container) comp).getComponents();
                        for (Component subComp : subComps) {
                            if (subComp instanceof JLabel && ((JLabel) subComp).getText().startsWith("Available Nodes:")) {
                                // Find the adjacent label
                                continue;
                            }
                        }
                    }
                }
                
                logMessage("Monitor data refreshed");
            } catch (Exception ex) {
                logMessage("Failed to refresh monitor data: " + ex.getMessage());
            }
        }
    }
    
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry = "[" + timestamp + "] " + message + "\n";
        
        SwingUtilities.invokeLater(() -> {
            UiTheme.appendStatusLine(simulatorLogArea, logEntry.trim());
            simulatorLogArea.setCaretPosition(simulatorLogArea.getDocument().getLength());
        });
    }
    
    private void startStatusTimer() {
        Timer timer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSimulationStatus();
                updateCurrentTimeLabel();
            }
        });
        timer.start();
    }
    
    private void updateSimulationStatus() {
        if (eventSimulator != null) {
            try {
                SimulationStatus status = eventSimulator.getSimulationStatus();
                SwingUtilities.invokeLater(() -> {
                    simulationStatusLabel.setText("Status: " + status.toString());
                });
            } catch (Exception ex) {
                // Ignore status update errors.
            }
        }
    }
    
    private void setSimulatedTime() {
        String timeText = absoluteTimeField.getText().trim();
        if (timeText.isEmpty()) {
            logMessage("Error: enter time (format: yyyy-MM-dd HH:mm)");
            return;
        }
        
        try {
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(timeText, 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            clockService.setSimulatedTime(dateTime);
            logMessage("Simulated time set to: " + dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            updateCurrentTimeLabel();
        } catch (java.time.format.DateTimeParseException e) {
            logMessage("Error: invalid time format, use yyyy-MM-dd HH:mm");
        }
    }
    
    private void resetSimulatedTime() {
        clockService.resetToRealTime();
        absoluteTimeField.setText(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        logMessage("Reset to real system time");
        updateCurrentTimeLabel();
    }
    
    private void updateCurrentTimeLabel() {
        if (currentTimeLabel != null) {
            SwingUtilities.invokeLater(() -> {
                currentTimeLabel.setText("Current Time: " + clockService.localNow().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            });
        }
    }
    
    /**
     * Create the execution chain panel.
     */
    private JPanel createExecutionChainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearButton = new JButton("Clear Chains");
        clearButton.addActionListener(e -> clearExecutionChains());
        controlPanel.add(clearButton);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshExecutionChains());
        controlPanel.add(refreshButton);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        // Execution chain detail area
        executionChainArea = UiTheme.createLogPane(true);
        JPanel areaPanel = new JPanel(new BorderLayout());
        areaPanel.add(new JLabel("Execution Chain Details:"), BorderLayout.NORTH);
        areaPanel.add(new JScrollPane(executionChainArea), BorderLayout.CENTER);
        panel.add(areaPanel, BorderLayout.CENTER);
        
        // Register execution chain listener
        ExecutionChainTracker.getInstance().addListener(new ExecutionChainListenerImpl());
        
        return panel;
    }
    
    /**
     * Clear execution chain display.
     */
    private void clearExecutionChains() {
        SwingUtilities.invokeLater(() -> {
            UiTheme.setStatusText(executionChainArea, "");
            ExecutionChainTracker.getInstance().clearAllChains();
            logMessage("Execution chains cleared");
        });
    }
    
    /**
     * Refresh execution chain display.
     */
    private void refreshExecutionChains() {
        SwingUtilities.invokeLater(() -> {
            UiTheme.setStatusText(executionChainArea, "");
            
            List<ExecutionChainTracker.ExecutionChain> chains = 
                    ExecutionChainTracker.getInstance().getAllChains();
            
            StringBuilder sb = new StringBuilder();
            for (ExecutionChainTracker.ExecutionChain chain : chains) {
                sb.append("Chain: ").append(chain.getChainId()).append("\n");
                sb.append("Event ID: ").append(chain.getEventId()).append("\n");
                sb.append("Status: ").append(chain.isCompleted() ? "Completed" : "In Progress").append("\n");
                sb.append("Steps: ").append(chain.getSteps().size()).append("\n");
                sb.append("Step Details:\n");
                
                for (ExecutionChainTracker.ChainStep step : chain.getSteps()) {
                    sb.append("  ").append(step.toString()).append("\n");
                }
                sb.append("\n");
            }
            
            if (chains.isEmpty()) {
                sb.append("No execution chains available.\n");
            }
            
            UiTheme.setStatusText(executionChainArea, sb.toString());
            logMessage("Execution chains refreshed: " + chains.size());
        });
    }
    

    
    /**
     * Update execution chain text area.
     */
    private void updateExecutionChainArea(ExecutionChainTracker.ExecutionChain chain) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Chain: ").append(chain.getChainId()).append("\n");
            sb.append("Event ID: ").append(chain.getEventId()).append("\n");
            sb.append("Status: ").append(chain.isCompleted() ? "Completed" : "In Progress").append("\n");
            sb.append("Steps: ").append(chain.getSteps().size()).append("\n");
            sb.append("Step Details:\n");
            
            for (ExecutionChainTracker.ChainStep step : chain.getSteps()) {
                sb.append("  ").append(step.toString()).append("\n");
            }
            
            UiTheme.setStatusText(executionChainArea, sb.toString());
        });
    }
    
    /**
     * Execution chain listener.
     */
    private class ExecutionChainListenerImpl implements ExecutionChainTracker.ExecutionChainListener {
        @Override
        public void onChainStarted(ExecutionChainTracker.ExecutionChain chain) {
            logMessage("Chain started: " + chain.getChainId() + " (Event: " + chain.getEventId() + ")");
            updateExecutionChainArea(chain);
        }
        
        @Override
        public void onStepAdded(ExecutionChainTracker.ExecutionChain chain, 
                               ExecutionChainTracker.ChainStep step) {
            updateExecutionChainArea(chain);
        }
        
        @Override
        public void onChainCompleted(ExecutionChainTracker.ExecutionChain chain) {
            logMessage("Chain completed: " + chain.getChainId() + " (Event: " + chain.getEventId() + ")");
            updateExecutionChainArea(chain);
        }
    }

    private JPanel createScenarioConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel("Enabled:"), gbc);
        gbc.gridx = 1;
        scenarioEnabledCheck = new JCheckBox();
        scenarioEnabledCheck.setSelected(true);
        topPanel.add(scenarioEnabledCheck, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        topPanel.add(new JLabel("Step Delay (ms):"), gbc);
        gbc.gridx = 1;
        scenarioStepDelayField = new JTextField(10);
        topPanel.add(scenarioStepDelayField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> loadScenarioConfig());
        buttonPanel.add(loadButton);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveScenarioConfig());
        buttonPanel.add(saveButton);

        JButton validateButton = new JButton("Validate");
        validateButton.addActionListener(e -> validateScenarioConfig());
        buttonPanel.add(validateButton);

        topPanel.add(buttonPanel, gbc);

        panel.add(topPanel, BorderLayout.NORTH);

        String[] columns = {"Path Name", "Resource IDs (comma-separated)"};
        scenarioTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        scenarioTable = new JTable(scenarioTableModel);
        scenarioTable.setAutoCreateRowSorter(true);
        panel.add(new JScrollPane(scenarioTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JLabel("Path Name:"));
        scenarioNameField = new JTextField(12);
        bottomPanel.add(scenarioNameField);
        bottomPanel.add(new JLabel("Resource IDs:"));
        scenarioResourcesField = new JTextField(30);
        bottomPanel.add(scenarioResourcesField);

        JButton addPathButton = new JButton("Add Path");
        addPathButton.addActionListener(e -> addScenarioPath());
        bottomPanel.add(addPathButton);

        JButton removePathButton = new JButton("Remove Path");
        removePathButton.addActionListener(e -> removeScenarioPath());
        bottomPanel.add(removePathButton);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        loadScenarioConfig();
        return panel;
    }

    private void addScenarioPath() {
        String name = scenarioNameField.getText().trim();
        String resources = scenarioResourcesField.getText().trim();
        if (name.isEmpty() || resources.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter path name and resource IDs");
            return;
        }
        scenarioTableModel.addRow(new Object[]{name, resources});
        scenarioNameField.setText("");
        scenarioResourcesField.setText("");
    }

    private void removeScenarioPath() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to remove.");
            return;
        }
        int modelRow = scenarioTable.convertRowIndexToModel(row);
        scenarioTableModel.removeRow(modelRow);
    }

    private void loadScenarioConfig() {
        SimulationScenarioConfig config = readScenarioConfig();
        if (config == null) {
            JOptionPane.showMessageDialog(this, "Scenario config not found.");
            return;
        }
        scenarioEnabledCheck.setSelected(config.isEnabled());
        scenarioStepDelayField.setText(config.getStepDelayMs() == null ? "" : String.valueOf(config.getStepDelayMs()));
        scenarioTableModel.setRowCount(0);
        if (config.getPaths() != null) {
            for (SimulationPath path : config.getPaths()) {
                String name = path.getName() == null ? "" : path.getName();
                String resources = "";
                if (path.getResourceIds() != null && !path.getResourceIds().isEmpty()) {
                    resources = String.join(",", path.getResourceIds());
                }
                scenarioTableModel.addRow(new Object[]{name, resources});
            }
        }
    }

    private void saveScenarioConfig() {
        try {
            SimulationScenarioConfig config = new SimulationScenarioConfig();
            config.setEnabled(scenarioEnabledCheck.isSelected());
            String delayText = scenarioStepDelayField.getText().trim();
            if (!delayText.isEmpty()) {
                config.setStepDelayMs(Integer.parseInt(delayText));
            }
            List<SimulationPath> paths = new ArrayList<>();
            for (int i = 0; i < scenarioTableModel.getRowCount(); i++) {
                String name = String.valueOf(scenarioTableModel.getValueAt(i, 0)).trim();
                String resources = String.valueOf(scenarioTableModel.getValueAt(i, 1)).trim();
                if (name.isEmpty() || resources.isEmpty()) {
                    continue;
                }
                SimulationPath path = new SimulationPath();
                path.setName(name);
                List<String> resourceIds = new ArrayList<>();
                for (String token : resources.split(",")) {
                    String trimmed = token.trim();
                    if (!trimmed.isEmpty()) {
                        resourceIds.add(trimmed);
                    }
                }
                path.setResourceIds(resourceIds);
                paths.add(path);
            }
            config.setPaths(paths);

            writeScenarioConfig(config);
            JOptionPane.showMessageDialog(this, "Saved successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }

    private void validateScenarioConfig() {
        try {
            SimulationScenarioConfig config = buildScenarioConfigFromUI();
            if (config.getPaths() == null || config.getPaths().isEmpty()) {
                JOptionPane.showMessageDialog(this, "No paths configured.");
                return;
            }
            JOptionPane.showMessageDialog(this, "Validation passed.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Validation failed: " + ex.getMessage());
        }
    }

    private SimulationScenarioConfig buildScenarioConfigFromUI() {
        SimulationScenarioConfig config = new SimulationScenarioConfig();
        config.setEnabled(scenarioEnabledCheck.isSelected());
        String delayText = scenarioStepDelayField.getText().trim();
        if (!delayText.isEmpty()) {
            config.setStepDelayMs(Integer.parseInt(delayText));
        }
        List<SimulationPath> paths = new ArrayList<>();
        for (int i = 0; i < scenarioTableModel.getRowCount(); i++) {
            String name = String.valueOf(scenarioTableModel.getValueAt(i, 0)).trim();
            String resources = String.valueOf(scenarioTableModel.getValueAt(i, 1)).trim();
            if (name.isEmpty() || resources.isEmpty()) {
                continue;
            }
            SimulationPath path = new SimulationPath();
            path.setName(name);
            List<String> resourceIds = new ArrayList<>();
            for (String token : resources.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    resourceIds.add(trimmed);
                }
            }
            path.setResourceIds(resourceIds);
            paths.add(path);
        }
        config.setPaths(paths);
        return config;
    }

    private SimulationScenarioConfig readScenarioConfig() {
        try {
            Path sourcePath = getScenarioSourcePath();
            if (Files.exists(sourcePath)) {
                String json = Files.readString(sourcePath, StandardCharsets.UTF_8);
                return objectMapper.readValue(json, SimulationScenarioConfig.class);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void writeScenarioConfig(SimulationScenarioConfig config) throws Exception {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
        Path sourcePath = getScenarioSourcePath();
        Files.createDirectories(sourcePath.getParent());
        Files.writeString(sourcePath, json, StandardCharsets.UTF_8);

        Path targetPath = getScenarioTargetPath();
        if (targetPath != null) {
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, json, StandardCharsets.UTF_8);
        }
    }

    private Path getScenarioSourcePath() {
        return Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "simulator", "scenarios.json");
    }

    private Path getScenarioTargetPath() {
        Path target = Paths.get(System.getProperty("user.dir"), "target", "classes", "simulator", "scenarios.json");
        return target;
    }

}
