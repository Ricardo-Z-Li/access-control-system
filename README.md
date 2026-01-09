
```
access-control-system
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
   │  │     │  ├─ Employee.java
   │  │     │  ├─ Group.java
   │  │     │  ├─ LogEntry.java
   │  │     │  ├─ Profile.java
   │  │     │  ├─ ReasonCode.java
   │  │     │  ├─ Resource.java
   │  │     │  ├─ ResourceDependency.java
   │  │     │  ├─ ResourceState.java
   │  │     │  ├─ ResourceType.java
   │  │     │  ├─ TimeFilter.java
   │  │     │  └─ UserType.java
   │  │     ├─ log
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
   │  │     │  ├─ ResourceDependencyRepository.java
   │  │     │  ├─ ResourceRepository.java
   │  │     │  └─ TimeFilterRepository.java
   │  │     ├─ service
   │  │     │  ├─ AccessControlService.java
   │  │     │  ├─ AccessLimitService.java
   │  │     │  ├─ AdminService.java
   │  │     │  ├─ GroupFileService.java
   │  │     │  ├─ impl
   │  │     │  │  ├─ AccessControlServiceImpl.java
   │  │     │  │  ├─ AccessLimitServiceImpl.java
   │  │     │  │  ├─ AdminServiceImpl.java
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
   │  │     │  ├─ EventSimulator.java
   │  │     │  ├─ EventSimulatorImpl.java
   │  │     │  ├─ LoadBalanceStats.java
   │  │     │  ├─ ResourceController.java
   │  │     │  ├─ ResourceControllerImpl.java
   │  │     │  ├─ RouterSystem.java
   │  │     │  ├─ RouterSystemImpl.java
   │  │     │  ├─ SimulationListener.java
   │  │     │  ├─ SimulationStatus.java
   │  │     │  └─ SystemHealth.java
   │  │     └─ ui
   │  │        ├─ AccessLimitPanel.java
   │  │        ├─ AdminPanel.java
   │  │        ├─ GroupFilePanel.java
   │  │        ├─ MainApp.java
   │  │        ├─ MonitorPanel.java
   │  │        ├─ ScanPanel.java
   │  │        ├─ SimulatorPanel.java
   │  │        ├─ SiteMapPanel.java
   │  │        ├─ TimeFilterPanel.java
   │  │        └─ UIStarter.java
   │  └─ resources
   │     ├─ application.properties
   │     └─ db
   │        └─ access_control_db.sql
   └─ test
      ├─ java
      │  └─ acs
      │     ├─ cache
      │     │  └─ LocalCacheManagerIntegrationTest.java
      │     ├─ service
      │     │  └─ impl
      │     │     ├─ AccessControlServiceImplTest.java
      │     │     ├─ AdminServiceImplTest.java
      │     │     └─ LogQueryServiceImplTest.java
      │     └─ simulator
      │        └─ BadgeReaderSimulatorImplTest.java
      └─ resources
         ├─ application-test.properties
         └─ db
            └─ init-test-data.sql

```