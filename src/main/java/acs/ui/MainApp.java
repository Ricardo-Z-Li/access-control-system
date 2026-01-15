package acs.ui;

import javax.swing.*;
import java.awt.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import acs.service.AccessControlService;
import acs.service.AdminService;
import acs.service.LogQueryService;
import acs.service.ProfileFileService;
import acs.service.AccessLimitService;
import acs.service.TimeFilterService;
import acs.service.GroupFileService;
import acs.service.EmergencyControlService;
import acs.service.ClockService;
import acs.cache.LocalCacheManager;
import acs.simulator.BadgeReaderSimulator;
import acs.simulator.EventSimulator;
import acs.simulator.RouterSystem;
import acs.simulator.BadgeCodeUpdateService;
import acs.repository.EmployeeRepository;
import acs.repository.ProfileRepository;
import acs.repository.ResourceRepository;
import acs.repository.ProfileResourceLimitRepository;
import acs.repository.TimeFilterRepository;
import acs.repository.GroupRepository;
import acs.log.csv.CsvLogExporter;
import acs.service.LogCleanupService;

@Component
@Profile("!test")
public class MainApp extends JFrame {
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JList<NavItem> navList;
    private DefaultListModel<NavItem> navModel;
    private JLabel sectionTitle;
    private JLabel sectionSubtitle;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private LogQueryService logQueryService;

    @Autowired
    private ProfileFileService profileFileService;

    @Autowired
    private AccessLimitService accessLimitService;

    @Autowired
    private TimeFilterService timeFilterService;

    @Autowired
    private GroupFileService groupFileService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ProfileResourceLimitRepository profileResourceLimitRepository;

    @Autowired
    private TimeFilterRepository timeFilterRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private LocalCacheManager localCacheManager;

    @Autowired
    private CsvLogExporter csvLogExporter;

    @Autowired
    private LogCleanupService logCleanupService;

    @Autowired
    private BadgeReaderSimulator badgeReaderSimulator;

    @Autowired
    private EventSimulator eventSimulator;

    @Autowired
    private RouterSystem routerSystem;

    @Autowired
    private BadgeCodeUpdateService badgeCodeUpdateService;

    @Autowired
    private EmergencyControlService emergencyControlService;

    @Autowired
    private ClockService clockService;

    private AdminPanel adminPanel;
    private ScanPanel scanPanel;
    private MonitorPanel monitorPanel;
    private SimulatorPanel simulatorPanel;
    private AccessLimitPanel accessLimitPanel;
    private TimeFilterPanel timeFilterPanel;
    private GroupFilePanel groupFilePanel;
    private EmergencyControlPanel emergencyControlPanel;
    @Autowired
    private SiteMapPanel siteMapPanel;

    public MainApp() {
        System.setProperty("java.awt.headless", "false");
    }

    private void initUI() {
        UiTheme.apply();
        setTitle("Access Control System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setLocationRelativeTo(null);

        adminPanel = new AdminPanel(adminService, profileFileService);
        scanPanel = new ScanPanel(accessControlService, badgeCodeUpdateService, clockService);
        monitorPanel = new MonitorPanel(logQueryService, accessControlService, localCacheManager, siteMapPanel, csvLogExporter, logCleanupService);
        simulatorPanel = new SimulatorPanel(badgeReaderSimulator, eventSimulator, routerSystem, clockService);
        accessLimitPanel = new AccessLimitPanel(accessLimitService, employeeRepository, profileRepository, resourceRepository, profileResourceLimitRepository);
        timeFilterPanel = new TimeFilterPanel(timeFilterService, timeFilterRepository);
        groupFilePanel = new GroupFilePanel(groupFileService, groupRepository);
        emergencyControlPanel = new EmergencyControlPanel(emergencyControlService);

        buildNavigation();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.background());
        root.add(createHeaderBar(), BorderLayout.NORTH);
        root.add(createMainLayout(), BorderLayout.CENTER);
        root.add(createStatusPanel(), BorderLayout.SOUTH);
        add(root, BorderLayout.CENTER);

        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);
    }

    private void buildNavigation() {
        navModel = new DefaultListModel<>();
        navModel.addElement(new NavItem("admin", "Admin"));
        navModel.addElement(new NavItem("scan", "Scan"));
        navModel.addElement(new NavItem("monitor", "Monitor"));
        navModel.addElement(new NavItem("simulator", "Simulator"));
        navModel.addElement(new NavItem("limits", "Access Limits"));
        navModel.addElement(new NavItem("filters", "Time Filters"));
        navModel.addElement(new NavItem("groups", "Group Files"));
        navModel.addElement(new NavItem("emergency", "Emergency"));

        navList = new JList<>(navModel);
        navList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navList.setFixedCellHeight(44);
        navList.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        navList.setBackground(UiTheme.surface());
        navList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.label);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            if (isSelected) {
                label.setBackground(UiTheme.accent());
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(UiTheme.surface());
                label.setForeground(UiTheme.mutedText());
            }
            return label;
        });
        navList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                NavItem selected = navList.getSelectedValue();
                if (selected != null) {
                    showSection(selected);
                }
            }
        });

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(adminPanel, "admin");
        contentPanel.add(scanPanel, "scan");
        contentPanel.add(monitorPanel, "monitor");
        contentPanel.add(simulatorPanel, "simulator");
        contentPanel.add(accessLimitPanel, "limits");
        contentPanel.add(timeFilterPanel, "filters");
        contentPanel.add(groupFilePanel, "groups");
        contentPanel.add(emergencyControlPanel, "emergency");
    }

    private JPanel createHeaderBar() {
        JButton refreshButton = UiTheme.secondaryButton("Refresh Cache");
        refreshButton.addActionListener(e -> refreshCache());
        refreshButton.setEnabled(localCacheManager != null);

        JButton resetClockButton = UiTheme.secondaryButton("Reset Time");
        resetClockButton.addActionListener(e -> clockService.resetToRealTime());

        JButton aboutButton = UiTheme.secondaryButton("About");
        aboutButton.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Access Control System\nPrototype UI",
            "About", JOptionPane.INFORMATION_MESSAGE));

        sectionTitle = new JLabel("Admin");
        sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 20f));
        sectionSubtitle = new JLabel("Manage people, permissions, and resources");
        sectionSubtitle.setForeground(UiTheme.mutedText());

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(sectionTitle);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(sectionSubtitle);

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));
        header.setBackground(UiTheme.background());
        header.add(titlePanel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshButton);
        actions.add(resetClockButton);
        actions.add(aboutButton);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JSplitPane createMainLayout() {
        JPanel navContainer = new JPanel(new BorderLayout());
        navContainer.setBackground(UiTheme.background());
        navContainer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        navContainer.add(navList, BorderLayout.CENTER);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(UiTheme.background());
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));
        contentWrapper.add(contentPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navContainer, contentWrapper);
        splitPane.setDividerLocation(220);
        splitPane.setResizeWeight(0.18);
        splitPane.setDividerSize(1);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setContinuousLayout(true);

        navList.setSelectedIndex(0);
        return splitPane;
    }

    private void showSection(NavItem item) {
        cardLayout.show(contentPanel, item.id);
        sectionTitle.setText(item.label);
        sectionSubtitle.setText(getSubtitle(item.id));
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        JMenu viewMenu = new JMenu("View");
        JMenuItem adminViewItem = new JMenuItem("Admin");
        adminViewItem.addActionListener(e -> navList.setSelectedIndex(0));
        viewMenu.add(adminViewItem);

        JMenuItem scanViewItem = new JMenuItem("Scan");
        scanViewItem.addActionListener(e -> navList.setSelectedIndex(1));
        viewMenu.add(scanViewItem);

        JMenuItem monitorViewItem = new JMenuItem("Monitor");
        monitorViewItem.addActionListener(e -> navList.setSelectedIndex(2));
        viewMenu.add(monitorViewItem);

        JMenuItem simulatorViewItem = new JMenuItem("Simulator");
        simulatorViewItem.addActionListener(e -> navList.setSelectedIndex(3));
        viewMenu.add(simulatorViewItem);

        JMenuItem limitViewItem = new JMenuItem("Access Limits");
        limitViewItem.addActionListener(e -> navList.setSelectedIndex(4));
        viewMenu.add(limitViewItem);

        JMenuItem filterViewItem = new JMenuItem("Time Filters");
        filterViewItem.addActionListener(e -> navList.setSelectedIndex(5));
        viewMenu.add(filterViewItem);

        JMenuItem groupViewItem = new JMenuItem("Group Files");
        groupViewItem.addActionListener(e -> navList.setSelectedIndex(6));
        viewMenu.add(groupViewItem);

        JMenuItem emergencyViewItem = new JMenuItem("Emergency");
        emergencyViewItem.addActionListener(e -> navList.setSelectedIndex(7));
        viewMenu.add(emergencyViewItem);

        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem reloadItem = new JMenuItem("Refresh Cache");
        reloadItem.addActionListener(e -> refreshCache());
        toolsMenu.add(reloadItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e ->
            JOptionPane.showMessageDialog(this, "Access Control System\nPrototype UI", "About", JOptionPane.INFORMATION_MESSAGE)
        );
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        panel.setBackground(UiTheme.background());

        JLabel statusLabel = new JLabel("Ready");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(statusLabel, BorderLayout.WEST);

        JLabel timeLabel = new JLabel("Current Time: " + java.time.LocalDateTime.now());
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(timeLabel, BorderLayout.EAST);

        Timer timer = new Timer(1000, e -> timeLabel.setText("Current Time: " + java.time.LocalDateTime.now()));
        timer.start();

        return panel;
    }

    private void refreshCache() {
        try {
            if (localCacheManager == null) {
                JOptionPane.showMessageDialog(this, "Cache manager unavailable", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            localCacheManager.refreshAllCache();
            JOptionPane.showMessageDialog(this, "Cache refreshed", "Info", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Refresh failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private String getSubtitle(String id) {
        return switch (id) {
            case "admin" -> "Manage people, permissions, and resources";
            case "scan" -> "Simulate badge scan and update flow";
            case "monitor" -> "Real-time monitoring and log search";
            case "simulator" -> "Simulator control and system load test";
            case "limits" -> "Access count and resource limit management";
            case "filters" -> "Time rule parsing and testing";
            case "groups" -> "Group file import and management";
            case "emergency" -> "Emergency status and resource control";
            default -> "";
        };
    }

    public void showUI() {
        SwingUtilities.invokeLater(() -> {
            initUI();
            setVisible(true);
        });
    }

    private static class NavItem {
        private final String id;
        private final String label;

        private NavItem(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }
}
