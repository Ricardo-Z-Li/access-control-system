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
        add(UiTheme.createHeader("Emergency Control", "Unlock all or restore controlled state"), BorderLayout.NORTH);

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.CENTER);

        logArea = new JTextArea(10, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(UiTheme.surface());
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(UiTheme.subtleBorder(), "Activity Log"));
        add(scrollPane, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 12, 0);

        JButton emergencyButton = UiTheme.dangerButton("Unlock All Doors");
        emergencyButton.setToolTipText("Set all doors to uncontrolled");
        emergencyButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm emergency unlock?\nAll doors will be set to uncontrolled.\nContinue?",
                "Confirm Emergency Unlock",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    emergencyControlService.setAllDoorsUncontrolled();
                    log("Emergency unlock executed");
                    JOptionPane.showMessageDialog(this, "Emergency action completed. All doors set to uncontrolled.", "Info", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    log("Emergency action failed: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Emergency action failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JPanel emergencyCard = UiTheme.cardPanel();
        emergencyCard.add(new JLabel("Emergency Override"), BorderLayout.NORTH);
        emergencyCard.add(emergencyButton, BorderLayout.CENTER);

        JPanel typePanel = UiTheme.cardPanel();
        typePanel.add(new JLabel("Control by Resource Type"), BorderLayout.NORTH);

        JPanel typeControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JComboBox<ResourceType> typeCombo = new JComboBox<>(ResourceType.values());
        typeCombo.removeItem(ResourceType.PENDING);
        JButton setUncontrolledButton = UiTheme.secondaryButton("Set Uncontrolled");
        setUncontrolledButton.addActionListener(e -> {
            ResourceType selectedType = (ResourceType) typeCombo.getSelectedItem();
            try {
                emergencyControlService.setResourcesControlledByType(selectedType, false);
                log("Set type " + selectedType + " to uncontrolled");
            } catch (Exception ex) {
                log("Action failed: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Action failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton setControlledButton = UiTheme.secondaryButton("Set Controlled");
        setControlledButton.addActionListener(e -> {
            ResourceType selectedType = (ResourceType) typeCombo.getSelectedItem();
            try {
                emergencyControlService.setResourcesControlledByType(selectedType, true);
                log("Set type " + selectedType + " to controlled");
            } catch (Exception ex) {
                log("Action failed: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Action failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        typeControls.add(new JLabel("Resource Type:"));
        typeControls.add(typeCombo);
        typeControls.add(setUncontrolledButton);
        typeControls.add(setControlledButton);
        typePanel.add(typeControls, BorderLayout.CENTER);

        JPanel groupPanel = UiTheme.cardPanel();
        groupPanel.add(new JLabel("Control by Group"), BorderLayout.NORTH);
        JTextField groupIdField = new JTextField(15);
        JButton setGroupUncontrolledButton = UiTheme.secondaryButton("Set Group Uncontrolled");
        setGroupUncontrolledButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            if (groupId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter group ID", "Info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                emergencyControlService.setGroupResourcesControlled(groupId, false);
                log("Set group " + groupId + " to uncontrolled");
            } catch (Exception ex) {
                log("Action failed: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Action failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton setGroupControlledButton = UiTheme.secondaryButton("Set Group Controlled");
        setGroupControlledButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            if (groupId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter group ID", "Info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                emergencyControlService.setGroupResourcesControlled(groupId, true);
                log("Set group " + groupId + " to controlled");
            } catch (Exception ex) {
                log("Action failed: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Action failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel groupControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        groupControls.add(new JLabel("Group ID:"));
        groupControls.add(groupIdField);
        groupControls.add(setGroupUncontrolledButton);
        groupControls.add(setGroupControlledButton);
        groupPanel.add(groupControls, BorderLayout.CENTER);

        JButton restoreButton = UiTheme.successButton("Restore All to Controlled");
        restoreButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Restore all doors to controlled?",
                "Confirm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    emergencyControlService.restoreAllToControlled();
                    log("Restored all doors to controlled");
                    JOptionPane.showMessageDialog(this, "Restore completed", "Info", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    log("Action failed: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Action failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        panel.add(UiTheme.wrapContent(emergencyCard), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(0, 0, 12, 6);
        panel.add(UiTheme.wrapContent(typePanel), gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 6, 12, 0);
        panel.add(UiTheme.wrapContent(groupPanel), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(UiTheme.wrapContent(restoreButton), gbc);

        return panel;
    }

    private void log(String message) {
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
