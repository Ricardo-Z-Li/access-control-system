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
        add(UiTheme.createHeader("Admin Panel", "People, badges, resources, and permissions"), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Employees", createEmployeePanel());
        tabbedPane.addTab("Badges", createBadgePanel());
        tabbedPane.addTab("Resources", createResourcePanel());
        tabbedPane.addTab("Groups", createGroupPanel());
        tabbedPane.addTab("Permissions", createPermissionPanel());
        tabbedPane.addTab("Profile Files", createProfilePanel());
        tabbedPane.addTab("Profile Binding", createProfileBindingPanel());
        add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("AdminService: " + (adminService != null ? "Available" : "Unavailable")));
        statusPanel.add(new JLabel("ProfileFileService: " + (profileFileService != null ? "Available" : "Unavailable")));
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        JTextField empIdField = new JTextField(20);
        JTextField nameField = new JTextField(20);
        inputPanel.add(UiTheme.formRow("Employee ID", empIdField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Name", nameField));

        JButton registerButton = UiTheme.primaryButton("Register Employee");
        registerButton.addActionListener(e -> {
            String empId = empIdField.getText().trim();
            String name = nameField.getText().trim();
            if (empId.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter employee ID and name");
                return;
            }
            try {
                adminService.registerEmployee(empId, name);
                JOptionPane.showMessageDialog(panel, "Employee registered: " + empId);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Register failed: " + ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.add(registerButton);

        JPanel card = UiTheme.cardPanel();
        card.add(inputPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(UiTheme.wrapContent(card), BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTextArea(8, 50)), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBadgePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JTextField empIdField = new JTextField(15);
        JTextField badgeIdField = new JTextField(15);
        JComboBox<BadgeStatus> statusCombo = new JComboBox<>(BadgeStatus.values());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.add(UiTheme.formRow("Employee ID", empIdField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Badge ID", badgeIdField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Status", statusCombo));

        JButton issueButton = UiTheme.primaryButton("Issue Badge");
        issueButton.addActionListener(e -> {
            String empId = empIdField.getText().trim();
            String badgeId = badgeIdField.getText().trim();
            if (empId.isEmpty() || badgeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter employee ID and badge ID");
                return;
            }
            try {
                adminService.issueBadge(empId, badgeId);
                JOptionPane.showMessageDialog(panel, "Badge issued");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Issue failed: " + ex.getMessage());
            }
        });

        JButton statusButton = UiTheme.secondaryButton("Update Status");
        statusButton.addActionListener(e -> {
            String badgeId = badgeIdField.getText().trim();
            if (badgeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter badge ID");
                return;
            }
            BadgeStatus status = (BadgeStatus) statusCombo.getSelectedItem();
            try {
                adminService.setBadgeStatus(badgeId, status);
                JOptionPane.showMessageDialog(panel, "Badge status updated");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Update failed: " + ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.add(issueButton);
        buttonPanel.add(statusButton);

        JPanel card = UiTheme.cardPanel();
        card.add(inputPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(UiTheme.wrapContent(card), BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTextArea(8, 50)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createResourcePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JTextField resIdField = new JTextField(15);
        JTextField nameField = new JTextField(15);
        JComboBox<ResourceState> stateCombo = new JComboBox<>(ResourceState.values());
        JTextField buildingField = new JTextField(15);
        JTextField floorField = new JTextField(15);
        JTextField coordXField = new JTextField(15);
        JTextField coordYField = new JTextField(15);
        JTextField locationField = new JTextField(15);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.add(UiTheme.formRow("Resource ID", resIdField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Resource Name", nameField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Status", stateCombo));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Building", buildingField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Floor", floorField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Coord X", coordXField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Coord Y", coordYField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Location", locationField));

        JButton registerButton = UiTheme.primaryButton("Register Resource");
        registerButton.addActionListener(e -> {
            String resId = resIdField.getText().trim();
            String name = nameField.getText().trim();
            if (resId.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter resource ID and name");
                return;
            }
            try {
                adminService.registerResource(resId, name, ResourceType.PENDING);
                JOptionPane.showMessageDialog(panel, "Resource registered");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Register failed: " + ex.getMessage());
            }
        });

        JButton stateButton = UiTheme.secondaryButton("Update Status");
        stateButton.addActionListener(e -> {
            String resId = resIdField.getText().trim();
            ResourceState state = (ResourceState) stateCombo.getSelectedItem();
            if (resId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter resource ID");
                return;
            }
            try {
                adminService.setResourceState(resId, state);
                JOptionPane.showMessageDialog(panel, "Resource status updated");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Update failed: " + ex.getMessage());
            }
        });

        JButton locationButton = UiTheme.secondaryButton("Update Location");
        locationButton.addActionListener(e -> {
            String resId = resIdField.getText().trim();
            if (resId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter resource ID");
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
                JOptionPane.showMessageDialog(panel, "Resource location updated");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Update failed: " + ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.add(registerButton);
        buttonPanel.add(stateButton);
        buttonPanel.add(locationButton);

        JPanel card = UiTheme.cardPanel();
        card.add(inputPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(UiTheme.wrapContent(card), BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTextArea(8, 50)), BorderLayout.CENTER);
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

        JTextField groupIdField = new JTextField(15);
        JTextField groupNameField = new JTextField(15);
        JTextField empIdField = new JTextField(15);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.add(UiTheme.formRow("Group ID", groupIdField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Group Name", groupNameField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Employee ID", empIdField));

        JButton createGroupButton = UiTheme.primaryButton("Create Group");
        createGroupButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            String groupName = groupNameField.getText().trim();
            if (groupId.isEmpty() || groupName.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter group ID and name");
                return;
            }
            try {
                adminService.createGroup(groupId, groupName);
                JOptionPane.showMessageDialog(panel, "Group created");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Create failed: " + ex.getMessage());
            }
        });

        JButton assignButton = UiTheme.secondaryButton("Add Member");
        assignButton.addActionListener(e -> {
            String empId = empIdField.getText().trim();
            String groupId = groupIdField.getText().trim();
            if (empId.isEmpty() || groupId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter employee ID and group ID");
                return;
            }
            try {
                adminService.assignEmployeeToGroup(empId, groupId);
                JOptionPane.showMessageDialog(panel, "Employee added to group");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Add failed: " + ex.getMessage());
            }
        });

        JButton removeButton = UiTheme.secondaryButton("Remove Member");
        removeButton.addActionListener(e -> {
            String empId = empIdField.getText().trim();
            String groupId = groupIdField.getText().trim();
            if (empId.isEmpty() || groupId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter employee ID and group ID");
                return;
            }
            try {
                adminService.removeEmployeeFromGroup(empId, groupId);
                JOptionPane.showMessageDialog(panel, "Employee removed");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Remove failed: " + ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.add(createGroupButton);
        buttonPanel.add(assignButton);
        buttonPanel.add(removeButton);

        JPanel card = UiTheme.cardPanel();
        card.add(inputPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(UiTheme.wrapContent(card), BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTextArea(8, 50)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPermissionPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JTextField groupIdField = new JTextField(15);
        JTextField resourceIdField = new JTextField(15);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.add(UiTheme.formRow("Group ID", groupIdField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Resource ID", resourceIdField));

        JButton grantButton = UiTheme.primaryButton("Grant");
        grantButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            String resourceId = resourceIdField.getText().trim();
            if (groupId.isEmpty() || resourceId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter group ID and resource ID");
                return;
            }
            try {
                adminService.grantGroupAccessToResource(groupId, resourceId);
                JOptionPane.showMessageDialog(panel, "Granted");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Grant failed: " + ex.getMessage());
            }
        });

        JButton revokeButton = UiTheme.secondaryButton("Revoke");
        revokeButton.addActionListener(e -> {
            String groupId = groupIdField.getText().trim();
            String resourceId = resourceIdField.getText().trim();
            if (groupId.isEmpty() || resourceId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter group ID and resource ID");
                return;
            }
            try {
                adminService.revokeGroupAccessToResource(groupId, resourceId);
                JOptionPane.showMessageDialog(panel, "Revoked");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Revoke failed: " + ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.add(grantButton);
        buttonPanel.add(revokeButton);

        JPanel card = UiTheme.cardPanel();
        card.add(inputPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(UiTheme.wrapContent(card), BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTextArea(8, 50)), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JTextField filePathField = new JTextField(30);
        filePathField.setText("src/main/resources/profiles.json");

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.add(UiTheme.formRow("Profile File Path", filePathField));

        JButton loadButton = UiTheme.primaryButton("Load");
        loadButton.addActionListener(e -> {
            String filePath = filePathField.getText().trim();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter file path");
                return;
            }
            try {
                List<Profile> profiles = profileFileService.loadProfilesFromJson(filePath);
                JOptionPane.showMessageDialog(panel, "Profiles loaded: " + profiles.size());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Load failed: " + ex.getMessage());
            }
        });

        JButton validateButton = UiTheme.secondaryButton("Validate");
        validateButton.addActionListener(e -> {
            String filePath = filePathField.getText().trim();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter file path");
                return;
            }
            try {
                boolean valid = profileFileService.validateJsonFile(filePath);
                JOptionPane.showMessageDialog(panel, "Validation: " + (valid ? "Passed" : "Failed"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Validation failed: " + ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.add(loadButton);
        buttonPanel.add(validateButton);

        JPanel card = UiTheme.cardPanel();
        card.add(inputPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(UiTheme.wrapContent(card), BorderLayout.NORTH);

        JTextArea profileArea = new JTextArea(16, 60);
        profileArea.setEditable(false);
        panel.add(new JScrollPane(profileArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProfileBindingPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JTextField profileIdField = new JTextField(15);
        JTextField employeeIdField = new JTextField(15);
        JTextField badgeIdField = new JTextField(15);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.add(UiTheme.formRow("Profile ID", profileIdField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Employee ID", employeeIdField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Badge ID", badgeIdField));

        JButton bindEmployeeButton = UiTheme.primaryButton("Bind Employee");
        bindEmployeeButton.addActionListener(e -> {
            String profileId = profileIdField.getText().trim();
            String employeeId = employeeIdField.getText().trim();
            if (profileId.isEmpty() || employeeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter profile ID and employee ID");
                return;
            }
            try {
                adminService.assignProfileToEmployee(profileId, employeeId);
                JOptionPane.showMessageDialog(panel, "Bind succeeded");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Bind failed: " + ex.getMessage());
            }
        });

        JButton unbindEmployeeButton = UiTheme.secondaryButton("Unbind Employee");
        unbindEmployeeButton.addActionListener(e -> {
            String profileId = profileIdField.getText().trim();
            String employeeId = employeeIdField.getText().trim();
            if (profileId.isEmpty() || employeeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter profile ID and employee ID");
                return;
            }
            try {
                adminService.removeProfileFromEmployee(profileId, employeeId);
                JOptionPane.showMessageDialog(panel, "Unbind succeeded");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Unbind failed: " + ex.getMessage());
            }
        });

        JButton bindBadgeButton = UiTheme.secondaryButton("Bind Badge");
        bindBadgeButton.addActionListener(e -> {
            String profileId = profileIdField.getText().trim();
            String badgeId = badgeIdField.getText().trim();
            if (profileId.isEmpty() || badgeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter profile ID and badge ID");
                return;
            }
            try {
                adminService.assignProfileToBadge(profileId, badgeId);
                JOptionPane.showMessageDialog(panel, "Bind succeeded");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Bind failed: " + ex.getMessage());
            }
        });

        JButton unbindBadgeButton = UiTheme.secondaryButton("Unbind Badge");
        unbindBadgeButton.addActionListener(e -> {
            String profileId = profileIdField.getText().trim();
            String badgeId = badgeIdField.getText().trim();
            if (profileId.isEmpty() || badgeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter profile ID and badge ID");
                return;
            }
            try {
                adminService.removeProfileFromBadge(profileId, badgeId);
                JOptionPane.showMessageDialog(panel, "Unbind succeeded");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Unbind failed: " + ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.add(bindEmployeeButton);
        buttonPanel.add(unbindEmployeeButton);
        buttonPanel.add(bindBadgeButton);
        buttonPanel.add(unbindBadgeButton);

        JPanel card = UiTheme.cardPanel();
        card.add(inputPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(UiTheme.wrapContent(card), BorderLayout.NORTH);

        JTextArea resultArea = new JTextArea(12, 60);
        resultArea.setEditable(false);
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JPanel viewPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton viewEmployeeProfiles = UiTheme.secondaryButton("View Employee Profiles");
        viewEmployeeProfiles.addActionListener(e -> {
            String employeeId = employeeIdField.getText().trim();
            if (employeeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter employee ID");
                return;
            }
            try {
                List<Profile> profiles = adminService.getProfilesForEmployee(employeeId);
                StringBuilder sb = new StringBuilder();
                sb.append("Profiles (").append(profiles.size()).append(")\n");
                for (Profile profile : profiles) {
                    sb.append("- ").append(profile.getProfileId())
                        .append(" (").append(profile.getProfileName()).append(")\n");
                }
                resultArea.setText(sb.toString());
            } catch (Exception ex) {
                resultArea.setText("Query failed: " + ex.getMessage());
            }
        });
        viewPanel.add(viewEmployeeProfiles);

        JButton viewBadgeProfiles = UiTheme.secondaryButton("View Badge Profiles");
        viewBadgeProfiles.addActionListener(e -> {
            String badgeId = badgeIdField.getText().trim();
            if (badgeId.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Enter badge ID");
                return;
            }
            try {
                List<Profile> profiles = adminService.getProfilesForBadge(badgeId);
                StringBuilder sb = new StringBuilder();
                sb.append("Profiles (").append(profiles.size()).append(")\n");
                for (Profile profile : profiles) {
                    sb.append("- ").append(profile.getProfileId())
                        .append(" (").append(profile.getProfileName()).append(")\n");
                }
                resultArea.setText(sb.toString());
            } catch (Exception ex) {
                resultArea.setText("Query failed: " + ex.getMessage());
            }
        });
        viewPanel.add(viewBadgeProfiles);

        panel.add(viewPanel, BorderLayout.SOUTH);
        return panel;
    }
}
