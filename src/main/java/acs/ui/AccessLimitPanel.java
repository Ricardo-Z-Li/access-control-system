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

        JLabel titleLabel = new JLabel("访问限制管理", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("限制检查", createLimitCheckPanel());
        tabbedPane.addTab("员工统计", createEmployeeStatsPanel());
        tabbedPane.addTab("资源限额", createResourceLimitPanel());

        add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("服务状态: " + (accessLimitService != null ? "可用" : "不可用")));
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createLimitCheckPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("员工ID:"), gbc);

        gbc.gridx = 1;
        employeeIdField = new JTextField(20);
        employeeIdField.setText("EMP001");
        inputPanel.add(employeeIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("资源ID(可选):"), gbc);

        gbc.gridx = 1;
        resourceIdCheckField = new JTextField(20);
        inputPanel.add(resourceIdCheckField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton checkAllButton = new JButton("检查全部限制");
        checkAllButton.addActionListener(e -> checkAllLimits());
        buttonPanel.add(checkAllButton);

        JButton getCountsButton = new JButton("查询访问计数");
        getCountsButton.addActionListener(e -> getAccessCounts());
        buttonPanel.add(getCountsButton);

        inputPanel.add(buttonPanel, gbc);

        panel.add(inputPanel, BorderLayout.NORTH);

        resultArea = new JTextArea(15, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEmployeeStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"员工ID", "姓名", "今日次数", "本周次数", "Profile数", "状态"};
        limitTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        limitTable = new JTable(limitTableModel);
        limitTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(limitTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshLimitTable());
        controlPanel.add(refreshButton);

        panel.add(controlPanel, BorderLayout.NORTH);

        return panel;
    }

    private JPanel createResourceLimitPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Profile ID:"), gbc);
        gbc.gridx = 1;
        profileIdField = new JTextField(12);
        inputPanel.add(profileIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("资源ID:"), gbc);
        gbc.gridx = 1;
        resourceIdField = new JTextField(12);
        inputPanel.add(resourceIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("每日上限:"), gbc);
        gbc.gridx = 1;
        dailyLimitField = new JTextField(12);
        inputPanel.add(dailyLimitField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(new JLabel("每周上限:"), gbc);
        gbc.gridx = 1;
        weeklyLimitField = new JTextField(12);
        inputPanel.add(weeklyLimitField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        inputPanel.add(new JLabel("是否启用:"), gbc);
        gbc.gridx = 1;
        activeCheckBox = new JCheckBox();
        activeCheckBox.setSelected(true);
        inputPanel.add(activeCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton upsertButton = new JButton("保存/更新");
        upsertButton.addActionListener(e -> upsertResourceLimit());
        buttonPanel.add(upsertButton);

        JButton deactivateButton = new JButton("停用");
        deactivateButton.addActionListener(e -> deactivateResourceLimit());
        buttonPanel.add(deactivateButton);

        JButton deleteButton = new JButton("删除");
        deleteButton.addActionListener(e -> deleteResourceLimit());
        buttonPanel.add(deleteButton);

        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshResourceLimitTable());
        buttonPanel.add(refreshButton);

        inputPanel.add(buttonPanel, gbc);

        panel.add(inputPanel, BorderLayout.NORTH);

        String[] columns = {"ID", "Profile", "资源", "每日上限", "每周上限", "启用"};
        resourceLimitTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resourceLimitTable = new JTable(resourceLimitTableModel);
        resourceLimitTable.setAutoCreateRowSorter(true);
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
            JOptionPane.showMessageDialog(this, "请输入Profile ID和资源ID");
            return;
        }

        Integer daily = parseOptionalInt(dailyLimitField.getText().trim());
        Integer weekly = parseOptionalInt(weeklyLimitField.getText().trim());

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile不存在: " + profileId));
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("资源不存在: " + resourceId));

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
            JOptionPane.showMessageDialog(this, "请选择要操作的记录");
            return;
        }
        ProfileResourceLimit limit = profileResourceLimitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("限制不存在: " + id));
        limit.setIsActive(false);
        profileResourceLimitRepository.save(limit);
        refreshResourceLimitTable();
    }

    private void deleteResourceLimit() {
        Long id = getSelectedLimitId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "请选择要操作的记录");
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
            throw new IllegalArgumentException("无效数字: " + value);
        }
    }

    private void checkAllLimits() {
        String employeeId = employeeIdField.getText().trim();
        if (employeeId.isEmpty()) {
            resultArea.setText("错误: 请输入员工ID");
            return;
        }

        try {
            Employee employeeWithGroups = employeeRepository.findByIdWithGroups(employeeId).orElse(null);
            if (employeeWithGroups == null) {
                resultArea.setText("错误: 未找到员工 - " + employeeId);
                return;
            }

            String resourceId = resourceIdCheckField == null ? "" : resourceIdCheckField.getText().trim();
            boolean hasResource = resourceId != null && !resourceId.isEmpty();
            Resource resource = null;
            if (hasResource) {
                resource = resourceRepository.findById(resourceId).orElse(null);
                if (resource == null) {
                    resultArea.setText("错误: 未找到资源 - " + resourceId);
                    return;
                }
            }

            boolean withinAllLimits = accessLimitService.checkAllLimits(employeeWithGroups);
            int todayCount = accessLimitService.getTodayAccessCount(employeeWithGroups);
            int weekCount = accessLimitService.getWeekAccessCount(employeeWithGroups);

            StringBuilder sb = new StringBuilder();
            sb.append("访问限制检查结果\n");
            sb.append("员工: ").append(employeeId).append(" (")
                .append(employeeWithGroups.getEmployeeName()).append(")\n");
            sb.append("今日次数: ").append(todayCount).append("\n");
            sb.append("本周次数: ").append(weekCount).append("\n");
            sb.append("是否合规: ").append(withinAllLimits ? "是" : "否").append("\n");

            Set<Profile> profiles = getProfilesForEmployee(employeeWithGroups);
            sb.append("\nProfile列表 (").append(profiles.size()).append(")\n");

            if (profiles.isEmpty()) {
                sb.append("  无\n");
            } else {
                for (Profile profile : profiles) {
                    sb.append("  - ").append(profile.getProfileId())
                        .append(" (").append(profile.getProfileName()).append(")\n");
                    sb.append("    每日上限: ")
                        .append(profile.getMaxDailyAccess() != null ? profile.getMaxDailyAccess() : "不限")
                        .append("\n");
                    sb.append("    每周上限: ")
                        .append(profile.getMaxWeeklyAccess() != null ? profile.getMaxWeeklyAccess() : "不限")
                        .append("\n");
                }
            }


            List<ProfileResourceLimit> activeLimits = new ArrayList<>();
            for (Profile profile : profiles) {
                activeLimits.addAll(profileResourceLimitRepository.findByProfileAndIsActiveTrue(profile));
            }

            if (!activeLimits.isEmpty()) {
                sb.append("\n资源限制明细\n");
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
                    String status = (dayOk && weekOk) ? "合规" : "超限";
                    sb.append("  - Profile ").append(profileId)
                        .append(" / 资源 ").append(limitResource.getResourceId())
                        .append(" (").append(limitResource.getResourceName()).append(")")
                        .append(" | 日上限=").append(dailyLimit != null ? dailyLimit : "不限")
                        .append(" | 今日已用=").append(usedDay)
                        .append(" | 周上限=").append(weeklyLimit != null ? weeklyLimit : "不限")
                        .append(" | 本周已用=").append(usedWeek)
                        .append(" | 状态=").append(status)
                        .append("\n");
                }
            } else {
                sb.append("\n资源限制: 无\n");
            }
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("查询失败: " + ex.getMessage());
        }
    }

    private void getAccessCounts() {
        String employeeId = employeeIdField.getText().trim();
        String resourceId = resourceIdCheckField == null ? "" : resourceIdCheckField.getText().trim();
        if (employeeId.isEmpty()) {
            resultArea.setText("错误: 请输入员工ID");
            return;
        }

        try {
            Employee employeeWithGroups = employeeRepository.findByIdWithGroups(employeeId).orElse(null);
            if (employeeWithGroups == null) {
                resultArea.setText("错误: 未找到员工 - " + employeeId);
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
                    resultArea.setText("错误: 未找到资源 - " + resourceId);
                    return;
                }
                todayResourceCount = accessLimitService.getTodayAccessCount(employeeWithGroups, resource);
                weekResourceCount = accessLimitService.getWeekAccessCount(employeeWithGroups, resource);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("访问次数统计\n");
            sb.append("员工: ").append(employeeId).append(" (")
                .append(employeeWithGroups.getEmployeeName()).append(")\n");
            sb.append("今日次数: ").append(todayCount).append("\n");
            sb.append("本周次数: ").append(weekCount).append("\n");

            if (resource != null) {
                sb.append("资源: ").append(resource.getResourceId())
                    .append(" (").append(resource.getResourceName()).append(")\n");
                sb.append("今日次数: ").append(todayResourceCount).append("\n");
                sb.append("本周次数: ").append(weekResourceCount).append("\n");
            }

            sb.append("统计时间: ").append(java.time.LocalDateTime.now()).append("\n");

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
                    sb.append("最严每日上限: ").append(strictestDailyLimit).append("\n");
                    sb.append("今日占用比例: ").append(String.format("%.1f%%", dailyPercentage)).append("\n");
                    if (dailyPercentage >= 90) {
                        sb.append("提示: 已接近日上限\n");
                    }
                }

                if (strictestWeeklyLimit != null) {
                    double weeklyPercentage = (double) weekCount / strictestWeeklyLimit * 100;
                    sb.append("最严每周上限: ").append(strictestWeeklyLimit).append("\n");
                    sb.append("本周占用比例: ").append(String.format("%.1f%%", weeklyPercentage)).append("\n");
                    if (weeklyPercentage >= 90) {
                        sb.append("提示: 已接近周上限\n");
                    }
                }
            }


            List<ProfileResourceLimit> activeLimits = new ArrayList<>();
            for (Profile profile : profiles) {
                activeLimits.addAll(profileResourceLimitRepository.findByProfileAndIsActiveTrue(profile));
            }

            if (!activeLimits.isEmpty()) {
                sb.append("\n资源限制明细\n");
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
                        .append(" / 资源 ").append(limitResource.getResourceId())
                        .append(" (").append(limitResource.getResourceName()).append(")")
                        .append(" | 日上限=").append(dailyLimit != null ? dailyLimit : "不限")
                        .append(" | 今日已用=").append(usedDay)
                        .append(" | 周上限=").append(weeklyLimit != null ? weeklyLimit : "不限")
                        .append(" | 本周已用=").append(usedWeek)
                        .append("\n");
                }
            } else {
                sb.append("\n资源限制: 无\n");
            }
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("查询失败: " + ex.getMessage());
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

                        String status = "正常";
                        boolean withinLimits = accessLimitService.checkAllLimits(employee);
                        if (!withinLimits) {
                            status = "超限";
                        } else if (!profiles.isEmpty()) {
                            for (Profile profile : profiles) {
                                Integer dailyLimit = profile.getMaxDailyAccess();
                                Integer weeklyLimit = profile.getMaxWeeklyAccess();

                                if (dailyLimit != null && dailyLimit > 0 && todayCount >= dailyLimit * 0.8) {
                                    status = "接近日上限";
                                    break;
                                }
                                if (weeklyLimit != null && weeklyLimit > 0 && weekCount >= weeklyLimit * 0.8) {
                                    status = "接近周上限";
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

                    resultArea.setText("员工数量: " + employees.size());
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> resultArea.setText("刷新失败: " + ex.getMessage()));
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
