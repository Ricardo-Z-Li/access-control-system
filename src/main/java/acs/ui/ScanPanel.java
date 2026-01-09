package acs.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import acs.service.AccessControlService;
import acs.domain.AccessRequest;
import acs.domain.AccessResult;

public class ScanPanel extends JPanel {
    private AccessControlService accessControlService;
    private JTextField badgeIdField;
    private JTextField resourceIdField;
    private JTextArea resultArea;
    
    public ScanPanel(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
        initUI();
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("模拟扫描访问", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("徽章ID:"), gbc);
        
        gbc.gridx = 1;
        badgeIdField = new JTextField(20);
        inputPanel.add(badgeIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("资源ID:"), gbc);
        
        gbc.gridx = 1;
        resourceIdField = new JTextField(20);
        inputPanel.add(resourceIdField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton scanButton = new JButton("模拟扫描");
        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simulateScan();
            }
        });
        inputPanel.add(scanButton, gbc);
        
        add(inputPanel, BorderLayout.CENTER);
        
        resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("宋体", Font.PLAIN, 12));
        add(new JScrollPane(resultArea), BorderLayout.SOUTH);
    }
    
    private void simulateScan() {
        String badgeId = badgeIdField.getText().trim();
        String resourceId = resourceIdField.getText().trim();
        
        if (badgeId.isEmpty() || resourceId.isEmpty()) {
            resultArea.setText("错误: 请输入徽章ID和资源ID");
            return;
        }
        
        try {
            AccessRequest request = new AccessRequest(badgeId, resourceId, java.time.LocalDateTime.now());
            AccessResult result = accessControlService.processAccess(request);
            
            StringBuilder sb = new StringBuilder();
            sb.append("扫描结果:\n");
            sb.append("徽章ID: ").append(badgeId).append("\n");
            sb.append("资源ID: ").append(resourceId).append("\n");
            sb.append("决策: ").append(result.getDecision()).append("\n");
            sb.append("原因代码: ").append(result.getReasonCode()).append("\n");
            sb.append("消息: ").append(result.getMessage()).append("\n");
            sb.append("请求时间: ").append(request.getTimestamp()).append("\n");
            
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("错误: " + ex.getMessage());
        }
    }
}
