package acs.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import acs.service.GroupFileService;
import acs.domain.Group;
import acs.repository.GroupRepository;

public class GroupFilePanel extends JPanel {
    private final GroupFileService groupFileService;
    private final GroupRepository groupRepository;

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

        JLabel titleLabel = new JLabel("群组文件管理", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("文件操作", createFileOperationsPanel());
        tabbedPane.addTab("群组管理", createGroupManagementPanel());
        add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("服务状态: " + (groupFileService != null ? "可用" : "不可用")));
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

        JButton loadButton = new JButton("加载文件");
        loadButton.addActionListener(e -> loadGroupsFromFile());
        buttonPanel.add(loadButton);

        JButton validateButton = new JButton("校验文件");
        validateButton.addActionListener(e -> validateGroupFile());
        buttonPanel.add(validateButton);

        JButton mappingButton = new JButton("加载映射");
        mappingButton.addActionListener(e -> loadGroupResourceMapping());
        buttonPanel.add(mappingButton);

        JButton browseButton = new JButton("浏览...");
        browseButton.addActionListener(e -> browseFile());
        buttonPanel.add(browseButton);

        filePanel.add(buttonPanel, gbc);
        panel.add(filePanel, BorderLayout.NORTH);

        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGroupManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"组ID", "组名", "成员数", "资源数"};
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
        JButton refreshButton = new JButton("刷新列表");
        refreshButton.addActionListener(e -> refreshGroupTable());
        controlPanel.add(refreshButton);

        JButton exportButton = new JButton("导出文件");
        exportButton.addActionListener(e -> exportToFile());
        controlPanel.add(exportButton);

        JButton detailsButton = new JButton("查看详情");
        detailsButton.addActionListener(e -> showGroupDetails());
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
            sb.append("加载结果\n");
            sb.append("文件路径: ").append(filePath).append("\n");
            sb.append("群组数量: ").append(groups.size()).append("\n");
            sb.append("===============================\n");

            for (Group group : groups) {
                sb.append("组ID: ").append(group.getGroupId()).append("\n");
                sb.append("组名: ").append(group.getName()).append("\n");
                sb.append("成员数: ").append(group.getEmployees() != null ? group.getEmployees().size() : 0).append("\n");
                sb.append("资源数: ").append(group.getResources() != null ? group.getResources().size() : 0).append("\n");
                sb.append("------------------------------\n");
            }

            resultArea.setText(sb.toString());
            refreshGroupTable();
        } catch (Exception ex) {
            resultArea.setText("文件解析失败: " + ex.getMessage() +
                "\n\n示例格式:\nGROUP001:Administrators:RES001,RES002,RES003");
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
                resultArea.setText("校验通过: " + filePath + "\n\n文件格式正确。");
            } else {
                resultArea.setText("校验失败: " + filePath +
                    "\n\n文件格式不符合要求。\n\n示例:\nGROUP001:Administrators:RES001,RES002,RES003");
            }
        } catch (Exception ex) {
            resultArea.setText("校验失败: " + ex.getMessage());
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
            sb.append("组-资源映射:\n");
            sb.append("文件路径: ").append(filePath).append("\n");
            sb.append("群组数量: ").append(mapping.size()).append("\n");
            sb.append("===============================\n");

            for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
                String groupId = entry.getKey();
                List<String> resources = entry.getValue();

                sb.append("组ID: ").append(groupId).append("\n");
                sb.append("资源列表(").append(resources.size()).append("):\n");

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
            resultArea.setText("加载映射失败: " + ex.getMessage());
        }
    }

    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择群组文件");
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

                    resultArea.setText("已加载群组 " + groups.size() + " 个");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> resultArea.setText("刷新失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void exportToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出群组文件");
        fileChooser.setCurrentDirectory(new java.io.File("."));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            resultArea.setText("导出准备: 将导出到 " + selectedFile.getAbsolutePath() +
                             "\n时间: " + java.time.LocalDateTime.now() +
                             "\n记录数: " + groupTableModel.getRowCount());
        }
    }

    private void showGroupDetails() {
        int selectedRow = groupTable.getSelectedRow();
        if (selectedRow == -1) {
            resultArea.setText("错误: 请选择群组");
            return;
        }

        int modelRow = groupTable.convertRowIndexToModel(selectedRow);
        String groupId = (String) groupTableModel.getValueAt(modelRow, 0);

        try {
            Group group = groupRepository.findByIdWithEmployeesAndResources(groupId).orElse(null);
            if (group == null) {
                resultArea.setText("错误: 未找到群组 - " + groupId);
                return;
            }

            StringBuilder sb = new StringBuilder();
            int memberCount = group.getEmployees() != null ? group.getEmployees().size() : 0;
            int resourceCount = group.getResources() != null ? group.getResources().size() : 0;
            sb.append("群组详情\n");
            sb.append("组ID: ").append(group.getGroupId()).append("\n");
            sb.append("组名: ").append(group.getName()).append("\n");
            sb.append("状态: 未知\n");
            sb.append("成员数: ").append(memberCount).append("\n");
            sb.append("资源数: ").append(resourceCount).append("\n");
            sb.append("===============================\n");

            sb.append("成员 (").append(memberCount).append(" 人):\n");
            if (group.getEmployees() != null && !group.getEmployees().isEmpty()) {
                int count = 0;
                for (var employee : group.getEmployees()) {
                    sb.append("  ").append(++count).append(". ")
                      .append(employee.getEmployeeId())
                      .append(" (").append(employee.getEmployeeName()).append(")\n");
                }
            } else {
                sb.append("  无\n");
            }

            sb.append("===============================\n");

            sb.append("资源列表 (").append(resourceCount).append(" 个):\n");
            if (group.getResources() != null && !group.getResources().isEmpty()) {
                int count = 0;
                for (var resource : group.getResources()) {
                    sb.append("  ").append(++count).append(". ")
                      .append(resource.getResourceId())
                      .append(" (").append(resource.getResourceName()).append(") - ")
                      .append(resource.getResourceState()).append("\n");
                }
            } else {
                sb.append("  无\n");
            }

            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("查询失败: " + ex.getMessage());
        }
    }
}
