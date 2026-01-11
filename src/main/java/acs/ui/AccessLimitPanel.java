package acs.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import acs.service.AccessLimitService;
import acs.service.AdminService;
import acs.domain.Employee;
import acs.domain.Profile;
import acs.domain.Group;
import acs.repository.EmployeeRepository;
import acs.repository.ProfileRepository;

public class AccessLimitPanel extends JPanel {
    private AccessLimitService accessLimitService;
    private EmployeeRepository employeeRepository;
    private ProfileRepository profileRepository;
    
    private JTable limitTable;
    private DefaultTableModel limitTableModel;
    private JTextField employeeIdField;
    private JTextArea resultArea;
    
    public AccessLimitPanel(AccessLimitService accessLimitService,
                           EmployeeRepository employeeRepository,
                           ProfileRepository profileRepository) {
        this.accessLimitService = accessLimitService;
        this.employeeRepository = employeeRepository;
        this.profileRepository = profileRepository;
        initUI();
        refreshLimitTable();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("访问限制管理面板", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("限制检查", createLimitCheckPanel());
        tabbedPane.addTab("员工统计", createEmployeeStatsPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("访问限制服务: " + (accessLimitService != null ? "可用" : "不可用")));
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
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton checkAllButton = new JButton("检查所有限制");
        checkAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkAllLimits();
            }
        });
        buttonPanel.add(checkAllButton);
        
        JButton getCountsButton = new JButton("获取访问次数");
        getCountsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getAccessCounts();
            }
        });
        buttonPanel.add(getCountsButton);
        
        inputPanel.add(buttonPanel, gbc);
        
        panel.add(inputPanel, BorderLayout.NORTH);
        
        resultArea = new JTextArea(15, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("宋体", Font.PLAIN, 12));
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createEmployeeStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"员工ID", "员工姓名", "今日访问", "本周访问", "配置文件数", "状态"};
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
        JButton refreshButton = new JButton("刷新表格");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshLimitTable();
            }
        });
        controlPanel.add(refreshButton);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    private void checkAllLimits() {
        String employeeId = employeeIdField.getText().trim();
        
        if (employeeId.isEmpty()) {
            resultArea.setText("错误: 请输入员工ID");
            return;
        }
        
        try {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            
            if (employee == null) {
                resultArea.setText("错误: 员工不存在 - " + employeeId);
                return;
            }
            
            boolean withinAllLimits = accessLimitService.checkAllLimits(employee);
            int todayCount = accessLimitService.getTodayAccessCount(employee);
            int weekCount = accessLimitService.getWeekAccessCount(employee);
            
            StringBuilder sb = new StringBuilder();
            sb.append("所有限制检查结果:\n");
            sb.append("员工: ").append(employeeId).append(" (").append(employee.getEmployeeName()).append(")\n");
            sb.append("今日访问次数: ").append(todayCount).append("\n");
            sb.append("本周访问次数: ").append(weekCount).append("\n");
            sb.append("检查结果: ").append(withinAllLimits ? "✅ 所有限制都满足" : "❌ 超过某些限制").append("\n");
            
            // 获取员工的配置文件信息（通过组）
            Set<Profile> profiles = getProfilesForEmployee(employee);
            sb.append("\n关联配置文件 (").append(profiles.size()).append(" 个):\n");
            
            if (profiles.isEmpty()) {
                sb.append("  无配置文件关联\n");
            } else {
                for (Profile profile : profiles) {
                    sb.append("  - ").append(profile.getProfileId())
                      .append(" (").append(profile.getProfileName()).append(")\n");
                    sb.append("    每日限制: ").append(profile.getMaxDailyAccess() != null ? profile.getMaxDailyAccess() : "无限制").append("\n");
                    sb.append("    每周限制: ").append(profile.getMaxWeeklyAccess() != null ? profile.getMaxWeeklyAccess() : "无限制").append("\n");
                }
            }
            
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("检查失败: " + ex.getMessage());
        }
    }
    
    private void getAccessCounts() {
        String employeeId = employeeIdField.getText().trim();
        
        if (employeeId.isEmpty()) {
            resultArea.setText("错误: 请输入员工ID");
            return;
        }
        
        try {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            
            if (employee == null) {
                resultArea.setText("错误: 员工不存在 - " + employeeId);
                return;
            }
            
            int todayCount = accessLimitService.getTodayAccessCount(employee);
            int weekCount = accessLimitService.getWeekAccessCount(employee);
            
            StringBuilder sb = new StringBuilder();
            sb.append("访问次数统计:\n");
            sb.append("员工: ").append(employeeId).append(" (").append(employee.getEmployeeName()).append(")\n");
            sb.append("今日访问次数: ").append(todayCount).append("\n");
            sb.append("本周访问次数: ").append(weekCount).append("\n");
            sb.append("统计时间: ").append(java.time.LocalDateTime.now()).append("\n");
            
            // 检查是否接近限制（通过所有配置文件）
            Set<Profile> profiles = getProfilesForEmployee(employee);
            if (!profiles.isEmpty()) {
                // 找到最严格的限制
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
                    sb.append("最严格每日限制: ").append(strictestDailyLimit).append("\n");
                    sb.append("每日使用率: ").append(String.format("%.1f%%", dailyPercentage)).append("\n");
                    if (dailyPercentage >= 90) {
                        sb.append("警告: 接近每日限制!\n");
                    }
                }
                
                if (strictestWeeklyLimit != null) {
                    double weeklyPercentage = (double) weekCount / strictestWeeklyLimit * 100;
                    sb.append("最严格每周限制: ").append(strictestWeeklyLimit).append("\n");
                    sb.append("本周使用率: ").append(String.format("%.1f%%", weeklyPercentage)).append("\n");
                    if (weeklyPercentage >= 90) {
                        sb.append("警告: 接近每周限制!\n");
                    }
                }
            }
            
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("获取次数失败: " + ex.getMessage());
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
                        
                        // 检查状态
                        String status = "正常";
                        boolean withinLimits = accessLimitService.checkAllLimits(employee);
                        if (!withinLimits) {
                            status = "超过限制";
                        } else if (!profiles.isEmpty()) {
                            // 检查是否接近限制
                            for (Profile profile : profiles) {
                                Integer dailyLimit = profile.getMaxDailyAccess();
                                Integer weeklyLimit = profile.getMaxWeeklyAccess();
                                
                                if (dailyLimit != null && dailyLimit > 0 && todayCount >= dailyLimit * 0.8) {
                                    status = "接近每日限制";
                                    break;
                                }
                                if (weeklyLimit != null && weeklyLimit > 0 && weekCount >= weeklyLimit * 0.8) {
                                    status = "接近每周限制";
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
                    
                    resultArea.setText("表格已刷新，共 " + employees.size() + " 名员工");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    resultArea.setText("刷新表格失败: " + ex.getMessage());
                });
            }
        }).start();
    }
    
    private Set<Profile> getProfilesForEmployee(Employee employee) {
        Set<Profile> profiles = new HashSet<>();
        
        if (employee == null) {
            return profiles;
        }
        
        // Reload employee with groups to avoid lazy initialization exception
        Employee employeeWithGroups = employeeRepository.findByIdWithGroups(employee.getEmployeeId()).orElse(null);
        if (employeeWithGroups == null || employeeWithGroups.getGroups() == null) {
            return profiles;
        }
        
        for (Group group : employeeWithGroups.getGroups()) {
            List<Profile> groupProfiles = profileRepository.findByGroupsContaining(group);
            profiles.addAll(groupProfiles);
        }
        
        return profiles;
    }
}