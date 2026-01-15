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
        add(UiTheme.createHeader("Group Files", "Import group resource files and manage groups"), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("File Operations", createFileOperationsPanel());
        tabbedPane.addTab("Group Management", createGroupManagementPanel());
        add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Service Status: " + (groupFileService != null ? "OK" : "Unavailable")));
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createFileOperationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
        filePathField = new JTextField(40);
        filePathField.setText("src/main/resources/groups.txt");
        filePanel.add(UiTheme.formRow("File Path", filePathField));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton loadButton = UiTheme.primaryButton("Load File");
        loadButton.addActionListener(e -> loadGroupsFromFile());
        JButton validateButton = UiTheme.secondaryButton("Validate File");
        validateButton.addActionListener(e -> validateGroupFile());
        JButton mappingButton = UiTheme.secondaryButton("View Mapping");
        mappingButton.addActionListener(e -> loadGroupResourceMapping());
        JButton browseButton = UiTheme.secondaryButton("Browse...");
        browseButton.addActionListener(e -> browseFile());
        buttonPanel.add(loadButton);
        buttonPanel.add(validateButton);
        buttonPanel.add(mappingButton);
        buttonPanel.add(browseButton);

        JPanel card = UiTheme.cardPanel();
        card.add(filePanel, BorderLayout.CENTER);
        card.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(UiTheme.wrapContent(card), BorderLayout.NORTH);

        resultArea = new JTextArea(20, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGroupManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Group ID", "Group Name", "Members", "Resources"};
        groupTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        groupTable = new JTable(groupTableModel);
        groupTable.setAutoCreateRowSorter(true);
        UiTheme.styleTable(groupTable);

        JScrollPane scrollPane = new JScrollPane(groupTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton refreshButton = UiTheme.secondaryButton("Refresh List");
        refreshButton.addActionListener(e -> refreshGroupTable());
        JButton exportButton = UiTheme.secondaryButton("Export File");
        exportButton.addActionListener(e -> exportToFile());
        JButton detailsButton = UiTheme.secondaryButton("View Details");
        detailsButton.addActionListener(e -> showGroupDetails());
        controlPanel.add(refreshButton);
        controlPanel.add(exportButton);
        controlPanel.add(detailsButton);

        panel.add(controlPanel, BorderLayout.NORTH);

        return panel;
    }

    private void loadGroupsFromFile() {
        String filePath = filePathField.getText().trim();

        if (filePath.isEmpty()) {
            resultArea.setText("Error: enter file path");
            return;
        }

        try {
            List<Group> groups = groupFileService.loadGroupsFromFile(filePath);

            StringBuilder sb = new StringBuilder();
            sb.append("Load completed\n");
            sb.append("File Path: ").append(filePath).append("\n");
            sb.append("Groups: ").append(groups.size()).append("\n");
            sb.append("===============================\n");

            for (Group group : groups) {
                sb.append("Group ID: ").append(group.getGroupId()).append("\n");
                sb.append("Group Name: ").append(group.getName()).append("\n");
                sb.append("Members: ").append(group.getEmployees() != null ? group.getEmployees().size() : 0).append("\n");
                sb.append("Resources: ").append(group.getResources() != null ? group.getResources().size() : 0).append("\n");
                sb.append("------------------------------\n");
            }

            resultArea.setText(sb.toString());
            refreshGroupTable();
        } catch (Exception ex) {
            resultArea.setText("Load failed: " + ex.getMessage() +
                "\n\nExample:\nGROUP001:Administrators:RES001,RES002,RES003");
        }
    }

    private void validateGroupFile() {
        String filePath = filePathField.getText().trim();

        if (filePath.isEmpty()) {
            resultArea.setText("Error: enter file path");
            return;
        }

        try {
            boolean valid = groupFileService.validateGroupFile(filePath);

            if (valid) {
                resultArea.setText("Validation passed: " + filePath + "\n\nFile format looks valid");
            } else {
                resultArea.setText("Validation failed: " + filePath +
                    "\n\nFile format may be invalid\n\nExample:\nGROUP001:Administrators:RES001,RES002,RES003");
            }
        } catch (Exception ex) {
            resultArea.setText("Validation failed: " + ex.getMessage());
        }
    }

    private void loadGroupResourceMapping() {
        String filePath = filePathField.getText().trim();

        if (filePath.isEmpty()) {
            resultArea.setText("Error: enter file path");
            return;
        }

        try {
            Map<String, List<String>> mapping = groupFileService.loadGroupResourceMapping(filePath);

            StringBuilder sb = new StringBuilder();
            sb.append("Group-Resource Mapping:\n");
            sb.append("File Path: ").append(filePath).append("\n");
            sb.append("Groups: ").append(mapping.size()).append("\n");
            sb.append("===============================\n");

            for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
                String groupId = entry.getKey();
                List<String> resources = entry.getValue();

                sb.append("Group ID: ").append(groupId).append("\n");
                sb.append("Resources (").append(resources.size()).append("):\n");

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
            resultArea.setText("Failed to load mapping: " + ex.getMessage());
        }
    }

    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Group File");
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

                    resultArea.setText("Loaded groups: " + groups.size());
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> resultArea.setText("Refresh failed: " + ex.getMessage()));
            }
        }).start();
    }

    private void exportToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Group File");
        fileChooser.setCurrentDirectory(new java.io.File("."));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            resultArea.setText("Ready to export: " + selectedFile.getAbsolutePath() +
                             "\nTime: " + java.time.LocalDateTime.now() +
                             "\nRecords: " + groupTableModel.getRowCount());
        }
    }

    private void showGroupDetails() {
        int selectedRow = groupTable.getSelectedRow();
        if (selectedRow == -1) {
            resultArea.setText("Error: select a group");
            return;
        }

        int modelRow = groupTable.convertRowIndexToModel(selectedRow);
        String groupId = (String) groupTableModel.getValueAt(modelRow, 0);

        try {
            Group group = groupRepository.findByIdWithEmployeesAndResources(groupId).orElse(null);
            if (group == null) {
                resultArea.setText("Error: group not found - " + groupId);
                return;
            }

            StringBuilder sb = new StringBuilder();
            int memberCount = group.getEmployees() != null ? group.getEmployees().size() : 0;
            int resourceCount = group.getResources() != null ? group.getResources().size() : 0;
            sb.append("Group Details\n");
            sb.append("Group ID: ").append(group.getGroupId()).append("\n");
            sb.append("Group Name: ").append(group.getName()).append("\n");
            sb.append("Members: ").append(memberCount).append("\n");
            sb.append("Resources: ").append(resourceCount).append("\n");
            sb.append("===============================\n");

            sb.append("Members (").append(memberCount).append("):\n");
            if (group.getEmployees() != null && !group.getEmployees().isEmpty()) {
                int count = 0;
                for (var employee : group.getEmployees()) {
                    sb.append("  ").append(++count).append(". ")
                      .append(employee.getEmployeeId())
                      .append(" (").append(employee.getEmployeeName()).append(")\n");
                }
            } else {
                sb.append("  None\n");
            }

            sb.append("===============================\n");
            sb.append("Resources (").append(resourceCount).append("):\n");
            if (group.getResources() != null && !group.getResources().isEmpty()) {
                int count = 0;
                for (var resource : group.getResources()) {
                    sb.append("  ").append(++count).append(". ")
                      .append(resource.getResourceId())
                      .append(" (").append(resource.getResourceName()).append(") - ")
                      .append(resource.getResourceState()).append("\n");
                }
            } else {
                sb.append("  None\n");
            }

            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("Query failed: " + ex.getMessage());
        }
    }
}
