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
import acs.repository.TimeFilterRepository;
import acs.repository.GroupRepository;
import acs.ui.SiteMapPanel;
import acs.log.csv.CsvLogExporter;
import acs.service.LogCleanupService;

@Component
@Profile("!test")
public class MainApp extends JFrame {
    private JTabbedPane tabbedPane;
    
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
        // 设置headless模式以避免初始化错误
        System.setProperty("java.awt.headless", "false");
    }
    
    private void initUI() {
        setTitle("访问控制系统 - 管理控制台");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        tabbedPane = new JTabbedPane();
        
        adminPanel = new AdminPanel(adminService, profileFileService);
        scanPanel = new ScanPanel(accessControlService, badgeCodeUpdateService, clockService);
        monitorPanel = new MonitorPanel(logQueryService, siteMapPanel, csvLogExporter, logCleanupService);
        simulatorPanel = new SimulatorPanel(badgeReaderSimulator, eventSimulator, routerSystem, clockService);
        accessLimitPanel = new AccessLimitPanel(accessLimitService, employeeRepository, profileRepository);
        timeFilterPanel = new TimeFilterPanel(timeFilterService, timeFilterRepository);
        groupFilePanel = new GroupFilePanel(groupFileService, groupRepository);
        emergencyControlPanel = new EmergencyControlPanel(emergencyControlService);
        
        tabbedPane.addTab("管理", adminPanel);
        tabbedPane.addTab("扫描", scanPanel);
        tabbedPane.addTab("监控", monitorPanel);
        tabbedPane.addTab("模拟器", simulatorPanel);
        tabbedPane.addTab("访问限制", accessLimitPanel);
        tabbedPane.addTab("时间过滤器", timeFilterPanel);
        tabbedPane.addTab("组文件", groupFilePanel);
        tabbedPane.addTab("紧急控制", emergencyControlPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);
        
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("文件");
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        JMenu viewMenu = new JMenu("视图");
        JMenuItem adminViewItem = new JMenuItem("切换到管理面板");
        adminViewItem.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        viewMenu.add(adminViewItem);
        
        JMenuItem scanViewItem = new JMenuItem("切换到扫描面板");
        scanViewItem.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        viewMenu.add(scanViewItem);
        
        JMenuItem monitorViewItem = new JMenuItem("切换到监控面板");
        monitorViewItem.addActionListener(e -> tabbedPane.setSelectedIndex(2));
        viewMenu.add(monitorViewItem);
        
        JMenuItem simulatorViewItem = new JMenuItem("切换到模拟器面板");
        simulatorViewItem.addActionListener(e -> tabbedPane.setSelectedIndex(3));
        viewMenu.add(simulatorViewItem);
        
        JMenuItem limitViewItem = new JMenuItem("切换到访问限制面板");
        limitViewItem.addActionListener(e -> tabbedPane.setSelectedIndex(4));
        viewMenu.add(limitViewItem);
        
        JMenuItem filterViewItem = new JMenuItem("切换到时间过滤器面板");
        filterViewItem.addActionListener(e -> tabbedPane.setSelectedIndex(5));
        viewMenu.add(filterViewItem);
        
        JMenuItem groupViewItem = new JMenuItem("切换到组文件面板");
        groupViewItem.addActionListener(e -> tabbedPane.setSelectedIndex(6));
        viewMenu.add(groupViewItem);
        
        JMenuItem emergencyViewItem = new JMenuItem("切换到紧急控制面板");
        emergencyViewItem.addActionListener(e -> tabbedPane.setSelectedIndex(7));
        viewMenu.add(emergencyViewItem);
        
        JMenu toolsMenu = new JMenu("工具");
        JMenuItem reloadItem = new JMenuItem("重新加载缓存");
        reloadItem.addActionListener(e -> {
            try {
                if (localCacheManager == null) {
                    JOptionPane.showMessageDialog(this, "错误：缓存管理器未初始化", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                localCacheManager.refreshAllCache();
                JOptionPane.showMessageDialog(this, "缓存重新加载成功", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "缓存重新加载失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
        toolsMenu.add(reloadItem);
        
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "访问控制系统 v2.0\n基于Spring Boot的物理访问控制模拟\n已集成所有功能模块");
        });
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        
        return menuBar;
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        JLabel statusLabel = new JLabel("就绪");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(statusLabel, BorderLayout.WEST);
        
        JLabel timeLabel = new JLabel("系统时间: " + java.time.LocalDateTime.now());
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(timeLabel, BorderLayout.EAST);
        
        Timer timer = new Timer(1000, e -> {
            timeLabel.setText("系统时间: " + java.time.LocalDateTime.now());
        });
        timer.start();
        
        return panel;
    }
    
    public void showUI() {
        SwingUtilities.invokeLater(() -> {
            initUI();
            setVisible(true);
        });
    }
}