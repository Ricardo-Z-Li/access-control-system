package acs.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import acs.service.GroupFileService;
import acs.domain.Group;
import acs.repository.GroupRepository;

public class GroupFilePanel extends JPanel {
    private GroupFileService groupFileService;
    private GroupRepository groupRepository;
    
    private JTable groupTable;
    private DefaultTableModel groupTableModel;
    private JTextField filePathField;
    private JTextArea resultArea;
    
    public GroupFilePanel(GroupFileService groupFileService,
                         GroupRepository groupRepository) {
        this.groupFileService = groupFileService;
        this.groupRepository = groupRepository;
        initUI();
        refreshGroupTable();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("组文件管理面板", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("文件操作", createFileOperationsPanel());
        tabbedPane.addTab("组管理", createGroupManagementPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("组文件服务: " + (groupFileService != null ? "可用" : "不可用")));
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createFileOperationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel filePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        filePanel.add(new JLabel("文件路径:"), gbc);
        
        gbc.gridx = 1;
        filePathField = new JTextField(40);
        filePathField.setText("src/main/resources/groups.txt");
        filePanel.add(filePathField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton loadButton = new JButton("加载组文件");
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadGroupsFromFile();
            }
        });
        buttonPanel.add(loadButton);
        
        JButton validateButton = new JButton("验证文件");
        validateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validateGroupFile();
            }
        });
        buttonPanel.add(validateButton);
        
        JButton mappingButton = new JButton("加载映射关系");
        mappingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadGroupResourceMapping();
            }
        });
        buttonPanel.add(mappingButton);
        
        JButton browseButton = new JButton("浏览...");
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseFile();
            }
        });
        buttonPanel.add(browseButton);
        
        filePanel.add(buttonPanel, gbc);
        
        panel.add(filePanel, BorderLayout.NORTH);
        
        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("宋体", Font.PLAIN, 12));
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createGroupManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"组ID", "组名称", "成员数", "资源数"};
        groupTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        groupTable = new JTable(groupTableModel);
        groupTable.setAutoCreateRowSorter(true);
        
        JScrollPane scrollPane = new JScrollPane(groupTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("刷新表格");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshGroupTable();
            }
        });
        controlPanel.add(refreshButton);
        
        JButton exportButton = new JButton("导出到文件");
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportToFile();
            }
        });
        controlPanel.add(exportButton);
        
        JButton detailsButton = new JButton("查看详情");
        detailsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showGroupDetails();
            }
        });
        controlPanel.add(detailsButton);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    private void loadGroupsFromFile() {
        String filePath = filePathField.getText().trim();
        
        if (filePath.isEmpty()) {
            resultArea.setText("错误: 请输入文件路径");
            return;
        }
        
        try {
            List<Group> groups = groupFileService.loadGroupsFromFile(filePath);
            
            StringBuilder sb = new StringBuilder();
            sb.append("组文件加载成功:\n");
            sb.append("文件路径: ").append(filePath).append("\n");
            sb.append("加载组数: ").append(groups.size()).append("\n");
            sb.append("===============================\n");
            
            for (Group group : groups) {
                sb.append("组ID: ").append(group.getGroupId()).append("\n");
                sb.append("组名称: ").append(group.getName()).append("\n");
                sb.append("成员数: ").append(group.getEmployees() != null ? group.getEmployees().size() : 0).append("\n");
                sb.append("资源数: ").append(group.getResources() != null ? group.getResources().size() : 0).append("\n");
                // Group实体没有描述字段，跳过描述显示
                sb.append("------------------------------\n");
            }
            
            resultArea.setText(sb.toString());
            refreshGroupTable();
        } catch (Exception ex) {
            resultArea.setText("加载组文件失败: " + ex.getMessage() + "\n\n文件格式应为:\ngroupId:groupName:resourceId1,resourceId2,...");
        }
    }
    
    private void validateGroupFile() {
        String filePath = filePathField.getText().trim();
        
        if (filePath.isEmpty()) {
            resultArea.setText("错误: 请输入文件路径");
            return;
        }
        
        try {
            boolean valid = groupFileService.validateGroupFile(filePath);
            
            if (valid) {
                resultArea.setText("文件验证通过: " + filePath + "\n\n文件格式正确，可以被成功解析。");
            } else {
                resultArea.setText("文件验证失败: " + filePath + "\n\n文件格式错误，请检查文件内容。\n\n正确格式示例:\nGROUP001:Administrators:RES001,RES002,RES003\nGROUP002:Engineering:RES004,RES005,RES006");
            }
        } catch (Exception ex) {
            resultArea.setText("验证过程中出错: " + ex.getMessage());
        }
    }
    
    private void loadGroupResourceMapping() {
        String filePath = filePathField.getText().trim();
        
        if (filePath.isEmpty()) {
            resultArea.setText("错误: 请输入文件路径");
            return;
        }
        
        try {
            Map<String, List<String>> mapping = groupFileService.loadGroupResourceMapping(filePath);
            
            StringBuilder sb = new StringBuilder();
            sb.append("组-资源映射关系:\n");
            sb.append("文件路径: ").append(filePath).append("\n");
            sb.append("映射条目: ").append(mapping.size()).append("\n");
            sb.append("===============================\n");
            
            for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
                String groupId = entry.getKey();
                List<String> resources = entry.getValue();
                
                sb.append("组ID: ").append(groupId).append("\n");
                sb.append("可访问资源 (").append(resources.size()).append(" 个):\n");
                
                int count = 0;
                for (String resource : resources) {
                    sb.append("  - ").append(resource);
                    if (++count % 3 == 0) sb.append("\n");
                    else if (count < resources.size()) sb.append(", ");
                }
                if (resources.size() % 3 != 0) sb.append("\n");
                sb.append("------------------------------\n");
            }
            
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("加载映射关系失败: " + ex.getMessage());
        }
    }
    
    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择组文件");
        fileChooser.setCurrentDirectory(new java.io.File("."));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void refreshGroupTable() {
        if (groupRepository == null) {
            return;
        }
        
        new Thread(() -> {
            try {
                List<Group> groups = groupRepository.findAllWithEmployeesAndResources();
                SwingUtilities.invokeLater(() -> {
                    groupTableModel.setRowCount(0);
                    
                    for (Group group : groups) {
                        int memberCount = group.getEmployees() != null ? group.getEmployees().size() : 0;
                        int resourceCount = group.getResources() != null ? group.getResources().size() : 0;
                        
                        groupTableModel.addRow(new Object[]{
                            group.getGroupId(),
                            group.getName(),
                            memberCount,
                            resourceCount
                        });
                    }
                    
                    resultArea.setText("组列表已刷新，共 " + groups.size() + " 个组");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    resultArea.setText("刷新表格失败: " + ex.getMessage());
                });
            }
        }).start();
    }
    
    private void exportToFile() {
        // 简化实现：显示导出提示
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出组数据");
        fileChooser.setCurrentDirectory(new java.io.File("."));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            resultArea.setText("导出功能: 数据将导出到 " + selectedFile.getAbsolutePath() + 
                             "\n当前时间: " + java.time.LocalDateTime.now() + 
                             "\n总组数: " + groupTableModel.getRowCount());
        }
    }
    
    private void showGroupDetails() {
        int selectedRow = groupTable.getSelectedRow();
        if (selectedRow == -1) {
            resultArea.setText("错误: 请先选择一行");
            return;
        }
        
        int modelRow = groupTable.convertRowIndexToModel(selectedRow);
        String groupId = (String) groupTableModel.getValueAt(modelRow, 0);
        
        try {
            Group group = groupRepository.findByIdWithEmployeesAndResources(groupId).orElse(null);
            if (group == null) {
                resultArea.setText("错误: 组不存在 - " + groupId);
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("组详细信息:\n");
            sb.append("组ID: ").append(group.getGroupId()).append("\n");
            sb.append("组名称: ").append(group.getName()).append("\n");
            // Group实体没有描述、创建时间、更新时间字段
            sb.append("描述: 无\n");
            sb.append("创建时间: 未知\n");
            sb.append("更新时间: 未知\n");
            sb.append("===============================\n");
            
            // 成员信息
            sb.append("成员 (").append(group.getEmployees() != null ? group.getEmployees().size() : 0).append(" 人):\n");
            if (group.getEmployees() != null && !group.getEmployees().isEmpty()) {
                int count = 0;
                for (var employee : group.getEmployees()) {
                    sb.append("  ").append(++count).append(". ").append(employee.getEmployeeId())
                      .append(" (").append(employee.getEmployeeName()).append(")\n");
                }
            } else {
                sb.append("  无成员\n");
            }
            
            sb.append("===============================\n");
            
            // 资源信息
            sb.append("可访问资源 (").append(group.getResources() != null ? group.getResources().size() : 0).append(" 个):\n");
            if (group.getResources() != null && !group.getResources().isEmpty()) {
                int count = 0;
                for (var resource : group.getResources()) {
                    sb.append("  ").append(++count).append(". ").append(resource.getResourceId())
                      .append(" (").append(resource.getResourceName()).append(") - ")
                      .append(resource.getResourceState()).append("\n");
                }
            } else {
                sb.append("  无资源\n");
            }
            
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("获取组详情失败: " + ex.getMessage());
        }
    }
}