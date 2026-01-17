
```
access-control-system
├─ .editorconfig
├─ pom.xml
└─ src
   ├─ main
   │  ├─ java
   │  │  └─ acs
   │  │     ├─ AccessControlApplication.java
   │  │     ├─ cache
   │  │     │  └─ LocalCacheManager.java
   │  │     ├─ domain
   │  │     │  ├─ AccessDecision.java
   │  │     │  ├─ AccessRequest.java
   │  │     │  ├─ AccessResult.java
   │  │     │  ├─ Badge.java
   │  │     │  ├─ BadgeReader.java
   │  │     │  ├─ BadgeStatus.java
   │  │     │  ├─ BadgeUpdateStatus.java
   │  │     │  ├─ Employee.java
   │  │     │  ├─ Group.java
   │  │     │  ├─ LogEntry.java
   │  │     │  ├─ Profile.java
   │  │     │  ├─ ProfileResourceLimit.java
   │  │     │  ├─ ReasonCode.java
   │  │     │  ├─ Resource.java
   │  │     │  ├─ ResourceDependency.java
   │  │     │  ├─ ResourceState.java
   │  │     │  ├─ ResourceType.java
   │  │     │  └─ TimeFilter.java
   │  │     ├─ log
   │  │     │  ├─ csv
   │  │     │  │  ├─ CsvLogExporter.java
   │  │     │  │  ├─ CsvLogService.java
   │  │     │  │  └─ CsvLogWriter.java
   │  │     │  ├─ impl
   │  │     │  │  └─ LogServiceImpl.java
   │  │     │  └─ LogService.java
   │  │     ├─ repository
   │  │     │  ├─ AccessLogRepository.java
   │  │     │  ├─ BadgeReaderRepository.java
   │  │     │  ├─ BadgeRepository.java
   │  │     │  ├─ EmployeeRepository.java
   │  │     │  ├─ GroupRepository.java
   │  │     │  ├─ ProfileRepository.java
   │  │     │  ├─ ProfileResourceLimitRepository.java
   │  │     │  ├─ ResourceDependencyRepository.java
   │  │     │  ├─ ResourceRepository.java
   │  │     │  └─ TimeFilterRepository.java
   │  │     ├─ service
   │  │     │  ├─ AccessControlService.java
   │  │     │  ├─ AccessLimitService.java
   │  │     │  ├─ AdminService.java
   │  │     │  ├─ ClockService.java
   │  │     │  ├─ EmergencyControlService.java
   │  │     │  ├─ GroupFileService.java
   │  │     │  ├─ impl
   │  │     │  │  ├─ AccessControlServiceImpl.java
   │  │     │  │  ├─ AccessLimitServiceImpl.java
   │  │     │  │  ├─ AdminServiceImpl.java
   │  │     │  │  ├─ ClockServiceImpl.java
   │  │     │  │  ├─ EmergencyControlServiceImpl.java
   │  │     │  │  ├─ GroupFileServiceImpl.java
   │  │     │  │  ├─ LogQueryServiceImpl.java
   │  │     │  │  ├─ ProfileFileServiceImpl.java
   │  │     │  │  └─ TimeFilterServiceImpl.java
   │  │     │  ├─ LogCleanupService.java
   │  │     │  ├─ LogQueryService.java
   │  │     │  ├─ ProfileFileService.java
   │  │     │  └─ TimeFilterService.java
   │  │     ├─ simulator
   │  │     │  ├─ BadgeCodeUpdateService.java
   │  │     │  ├─ BadgeCodeUpdateServiceImpl.java
   │  │     │  ├─ BadgeReaderSimulator.java
   │  │     │  ├─ BadgeReaderSimulatorImpl.java
   │  │     │  ├─ BadgeUpdateScheduler.java
   │  │     │  ├─ EventSimulator.java
   │  │     │  ├─ EventSimulatorImpl.java
   │  │     │  ├─ ExecutionChainTracker.java
   │  │     │  ├─ LoadBalanceStats.java
   │  │     │  ├─ ResourceController.java
   │  │     │  ├─ ResourceControllerImpl.java
   │  │     │  ├─ RouterSystem.java
   │  │     │  ├─ RouterSystemImpl.java
   │  │     │  ├─ SimulationListener.java
   │  │     │  ├─ SimulationPath.java
   │  │     │  ├─ SimulationScenarioConfig.java
   │  │     │  ├─ SimulationStatus.java
   │  │     │  └─ SystemHealth.java
   │  │     ├─ tools
   │  │     │  └─ DbInitRunner.java
   │  │     └─ ui
   │  │        ├─ AccessLimitPanel.java
   │  │        ├─ AdminPanel.java
   │  │        ├─ EmergencyControlPanel.java
   │  │        ├─ GroupFilePanel.java
   │  │        ├─ MainApp.java
   │  │        ├─ MonitorPanel.java
   │  │        ├─ ScanPanel.java
   │  │        ├─ SimulatorPanel.java
   │  │        ├─ SiteMapPanel.java
   │  │        ├─ TimeFilterPanel.java
   │  │        ├─ UIStarter.java
   │  │        └─ UiTheme.java
   │  └─ resources
   │     ├─ application.properties
   │     ├─ db
   │     │  └─ access_control_db.sql
   │     ├─ office-layout.png
   │     ├─ simulator
   │     │  └─ scenarios.json
   │     └─ site-layout.png
   └─ test
      ├─ java
      │  └─ acs
      │     ├─ cache
      │     │  └─ LocalCacheManagerIntegrationTest.java
      │     ├─ integration
      │     │  └─ SimulatedTimeAccessControlIntegrationTest.java
      │     ├─ log
      │     │  └─ csv
      │     │     └─ CsvLogWriterTest.java
      │     ├─ performance
      │     │  └─ PerformanceTest.java
      │     ├─ service
      │     │  └─ impl
      │     │     ├─ AccessControlServiceImplTest.java
      │     │     ├─ AdminServiceImplTest.java
      │     │     ├─ ClockServiceImplTest.java
      │     │     ├─ EmergencyControlServiceImplTest.java
      │     │     ├─ LogQueryServiceImplTest.java
      │     │     └─ TimeFilterServiceImplTest.java
      │     ├─ simulator
      │     │  ├─ BadgeCodeUpdateServiceImplTest.java
      │     │  └─ BadgeReaderSimulatorImplTest.java
      │     └─ tools
      │        └─ TestDataInitRunner.java
      └─ resources
         ├─ application-test.properties
         └─ db
            └─ init-test-data.sql

```