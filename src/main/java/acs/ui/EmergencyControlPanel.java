package acs.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import acs.service.EmergencyControlService;
import acs.domain.ResourceType;

public class EmergencyControlPanel extends JPanel {
    private EmergencyControlService emergencyControlService;
    private JTextArea logArea;
    
    public EmergencyControlPanel(EmergencyControlService emergencyControlService) {
        this.emergencyControlService = emergencyControlService;
        initUI();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("紧急控制面板 - 火灾疏散与紧急状态管理", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
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
        
        // 紧急疏散按钮（红色高亮）
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JButton emergencyButton = new JButton("🚨 紧急疏散：一键将所有门设置为非受控");
        emergencyButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        emergencyButton.setBackground(Color.RED);
        emergencyButton.setForeground(Color.WHITE);
        emergencyButton.setToolTipText("火灾疏散时使用，将自动打开所有门");
        emergencyButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "警告：此操作将禁用所有门的访问控制，允许自由通行。\n仅在火灾等紧急情况下使用。\n确认执行紧急疏散？", 
                "紧急疏散确认", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    emergencyControlService.setAllDoorsUncontrolled();
                    log("紧急疏散已执行：所有门已设置为非受控状态");
                    JOptionPane.showMessageDialog(this, "紧急疏散执行成功，所有门已开放", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    log("紧急疏散失败: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "执行失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        panel.add(emergencyButton, gbc);
        
        // 分隔线
        gbc.gridy++;
        panel.add(new JSeparator(), gbc);
        
        // 按资源类型控制
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("按资源类型控制:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<ResourceType> typeCombo = new JComboBox<>(ResourceType.values());
        typeCombo.removeItem(ResourceType.PENDING); // 移除PENDING类型
        panel.add(typeCombo, gbc);
        
        gbc.gridy++;
        gbc.gridx = 0;
        JButton setUncontrolledButton = new JButton("设为非受控");
        setUncontrolledButton.addActionListener(e -> {
            ResourceType selectedType = (ResourceType) typeCombo.getSelectedItem();
            try {
                emergencyControlService.setResourcesControlledByType(selectedType, false);
                log("已将类型为 " + selectedType + " 的所有资源设置为非受控");
            } catch (Exception ex) {
                log("设置失败: " + ex.getMessage());
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
                log("已将类型为 " + selectedType + " 的所有资源设置为受控");
            } catch (Exception ex) {
                log("设置失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "操作失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(setControlledButton, gbc);
        
        // 分隔线
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);
        
        // 按组控制
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("按组控制:"), gbc);
        
        gbc.gridx = 1;
        JTextField groupIdField = new JTextField(15);
        panel.add(groupIdField, gbc);
        
        gbc.gridy++;
        gbc.gridx = 0;
        JButton setGroupUncontrolledButton = new JButton("组资源设为非受控");
        setGroupUncontrolledButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            if (groupId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入组ID", "警告", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                emergencyControlService.setGroupResourcesControlled(groupId, false);
                log("已将组 " + groupId + " 的所有资源设置为非受控");
            } catch (Exception ex) {
                log("设置组资源失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "操作失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(setGroupUncontrolledButton, gbc);
        
        gbc.gridx = 1;
        JButton setGroupControlledButton = new JButton("组资源设为受控");
        setGroupControlledButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            if (groupId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入组ID", "警告", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                emergencyControlService.setGroupResourcesControlled(groupId, true);
                log("已将组 " + groupId + " 的所有资源设置为受控");
            } catch (Exception ex) {
                log("设置组资源失败: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "操作失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(setGroupControlledButton, gbc);
        
        // 分隔线
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);
        
        // 恢复按钮
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton restoreButton = new JButton("🔧 恢复正常操作：将所有资源恢复为受控状态");
        restoreButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        restoreButton.setBackground(Color.GREEN);
        restoreButton.setForeground(Color.BLACK);
        restoreButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "确认将所有资源恢复为受控状态？\n此操作将重新启用访问控制。", 
                "恢复确认", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    emergencyControlService.restoreAllToControlled();
                    log("已将所有资源恢复为受控状态");
                    JOptionPane.showMessageDialog(this, "恢复正常操作成功", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    log("恢复失败: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "恢复失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
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