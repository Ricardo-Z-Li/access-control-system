package acs.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import acs.service.AccessControlService;
import acs.service.ClockService;
import acs.domain.AccessRequest;
import acs.domain.AccessResult;
import acs.simulator.BadgeCodeUpdateService;
import acs.domain.Badge;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

public class ScanPanel extends JPanel {
    private final AccessControlService accessControlService;
    private final BadgeCodeUpdateService badgeCodeUpdateService;
    private final ClockService clockService;
    private JTextField badgeIdField;
    private JTextField resourceIdField;
    private JComboBox<String> modeComboBox;
    private JTextArea resultArea;
    private static final String MODE_SWIPE = "Swipe";
    private static final String MODE_UPDATE = "Update Code";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    public ScanPanel(AccessControlService accessControlService,
                     BadgeCodeUpdateService badgeCodeUpdateService,
                     ClockService clockService) {
        this.accessControlService = accessControlService;
        this.badgeCodeUpdateService = badgeCodeUpdateService;
        this.clockService = clockService;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        add(UiTheme.createHeader("Scan Console", "Simulate badge swipe or run update flow"), BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.add(UiTheme.formRow("Badge ID", badgeIdField = new JTextField()));
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(UiTheme.formRow("Resource ID", resourceIdField = new JTextField()));
        formPanel.add(Box.createVerticalStrut(8));
        modeComboBox = new JComboBox<>(new String[]{MODE_SWIPE, MODE_UPDATE});
        formPanel.add(UiTheme.formRow("Mode", modeComboBox));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setOpaque(false);
        JButton runButton = UiTheme.primaryButton("Run");
        runButton.addActionListener(e -> simulateScan());
        JButton clearButton = UiTheme.secondaryButton("Clear");
        clearButton.addActionListener(e -> resultArea.setText(""));
        JButton sampleButton = UiTheme.secondaryButton("Fill Sample");
        sampleButton.addActionListener(e -> {
            badgeIdField.setText("BADGE001");
            resourceIdField.setText("RES001");
            modeComboBox.setSelectedItem(MODE_SWIPE);
        });
        actionRow.add(runButton);
        actionRow.add(clearButton);
        actionRow.add(sampleButton);

        JPanel leftCard = UiTheme.cardPanel();
        JPanel leftContent = new JPanel();
        leftContent.setOpaque(false);
        leftContent.setLayout(new BoxLayout(leftContent, BoxLayout.Y_AXIS));
        leftContent.add(formPanel);
        leftContent.add(Box.createVerticalStrut(12));
        leftContent.add(actionRow);
        leftCard.add(leftContent, BorderLayout.NORTH);

        resultArea = new JTextArea(16, 40);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        resultArea.setBackground(UiTheme.surface());
        JScrollPane resultScroll = new JScrollPane(resultArea);

        JButton copyButton = UiTheme.secondaryButton("Copy Result");
        copyButton.addActionListener(e -> copyResult());
        JPanel resultActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        resultActions.setOpaque(false);
        resultActions.add(copyButton);

        JPanel rightCard = UiTheme.cardPanel();
        rightCard.add(resultActions, BorderLayout.NORTH);
        rightCard.add(resultScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            UiTheme.wrapContent(leftCard),
            UiTheme.wrapContent(rightCard));
        splitPane.setDividerLocation(360);
        splitPane.setResizeWeight(0.38);
        splitPane.setDividerSize(1);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setContinuousLayout(true);

        add(splitPane, BorderLayout.CENTER);
    }

    private void simulateScan() {
        String badgeId = badgeIdField.getText().trim();
        String resourceId = resourceIdField.getText().trim();
        String selectedMode = (String) modeComboBox.getSelectedItem();

        if (badgeId.isEmpty()) {
            resultArea.setText("Error: enter badge ID");
            return;
        }

        if (MODE_SWIPE.equals(selectedMode) && resourceId.isEmpty()) {
            resultArea.setText("Error: swipe mode requires resource ID");
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Result\n");
            sb.append("Time Source: ").append(clockService.isSimulated() ? "Simulated Time" : "System Time").append("\n");
            sb.append("Badge ID: ").append(badgeId).append("\n");
            sb.append("Mode: ").append(selectedMode).append("\n");

            if (MODE_SWIPE.equals(selectedMode)) {
                AccessRequest request = new AccessRequest(badgeId, resourceId, clockService.localNow());
                AccessResult result = accessControlService.processAccess(request);

                String timeStr = java.time.LocalDateTime.ofInstant(request.getTimestamp(), ZONE_ID).format(TIME_FORMATTER);
                sb.append("Decision: ").append(result.getDecision()).append("\n");
                sb.append("Reason: ").append(result.getReasonCode()).append("\n");
                sb.append("Message: ").append(result.getMessage()).append("\n");
                sb.append("Resource ID: ").append(resourceId).append("\n");
                sb.append("Time: ").append(timeStr).append("\n");
            } else {
                sb.append("Badge Update Flow\n");

                boolean needsUpdate = badgeCodeUpdateService.checkBadgeNeedsUpdate(badgeId);
                if (!needsUpdate) {
                    sb.append("Status: No update needed\n");
                    sb.append("Note: Current badge code is valid\n");
                } else {
                    sb.append("Status: Update required\n");
                    Badge updatedBadge = badgeCodeUpdateService.updateBadgeCode(badgeId);
                    if (updatedBadge != null) {
                        sb.append("Status: Updated\n");
                        sb.append("New Badge Code: ").append(updatedBadge.getBadgeCode()).append("\n");
                        sb.append("Expires At: ").append(updatedBadge.getExpirationDate()).append("\n");
                        sb.append("Updated At: ").append(updatedBadge.getLastCodeUpdate()).append("\n");
                    } else {
                        sb.append("Status: Update failed\n");
                    }
                }
            }

            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("Execution failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void copyResult() {
        String text = resultArea.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }
}
