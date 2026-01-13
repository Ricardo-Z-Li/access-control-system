package acs.ui;

import javax.swing.*;
import java.awt.*;
import acs.service.EmergencyControlService;
import acs.domain.ResourceType;

public class EmergencyControlPanel extends JPanel {
    private final EmergencyControlService emergencyControlService;
    private JTextArea logArea;

    public EmergencyControlPanel(EmergencyControlService emergencyControlService) {
        this.emergencyControlService = emergencyControlService;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("应急控制 - 紧急门禁管理", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(Color.RED);
        add(titleLabel, BorderLayout.NORTH);

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.CENTER);

        logArea = new JTextArea(10, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("操作日志"));
        add(scrollPane, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JButton emergencyButton = new JButton("一键解除所有门禁控制");
        emergencyButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        emergencyButton.setBackground(Color.RED);
        emergencyButton.setForeground(Color.WHITE);
        emergencyButton.setToolTipText("立即将所有门禁设为不受控");
        emergencyButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "该操作将解除所有门禁控制状态。\n所有门禁将处于常开状态。\n是否继续？",
                "确认应急操作",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    emergencyControlService.setAllDoorsUncontrolled();
                    log("已解除所有门禁控制");
                    JOptionPane.showMessageDialog(this, "操作成功，所有门禁已解除控制。", "提示", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    log("应急操作失败: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "操作失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        panel.add(emergencyButton, gbc);

        gbc.gridy++;
        panel.add(new JSeparator(), gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("资源类型:"), gbc);

        gbc.gridx = 1;
        JComboBox<ResourceType> typeCombo = new JComboBox<>(ResourceType.values());
        typeCombo.removeItem(ResourceType.PENDING);
        panel.add(typeCombo, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        JButton setUncontrolledButton = new JButton("设为不受控");
        setUncontrolledButton.addActionListener(e -> {
            ResourceType selectedType = (ResourceType) typeCombo.getSelectedItem();
            try {
                emergencyControlService.setResourcesControlledByType(selectedType, false);
                log("已将类型 " + selectedType + " 设为不受控");
            } catch (Exception ex) {
                log("操作失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "操作失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(setUncontrolledButton, gbc);

        gbc.gridx = 1;
        JButton setControlledButton = new JButton("设为受控");
        setControlledButton.addActionListener(e -> {
            ResourceType selectedType = (ResourceType) typeCombo.getSelectedItem();
            try {
                emergencyControlService.setResourcesControlledByType(selectedType, true);
                log("已将类型 " + selectedType + " 设为受控");
            } catch (Exception ex) {
                log("操作失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "操作失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(setControlledButton, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("群组ID:"), gbc);

        gbc.gridx = 1;
        JTextField groupIdField = new JTextField(15);
        panel.add(groupIdField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        JButton setGroupUncontrolledButton = new JButton("群组设为不受控");
        setGroupUncontrolledButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            if (groupId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入群组ID", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                emergencyControlService.setGroupResourcesControlled(groupId, false);
                log("已将群组 " + groupId + " 设为不受控");
            } catch (Exception ex) {
                log("操作失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "操作失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(setGroupUncontrolledButton, gbc);

        gbc.gridx = 1;
        JButton setGroupControlledButton = new JButton("群组设为受控");
        setGroupControlledButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            if (groupId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入群组ID", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                emergencyControlService.setGroupResourcesControlled(groupId, true);
                log("已将群组 " + groupId + " 设为受控");
            } catch (Exception ex) {
                log("操作失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "操作失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(setGroupControlledButton, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton restoreButton = new JButton("恢复所有门禁为受控");
        restoreButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        restoreButton.setBackground(Color.GREEN);
        restoreButton.setForeground(Color.BLACK);
        restoreButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "该操作将恢复所有门禁为受控。\n是否继续？",
                "确认",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    emergencyControlService.restoreAllToControlled();
                    log("已恢复所有门禁受控");
                    JOptionPane.showMessageDialog(this, "恢复成功。", "提示", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    log("操作失败: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "操作失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        panel.add(restoreButton, gbc);

        return panel;
    }

    private void log(String message) {
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
