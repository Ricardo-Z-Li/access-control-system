package acs.ui;

import acs.domain.BadgeStatus;
import acs.domain.Profile;
import acs.domain.ResourceState;
import acs.domain.ResourceType;
import acs.service.AdminService;
import acs.service.ProfileFileService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AdminPanel extends JPanel {
    private final AdminService adminService;
    private final ProfileFileService profileFileService;
    private JTabbedPane tabbedPane;

    public AdminPanel(AdminService adminService, ProfileFileService profileFileService) {
        this.adminService = adminService;
        this.profileFileService = profileFileService;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("管理员控制台", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("员工", createEmployeePanel());
        tabbedPane.addTab("徽章", createBadgePanel());
        tabbedPane.addTab("资源", createResourcePanel());
        tabbedPane.addTab("群组", createGroupPanel());
        tabbedPane.addTab("权限", createPermissionPanel());
        tabbedPane.addTab("Profile", createProfilePanel());
        tabbedPane.addTab("Profile绑定", createProfileBindingPanel());
        add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("AdminService: " + (adminService != null ? "可用" : "不可用")));
        statusPanel.add(new JLabel("ProfileFileService: " + (profileFileService != null ? "可用" : "不可用")));
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
        registerButton.addActionListener(e -> {
            String empId = empIdField.getText().trim();
            String name = nameField.getText().trim();
            if (empId.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入员工ID和姓名");
                return;
            }
            try {
                adminService.registerEmployee(empId, name);
                JOptionPane.showMessageDialog(panel, "员工已注册: " + empId);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "注册失败: " + ex.getMessage());
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
                JOptionPane.showMessageDialog(panel, "徽章已发放");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "发放失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(issueButton);

        JButton statusButton = new JButton("设置状态");
        statusButton.addActionListener(e -> {
            String badgeId = badgeIdField.getText().trim();
            if (badgeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入徽章ID");
                return;
            }
            BadgeStatus status = (BadgeStatus) statusCombo.getSelectedItem();
            try {
                adminService.setBadgeStatus(badgeId, status);
                JOptionPane.showMessageDialog(panel, "徽章状态已更新");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "设置失败: " + ex.getMessage());
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
        inputPanel.add(new JLabel("名称:"), gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(15);
        inputPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("状态:"), gbc);

        gbc.gridx = 1;
        JComboBox<ResourceState> stateCombo = new JComboBox<>(ResourceState.values());
        inputPanel.add(stateCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(new JLabel("楼栋:"), gbc);

        gbc.gridx = 1;
        JTextField buildingField = new JTextField(15);
        inputPanel.add(buildingField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        inputPanel.add(new JLabel("楼层:"), gbc);

        gbc.gridx = 1;
        JTextField floorField = new JTextField(15);
        inputPanel.add(floorField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        inputPanel.add(new JLabel("坐标X:"), gbc);

        gbc.gridx = 1;
        JTextField coordXField = new JTextField(15);
        inputPanel.add(coordXField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        inputPanel.add(new JLabel("坐标Y:"), gbc);

        gbc.gridx = 1;
        JTextField coordYField = new JTextField(15);
        inputPanel.add(coordYField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        inputPanel.add(new JLabel("位置描述:"), gbc);

        gbc.gridx = 1;
        JTextField locationField = new JTextField(15);
        inputPanel.add(locationField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton registerButton = new JButton("注册资源");
        registerButton.addActionListener(e -> {
            String resId = resIdField.getText().trim();
            String name = nameField.getText().trim();
            if (resId.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入资源ID和名称");
                return;
            }
            try {
                adminService.registerResource(resId, name, ResourceType.PENDING);
                JOptionPane.showMessageDialog(panel, "资源已注册");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "注册失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(registerButton);

        JButton stateButton = new JButton("设置状态");
        stateButton.addActionListener(e -> {
            String resId = resIdField.getText().trim();
            ResourceState state = (ResourceState) stateCombo.getSelectedItem();
            if (resId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入资源ID");
                return;
            }
            try {
                adminService.setResourceState(resId, state);
                JOptionPane.showMessageDialog(panel, "资源状态已更新");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "设置失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(stateButton);

        JButton locationButton = new JButton("更新位置");
        locationButton.addActionListener(e -> {
            String resId = resIdField.getText().trim();
            if (resId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入资源ID");
                return;
            }
            try {
                Integer coordX = parseNullableInt(coordXField.getText().trim());
                Integer coordY = parseNullableInt(coordYField.getText().trim());
                adminService.updateResourceLocation(
                        resId,
                        emptyToNull(buildingField.getText()),
                        emptyToNull(floorField.getText()),
                        coordX,
                        coordY,
                        emptyToNull(locationField.getText())
                );
                JOptionPane.showMessageDialog(panel, "资源位置已更新");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "更新失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(locationButton);

        inputPanel.add(buttonPanel, gbc);

        panel.add(inputPanel, BorderLayout.NORTH);

        JTextArea logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        return panel;
    }

    private Integer parseNullableInt(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return Integer.valueOf(trimmed);
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
        inputPanel.add(new JLabel("组名:"), gbc);

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
                JOptionPane.showMessageDialog(panel, "请输入组ID和组名");
                return;
            }
            try {
                adminService.createGroup(groupId, groupName);
                JOptionPane.showMessageDialog(panel, "组已创建");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "创建失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(createGroupButton);

        JButton assignButton = new JButton("添加成员");
        assignButton.addActionListener(e -> {
            String empId = empIdField.getText().trim();
            String groupId = groupIdField.getText().trim();
            if (empId.isEmpty() || groupId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入员工ID和组ID");
                return;
            }
            try {
                adminService.assignEmployeeToGroup(empId, groupId);
                JOptionPane.showMessageDialog(panel, "成员已加入");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "添加失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(assignButton);

        JButton removeButton = new JButton("移除成员");
        removeButton.addActionListener(e -> {
            String empId = empIdField.getText().trim();
            String groupId = groupIdField.getText().trim();
            if (empId.isEmpty() || groupId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入员工ID和组ID");
                return;
            }
            try {
                adminService.removeEmployeeFromGroup(empId, groupId);
                JOptionPane.showMessageDialog(panel, "成员已移除");
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

        JButton grantButton = new JButton("授权");
        grantButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            String resourceId = resourceIdField.getText().trim();
            if (groupId.isEmpty() || resourceId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入组ID和资源ID");
                return;
            }
            try {
                adminService.grantGroupAccessToResource(groupId, resourceId);
                JOptionPane.showMessageDialog(panel, "已授权");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "授权失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(grantButton);

        JButton revokeButton = new JButton("撤销授权");
        revokeButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            String resourceId = resourceIdField.getText().trim();
            if (groupId.isEmpty() || resourceId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入组ID和资源ID");
                return;
            }
            try {
                adminService.revokeGroupAccessToResource(groupId, resourceId);
                JOptionPane.showMessageDialog(panel, "已撤销");
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
        controlPanel.add(new JLabel("Profile文件路径:"));
        controlPanel.add(filePathField);

        JButton loadButton = new JButton("加载");
        loadButton.addActionListener(e -> {
            String filePath = filePathField.getText().trim();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入文件路径");
                return;
            }
            try {
                List<Profile> profiles = profileFileService.loadProfilesFromJson(filePath);
                JOptionPane.showMessageDialog(panel, "加载Profile数量: " + profiles.size());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "加载失败: " + ex.getMessage());
            }
        });
        controlPanel.add(loadButton);

        JButton validateButton = new JButton("校验");
        validateButton.addActionListener(e -> {
            String filePath = filePathField.getText().trim();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入文件路径");
                return;
            }
            try {
                boolean valid = profileFileService.validateJsonFile(filePath);
                JOptionPane.showMessageDialog(panel, "校验结果: " + (valid ? "通过" : "未通过"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "校验失败: " + ex.getMessage());
            }
        });
        controlPanel.add(validateButton);

        panel.add(controlPanel, BorderLayout.NORTH);

        JTextArea profileArea = new JTextArea(20, 60);
        profileArea.setEditable(false);
        panel.add(new JScrollPane(profileArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createProfileBindingPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Profile ID:"), gbc);

        gbc.gridx = 1;
        JTextField profileIdField = new JTextField(15);
        inputPanel.add(profileIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("员工ID:"), gbc);

        gbc.gridx = 1;
        JTextField employeeIdField = new JTextField(15);
        inputPanel.add(employeeIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("徽章ID:"), gbc);

        gbc.gridx = 1;
        JTextField badgeIdField = new JTextField(15);
        inputPanel.add(badgeIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton bindEmployeeButton = new JButton("绑定员工");
        bindEmployeeButton.addActionListener(e -> {
            String profileId = profileIdField.getText().trim();
            String employeeId = employeeIdField.getText().trim();
            if (profileId.isEmpty() || employeeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入Profile ID和员工ID");
                return;
            }
            try {
                adminService.assignProfileToEmployee(profileId, employeeId);
                JOptionPane.showMessageDialog(panel, "绑定成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "绑定失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(bindEmployeeButton);

        JButton unbindEmployeeButton = new JButton("解绑员工");
        unbindEmployeeButton.addActionListener(e -> {
            String profileId = profileIdField.getText().trim();
            String employeeId = employeeIdField.getText().trim();
            if (profileId.isEmpty() || employeeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入Profile ID和员工ID");
                return;
            }
            try {
                adminService.removeProfileFromEmployee(profileId, employeeId);
                JOptionPane.showMessageDialog(panel, "解绑成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "解绑失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(unbindEmployeeButton);

        JButton bindBadgeButton = new JButton("绑定徽章");
        bindBadgeButton.addActionListener(e -> {
            String profileId = profileIdField.getText().trim();
            String badgeId = badgeIdField.getText().trim();
            if (profileId.isEmpty() || badgeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入Profile ID和徽章ID");
                return;
            }
            try {
                adminService.assignProfileToBadge(profileId, badgeId);
                JOptionPane.showMessageDialog(panel, "绑定成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "绑定失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(bindBadgeButton);

        JButton unbindBadgeButton = new JButton("解绑徽章");
        unbindBadgeButton.addActionListener(e -> {
            String profileId = profileIdField.getText().trim();
            String badgeId = badgeIdField.getText().trim();
            if (profileId.isEmpty() || badgeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入Profile ID和徽章ID");
                return;
            }
            try {
                adminService.removeProfileFromBadge(profileId, badgeId);
                JOptionPane.showMessageDialog(panel, "解绑成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "解绑失败: " + ex.getMessage());
            }
        });
        buttonPanel.add(unbindBadgeButton);

        inputPanel.add(buttonPanel, gbc);
        panel.add(inputPanel, BorderLayout.NORTH);

        JTextArea resultArea = new JTextArea(14, 60);
        resultArea.setEditable(false);
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JPanel viewPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton viewEmployeeProfiles = new JButton("查看员工Profile");
        viewEmployeeProfiles.addActionListener(e -> {
            String employeeId = employeeIdField.getText().trim();
            if (employeeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入员工ID");
                return;
            }
            try {
                List<Profile> profiles = adminService.getProfilesForEmployee(employeeId);
                StringBuilder sb = new StringBuilder();
                sb.append("Profile列表(").append(profiles.size()).append(")\n");
                for (Profile profile : profiles) {
                    sb.append("- ").append(profile.getProfileId())
                        .append(" (").append(profile.getProfileName()).append(")\n");
                }
                resultArea.setText(sb.toString());
            } catch (Exception ex) {
                resultArea.setText("查询失败: " + ex.getMessage());
            }
        });
        viewPanel.add(viewEmployeeProfiles);

        JButton viewBadgeProfiles = new JButton("查看徽章Profile");
        viewBadgeProfiles.addActionListener(e -> {
            String badgeId = badgeIdField.getText().trim();
            if (badgeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "请输入徽章ID");
                return;
            }
            try {
                List<Profile> profiles = adminService.getProfilesForBadge(badgeId);
                StringBuilder sb = new StringBuilder();
                sb.append("Profile列表(").append(profiles.size()).append(")\n");
                for (Profile profile : profiles) {
                    sb.append("- ").append(profile.getProfileId())
                        .append(" (").append(profile.getProfileName()).append(")\n");
                }
                resultArea.setText(sb.toString());
            } catch (Exception ex) {
                resultArea.setText("查询失败: " + ex.getMessage());
            }
        });
        viewPanel.add(viewBadgeProfiles);

        panel.add(viewPanel, BorderLayout.SOUTH);

        return panel;
    }
}
