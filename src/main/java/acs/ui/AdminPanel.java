package acs.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import acs.service.AdminService;
import acs.service.ProfileFileService;
import acs.domain.BadgeStatus;
import acs.domain.ResourceState;
import acs.domain.ResourceType;
import acs.domain.Profile;
import acs.domain.TimeFilter;
import java.util.List;

public class AdminPanel extends JPanel {
    private AdminService adminService;
    private ProfileFileService profileFileService;
    private JTabbedPane tabbedPane;
    
    public AdminPanel(AdminService adminService, ProfileFileService profileFileService) {
        this.adminService = adminService;
        this.profileFileService = profileFileService;
        initUI();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("系统管理面板", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("员工管理", createEmployeePanel());
        tabbedPane.addTab("徽章管理", createBadgePanel());
        tabbedPane.addTab("资源管理", createResourcePanel());
        tabbedPane.addTab("组管理", createGroupPanel());
        tabbedPane.addTab("权限配置", createPermissionPanel());
        tabbedPane.addTab("配置文件", createProfilePanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("管理服务: " + (adminService != null ? "可用" : "不可用")));
        statusPanel.add(new JLabel("配置文件服务: " + (profileFileService != null ? "可用" : "不可用")));
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("员工ID:"), gbc);
        
        gbc.gridx = 1;
        JTextField empIdField = new JTextField(20);
        inputPanel.add(empIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("姓名:"), gbc);
        
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        inputPanel.add(nameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton registerButton = new JButton("注册员工");
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String empId = empIdField.getText().trim();
                String name = nameField.getText().trim();
                if (empId.isEmpty() || name.isEmpty()) {
                    JOptionPane.showMessageDialog(panel, "请输入员工ID和姓名");
                    return;
                }
                try {
                    adminService.registerEmployee(empId, name);
                    JOptionPane.showMessageDialog(panel, "员工注册成功: " + empId);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, "注册失败: " + ex.getMessage());
                }
            }
        });
        inputPanel.add(registerButton, gbc);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        JTextArea logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBadgePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("员工ID:"), gbc);
        
        gbc.gridx = 1;
        JTextField empIdField = new JTextField(15);
        inputPanel.add(empIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("徽章ID:"), gbc);
        
        gbc.gridx = 1;
        JTextField badgeIdField = new JTextField(15);
        inputPanel.add(badgeIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("状态:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<BadgeStatus> statusCombo = new JComboBox<>(BadgeStatus.values());
        inputPanel.add(statusCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton issueButton = new JButton("发放徽章");
        issueButton.addActionListener(e -> {
            String empId = empIdField.getText().trim();
            String badgeId = badgeIdField.getText().trim();
            if (empId.isEmpty() || badgeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入员工ID和徽章ID");
                return;
            }
            try {
                adminService.issueBadge(empId, badgeId);
                JOptionPane.showMessageDialog(panel, "徽章发放成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "发放失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(issueButton);
        
        JButton statusButton = new JButton("更新状态");
        statusButton.addActionListener(e -> {
            String badgeId = badgeIdField.getText().trim();
            if (badgeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入徽章ID");
                return;
            }
            BadgeStatus status = (BadgeStatus) statusCombo.getSelectedItem();
            try {
                adminService.setBadgeStatus(badgeId, status);
                JOptionPane.showMessageDialog(panel, "状态更新成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "更新失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(statusButton);
        
        inputPanel.add(buttonPanel, gbc);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        JTextArea logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createResourcePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("资源ID:"), gbc);
        
        gbc.gridx = 1;
        JTextField resIdField = new JTextField(15);
        inputPanel.add(resIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("资源名称:"), gbc);
        
        gbc.gridx = 1;
        JTextField nameField = new JTextField(15);
        inputPanel.add(nameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("资源类型:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<ResourceType> typeCombo = new JComboBox<>(ResourceType.values());
        inputPanel.add(typeCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(new JLabel("资源状态:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<ResourceState> stateCombo = new JComboBox<>(ResourceState.values());
        inputPanel.add(stateCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton registerButton = new JButton("注册资源");
        registerButton.addActionListener(e -> {
            String resId = resIdField.getText().trim();
            String name = nameField.getText().trim();
            ResourceType type = (ResourceType) typeCombo.getSelectedItem();
            if (resId.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入资源ID和名称");
                return;
            }
            try {
                adminService.registerResource(resId, name, type);
                JOptionPane.showMessageDialog(panel, "资源注册成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "注册失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(registerButton);
        
        JButton stateButton = new JButton("更新状态");
        stateButton.addActionListener(e -> {
            String resId = resIdField.getText().trim();
            ResourceState state = (ResourceState) stateCombo.getSelectedItem();
            if (resId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入资源ID");
                return;
            }
            try {
                adminService.setResourceState(resId, state);
                JOptionPane.showMessageDialog(panel, "状态更新成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "更新失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(stateButton);
        
        inputPanel.add(buttonPanel, gbc);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        JTextArea logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createGroupPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("组ID:"), gbc);
        
        gbc.gridx = 1;
        JTextField groupIdField = new JTextField(15);
        inputPanel.add(groupIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("组名称:"), gbc);
        
        gbc.gridx = 1;
        JTextField groupNameField = new JTextField(15);
        inputPanel.add(groupNameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("员工ID:"), gbc);
        
        gbc.gridx = 1;
        JTextField empIdField = new JTextField(15);
        inputPanel.add(empIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton createGroupButton = new JButton("创建组");
        createGroupButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            String groupName = groupNameField.getText().trim();
            if (groupId.isEmpty() || groupName.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入组ID和名称");
                return;
            }
            try {
                adminService.createGroup(groupId, groupName);
                JOptionPane.showMessageDialog(panel, "组创建成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "创建失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(createGroupButton);
        
        JButton assignButton = new JButton("分配员工到组");
        assignButton.addActionListener(e -> {
            String empId = empIdField.getText().trim();
            String groupId = groupIdField.getText().trim();
            if (empId.isEmpty() || groupId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入员工ID和组ID");
                return;
            }
            try {
                adminService.assignEmployeeToGroup(empId, groupId);
                JOptionPane.showMessageDialog(panel, "分配成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "分配失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(assignButton);
        
        JButton removeButton = new JButton("从组移除员工");
        removeButton.addActionListener(e -> {
            String empId = empIdField.getText().trim();
            String groupId = groupIdField.getText().trim();
            if (empId.isEmpty() || groupId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入员工ID和组ID");
                return;
            }
            try {
                adminService.removeEmployeeFromGroup(empId, groupId);
                JOptionPane.showMessageDialog(panel, "移除成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "移除失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(removeButton);
        
        inputPanel.add(buttonPanel, gbc);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        JTextArea logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createPermissionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("组ID:"), gbc);
        
        gbc.gridx = 1;
        JTextField groupIdField = new JTextField(15);
        inputPanel.add(groupIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("资源ID:"), gbc);
        
        gbc.gridx = 1;
        JTextField resourceIdField = new JTextField(15);
        inputPanel.add(resourceIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton grantButton = new JButton("授予访问权限");
        grantButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            String resourceId = resourceIdField.getText().trim();
            if (groupId.isEmpty() || resourceId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入组ID和资源ID");
                return;
            }
            try {
                adminService.grantGroupAccessToResource(groupId, resourceId);
                JOptionPane.showMessageDialog(panel, "权限授予成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "授予失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(grantButton);
        
        JButton revokeButton = new JButton("撤销访问权限");
        revokeButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            String resourceId = resourceIdField.getText().trim();
            if (groupId.isEmpty() || resourceId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入组ID和资源ID");
                return;
            }
            try {
                adminService.revokeGroupAccessToResource(groupId, resourceId);
                JOptionPane.showMessageDialog(panel, "权限撤销成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "撤销失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(revokeButton);
        
        inputPanel.add(buttonPanel, gbc);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        JTextArea logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JTextField filePathField = new JTextField(30);
        filePathField.setText("src/main/resources/profiles.json");
        controlPanel.add(new JLabel("配置文件路径:"));
        controlPanel.add(filePathField);
        
        JButton loadButton = new JButton("加载配置文件");
        loadButton.addActionListener(e -> {
            String filePath = filePathField.getText().trim();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入文件路径");
                return;
            }
            try {
                List<Profile> profiles = profileFileService.loadProfilesFromJson(filePath);
                JOptionPane.showMessageDialog(panel, "成功加载 " + profiles.size() + " 个配置文件");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "加载失败: " + ex.getMessage());
            }
        });
        controlPanel.add(loadButton);
        
        JButton validateButton = new JButton("验证文件");
        validateButton.addActionListener(e -> {
            String filePath = filePathField.getText().trim();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入文件路径");
                return;
            }
            try {
                boolean valid = profileFileService.validateJsonFile(filePath);
                JOptionPane.showMessageDialog(panel, "文件验证: " + (valid ? "通过" : "失败"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "验证失败: " + ex.getMessage());
            }
        });
        controlPanel.add(validateButton);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        JTextArea profileArea = new JTextArea(20, 60);
        profileArea.setEditable(false);
        panel.add(new JScrollPane(profileArea), BorderLayout.CENTER);
        
        return panel;
    }
}
