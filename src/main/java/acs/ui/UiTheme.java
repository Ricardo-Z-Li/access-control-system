package acs.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;

public final class UiTheme {
    private UiTheme() {
    }

    public static void apply() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        Font baseFont = new Font("Microsoft YaHei UI", Font.PLAIN, 13);
        if (!"Microsoft YaHei UI".equals(baseFont.getFamily())) {
            baseFont = UIManager.getFont("Label.font");
        }
        if (baseFont != null) {
            for (Object key : UIManager.getDefaults().keySet()) {
                Object value = UIManager.get(key);
                if (value instanceof Font) {
                    UIManager.put(key, baseFont);
                }
            }
        }

        UIManager.put("control", new Color(245, 247, 250));
        UIManager.put("info", new Color(250, 250, 250));
        UIManager.put("nimbusBase", new Color(37, 99, 235));
        UIManager.put("nimbusBlueGrey", new Color(218, 223, 230));
        UIManager.put("nimbusLightBackground", Color.WHITE);
        UIManager.put("text", new Color(33, 37, 41));
        UIManager.put("Table.alternateRowColor", new Color(248, 249, 252));
        UIManager.put("Table.showGrid", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 14, 8, 14));
    }

    public static JPanel wrapContent(JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setOpaque(false);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel createHeader(String title, String subtitle, JComponent... actions) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(16, 16, 8, 16));
        header.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));

        JLabel subtitleLabel = new JLabel(subtitle == null ? "" : subtitle);
        subtitleLabel.setForeground(new Color(102, 107, 114));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(titleLabel);
        if (subtitle != null && !subtitle.isBlank()) {
            textPanel.add(Box.createVerticalStrut(4));
            textPanel.add(subtitleLabel);
        }

        header.add(textPanel, BorderLayout.WEST);

        if (actions != null && actions.length > 0) {
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actionPanel.setOpaque(false);
            for (JComponent action : actions) {
                actionPanel.add(action);
            }
            header.add(actionPanel, BorderLayout.EAST);
        }

        return header;
    }

    public static JPanel createSection(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(label, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(226, 230, 235)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        return panel;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(37, 99, 235));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 14, 8, 14));
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(241, 244, 248));
        button.setForeground(new Color(33, 37, 41));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 14, 8, 14));
        return button;
    }

    public static JLabel statusPill(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setBorder(new EmptyBorder(4, 10, 4, 10));
        return label;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(new Color(244, 246, 249));
            header.setForeground(new Color(55, 65, 81));
            header.setBorder(new LineBorder(new Color(226, 230, 235)));
        }
    }

    public static JPanel formRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel labelComp = new JLabel(label);
        labelComp.setPreferredSize(new Dimension(140, 24));
        row.add(labelComp, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    public static JPanel cardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(226, 230, 235)),
                new EmptyBorder(12, 12, 12, 12)
        ));
        return panel;
    }

    public static Border subtleBorder() {
        return new LineBorder(new Color(226, 230, 235));
    }
}
