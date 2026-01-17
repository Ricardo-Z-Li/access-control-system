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
import acs.domain.BadgeStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

public class ScanPanel extends JPanel {
    private final AccessControlService accessControlService;
    private final BadgeCodeUpdateService badgeCodeUpdateService;
    private final ClockService clockService;
    private JTextField badgeIdField;
    private JTextArea resultArea;
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
        add(UiTheme.createHeader("Badge Update Console", "Update badge codes and check status"), BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.add(UiTheme.formRow("Badge ID", badgeIdField = new JTextField()));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setOpaque(false);
        JButton runButton = UiTheme.primaryButton("Run");
        runButton.addActionListener(e -> simulateScan());
        JButton clearButton = UiTheme.secondaryButton("Clear");
        clearButton.addActionListener(e -> resultArea.setText(""));
        JButton sampleButton = UiTheme.secondaryButton("Fill Sample");
        sampleButton.addActionListener(e -> {
            badgeIdField.setText("BADGE001");
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

        if (badgeId.isEmpty()) {
            resultArea.setText("Error: enter badge ID");
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Badge Update Service\n");
            sb.append("Time Source: ").append(clockService.isSimulated() ? "Simulated Time" : "System Time").append("\n");
            sb.append("Badge ID: ").append(badgeId).append("\n\n");

            Badge badge = badgeCodeUpdateService.getBadge(badgeId);
            if (badge == null) {
                sb.append("invalid badge");
                resultArea.setText(sb.toString());
                return;
            }

            String badgeCode = badge.getBadgeCode();
            BadgeStatus status = badge.getStatus();
             LocalDate codeExpirationDate = badge.getCodeExpirationDate();
            LocalDate today = clockService.localNow().toLocalDate();

            sb.append("Badge Code: ").append(badgeCode != null ? badgeCode : "N/A").append("\n");

            if (status == BadgeStatus.LOST || status == BadgeStatus.DISABLED) {
                sb.append("Status: ").append(status).append("\n");
                sb.append("Note: Badge is ").append(status.toString().toLowerCase()).append("\n");
                resultArea.setText(sb.toString());
                return;
            }

             boolean isExpired = codeExpirationDate != null && today.isAfter(codeExpirationDate);
            if (!isExpired) {
                sb.append("Status: No update needed\n");
                sb.append("Note: Current badge code is valid\n");
            } else {
                sb.append("Status: Expired\n");
                sb.append("Note: Badge expired, updating...\n\n");
                Badge updatedBadge = badgeCodeUpdateService.updateBadgeCode(badgeId);
                if (updatedBadge != null) {
                    sb.append("Badge updated successfully\n");
                    sb.append("New Badge Code: ").append(updatedBadge.getBadgeCode()).append("\n");
                     sb.append("New Code Expiration Date: ").append(updatedBadge.getCodeExpirationDate()).append("\n");
                    sb.append("Updated At: ").append(updatedBadge.getLastCodeUpdate()).append("\n");
                } else {
                    sb.append("Status: Update failed\n");
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
