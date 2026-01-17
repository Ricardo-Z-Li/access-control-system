package acs.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public final class UiTheme {
    private static final Color COLOR_BG = new Color(240, 243, 247);
    private static final Color COLOR_SURFACE = new Color(255, 255, 255);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_TEXT = new Color(15, 23, 42);
    private static final Color COLOR_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_ACCENT = new Color(37, 99, 235);
    private static final Color COLOR_ACCENT_SOFT = new Color(219, 234, 254);
    private static final Color COLOR_DANGER = new Color(216, 85, 78);
    private static final Color COLOR_SUCCESS = new Color(42, 157, 113);
    private static final Color COLOR_BUTTON = new Color(235, 240, 246);

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
            if (baseFont == null) {
                baseFont = new Font("SansSerif", Font.PLAIN, 13);
            }
        }
        if (baseFont != null) {
            for (Object key : UIManager.getDefaults().keySet()) {
                Object value = UIManager.get(key);
                if (value instanceof Font) {
                    UIManager.put(key, baseFont);
                }
            }
        }

        UIManager.put("control", COLOR_BG);
        UIManager.put("info", COLOR_SURFACE);
        UIManager.put("nimbusBase", COLOR_ACCENT);
        UIManager.put("nimbusBlueGrey", new Color(221, 228, 236));
        UIManager.put("nimbusLightBackground", COLOR_SURFACE);
        UIManager.put("nimbusFocus", COLOR_ACCENT_SOFT);
        UIManager.put("text", COLOR_TEXT);
        UIManager.put("Table.alternateRowColor", new Color(248, 250, 252));
        UIManager.put("Table.showGrid", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 14, 8, 14));
        UIManager.put("ScrollPane.border", new LineBorder(COLOR_BORDER, 1, true));
        UIManager.put("TextField.border", roundedBorder());
        UIManager.put("TextArea.border", roundedBorder());
        UIManager.put("ComboBox.border", roundedBorder());
        UIManager.put("Button.focus", COLOR_ACCENT_SOFT);
        UIManager.put("Label.foreground", COLOR_TEXT);
        UIManager.put("TabbedPane.selected", new Color(235, 242, 255));
        UIManager.put("TabbedPane.background", COLOR_BG);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 2, 2, 2));
    }

    public static Color background() {
        return COLOR_BG;
    }

    public static Color surface() {
        return COLOR_SURFACE;
    }

    public static Color border() {
        return COLOR_BORDER;
    }

    public static Color mutedText() {
        return COLOR_MUTED;
    }

    public static Color accent() {
        return COLOR_ACCENT;
    }

    public static Color accentSoft() {
        return COLOR_ACCENT_SOFT;
    }

    public static JTextPane createLogPane(boolean monospace) {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setOpaque(true);
        pane.setBackground(COLOR_SURFACE);
        if (monospace) {
            Font font = new Font("Consolas", Font.PLAIN, 12);
            pane.setFont(font);
        }
        ensureTextStyles(pane);
        return pane;
    }

    public static void setStatusText(JTextPane pane, String text) {
        if (pane == null) {
            return;
        }
        pane.setText("");
        if (text == null || text.isEmpty()) {
            return;
        }
        appendStatusText(pane, text);
    }

    public static void appendStatusText(JTextPane pane, String text) {
        if (pane == null || text == null) {
            return;
        }
        if (text.isEmpty()) {
            return;
        }
        ensureTextStyles(pane);
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            appendStatusLine(pane, line);
        }
    }

    public static void appendStatusLine(JTextPane pane, String line) {
        if (pane == null || line == null) {
            return;
        }
        ensureTextStyles(pane);
        StyledDocument doc = pane.getStyledDocument();
        Style style = getStyleForLine(pane, line);
        try {
            doc.insertString(doc.getLength(), line + "\n", style);
        } catch (BadLocationException ignored) {
        }
    }

    public static JPanel wrapContent(JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
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
        subtitleLabel.setForeground(COLOR_MUTED);

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
        panel.setBackground(COLOR_SURFACE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(label, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        return panel;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(COLOR_ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(buttonBorder(COLOR_ACCENT));
        button.setOpaque(true);
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(COLOR_BUTTON);
        button.setForeground(COLOR_TEXT);
        button.setFocusPainted(false);
        button.setBorder(buttonBorder(COLOR_BORDER));
        button.setOpaque(true);
        return button;
    }

    public static JButton dangerButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(COLOR_DANGER);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(buttonBorder(COLOR_DANGER));
        button.setOpaque(true);
        return button;
    }

    public static JButton successButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(COLOR_SUCCESS);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(buttonBorder(COLOR_SUCCESS));
        button.setOpaque(true);
        return button;
    }

    public static JLabel statusPill(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setBorder(new CompoundBorder(new LineBorder(background.darker(), 1, true), new EmptyBorder(4, 10, 4, 10)));
        return label;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(COLOR_ACCENT_SOFT);
        table.setSelectionForeground(COLOR_TEXT);
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(new Color(241, 245, 249));
            header.setForeground(new Color(51, 65, 85));
            header.setBorder(new LineBorder(COLOR_BORDER));
        }
    }

    public static JPanel formRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel labelComp = new JLabel(label);
        labelComp.setPreferredSize(new Dimension(150, 26));
        labelComp.setForeground(COLOR_MUTED);
        row.add(labelComp, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    public static JPanel cardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        return panel;
    }

    public static Border subtleBorder() {
        return new LineBorder(COLOR_BORDER, 1, true);
    }

    public static JPanel footerBar(JComponent... items) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);
        panel.setBorder(new CompoundBorder(new LineBorder(COLOR_BORDER), new EmptyBorder(6, 12, 6, 12)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        if (items != null) {
            for (JComponent item : items) {
                left.add(item);
            }
        }
        panel.add(left, BorderLayout.WEST);
        return panel;
    }

    private static Border roundedBorder() {
        return new CompoundBorder(new LineBorder(COLOR_BORDER, 1, true), new EmptyBorder(6, 10, 6, 10));
    }

    private static Border buttonBorder(Color outline) {
        return new CompoundBorder(new LineBorder(outline, 1, true), new EmptyBorder(8, 14, 8, 14));
    }

    private static void ensureTextStyles(JTextPane pane) {
        if (pane.getStyle("status-default") != null) {
            return;
        }
        Style defaultStyle = pane.addStyle("status-default", null);
        StyleConstants.setForeground(defaultStyle, COLOR_TEXT);
        Style successStyle = pane.addStyle("status-success", null);
        StyleConstants.setForeground(successStyle, new Color(22, 124, 78));
        Style errorStyle = pane.addStyle("status-error", null);
        StyleConstants.setForeground(errorStyle, new Color(190, 46, 46));
        Style warnStyle = pane.addStyle("status-warn", null);
        StyleConstants.setForeground(warnStyle, new Color(161, 98, 7));
        Style infoStyle = pane.addStyle("status-info", null);
        StyleConstants.setForeground(infoStyle, new Color(30, 64, 175));
    }

    private static Style getStyleForLine(JTextPane pane, String line) {
        String normalized = line.toLowerCase();
        if (normalized.contains("error")
            || normalized.contains("failed")
            || normalized.contains("fail")
            || normalized.contains("deny")
            || normalized.contains("denied")
            || normalized.contains("invalid")
            || normalized.contains("unavailable")
            || normalized.contains("not found")
            || normalized.contains("missing")
            || normalized.contains("exception")) {
            return pane.getStyle("status-error");
        }
        if (normalized.contains("success")
            || normalized.contains("completed")
            || normalized.contains("ok")
            || normalized.contains("available")
            || normalized.contains("passed")
            || normalized.contains("restored")
            || normalized.contains("updated")
            || normalized.contains("saved")
            || normalized.contains("refreshed")
            || normalized.contains("allow")
            || normalized.contains("granted")) {
            return pane.getStyle("status-success");
        }
        if (normalized.contains("warning")
            || normalized.contains("warn")
            || normalized.contains("near limit")) {
            return pane.getStyle("status-warn");
        }
        if (normalized.contains("info")
            || normalized.contains("ready")
            || normalized.contains("started")
            || normalized.contains("loading")) {
            return pane.getStyle("status-info");
        }
        return pane.getStyle("status-default");
    }
}
