package acs.ui;

import javax.swing.*;
import java.awt.*;
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
    private static final String MODE_SWIPE = "刷卡";
    private static final String MODE_UPDATE = "码更新";
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

        JLabel titleLabel = new JLabel("刷卡模拟", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("徽章ID:"), gbc);

        gbc.gridx = 1;
        badgeIdField = new JTextField(20);
        inputPanel.add(badgeIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("资源ID:"), gbc);

        gbc.gridx = 1;
        resourceIdField = new JTextField(20);
        inputPanel.add(resourceIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("模式:"), gbc);

        gbc.gridx = 1;
        modeComboBox = new JComboBox<>(new String[]{MODE_SWIPE, MODE_UPDATE});
        inputPanel.add(modeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton scanButton = new JButton("执行");
        scanButton.addActionListener(e -> simulateScan());
        inputPanel.add(scanButton, gbc);

        add(inputPanel, BorderLayout.CENTER);

        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        add(new JScrollPane(resultArea), BorderLayout.SOUTH);
    }

    private void simulateScan() {
        String badgeId = badgeIdField.getText().trim();
        String resourceId = resourceIdField.getText().trim();
        String selectedMode = (String) modeComboBox.getSelectedItem();

        if (badgeId.isEmpty()) {
            resultArea.setText("错误: 请输入徽章ID");
            return;
        }

        if (MODE_SWIPE.equals(selectedMode) && resourceId.isEmpty()) {
            resultArea.setText("错误: 请输入资源ID");
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("操作结果:\n");
            if (clockService.isSimulated()) {
                sb.append("时间来源: 模拟时间\n");
            } else {
                sb.append("时间来源: 系统时间\n");
            }
            sb.append("徽章ID: ").append(badgeId).append("\n");
            sb.append("模式: ").append(selectedMode).append("\n");

            if (MODE_SWIPE.equals(selectedMode)) {
                AccessRequest request = new AccessRequest(badgeId, resourceId, clockService.localNow());
                AccessResult result = accessControlService.processAccess(request);

                String timeStr = java.time.LocalDateTime.ofInstant(request.getTimestamp(), ZONE_ID).format(TIME_FORMATTER);
                sb.append("日志: 时间: ").append(timeStr)
                    .append(" | 徽章: ").append(badgeId)
                    .append(" | 模式: ").append(selectedMode)
                    .append(" | 资源: ").append(resourceId)
                    .append(" | 决策: ").append(result.getDecision())
                    .append(" | 原因码: ").append(result.getReasonCode())
                    .append(" | 信息: ").append(result.getMessage()).append("\n");

                sb.append("资源ID: ").append(resourceId).append("\n");
                sb.append("决策: ").append(result.getDecision()).append("\n");
                sb.append("原因码: ").append(result.getReasonCode()).append("\n");
                sb.append("信息: ").append(result.getMessage()).append("\n");
                sb.append("时间: ").append(timeStr).append("\n");
            } else {
                sb.append("码更新流程:\n");

                boolean needsUpdate = badgeCodeUpdateService.checkBadgeNeedsUpdate(badgeId);
                if (!needsUpdate) {
                    sb.append("状态: 无需更新\n");
                    sb.append("说明: 当前码仍然有效\n");
                } else {
                    sb.append("状态: 需要更新，开始更新...\n");
                    Badge updatedBadge = badgeCodeUpdateService.updateBadgeCode(badgeId);
                    if (updatedBadge != null) {
                        sb.append("状态: 更新成功\n");
                        sb.append("新徽章码: ").append(updatedBadge.getBadgeCode()).append("\n");
                        sb.append("过期时间: ").append(updatedBadge.getExpirationDate()).append("\n");
                        sb.append("更新时间: ").append(updatedBadge.getLastCodeUpdate()).append("\n");
                        sb.append("说明: 徽章码已更新\n");
                    } else {
                        sb.append("状态: 更新失败\n");
                        sb.append("说明: 未能更新徽章码\n");
                    }
                }
            }

            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("执行失败: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}