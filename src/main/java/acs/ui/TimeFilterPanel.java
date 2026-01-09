package acs.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import acs.service.TimeFilterService;
import acs.domain.TimeFilter;
import acs.repository.TimeFilterRepository;
import java.time.LocalDateTime;

public class TimeFilterPanel extends JPanel {
    private TimeFilterService timeFilterService;
    private TimeFilterRepository timeFilterRepository;
    
    private JTable filterTable;
    private DefaultTableModel filterTableModel;
    private JTextField ruleField;
    private JTextField testTimeField;
    private JTextArea resultArea;
    
    public TimeFilterPanel(TimeFilterService timeFilterService,
                          TimeFilterRepository timeFilterRepository) {
        this.timeFilterService = timeFilterService;
        this.timeFilterRepository = timeFilterRepository;
        initUI();
        refreshFilterTable();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("时间过滤器管理面板", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("规则管理", createRuleManagementPanel());
        tabbedPane.addTab("规则测试", createRuleTestPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("时间过滤器服务: " + (timeFilterService != null ? "可用" : "不可用")));
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createRuleManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"过滤器ID", "过滤器名称", "原始规则", "年份", "月份", "开始时间", "结束时间"};
        filterTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        filterTable = new JTable(filterTableModel);
        filterTable.setAutoCreateRowSorter(true);
        
        JScrollPane scrollPane = new JScrollPane(filterTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        controlPanel.add(new JLabel("时间规则:"), gbc);
        
        gbc.gridx = 1;
        ruleField = new JTextField(40);
        ruleField.setText("2025.July,August.Monday-Friday.8:00-12:00");
        controlPanel.add(ruleField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton parseButton = new JButton("解析规则");
        parseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parseTimeRule();
            }
        });
        buttonPanel.add(parseButton);
        
        JButton validateButton = new JButton("验证规则");
        validateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validateTimeRule();
            }
        });
        buttonPanel.add(validateButton);
        
        JButton refreshButton = new JButton("刷新列表");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshFilterTable();
            }
        });
        buttonPanel.add(refreshButton);
        
        JButton clearButton = new JButton("清除规则");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ruleField.setText("");
            }
        });
        buttonPanel.add(clearButton);
        
        controlPanel.add(buttonPanel, gbc);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    private JPanel createRuleTestPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("时间规则:"), gbc);
        
        gbc.gridx = 1;
        JTextField testRuleField = new JTextField(30);
        testRuleField.setText("2025.July,August.Monday-Friday.8:00-12:00");
        inputPanel.add(testRuleField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("测试时间:"), gbc);
        
        gbc.gridx = 1;
        testTimeField = new JTextField(25);
        testTimeField.setText(LocalDateTime.now().toString());
        inputPanel.add(testTimeField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton testMatchButton = new JButton("测试匹配");
        testMatchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testTimeMatch(testRuleField.getText());
            }
        });
        buttonPanel.add(testMatchButton);
        
        JButton nowButton = new JButton("当前时间");
        nowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testTimeField.setText(LocalDateTime.now().toString());
            }
        });
        buttonPanel.add(nowButton);
        
        JButton exampleButton = new JButton("示例规则");
        exampleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showExampleRules();
            }
        });
        buttonPanel.add(exampleButton);
        
        inputPanel.add(buttonPanel, gbc);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("宋体", Font.PLAIN, 12));
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private void parseTimeRule() {
        String rawRule = ruleField.getText().trim();
        
        if (rawRule.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入时间规则");
            return;
        }
        
        try {
            TimeFilter timeFilter = timeFilterService.parseTimeRule(rawRule);
            
            StringBuilder sb = new StringBuilder();
            sb.append("规则解析成功:\n");
            sb.append("原始规则: ").append(rawRule).append("\n");
            sb.append("过滤器ID: ").append(timeFilter.getTimeFilterId()).append("\n");
            sb.append("过滤器名称: ").append(timeFilter.getFilterName() != null ? timeFilter.getFilterName() : "未命名").append("\n");
            sb.append("年份: ").append(timeFilter.getYear() != null ? timeFilter.getYear() : "所有年份").append("\n");
            sb.append("月份: ").append(timeFilter.getMonths() != null ? timeFilter.getMonths() : "所有月份").append("\n");
            sb.append("日期: ").append(timeFilter.getDaysOfMonth() != null ? timeFilter.getDaysOfMonth() : "所有日期").append("\n");
            sb.append("星期: ").append(timeFilter.getDaysOfWeek() != null ? timeFilter.getDaysOfWeek() : "所有星期").append("\n");
            sb.append("开始时间: ").append(timeFilter.getStartTime() != null ? timeFilter.getStartTime() : "不限").append("\n");
            sb.append("结束时间: ").append(timeFilter.getEndTime() != null ? timeFilter.getEndTime() : "不限").append("\n");
            sb.append("是否循环: ").append(timeFilter.getIsRecurring() != null && timeFilter.getIsRecurring() ? "是" : "否").append("\n");
            
            resultArea.setText(sb.toString());
            refreshFilterTable();
        } catch (Exception ex) {
            resultArea.setText("规则解析失败: " + ex.getMessage());
        }
    }
    
    private void validateTimeRule() {
        String rawRule = ruleField.getText().trim();
        
        if (rawRule.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入时间规则");
            return;
        }
        
        try {
            boolean valid = timeFilterService.validateTimeRule(rawRule);
            
            if (valid) {
                resultArea.setText("规则验证通过: " + rawRule + "\n\n格式正确，可以被成功解析。");
            } else {
                resultArea.setText("规则验证失败: " + rawRule + "\n\n格式错误，请检查规则语法。");
            }
        } catch (Exception ex) {
            resultArea.setText("验证过程中出错: " + ex.getMessage());
        }
    }
    
    private void testTimeMatch(String rawRule) {
        String timeStr = testTimeField.getText().trim();
        
        if (rawRule.isEmpty() || timeStr.isEmpty()) {
            resultArea.setText("错误: 请输入时间规则和测试时间");
            return;
        }
        
        try {
            LocalDateTime testTime = LocalDateTime.parse(timeStr);
            TimeFilter timeFilter = timeFilterService.parseTimeRule(rawRule);
            boolean matches = timeFilterService.matches(timeFilter, testTime);
            
            StringBuilder sb = new StringBuilder();
            sb.append("时间匹配测试:\n");
            sb.append("时间规则: ").append(rawRule).append("\n");
            sb.append("测试时间: ").append(testTime).append("\n");
            sb.append("匹配结果: ").append(matches ? "✅ 匹配" : "❌ 不匹配").append("\n\n");
            
            // 显示详细信息
            sb.append("规则解析详情:\n");
            sb.append("过滤器ID: ").append(timeFilter.getTimeFilterId()).append("\n");
            sb.append("过滤器名称: ").append(timeFilter.getFilterName() != null ? timeFilter.getFilterName() : "未命名").append("\n");
            sb.append("年份匹配: ").append(timeFilter.getYear() != null ? timeFilter.getYear() : "所有年份").append("\n");
            sb.append("月份匹配: ").append(timeFilter.getMonths() != null ? timeFilter.getMonths() : "所有月份").append("\n");
            sb.append("日期匹配: ").append(timeFilter.getDaysOfMonth() != null ? timeFilter.getDaysOfMonth() : "所有日期").append("\n");
            sb.append("星期匹配: ").append(timeFilter.getDaysOfWeek() != null ? timeFilter.getDaysOfWeek() : "所有星期").append("\n");
            sb.append("开始时间: ").append(timeFilter.getStartTime() != null ? timeFilter.getStartTime() : "不限").append("\n");
            sb.append("结束时间: ").append(timeFilter.getEndTime() != null ? timeFilter.getEndTime() : "不限").append("\n");
            sb.append("是否循环: ").append(timeFilter.getIsRecurring() != null && timeFilter.getIsRecurring() ? "是" : "否").append("\n");
            
            // 显示测试时间的各个部分
            sb.append("\n测试时间分析:\n");
            sb.append("年份: ").append(testTime.getYear()).append("\n");
            sb.append("月份: ").append(testTime.getMonthValue()).append(" (").append(testTime.getMonth()).append(")\n");
            sb.append("日期: ").append(testTime.getDayOfMonth()).append("\n");
            sb.append("星期: ").append(testTime.getDayOfWeek()).append("\n");
            sb.append("时间: ").append(String.format("%02d:%02d", testTime.getHour(), testTime.getMinute())).append("\n");
            
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("测试失败: " + ex.getMessage() + "\n\n时间格式应为: yyyy-MM-ddTHH:mm:ss");
        }
    }
    
    private void showExampleRules() {
        StringBuilder sb = new StringBuilder();
        sb.append("时间规则示例:\n\n");
        sb.append("1. 全年工作日上午9点到下午5点:\n");
        sb.append("   *.January-December.Monday-Friday.9:00-17:00\n\n");
        sb.append("2. 2025年夏季工作日上午8点到12点:\n");
        sb.append("   2025.July,August.Monday-Friday.8:00-12:00\n\n");
        sb.append("3. 2025年12月25日全天（圣诞节）:\n");
        sb.append("   2025.December.25.0:00-23:59\n\n");
        sb.append("4. 每周三下午2点到4点:\n");
        sb.append("   *.*.Wednesday.14:00-16:00\n\n");
        sb.append("5. 2025年1月和2月的周一至周五:\n");
        sb.append("   2025.January,February.Monday-Friday.*\n\n");
        sb.append("6. 工作日上午和下午两个时段:\n");
        sb.append("   *.*.Monday-Friday.8:00-12:00,13:00-17:00\n\n");
        sb.append("规则格式说明:\n");
        sb.append("   [年份].[月份].[日期/星期].[时间范围]\n");
        sb.append("   * 表示通配符\n");
        sb.append("   使用逗号分隔多个值\n");
        sb.append("   使用连字符表示范围\n");
        
        resultArea.setText(sb.toString());
    }
    
    private void refreshFilterTable() {
        if (timeFilterRepository == null) {
            return;
        }
        
        new Thread(() -> {
            try {
                List<TimeFilter> filters = timeFilterRepository.findAll();
                SwingUtilities.invokeLater(() -> {
                    filterTableModel.setRowCount(0);
                    
                    for (TimeFilter filter : filters) {
                        filterTableModel.addRow(new Object[]{
                            filter.getTimeFilterId(),
                            filter.getFilterName() != null ? filter.getFilterName() : "未命名",
                            filter.getRawRule() != null ? 
                                (filter.getRawRule().length() > 50 ? 
                                    filter.getRawRule().substring(0, 50) + "..." : 
                                    filter.getRawRule()) : "",
                            filter.getYear() != null ? filter.getYear() : "所有年份",
                            filter.getMonths() != null ? filter.getMonths() : "所有月份",
                            filter.getStartTime() != null ? filter.getStartTime() : "不限",
                            filter.getEndTime() != null ? filter.getEndTime() : "不限"
                        });
                    }
                    
                    resultArea.setText("时间过滤器列表已刷新，共 " + filters.size() + " 条规则");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    resultArea.setText("刷新列表失败: " + ex.getMessage());
                });
            }
        }).start();
    }
}