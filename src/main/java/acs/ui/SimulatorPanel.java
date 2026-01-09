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
import acs.simulator.LoadBalanceStats;

public class SimulatorPanel extends JPanel {
    private BadgeReaderSimulator badgeReaderSimulator;
    private EventSimulator eventSimulator;
    private RouterSystem routerSystem;
    
    private JTabbedPane tabbedPane;
    private JTextField readerIdField;
    private JTextField badgeIdField;
    private JTextArea simulatorLogArea;
    private JTable eventTable;
    private DefaultTableModel eventTableModel;
    private JLabel simulationStatusLabel;
    private JLabel systemHealthLabel;
    private JLabel loadBalanceLabel;
    
    public SimulatorPanel(BadgeReaderSimulator badgeReaderSimulator, 
                         EventSimulator eventSimulator,
                         RouterSystem routerSystem) {
        this.badgeReaderSimulator = badgeReaderSimulator;
        this.eventSimulator = eventSimulator;
        this.routerSystem = routerSystem;
        initUI();
        startStatusTimer();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("模拟器控制面板", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("读卡器模拟", createBadgeReaderPanel());
        tabbedPane.addTab("事件模拟", createEventSimulatorPanel());
        tabbedPane.addTab("路由系统", createRouterSystemPanel());
        tabbedPane.addTab("系统监控", createSystemMonitorPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("读卡器模拟器: " + (badgeReaderSimulator != null ? "可用" : "不可用")));
        statusPanel.add(new JLabel("事件模拟器: " + (eventSimulator != null ? "可用" : "不可用")));
        statusPanel.add(new JLabel("路由系统: " + (routerSystem != null ? "可用" : "不可用")));
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createBadgeReaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("读卡器ID:"), gbc);
        
        gbc.gridx = 1;
        readerIdField = new JTextField(20);
        readerIdField.setText("READER-001");
        inputPanel.add(readerIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("徽章ID:"), gbc);
        
        gbc.gridx = 1;
        badgeIdField = new JTextField(20);
        badgeIdField.setText("B-10001");
        inputPanel.add(badgeIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton swipeButton = new JButton("模拟刷卡");
        swipeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulateBadgeSwipe();
            }
        });
        buttonPanel.add(swipeButton);
        
        JButton readButton = new JButton("模拟读卡");
        readButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulateBadgeRead();
            }
        });
        buttonPanel.add(readButton);
        
        JButton statusButton = new JButton("获取统计");
        statusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getReaderStats();
            }
        });
        buttonPanel.add(statusButton);
        
        inputPanel.add(buttonPanel, gbc);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        simulatorLogArea = new JTextArea(15, 60);
        simulatorLogArea.setEditable(false);
        simulatorLogArea.setFont(new Font("宋体", Font.PLAIN, 12));
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
        controlPanel.add(new JLabel("事件总数:"), gbc);
        
        gbc.gridx = 1;
        JTextField numEventsField = new JTextField(10);
        numEventsField.setText("100");
        controlPanel.add(numEventsField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        controlPanel.add(new JLabel("并发级别:"), gbc);
        
        gbc.gridx = 1;
        JTextField concurrencyField = new JTextField(10);
        concurrencyField.setText("10");
        controlPanel.add(concurrencyField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        controlPanel.add(new JLabel("时间加速:"), gbc);
        
        gbc.gridx = 1;
        JTextField timeAccelField = new JTextField(10);
        timeAccelField.setText("5.0");
        controlPanel.add(timeAccelField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton startButton = new JButton("启动模拟");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int numEvents = Integer.parseInt(numEventsField.getText());
                    int concurrency = Integer.parseInt(concurrencyField.getText());
                    double acceleration = Double.parseDouble(timeAccelField.getText());
                    startEventSimulation(numEvents, concurrency, acceleration);
                } catch (NumberFormatException ex) {
                    logMessage("错误: 请输入有效的数字");
                }
            }
        });
        buttonPanel.add(startButton);
        
        JButton stopButton = new JButton("停止模拟");
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopEventSimulation();
            }
        });
        buttonPanel.add(stopButton);
        
        JButton resetButton = new JButton("重置模拟");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetEventSimulation();
            }
        });
        buttonPanel.add(resetButton);
        
        JButton metricsButton = new JButton("获取指标");
        metricsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getPerformanceMetrics();
            }
        });
        buttonPanel.add(metricsButton);
        
        controlPanel.add(buttonPanel, gbc);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        String[] columns = {"时间", "事件类型", "读卡器", "徽章", "资源", "结果"};
        eventTableModel = new DefaultTableModel(columns, 0);
        eventTable = new JTable(eventTableModel);
        eventTable.setAutoCreateRowSorter(true);
        
        JScrollPane scrollPane = new JScrollPane(eventTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        simulationStatusLabel = new JLabel("状态: 未启动");
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
        controlPanel.add(new JLabel("节点ID:"), gbc);
        
        gbc.gridx = 1;
        JTextField nodeIdField = new JTextField(15);
        nodeIdField.setText("NODE-001");
        controlPanel.add(nodeIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton failButton = new JButton("标记节点故障");
        failButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                markNodeAsFailed(nodeIdField.getText());
            }
        });
        buttonPanel.add(failButton);
        
        JButton recoverButton = new JButton("恢复节点");
        recoverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                recoverNode(nodeIdField.getText());
            }
        });
        buttonPanel.add(recoverButton);
        
        JButton statsButton = new JButton("获取负载统计");
        statsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLoadBalanceStats();
            }
        });
        buttonPanel.add(statsButton);
        
        JComboBox<String> strategyCombo = new JComboBox<>(new String[]{"ROUND_ROBIN", "RANDOM", "LEAST_CONNECTIONS"});
        JButton setStrategyButton = new JButton("设置策略");
        setStrategyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setLoadBalanceStrategy((String) strategyCombo.getSelectedItem());
            }
        });
        buttonPanel.add(new JLabel("负载策略:"));
        buttonPanel.add(strategyCombo);
        buttonPanel.add(setStrategyButton);
        
        controlPanel.add(buttonPanel, gbc);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        JTextArea routerInfoArea = new JTextArea(15, 60);
        routerInfoArea.setEditable(false);
        routerInfoArea.setFont(new Font("宋体", Font.PLAIN, 12));
        
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
        panel.add(new JLabel("系统监控", SwingConstants.CENTER), gbc);
        
        gbc.gridy++;
        panel.add(new JSeparator(), gbc);
        
        gbc.gridy++;
        panel.add(new JLabel("系统健康状态:"), gbc);
        gbc.gridx = 1;
        systemHealthLabel = new JLabel("未知");
        panel.add(systemHealthLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("负载均衡状态:"), gbc);
        gbc.gridx = 1;
        loadBalanceLabel = new JLabel("未知");
        panel.add(loadBalanceLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("可用节点数:"), gbc);
        gbc.gridx = 1;
        JLabel nodesLabel = new JLabel("未知");
        panel.add(nodesLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JSeparator(), gbc);
        
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton refreshButton = new JButton("刷新监控数据");
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
            logMessage("错误: 请输入读卡器ID和徽章ID");
            return;
        }
        
        if (badgeReaderSimulator == null) {
            logMessage("错误: 读卡器模拟器不可用");
            return;
        }
        
        new Thread(() -> {
            try {
                logMessage("开始模拟刷卡: 读卡器=" + readerId + ", 徽章=" + badgeId);
                var result = badgeReaderSimulator.simulateBadgeSwipe(readerId, badgeId);
                logMessage("刷卡结果: 决策=" + result.getDecision() + ", 原因=" + result.getReasonCode());
                logMessage("消息: " + result.getMessage());
            } catch (InterruptedException ex) {
                logMessage("模拟被中断: " + ex.getMessage());
            } catch (Exception ex) {
                logMessage("模拟失败: " + ex.getMessage());
            }
        }).start();
    }
    
    private void simulateBadgeRead() {
        String readerId = readerIdField.getText().trim();
        String badgeId = badgeIdField.getText().trim();
        
        if (readerId.isEmpty() || badgeId.isEmpty()) {
            logMessage("错误: 请输入读卡器ID和徽章ID");
            return;
        }
        
        if (badgeReaderSimulator == null) {
            logMessage("错误: 读卡器模拟器不可用");
            return;
        }
        
        new Thread(() -> {
            try {
                logMessage("开始模拟读卡: 读卡器=" + readerId + ", 徽章=" + badgeId);
                var code = badgeReaderSimulator.readBadgeCode(readerId, badgeId);
                logMessage("读卡结果: 徽章代码=" + (code != null ? code : "读取失败"));
            } catch (InterruptedException ex) {
                logMessage("模拟被中断: " + ex.getMessage());
            } catch (Exception ex) {
                logMessage("模拟失败: " + ex.getMessage());
            }
        }).start();
    }
    
    private void getReaderStats() {
        String readerId = readerIdField.getText().trim();
        
        if (readerId.isEmpty()) {
            logMessage("错误: 请输入读卡器ID");
            return;
        }
        
        if (badgeReaderSimulator == null) {
            logMessage("错误: 读卡器模拟器不可用");
            return;
        }
        
        try {
            String stats = badgeReaderSimulator.getSimulationStats(readerId);
            logMessage("读卡器统计: " + stats);
        } catch (Exception ex) {
            logMessage("获取统计失败: " + ex.getMessage());
        }
    }
    
    private void startEventSimulation(int numEvents, int concurrency, double acceleration) {
        if (eventSimulator == null) {
            logMessage("错误: 事件模拟器不可用");
            return;
        }
        
        new Thread(() -> {
            try {
                eventSimulator.setTimeAcceleration(acceleration);
                eventSimulator.startSimulation(numEvents, concurrency);
                logMessage("事件模拟已启动: 事件数=" + numEvents + ", 并发级别=" + concurrency + ", 时间加速=" + acceleration);
            } catch (Exception ex) {
                logMessage("启动模拟失败: " + ex.getMessage());
            }
        }).start();
    }
    
    private void stopEventSimulation() {
        if (eventSimulator == null) {
            logMessage("错误: 事件模拟器不可用");
            return;
        }
        
        try {
            eventSimulator.stopSimulation();
            logMessage("事件模拟已停止");
        } catch (Exception ex) {
            logMessage("停止模拟失败: " + ex.getMessage());
        }
    }
    
    private void resetEventSimulation() {
        if (eventSimulator == null) {
            logMessage("错误: 事件模拟器不可用");
            return;
        }
        
        try {
            eventSimulator.resetSimulation();
            logMessage("事件模拟已重置");
        } catch (Exception ex) {
            logMessage("重置模拟失败: " + ex.getMessage());
        }
    }
    
    private void getPerformanceMetrics() {
        if (eventSimulator == null) {
            logMessage("错误: 事件模拟器不可用");
            return;
        }
        
        try {
            Map<String, Object> metrics = eventSimulator.getPerformanceMetrics();
            StringBuilder sb = new StringBuilder();
            sb.append("性能指标:\n");
            for (Map.Entry<String, Object> entry : metrics.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            logMessage(sb.toString());
        } catch (Exception ex) {
            logMessage("获取指标失败: " + ex.getMessage());
        }
    }
    
    private void markNodeAsFailed(String nodeId) {
        if (routerSystem == null) {
            logMessage("错误: 路由系统不可用");
            return;
        }
        
        try {
            routerSystem.markNodeAsFailed(nodeId);
            logMessage("节点标记为故障: " + nodeId);
            updateRouterInfo();
        } catch (Exception ex) {
            logMessage("标记节点故障失败: " + ex.getMessage());
        }
    }
    
    private void recoverNode(String nodeId) {
        if (routerSystem == null) {
            logMessage("错误: 路由系统不可用");
            return;
        }
        
        try {
            routerSystem.recoverNode(nodeId);
            logMessage("节点已恢复: " + nodeId);
            updateRouterInfo();
        } catch (Exception ex) {
            logMessage("恢复节点失败: " + ex.getMessage());
        }
    }
    
    private void getLoadBalanceStats() {
        if (routerSystem == null) {
            logMessage("错误: 路由系统不可用");
            return;
        }
        
        try {
                LoadBalanceStats stats = routerSystem.getLoadBalanceStats();
                StringBuilder sb = new StringBuilder();
                sb.append("负载均衡统计:\n");
                sb.append("  总请求数: ").append(stats.getTotalRequests()).append("\n");
                sb.append("  失败请求: ").append(stats.getFailedRequests()).append("\n");
                sb.append("  重路由请求: ").append(stats.getReroutedRequests()).append("\n");
                sb.append("  失败率: ").append(String.format("%.2f%%", stats.getFailureRate() * 100)).append("\n");
                sb.append("  请求分布: ").append(stats.getRequestsDistribution()).append("\n");
            logMessage(sb.toString());
        } catch (Exception ex) {
            logMessage("获取负载统计失败: " + ex.getMessage());
        }
    }
    
    private void setLoadBalanceStrategy(String strategy) {
        if (routerSystem == null) {
            logMessage("错误: 路由系统不可用");
            return;
        }
        
        try {
            routerSystem.setLoadBalanceStrategy(strategy);
            logMessage("负载均衡策略已设置为: " + strategy);
        } catch (Exception ex) {
            logMessage("设置策略失败: " + ex.getMessage());
        }
    }
    
    private void updateRouterInfo() {
        // 这个方法需要访问routerSystem来获取信息
        // 由于routerInfoArea不在当前作用域，我们暂时用logMessage代替
        if (routerSystem != null) {
            try {
                var nodes = routerSystem.getAvailableNodes();
                logMessage("可用节点: " + String.join(", ", nodes));
            } catch (Exception ex) {
                logMessage("获取节点信息失败: " + ex.getMessage());
            }
        }
    }
    
    private void refreshSystemMonitor() {
        if (routerSystem != null) {
            try {
                SystemHealth health = routerSystem.getSystemHealth();
                systemHealthLabel.setText(health.toString() + " - 故障率: " + String.format("%.1f%%", routerSystem.getLoadBalanceStats().getFailureRate() * 100));
                
                LoadBalanceStats stats = routerSystem.getLoadBalanceStats();
                loadBalanceLabel.setText("总请求: " + stats.getTotalRequests() + ", 失败: " + stats.getFailedRequests());
                
                var nodes = routerSystem.getAvailableNodes();
                // 更新节点数标签
                Component[] components = ((Container) tabbedPane.getComponentAt(3)).getComponents();
                for (Component comp : components) {
                    if (comp instanceof JPanel) {
                        Component[] subComps = ((Container) comp).getComponents();
                        for (Component subComp : subComps) {
                            if (subComp instanceof JLabel && ((JLabel) subComp).getText().startsWith("可用节点数:")) {
                                // 找到相邻的标签
                                continue;
                            }
                        }
                    }
                }
                
                logMessage("系统监控数据已刷新");
            } catch (Exception ex) {
                logMessage("刷新监控数据失败: " + ex.getMessage());
            }
        }
    }
    
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry = "[" + timestamp + "] " + message + "\n";
        
        SwingUtilities.invokeLater(() -> {
            simulatorLogArea.append(logEntry);
            simulatorLogArea.setCaretPosition(simulatorLogArea.getDocument().getLength());
        });
    }
    
    private void startStatusTimer() {
        Timer timer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSimulationStatus();
            }
        });
        timer.start();
    }
    
    private void updateSimulationStatus() {
        if (eventSimulator != null) {
            try {
                SimulationStatus status = eventSimulator.getSimulationStatus();
                SwingUtilities.invokeLater(() -> {
                    simulationStatusLabel.setText("状态: " + status.toString());
                });
            } catch (Exception ex) {
                // 忽略状态更新错误
            }
        }
    }
}