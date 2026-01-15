package acs.ui;

import acs.domain.Employee;
import acs.domain.Group;
import acs.domain.Profile;
import acs.domain.ProfileResourceLimit;
import acs.domain.Resource;
import acs.repository.EmployeeRepository;
import acs.repository.ProfileRepository;
import acs.repository.ProfileResourceLimitRepository;
import acs.repository.ResourceRepository;
import acs.service.AccessLimitService;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AccessLimitPanel extends JPanel {
    private final AccessLimitService accessLimitService;
    private final EmployeeRepository employeeRepository;
    private final ProfileRepository profileRepository;
    private final ResourceRepository resourceRepository;
    private final ProfileResourceLimitRepository profileResourceLimitRepository;

    private JTable limitTable;
    private DefaultTableModel limitTableModel;
    private JTextField employeeIdField;
    private JTextField resourceIdCheckField;
    private JTextArea resultArea;

    private JTable resourceLimitTable;
    private DefaultTableModel resourceLimitTableModel;
    private JTextField profileIdField;
    private JTextField resourceIdField;
    private JTextField dailyLimitField;
    private JTextField weeklyLimitField;
    private JCheckBox activeCheckBox;

    public AccessLimitPanel(AccessLimitService accessLimitService,
                            EmployeeRepository employeeRepository,
                            ProfileRepository profileRepository,
                            ResourceRepository resourceRepository,
                            ProfileResourceLimitRepository profileResourceLimitRepository) {
        this.accessLimitService = accessLimitService;
        this.employeeRepository = employeeRepository;
        this.profileRepository = profileRepository;
        this.resourceRepository = resourceRepository;
        this.profileResourceLimitRepository = profileResourceLimitRepository;
        initUI();
        refreshLimitTable();
        refreshResourceLimitTable();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        add(UiTheme.createHeader("Access Limits", "Review and maintain access count rules"), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Limit Check", createLimitCheckPanel());
        tabbedPane.addTab("Employee Stats", createEmployeeStatsPanel());
        tabbedPane.addTab("Resource Limits", createResourceLimitPanel());

        add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Service Status: " + (accessLimitService != null ? "OK" : "Unavailable")));
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createLimitCheckPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        employeeIdField = new JTextField(20);
        employeeIdField.setText("EMP001");
        resourceIdCheckField = new JTextField(20);

        inputPanel.add(UiTheme.formRow("Employee ID", employeeIdField));
        inputPanel.add(Box.createVerticalStrut(8));
        inputPanel.add(UiTheme.formRow("Resource ID (optional)", resourceIdCheckField));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton checkAllButton = UiTheme.primaryButton("Check Limits");
        checkAllButton.addActionListener(e -> checkAllLimits());
        JButton getCountsButton = UiTheme.secondaryButton("Access Stats");
        getCountsButton.addActionListener(e -> getAccessCounts());
        buttonPanel.add(checkAllButton);
        buttonPanel.add(getCountsButton);

        JPanel card = UiTheme.cardPanel();
        card.add(inputPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(UiTheme.wrapContent(card), BorderLayout.NORTH);

        resultArea = new JTextArea(15, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEmployeeStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Employee ID", "Name", "Today", "This Week", "Profiles", "Status"};
        limitTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        limitTable = new JTable(limitTableModel);
        limitTable.setAutoCreateRowSorter(true);
        UiTheme.styleTable(limitTable);

        JScrollPane scrollPane = new JScrollPane(limitTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton refreshButton = UiTheme.secondaryButton("Refresh");
        refreshButton.addActionListener(e -> refreshLimitTable());
        controlPanel.add(refreshButton);

        panel.add(controlPanel, BorderLayout.NORTH);

        return panel;
    }

    private JPanel createResourceLimitPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        profileIdField = new JTextField(12);
        resourceIdField = new JTextField(12);
        dailyLimitField = new JTextField(12);
        weeklyLimitField = new JTextField(12);
        activeCheckBox = new JCheckBox();
        activeCheckBox.setSelected(true);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.add(UiTheme.formRow("Profile ID", profileIdField));
        inputPanel.add(Box.createVerticalStrut(6));
        inputPanel.add(UiTheme.formRow("Resource ID", resourceIdField));
        inputPanel.add(Box.createVerticalStrut(6));
        inputPanel.add(UiTheme.formRow("Daily Limit", dailyLimitField));
        inputPanel.add(Box.createVerticalStrut(6));
        inputPanel.add(UiTheme.formRow("Weekly Limit", weeklyLimitField));
        inputPanel.add(Box.createVerticalStrut(6));
        inputPanel.add(UiTheme.formRow("Active", activeCheckBox));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton upsertButton = UiTheme.primaryButton("Add/Update");
        upsertButton.addActionListener(e -> upsertResourceLimit());
        JButton deactivateButton = UiTheme.secondaryButton("Deactivate");
        deactivateButton.addActionListener(e -> deactivateResourceLimit());
        JButton deleteButton = UiTheme.secondaryButton("Delete");
        deleteButton.addActionListener(e -> deleteResourceLimit());
        JButton refreshButton = UiTheme.secondaryButton("Refresh");
        refreshButton.addActionListener(e -> refreshResourceLimitTable());
        buttonPanel.add(upsertButton);
        buttonPanel.add(deactivateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        JPanel card = UiTheme.cardPanel();
        card.add(inputPanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(UiTheme.wrapContent(card), BorderLayout.NORTH);

        String[] columns = {"ID", "Profile", "Resource", "Daily Limit", "Weekly Limit", "Active"};
        resourceLimitTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resourceLimitTable = new JTable(resourceLimitTableModel);
        resourceLimitTable.setAutoCreateRowSorter(true);
        UiTheme.styleTable(resourceLimitTable);
        resourceLimitTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    populateResourceLimitFields();
                }
            }
        });

        panel.add(new JScrollPane(resourceLimitTable), BorderLayout.CENTER);
        return panel;
    }

    private void upsertResourceLimit() {
        String profileId = profileIdField.getText().trim();
        String resourceId = resourceIdField.getText().trim();
        if (profileId.isEmpty() || resourceId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter profile ID and resource ID");
            return;
        }

        Integer daily = parseOptionalInt(dailyLimitField.getText().trim());
        Integer weekly = parseOptionalInt(weeklyLimitField.getText().trim());

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceId));

        Optional<ProfileResourceLimit> existing = profileResourceLimitRepository
                .findByProfileAndResource(profile, resource);
        ProfileResourceLimit limit = existing.orElseGet(ProfileResourceLimit::new);
        limit.setProfile(profile);
        limit.setResource(resource);
        limit.setDailyLimit(daily);
        limit.setWeeklyLimit(weekly);
        limit.setIsActive(activeCheckBox.isSelected());

        profileResourceLimitRepository.save(limit);
        refreshResourceLimitTable();
    }

    private void deactivateResourceLimit() {
        Long id = getSelectedLimitId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Select a limit record");
            return;
        }
        ProfileResourceLimit limit = profileResourceLimitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource limit not found: " + id));
        limit.setIsActive(false);
        profileResourceLimitRepository.save(limit);
        refreshResourceLimitTable();
    }

    private void deleteResourceLimit() {
        Long id = getSelectedLimitId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Select a limit record to delete");
            return;
        }
        profileResourceLimitRepository.deleteById(id);
        refreshResourceLimitTable();
    }

    private void populateResourceLimitFields() {
        int row = resourceLimitTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = resourceLimitTable.convertRowIndexToModel(row);
        profileIdField.setText(String.valueOf(resourceLimitTableModel.getValueAt(modelRow, 1)));
        resourceIdField.setText(String.valueOf(resourceLimitTableModel.getValueAt(modelRow, 2)));
        Object daily = resourceLimitTableModel.getValueAt(modelRow, 3);
        Object weekly = resourceLimitTableModel.getValueAt(modelRow, 4);
        Object active = resourceLimitTableModel.getValueAt(modelRow, 5);
        dailyLimitField.setText(daily == null ? "" : daily.toString());
        weeklyLimitField.setText(weekly == null ? "" : weekly.toString());
        activeCheckBox.setSelected("true".equalsIgnoreCase(String.valueOf(active)));
    }

    private void refreshResourceLimitTable() {
        List<ProfileResourceLimit> limits = profileResourceLimitRepository.findAll();
        resourceLimitTableModel.setRowCount(0);
        for (ProfileResourceLimit limit : limits) {
            resourceLimitTableModel.addRow(new Object[]{
                    limit.getId(),
                    limit.getProfile() != null ? limit.getProfile().getProfileId() : "",
                    limit.getResource() != null ? limit.getResource().getResourceId() : "",
                    limit.getDailyLimit(),
                    limit.getWeeklyLimit(),
                    limit.getIsActive() != null ? limit.getIsActive() : Boolean.FALSE
            });
        }
    }

    private Long getSelectedLimitId() {
        int row = resourceLimitTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = resourceLimitTable.convertRowIndexToModel(row);
        Object value = resourceLimitTableModel.getValueAt(modelRow, 0);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseOptionalInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid integer: " + value);
        }
    }

    private void checkAllLimits() {
        String employeeId = employeeIdField.getText().trim();
        if (employeeId.isEmpty()) {
            resultArea.setText("Error: enter employee ID");
            return;
        }

        try {
            Employee employeeWithGroups = employeeRepository.findByIdWithGroups(employeeId).orElse(null);
            if (employeeWithGroups == null) {
                resultArea.setText("Error: employee not found - " + employeeId);
                return;
            }

            String resourceId = resourceIdCheckField == null ? "" : resourceIdCheckField.getText().trim();
            boolean hasResource = resourceId != null && !resourceId.isEmpty();
            Resource resource = null;
            if (hasResource) {
                resource = resourceRepository.findById(resourceId).orElse(null);
                if (resource == null) {
                    resultArea.setText("Error: resource not found - " + resourceId);
                    return;
                }
            }

            boolean withinAllLimits = accessLimitService.checkAllLimits(employeeWithGroups);
            int todayCount = accessLimitService.getTodayAccessCount(employeeWithGroups);
            int weekCount = accessLimitService.getWeekAccessCount(employeeWithGroups);

            StringBuilder sb = new StringBuilder();
            sb.append("Access Limit Check\n");
            sb.append("Employee: ").append(employeeId).append(" (")
                .append(employeeWithGroups.getEmployeeName()).append(")\n");
            sb.append("Today Count: ").append(todayCount).append("\n");
            sb.append("Week Count: ").append(weekCount).append("\n");
            sb.append("Within Limits: ").append(withinAllLimits ? "Yes" : "No").append("\n");

            Set<Profile> profiles = getProfilesForEmployee(employeeWithGroups);
            sb.append("\nProfiles (").append(profiles.size()).append(")\n");

            if (profiles.isEmpty()) {
                sb.append("  None\n");
            } else {
                for (Profile profile : profiles) {
                    sb.append("  - ").append(profile.getProfileId())
                        .append(" (").append(profile.getProfileName()).append(")\n");
                    sb.append("    Daily Limit: ")
                        .append(profile.getMaxDailyAccess() != null ? profile.getMaxDailyAccess() : "Unlimited")
                        .append("\n");
                    sb.append("    Weekly Limit: ")
                        .append(profile.getMaxWeeklyAccess() != null ? profile.getMaxWeeklyAccess() : "Unlimited")
                        .append("\n");
                }
            }

            List<ProfileResourceLimit> activeLimits = new ArrayList<>();
            for (Profile profile : profiles) {
                activeLimits.addAll(profileResourceLimitRepository.findByProfileAndIsActiveTrue(profile));
            }

            if (!activeLimits.isEmpty()) {
                sb.append("\nResource Limits\n");
                for (ProfileResourceLimit limit : activeLimits) {
                    if (limit == null || limit.getResource() == null) {
                        continue;
                    }
                    Resource limitResource = limit.getResource();
                    if (hasResource && !limitResource.getResourceId().equals(resource.getResourceId())) {
                        continue;
                    }
                    String profileId = limit.getProfile() != null ? limit.getProfile().getProfileId() : "";
                    Integer dailyLimit = limit.getDailyLimit();
                    Integer weeklyLimit = limit.getWeeklyLimit();
                    int usedDay = accessLimitService.getTodayAccessCount(employeeWithGroups, limitResource);
                    int usedWeek = accessLimitService.getWeekAccessCount(employeeWithGroups, limitResource);
                    boolean dayOk = dailyLimit == null || dailyLimit <= 0 || usedDay < dailyLimit;
                    boolean weekOk = weeklyLimit == null || weeklyLimit <= 0 || usedWeek < weeklyLimit;
                    String status = (dayOk && weekOk) ? "OK" : "Exceeded";
                    sb.append("  - Profile ").append(profileId)
                        .append(" / Resource ").append(limitResource.getResourceId())
                        .append(" (").append(limitResource.getResourceName()).append(")")
                        .append(" | Daily Limit=").append(dailyLimit != null ? dailyLimit : "Unlimited")
                        .append(" | Used Today=").append(usedDay)
                        .append(" | Weekly Limit=").append(weeklyLimit != null ? weeklyLimit : "Unlimited")
                        .append(" | Used This Week=").append(usedWeek)
                        .append(" | Status: ").append(status)
                        .append("\n");
                }
            } else {
                sb.append("\nResource Limits: None\n");
            }
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("Query failed: " + ex.getMessage());
        }
    }

    private void getAccessCounts() {
        String employeeId = employeeIdField.getText().trim();
        String resourceId = resourceIdCheckField == null ? "" : resourceIdCheckField.getText().trim();
        if (employeeId.isEmpty()) {
            resultArea.setText("Error: enter employee ID");
            return;
        }

        try {
            Employee employeeWithGroups = employeeRepository.findByIdWithGroups(employeeId).orElse(null);
            if (employeeWithGroups == null) {
                resultArea.setText("Error: employee not found - " + employeeId);
                return;
            }

            int todayCount = accessLimitService.getTodayAccessCount(employeeWithGroups);
            int weekCount = accessLimitService.getWeekAccessCount(employeeWithGroups);

            Resource resource = null;
            Integer todayResourceCount = null;
            Integer weekResourceCount = null;
            if (!resourceId.isEmpty()) {
                resource = resourceRepository.findById(resourceId).orElse(null);
                if (resource == null) {
                    resultArea.setText("Error: resource not found - " + resourceId);
                    return;
                }
                todayResourceCount = accessLimitService.getTodayAccessCount(employeeWithGroups, resource);
                weekResourceCount = accessLimitService.getWeekAccessCount(employeeWithGroups, resource);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Access Counts\n");
            sb.append("Employee: ").append(employeeId).append(" (")
                .append(employeeWithGroups.getEmployeeName()).append(")\n");
            sb.append("Today Count: ").append(todayCount).append("\n");
            sb.append("Week Count: ").append(weekCount).append("\n");

            if (resource != null) {
                sb.append("Resource: ").append(resource.getResourceId())
                    .append(" (").append(resource.getResourceName()).append(")\n");
                sb.append("Today Count: ").append(todayResourceCount).append("\n");
                sb.append("Week Count: ").append(weekResourceCount).append("\n");
            }

            sb.append("Timestamp: ").append(java.time.LocalDateTime.now()).append("\n");

            Set<Profile> profiles = getProfilesForEmployee(employeeWithGroups);
            if (!profiles.isEmpty()) {
                Integer strictestDailyLimit = null;
                Integer strictestWeeklyLimit = null;

                for (Profile profile : profiles) {
                    Integer dailyLimit = profile.getMaxDailyAccess();
                    Integer weeklyLimit = profile.getMaxWeeklyAccess();
                    if (dailyLimit != null && dailyLimit > 0) {
                        if (strictestDailyLimit == null || dailyLimit < strictestDailyLimit) {
                            strictestDailyLimit = dailyLimit;
                        }
                    }
                    if (weeklyLimit != null && weeklyLimit > 0) {
                        if (strictestWeeklyLimit == null || weeklyLimit < strictestWeeklyLimit) {
                            strictestWeeklyLimit = weeklyLimit;
                        }
                    }
                }

                if (strictestDailyLimit != null) {
                    double dailyPercentage = (double) todayCount / strictestDailyLimit * 100;
                    sb.append("Strictest Daily Limit: ").append(strictestDailyLimit).append("\n");
                    sb.append("Usage: ").append(String.format("%.1f%%", dailyPercentage)).append("\n");
                    if (dailyPercentage >= 90) {
                        sb.append("Note: near limit\n");
                    }
                }

                if (strictestWeeklyLimit != null) {
                    double weeklyPercentage = (double) weekCount / strictestWeeklyLimit * 100;
                    sb.append("Strictest Weekly Limit: ").append(strictestWeeklyLimit).append("\n");
                    sb.append("Usage: ").append(String.format("%.1f%%", weeklyPercentage)).append("\n");
                    if (weeklyPercentage >= 90) {
                        sb.append("Note: near limit\n");
                    }
                }
            }

            List<ProfileResourceLimit> activeLimits = new ArrayList<>();
            for (Profile profile : profiles) {
                activeLimits.addAll(profileResourceLimitRepository.findByProfileAndIsActiveTrue(profile));
            }

            if (!activeLimits.isEmpty()) {
                sb.append("\nResource Limits\n");
                for (ProfileResourceLimit limit : activeLimits) {
                    if (limit == null || limit.getResource() == null) {
                        continue;
                    }
                    Resource limitResource = limit.getResource();
                    if (resource != null && !limitResource.getResourceId().equals(resource.getResourceId())) {
                        continue;
                    }
                    String profileId = limit.getProfile() != null ? limit.getProfile().getProfileId() : "";
                    Integer dailyLimit = limit.getDailyLimit();
                    Integer weeklyLimit = limit.getWeeklyLimit();
                    int usedDay = accessLimitService.getTodayAccessCount(employeeWithGroups, limitResource);
                    int usedWeek = accessLimitService.getWeekAccessCount(employeeWithGroups, limitResource);
                    sb.append("  - Profile ").append(profileId)
                        .append(" / Resource ").append(limitResource.getResourceId())
                        .append(" (").append(limitResource.getResourceName()).append(")")
                        .append(" | Daily Limit=").append(dailyLimit != null ? dailyLimit : "Unlimited")
                        .append(" | Used Today=").append(usedDay)
                        .append(" | Weekly Limit=").append(weeklyLimit != null ? weeklyLimit : "Unlimited")
                        .append(" | Used This Week=").append(usedWeek)
                        .append("\n");
                }
            } else {
                sb.append("\nResource Limits: None\n");
            }
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("Query failed: " + ex.getMessage());
        }
    }

    private void refreshLimitTable() {
        if (employeeRepository == null) {
            return;
        }

        new Thread(() -> {
            try {
                List<Employee> employees = employeeRepository.findAllWithGroups();
                SwingUtilities.invokeLater(() -> {
                    limitTableModel.setRowCount(0);

                    for (Employee employee : employees) {
                        int todayCount = accessLimitService.getTodayAccessCount(employee);
                        int weekCount = accessLimitService.getWeekAccessCount(employee);

                        Set<Profile> profiles = getProfilesForEmployee(employee);
                        int profileCount = profiles.size();

                        String status = "OK";
                        boolean withinLimits = accessLimitService.checkAllLimits(employee);
                        if (!withinLimits) {
                            status = "Exceeded";
                        } else if (!profiles.isEmpty()) {
                            for (Profile profile : profiles) {
                                Integer dailyLimit = profile.getMaxDailyAccess();
                                Integer weeklyLimit = profile.getMaxWeeklyAccess();

                                if (dailyLimit != null && dailyLimit > 0 && todayCount >= dailyLimit * 0.8) {
                                    status = "Near Limit";
                                    break;
                                }
                                if (weeklyLimit != null && weeklyLimit > 0 && weekCount >= weeklyLimit * 0.8) {
                                    status = "Near Limit";
                                    break;
                                }
                            }
                        }

                        limitTableModel.addRow(new Object[]{
                            employee.getEmployeeId(),
                            employee.getEmployeeName(),
                            todayCount,
                            weekCount,
                            profileCount,
                            status
                        });
                    }

                    resultArea.setText("Employees: " + employees.size());
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> resultArea.setText("Refresh failed: " + ex.getMessage()));
            }
        }).start();
    }

    private Set<Profile> getProfilesForEmployee(Employee employee) {
        Set<Profile> profiles = new HashSet<>();
        if (employee == null) {
            return profiles;
        }

        Employee employeeWithGroups = employeeRepository.findByIdWithGroups(employee.getEmployeeId()).orElse(null);
        if (employeeWithGroups == null) {
            return profiles;
        }

        if (employeeWithGroups.getGroups() != null) {
            for (Group group : employeeWithGroups.getGroups()) {
                List<Profile> groupProfiles = profileRepository.findByGroupsContaining(group);
                profiles.addAll(groupProfiles);
            }
        }

        List<Profile> directProfiles = profileRepository.findByEmployeesContaining(employeeWithGroups);
        profiles.addAll(directProfiles);

        if (employeeWithGroups.getBadge() != null) {
            List<Profile> badgeProfiles = profileRepository.findByBadgesContaining(employeeWithGroups.getBadge());
            profiles.addAll(badgeProfiles);
        }

        List<Profile> sortedProfiles = new ArrayList<>(profiles);
        sortedProfiles.sort((p1, p2) -> {
            Integer p1Level = p1.getPriorityLevel();
            Integer p2Level = p2.getPriorityLevel();
            if (p1Level == null && p2Level == null) return 0;
            if (p1Level == null) return 1;
            if (p2Level == null) return -1;
            return Integer.compare(p1Level, p2Level);
        });

        Set<Profile> highestPriorityProfiles = new HashSet<>();
        if (!sortedProfiles.isEmpty()) {
            Integer highestPriority = sortedProfiles.get(0).getPriorityLevel();
            for (Profile profile : sortedProfiles) {
                if (profile.getPriorityLevel() == null || !profile.getPriorityLevel().equals(highestPriority)) {
                    break;
                }
                highestPriorityProfiles.add(profile);
            }
        }

        return highestPriorityProfiles;
    }
}
