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
        add(UiTheme.createHeader("Time Rules", "Parse, validate, and test time rules"), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Rule Management", createRuleManagementPanel());
        tabbedPane.addTab("Rule Test", createRuleTestPanel());
        add(tabbedPane, BorderLayout.CENTER);

        JLabel statusLabel = new JLabel("Service Status: " + (timeFilterService != null ? "OK" : "Unavailable"));
        add(UiTheme.footerBar(statusLabel), BorderLayout.SOUTH);
    }

    private JPanel createRuleManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Rule ID", "Rule Name", "Rule Summary", "Year", "Month", "Start Time", "End Time"};
        filterTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        filterTable = new JTable(filterTableModel);
        filterTable.setAutoCreateRowSorter(true);
        UiTheme.styleTable(filterTable);

        JScrollPane scrollPane = new JScrollPane(filterTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        ruleField = new JTextField(40);
        ruleField.setText("2025.July,August.Monday-Friday.8:00-12:00");
        controlPanel.add(UiTheme.formRow("Rule Expression", ruleField));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton parseButton = UiTheme.primaryButton("Parse Rule");
        parseButton.addActionListener(e -> parseTimeRule());
        JButton validateButton = UiTheme.secondaryButton("Validate Rule");
        validateButton.addActionListener(e -> validateTimeRule());
        JButton refreshButton = UiTheme.secondaryButton("Refresh List");
        refreshButton.addActionListener(e -> refreshFilterTable());
        JButton clearButton = UiTheme.secondaryButton("Clear");
        clearButton.addActionListener(e -> ruleField.setText(""));

        buttonPanel.add(parseButton);
        buttonPanel.add(validateButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(buttonPanel);

        panel.add(UiTheme.wrapContent(controlPanel), BorderLayout.NORTH);

        return panel;
    }

    private JPanel createRuleTestPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        JTextField testRuleField = new JTextField(30);
        testRuleField.setText("2025.July,August.Monday-Friday.8:00-12:00");
        testTimeField = new JTextField(25);
        testTimeField.setText(LocalDateTime.now().toString());

        inputPanel.add(UiTheme.formRow("Test Rule", testRuleField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Test Time", testTimeField));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton testMatchButton = UiTheme.primaryButton("Test Match");
        testMatchButton.addActionListener(e -> testTimeMatch(testRuleField.getText()));
        JButton nowButton = UiTheme.secondaryButton("Now");
        nowButton.addActionListener(e -> testTimeField.setText(LocalDateTime.now().toString()));
        JButton exampleButton = UiTheme.secondaryButton("Example Rules");
        exampleButton.addActionListener(e -> showExampleRules());
        buttonPanel.add(testMatchButton);
        buttonPanel.add(nowButton);
        buttonPanel.add(exampleButton);

        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(buttonPanel);

        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resultArea.setBackground(UiTheme.surface());

        JPanel card = UiTheme.cardPanel();
        card.add(inputPanel, BorderLayout.NORTH);
        card.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        panel.add(UiTheme.wrapContent(card), BorderLayout.CENTER);

        return panel;
    }

    private void parseTimeRule() {
        String rawRule = ruleField.getText().trim();

        if (rawRule.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter rule expression");
            return;
        }

        try {
            TimeFilter timeFilter = timeFilterService.parseTimeRule(rawRule);

            StringBuilder sb = new StringBuilder();
            sb.append("Parse Result\n");
            sb.append("Raw Rule: ").append(rawRule).append("\n");
            sb.append("Rule ID: ").append(timeFilter.getTimeFilterId()).append("\n");
            sb.append("Rule Name: ").append(timeFilter.getFilterName() != null ? timeFilter.getFilterName() : "Unnamed").append("\n");
            sb.append("Year: ").append(timeFilter.getYear() != null ? timeFilter.getYear() : "Unspecified").append("\n");
            sb.append("Month: ").append(timeFilter.getMonths() != null ? timeFilter.getMonths() : "Unspecified").append("\n");
            sb.append("Day: ").append(timeFilter.getDaysOfMonth() != null ? timeFilter.getDaysOfMonth() : "Unspecified").append("\n");
            sb.append("Weekday: ").append(timeFilter.getDaysOfWeek() != null ? timeFilter.getDaysOfWeek() : "Unspecified").append("\n");
            sb.append("Start Time: ").append(timeFilter.getStartTime() != null ? timeFilter.getStartTime() : "Unspecified").append("\n");
            sb.append("End Time: ").append(timeFilter.getEndTime() != null ? timeFilter.getEndTime() : "Unspecified").append("\n");
            sb.append("Recurring: ").append(timeFilter.getIsRecurring() != null && timeFilter.getIsRecurring() ? "Yes" : "No").append("\n");

            resultArea.setText(sb.toString());
            refreshFilterTable();
        } catch (Exception ex) {
            resultArea.setText("Parse failed: " + ex.getMessage());
        }
    }

    private void validateTimeRule() {
        String rawRule = ruleField.getText().trim();

        if (rawRule.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter rule expression");
            return;
        }

        try {
            boolean valid = timeFilterService.validateTimeRule(rawRule);

            if (valid) {
                resultArea.setText("Validation passed: " + rawRule + "\n\nRule format is valid");
            } else {
                resultArea.setText("Validation failed: " + rawRule + "\n\nRule format may be invalid");
            }
        } catch (Exception ex) {
            resultArea.setText("Validation failed: " + ex.getMessage());
        }
    }

    private void testTimeMatch(String rawRule) {
        String timeStr = testTimeField.getText().trim();

        if (rawRule.isEmpty() || timeStr.isEmpty()) {
            resultArea.setText("Error: enter rule and test time");
            return;
        }

        try {
            LocalDateTime testTime = LocalDateTime.parse(timeStr);
            TimeFilter timeFilter = timeFilterService.parseTimeRule(rawRule);
            boolean matches = timeFilterService.matches(timeFilter, testTime);

            StringBuilder sb = new StringBuilder();
            sb.append("Match Result\n");
            sb.append("Rule: ").append(rawRule).append("\n");
            sb.append("Test Time: ").append(testTime).append("\n");
            sb.append("Matches: ").append(matches ? "Match" : "No Match").append("\n\n");

            sb.append("Parsed Rule:\n");
            sb.append("Rule ID: ").append(timeFilter.getTimeFilterId()).append("\n");
            sb.append("Rule Name: ").append(timeFilter.getFilterName() != null ? timeFilter.getFilterName() : "Unnamed").append("\n");
            sb.append("Year: ").append(timeFilter.getYear() != null ? timeFilter.getYear() : "Unspecified").append("\n");
            sb.append("Month: ").append(timeFilter.getMonths() != null ? timeFilter.getMonths() : "Unspecified").append("\n");
            sb.append("Day: ").append(timeFilter.getDaysOfMonth() != null ? timeFilter.getDaysOfMonth() : "Unspecified").append("\n");
            sb.append("Weekday: ").append(timeFilter.getDaysOfWeek() != null ? timeFilter.getDaysOfWeek() : "Unspecified").append("\n");
            sb.append("Start Time: ").append(timeFilter.getStartTime() != null ? timeFilter.getStartTime() : "Unspecified").append("\n");
            sb.append("End Time: ").append(timeFilter.getEndTime() != null ? timeFilter.getEndTime() : "Unspecified").append("\n");
            sb.append("Recurring: ").append(timeFilter.getIsRecurring() != null && timeFilter.getIsRecurring() ? "Yes" : "No").append("\n");

            sb.append("\nTime Breakdown:\n");
            sb.append("Year: ").append(testTime.getYear()).append("\n");
            sb.append("Month: ").append(testTime.getMonthValue()).append(" (").append(testTime.getMonth()).append(")\n");
            sb.append("Day: ").append(testTime.getDayOfMonth()).append("\n");
            sb.append("Weekday: ").append(testTime.getDayOfWeek()).append("\n");
            sb.append("Time: ").append(String.format("%02d:%02d", testTime.getHour(), testTime.getMinute())).append("\n");

            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("Test failed: " + ex.getMessage() + "\n\nTime format: yyyy-MM-ddTHH:mm:ss");
        }
    }

    private void showExampleRules() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rule Examples:\n\n");
        sb.append("1. Weekdays all year 9:00-17:00\n");
        sb.append("   *.January-December.Monday-Friday.9:00-17:00\n\n");
        sb.append("2. Summer weekdays in 2025 8:00-12:00\n");
        sb.append("   2025.July,August.Monday-Friday.8:00-12:00\n\n");
        sb.append("3. Full day on Dec 25, 2025\n");
        sb.append("   2025.December.25.0:00-23:59\n\n");
        sb.append("4. Every Wednesday 14:00-16:00\n");
        sb.append("   *.*.Wednesday.14:00-16:00\n\n");
        sb.append("5. Weekdays in Jan-Feb 2025 (all day)\n");
        sb.append("   2025.January,February.Monday-Friday.*\n\n");
        sb.append("6. Split time on weekdays\n");
        sb.append("   *.*.Monday-Friday.8:00-12:00,13:00-17:00\n\n");
        sb.append("Rule Format:\n");
        sb.append("   [Year].[Month].[Day/Weekday].[Time Range]\n");
        sb.append("   * means any\n");
        sb.append("   Month/Day/Weekday can be comma-separated\n");
        sb.append("   Time ranges can be comma-separated\n");

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
                            filter.getFilterName() != null ? filter.getFilterName() : "Unnamed",
                            filter.getRawRule() != null ?
                                (filter.getRawRule().length() > 50 ?
                                    filter.getRawRule().substring(0, 50) + "..." :
                                    filter.getRawRule()) : "",
                            filter.getYear() != null ? filter.getYear() : "Unspecified",
                            filter.getMonths() != null ? filter.getMonths() : "Unspecified",
                            filter.getStartTime() != null ? filter.getStartTime() : "Unspecified",
                            filter.getEndTime() != null ? filter.getEndTime() : "Unspecified"
                        });
                    }

                    resultArea.setText("Loaded time rules: " + filters.size());
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> resultArea.setText("Refresh failed: " + ex.getMessage()));
            }
        }).start();
    }
}
