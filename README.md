#ACCESS-CONTROL-SYSTEM
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

# System Overview

This is a campus/building access control prototype system. It provides access request processing, personnel and resource configuration, time and quota limits, audit logging, and a complete local desktop GUI with simulator support. The system uses a unified access control service as the single entry point to ensure every access request is recorded and auditable, and it supports emergency operations that can coordinate resource control states.


# Key Features

- Core Access Control
  A single entry point processes access requests, returns allow/deny decisions, and writes audit logs. External modules do not directly access the persistence layer.

- Personnel and Permission Management
  Supports employee registration, badge issuance and invalidation, group creation and maintenance, and binding employees/badges to policy profiles.

- Resource Management
  Supports registering resources, updating resource location and state (controlled/uncontrolled), and managing group-to-resource permissions.

- Emergency Control
  Supports batch switching resource control states by resource type, resource list, or group, with a one-click restore capability.

- Access Quota and Limits
  Tracks access counts per employee and resource, supports daily/weekly/resource-level queries, and enforces quota checks.

- Time Filter Rules
  Parses and validates time rule strings and determines whether access is allowed based on time filters.

- Profile and Group File Loading
  Loads profiles and time filters from JSON; loads resource groups and group-to-resource mappings from text files.

- Audit Logs and Queries
  Supports log queries by badge, employee, resource, and denied records.

- Simulation and Runtime Support
  Provides badge/reader/event simulation, routing and resource control components, plus monitoring and health metrics to mimic real-world operation.

- Desktop GUI
  Includes panels for monitoring, scan simulation, resource grouping, time filters, emergency control, and simulator controls for configuration and real-time visibility.


# Package Guide 

- acs.domain
  Core domain models (people, badges, resources, access request/result, time filters).

- acs.service
  Business service interfaces and implementations (access control, admin management, logs, time rules).

- acs.repository
  Persistence layer interfaces (repositories for domain entities).

- acs.log
  Logging services and CSV export support.

- acs.simulator
  Simulators for readers/events, routing system, execution tracking, and system health.

- acs.ui
  Desktop GUI panels and application starters.

- acs.cache
  Local cache management for performance and reduced database dependency.