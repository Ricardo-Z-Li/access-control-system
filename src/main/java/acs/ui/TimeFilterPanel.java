package acs.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import acs.service.TimeFilterService;
import acs.domain.TimeFilter;
import acs.repository.TimeFilterRepository;

public class TimeFilterPanel extends JPanel {
    private final TimeFilterService timeFilterService;
    private final TimeFilterRepository timeFilterRepository;

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

        JLabel titleLabel = new JLabel("时间过滤规则", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("规则管理", createRuleManagementPanel());
        tabbedPane.addTab("规则测试", createRuleTestPanel());
        add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("服务状态: " + (timeFilterService != null ? "可用" : "不可用")));
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createRuleManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"过滤器ID", "名称", "规则摘要", "年份", "月份", "开始时间", "结束时间"};
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
        controlPanel.add(new JLabel("规则文本:"), gbc);

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
        parseButton.addActionListener(e -> parseTimeRule());
        buttonPanel.add(parseButton);

        JButton validateButton = new JButton("校验规则");
        validateButton.addActionListener(e -> validateTimeRule());
        buttonPanel.add(validateButton);

        JButton refreshButton = new JButton("刷新列表");
        refreshButton.addActionListener(e -> refreshFilterTable());
        buttonPanel.add(refreshButton);

        JButton clearButton = new JButton("清空");
        clearButton.addActionListener(e -> ruleField.setText(""));
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
        inputPanel.add(new JLabel("测试规则:"), gbc);

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
        testMatchButton.addActionListener(e -> testTimeMatch(testRuleField.getText()));
        buttonPanel.add(testMatchButton);

        JButton nowButton = new JButton("当前时间");
        nowButton.addActionListener(e -> testTimeField.setText(LocalDateTime.now().toString()));
        buttonPanel.add(nowButton);

        JButton exampleButton = new JButton("示例规则");
        exampleButton.addActionListener(e -> showExampleRules());
        buttonPanel.add(exampleButton);

        inputPanel.add(buttonPanel, gbc);
        panel.add(inputPanel, BorderLayout.NORTH);

        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        return panel;
    }

    private void parseTimeRule() {
        String rawRule = ruleField.getText().trim();

        if (rawRule.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入规则");
            return;
        }

        try {
            TimeFilter timeFilter = timeFilterService.parseTimeRule(rawRule);

            StringBuilder sb = new StringBuilder();
            sb.append("解析结果:\n");
            sb.append("原始规则: ").append(rawRule).append("\n");
            sb.append("过滤器ID: ").append(timeFilter.getTimeFilterId()).append("\n");
            sb.append("名称: ").append(timeFilter.getFilterName() != null ? timeFilter.getFilterName() : "未命名").append("\n");
            sb.append("年份: ").append(timeFilter.getYear() != null ? timeFilter.getYear() : "未设置").append("\n");
            sb.append("月份: ").append(timeFilter.getMonths() != null ? timeFilter.getMonths() : "未设置").append("\n");
            sb.append("日期: ").append(timeFilter.getDaysOfMonth() != null ? timeFilter.getDaysOfMonth() : "未设置").append("\n");
            sb.append("星期: ").append(timeFilter.getDaysOfWeek() != null ? timeFilter.getDaysOfWeek() : "未设置").append("\n");
            sb.append("开始时间: ").append(timeFilter.getStartTime() != null ? timeFilter.getStartTime() : "未设置").append("\n");
            sb.append("结束时间: ").append(timeFilter.getEndTime() != null ? timeFilter.getEndTime() : "未设置").append("\n");
            sb.append("是否重复: ").append(timeFilter.getIsRecurring() != null && timeFilter.getIsRecurring() ? "是" : "否").append("\n");

            resultArea.setText(sb.toString());
            refreshFilterTable();
        } catch (Exception ex) {
            resultArea.setText("解析失败: " + ex.getMessage());
        }
    }

    private void validateTimeRule() {
        String rawRule = ruleField.getText().trim();

        if (rawRule.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入规则");
            return;
        }

        try {
            boolean valid = timeFilterService.validateTimeRule(rawRule);

            if (valid) {
                resultArea.setText("校验通过: " + rawRule + "\n\n规则格式正确。");
            } else {
                resultArea.setText("校验失败: " + rawRule + "\n\n规则格式不符合要求。");
            }
        } catch (Exception ex) {
            resultArea.setText("校验失败: " + ex.getMessage());
        }
    }

    private void testTimeMatch(String rawRule) {
        String timeStr = testTimeField.getText().trim();

        if (rawRule.isEmpty() || timeStr.isEmpty()) {
            resultArea.setText("错误: 请输入规则和测试时间");
            return;
        }

        try {
            LocalDateTime testTime = LocalDateTime.parse(timeStr);
            TimeFilter timeFilter = timeFilterService.parseTimeRule(rawRule);
            boolean matches = timeFilterService.matches(timeFilter, testTime);

            StringBuilder sb = new StringBuilder();
            sb.append("测试结果:\n");
            sb.append("规则: ").append(rawRule).append("\n");
            sb.append("测试时间: ").append(testTime).append("\n");
            sb.append("匹配结果: ").append(matches ? "匹配" : "不匹配").append("\n\n");

            sb.append("过滤器详情:\n");
            sb.append("过滤器ID: ").append(timeFilter.getTimeFilterId()).append("\n");
            sb.append("名称: ").append(timeFilter.getFilterName() != null ? timeFilter.getFilterName() : "未命名").append("\n");
            sb.append("年份: ").append(timeFilter.getYear() != null ? timeFilter.getYear() : "未设置").append("\n");
            sb.append("月份: ").append(timeFilter.getMonths() != null ? timeFilter.getMonths() : "未设置").append("\n");
            sb.append("日期: ").append(timeFilter.getDaysOfMonth() != null ? timeFilter.getDaysOfMonth() : "未设置").append("\n");
            sb.append("星期: ").append(timeFilter.getDaysOfWeek() != null ? timeFilter.getDaysOfWeek() : "未设置").append("\n");
            sb.append("开始时间: ").append(timeFilter.getStartTime() != null ? timeFilter.getStartTime() : "未设置").append("\n");
            sb.append("结束时间: ").append(timeFilter.getEndTime() != null ? timeFilter.getEndTime() : "未设置").append("\n");
            sb.append("是否重复: ").append(timeFilter.getIsRecurring() != null && timeFilter.getIsRecurring() ? "是" : "否").append("\n");

            sb.append("\n时间拆解:\n");
            sb.append("年: ").append(testTime.getYear()).append("\n");
            sb.append("月: ").append(testTime.getMonthValue()).append(" (").append(testTime.getMonth()).append(")\n");
            sb.append("日: ").append(testTime.getDayOfMonth()).append("\n");
            sb.append("星期: ").append(testTime.getDayOfWeek()).append("\n");
            sb.append("时间: ").append(String.format("%02d:%02d", testTime.getHour(), testTime.getMinute())).append("\n");

            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("错误: " + ex.getMessage() + "\n\n时间格式: yyyy-MM-ddTHH:mm:ss");
        }
    }

    private void showExampleRules() {
        StringBuilder sb = new StringBuilder();
        sb.append("规则示例:\n\n");
        sb.append("1. 全年工作日 9:00-17:00\n");
        sb.append("   *.January-December.Monday-Friday.9:00-17:00\n\n");
        sb.append("2. 2025年暑期工作日 8:00-12:00\n");
        sb.append("   2025.July,August.Monday-Friday.8:00-12:00\n\n");
        sb.append("3. 2025年12月25日全天\n");
        sb.append("   2025.December.25.0:00-23:59\n\n");
        sb.append("4. 每周三 14:00-16:00\n");
        sb.append("   *.*.Wednesday.14:00-16:00\n\n");
        sb.append("5. 2025年1-2月工作日全天\n");
        sb.append("   2025.January,February.Monday-Friday.*\n\n");
        sb.append("6. 工作日上下午时段\n");
        sb.append("   *.*.Monday-Friday.8:00-12:00,13:00-17:00\n\n");
        sb.append("规则格式:\n");
        sb.append("   [年].[月].[日/周].[时间段]\n");
        sb.append("   * 表示任意\n");
        sb.append("   月份/星期可用逗号分隔\n");
        sb.append("   时间段可用逗号分隔\n");

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
                            filter.getYear() != null ? filter.getYear() : "未设置",
                            filter.getMonths() != null ? filter.getMonths() : "未设置",
                            filter.getStartTime() != null ? filter.getStartTime() : "未设置",
                            filter.getEndTime() != null ? filter.getEndTime() : "未设置"
                        });
                    }

                    resultArea.setText("已加载过滤规则 " + filters.size() + " 条");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> resultArea.setText("刷新失败: " + ex.getMessage()));
            }
        }).start();
    }
}
