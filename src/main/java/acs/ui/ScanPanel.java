package acs.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import acs.service.AccessControlService;
import acs.service.ClockService;
import acs.domain.AccessRequest;
import acs.domain.AccessResult;
import acs.simulator.BadgeCodeUpdateService;
import acs.domain.Badge;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

public class ScanPanel extends JPanel {
    private AccessControlService accessControlService;
    private BadgeCodeUpdateService badgeCodeUpdateService;
    private ClockService clockService;
    private JTextField badgeIdField;
    private JTextField resourceIdField;
    private JComboBox<String> modeComboBox;
    private JTextArea resultArea;
    private static final String MODE_SWIPE = "刷卡模式";
    private static final String MODE_HOLD = "更新模式";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    
    public ScanPanel(AccessControlService accessControlService, BadgeCodeUpdateService badgeCodeUpdateService, ClockService clockService) {
        this.accessControlService = accessControlService;
        this.badgeCodeUpdateService = badgeCodeUpdateService;
        this.clockService = clockService;
        initUI();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("模拟扫描访问", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
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
        modeComboBox = new JComboBox<>(new String[]{MODE_SWIPE, MODE_HOLD});
        inputPanel.add(modeComboBox, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton scanButton = new JButton("模拟扫描");
        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulateScan();
            }
        });
        inputPanel.add(scanButton, gbc);
        
        add(inputPanel, BorderLayout.CENTER);
        
        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("宋体", Font.PLAIN, 12));
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
        
        if (selectedMode.equals(MODE_SWIPE) && resourceId.isEmpty()) {
            resultArea.setText("错误: 刷卡模式需要资源ID");
            return;
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("扫描结果:\n");
             if (clockService.isSimulated()) {
                 sb.append("警告: 当前使用模拟时间，日志时间可能不正确。\n");
             } else {
                 sb.append("提示: 当前使用真实时间，如需模拟时间请在模拟器面板设置。\n");
             }
             sb.append("徽章ID: ").append(badgeId).append("\n");
             sb.append("模式: ").append(selectedMode).append("\n");
             
             if (selectedMode.equals(MODE_SWIPE)) {
                 // 刷卡模式：正常访问控制
                 AccessRequest request = new AccessRequest(badgeId, resourceId, clockService.localNow());
                 AccessResult result = accessControlService.processAccess(request);
                 
                 // 摘要行（与日志格式一致）
                  String timeStr = java.time.LocalDateTime.ofInstant(request.getTimestamp(), ZONE_ID).format(TIME_FORMATTER);
                 sb.append("摘要: 时间: ").append(timeStr)
                     .append(" | 徽章: ").append(badgeId)
                     .append(" | 模式: ").append(selectedMode)
                     .append(" | 资源: ").append(resourceId)
                     .append(" | 决策: ").append(result.getDecision())
                     .append(" | 原因: ").append(result.getReasonCode())
                     .append(" | 消息: ").append(result.getMessage()).append("\n");
                 
                 sb.append("资源ID: ").append(resourceId).append("\n");
                 sb.append("决策: ").append(result.getDecision()).append("\n");
                 sb.append("原因代码: ").append(result.getReasonCode()).append("\n");
                 sb.append("消息: ").append(result.getMessage()).append("\n");
                  sb.append("时间: ").append(timeStr).append("\n");
            } else {
                // 更新模式：徽章代码更新
                sb.append("操作: 徽章代码更新\n");
                
                // 检查徽章是否需要更新
                boolean needsUpdate = badgeCodeUpdateService.checkBadgeNeedsUpdate(badgeId);
                if (!needsUpdate) {
                    sb.append("状态: 徽章不需要更新\n");
                    sb.append("提示: 徽章代码仍有效，无需更新。\n");
                } else {
                    sb.append("状态: 徽章需要更新，正在执行更新流程...\n");
                    Badge updatedBadge = badgeCodeUpdateService.updateBadgeCode(badgeId);
                    if (updatedBadge != null) {
                        sb.append("结果: 徽章更新成功\n");
                        sb.append("新徽章代码: ").append(updatedBadge.getBadgeCode()).append("\n");
                        sb.append("过期日期: ").append(updatedBadge.getExpirationDate()).append("\n");
                        sb.append("最后更新: ").append(updatedBadge.getLastCodeUpdate()).append("\n");
                        sb.append("提示: 徽章已更新，请重新刷卡使用。\n");
                    } else {
                        sb.append("结果: 徽章更新失败\n");
                        sb.append("原因: 可能徽章已禁用或更新窗口已过。\n");
                    }
                }
            }
            
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("错误: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
