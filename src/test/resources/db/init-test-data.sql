-- 初始化测试数据库数据
USE access_control_db;

-- 清空现有数据（按依赖反向顺序）
SET FOREIGN_KEY_CHECKS = 0;

-- 清空所有表（按依赖顺序反向）
TRUNCATE access_logs;
TRUNCATE profile_time_filters;
TRUNCATE profile_groups;
TRUNCATE employee_groups;
TRUNCATE group_resources;
TRUNCATE resource_dependencies;
TRUNCATE badge_readers;
ALTER TABLE employees DROP FOREIGN KEY fk_employee_badge;
TRUNCATE badges;
TRUNCATE employees;
TRUNCATE profiles;
TRUNCATE time_filters;
TRUNCATE resources;
TRUNCATE group_permissions;

-- 重新添加employees表的外键约束
ALTER TABLE employees 
ADD CONSTRAINT fk_employee_badge 
FOREIGN KEY (badge_id) REFERENCES badges(badge_id) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. 插入权限组数据
INSERT INTO group_permissions (group_id, name) VALUES
('G001', 'Administrators'),
('G002', 'Engineering'),
('G003', 'Human Resources'),
('G004', 'IT Support'),
('G005', 'Visitors'),
('G006', 'Security'),
('G007', 'Finance'),
('G008', 'Maintenance');

-- 2. 插入资源数据
INSERT INTO resources (resource_id, resource_name, resource_type, resource_state, is_controlled) VALUES
('R001', 'Main Entrance', 'DOOR', 'AVAILABLE', TRUE),
('R002', 'Server Room', 'ROOM', 'LOCKED', TRUE),
('R003', 'Office Printer 1', 'PRINTER', 'AVAILABLE', TRUE),
('R004', 'Engineering Workshop', 'ROOM', 'OCCUPIED', TRUE),
('R005', 'HR Office', 'ROOM', 'AVAILABLE', TRUE),
('R006', 'Executive Laptop', 'COMPUTER', 'AVAILABLE', TRUE),
('R007', 'Cafeteria Door', 'DOOR', 'AVAILABLE', TRUE),
('R008', 'Parking Gate', 'DOOR', 'OFFLINE', TRUE),
('R009', 'Conference Room A', 'ROOM', 'AVAILABLE', TRUE),
('R010', 'Finance Safe', 'OTHER', 'LOCKED', TRUE),
('R011', 'Maintenance Closet', 'ROOM', 'AVAILABLE', FALSE),
('R012', 'Emergency Exit', 'DOOR', 'AVAILABLE', TRUE);

-- 3. 插入徽章数据（包含新增字段）
INSERT INTO badges (badge_id, status, expiration_date, badge_code, last_updated, last_code_update, code_expiration_date, needs_update, update_due_date) VALUES
('B001', 'ACTIVE', '2026-12-31', 'ABC123', '2025-01-10 08:00:00', '2025-01-10 08:00:00', '2026-12-31', FALSE, NULL),
('B002', 'ACTIVE', '2026-11-30', 'DEF456', '2025-01-10 09:15:00', '2025-01-10 09:15:00', '2026-11-30', FALSE, NULL),
('B003', 'DISABLED', '2025-06-30', 'GHI789', '2025-01-09 14:20:00', '2025-01-09 14:20:00', '2025-06-30', TRUE, '2025-07-01'),
('B004', 'ACTIVE', '2027-03-15', 'JKL012', '2025-01-11 10:30:00', '2025-01-11 10:30:00', '2027-03-15', FALSE, NULL),
('B005', 'LOST', '2026-08-20', 'MNO345', '2025-01-08 16:45:00', '2025-01-08 16:45:00', '2026-08-20', TRUE, '2025-02-01'),
('B006', 'ACTIVE', '2027-01-31', 'PQR678', '2025-01-11 11:00:00', '2025-01-11 11:00:00', '2027-01-31', FALSE, NULL),
('B007', 'ACTIVE', '2026-10-10', 'STU901', '2025-01-10 13:20:00', '2025-01-10 13:20:00', '2026-10-10', FALSE, NULL),
('B008', 'ACTIVE', '2027-05-05', 'VWX234', '2025-01-11 08:45:00', '2025-01-11 08:45:00', '2027-05-05', FALSE, NULL),
('B009', 'DISABLED', '2025-12-31', 'YZA567', '2025-01-09 10:10:00', '2025-01-09 10:10:00', '2025-12-31', TRUE, '2026-01-15'),
('B010', 'ACTIVE', '2026-09-30', 'BCD890', '2025-01-11 14:00:00', '2025-01-11 14:00:00', '2026-09-30', FALSE, NULL);

-- 4. 插入员工数据
INSERT INTO employees (employee_id, employee_name, badge_id) VALUES
('E001', 'John Doe', 'B001'),
('E002', 'Jane Smith', 'B002'),
('E003', 'Robert Johnson', 'B003'),
('E004', 'Emily Davis', 'B004'),
('E005', 'Michael Brown', 'B005'),
('E006', 'Sarah Wilson', 'B006'),
('E007', 'David Miller', 'B007'),
('E008', 'Lisa Taylor', 'B008'),
('E009', 'James Anderson', 'B009'),
('E010', 'Mary Thomas', 'B010');

-- 5. 插入员工-组关联数据
INSERT INTO employee_groups (employee_id, group_id) VALUES
('E001', 'G001'),  -- John Doe 属于管理员组
('E001', 'G006'),  -- John Doe 也属于安全组
('E002', 'G003'),  -- Jane Smith 属于HR组
('E003', 'G002'),  -- Robert Johnson 属于工程组
('E004', 'G002'),  -- Emily Davis 属于工程组
('E005', 'G004'),  -- Michael Brown 属于IT组
('E006', 'G004'),  -- Sarah Wilson 属于IT组
('E006', 'G008'),  -- Sarah Wilson 也属于维护组
('E007', 'G005'),  -- David Miller 属于访客组
('E008', 'G007'),  -- Lisa Taylor 属于财务组
('E009', 'G006'),  -- James Anderson 属于安全组
('E010', 'G008');  -- Mary Thomas 属于维护组

-- 6. 插入组-资源关联数据
INSERT INTO group_resources (group_id, resource_id) VALUES
('G001', 'R001'),  -- 管理员可访问主入口
('G001', 'R002'),  -- 管理员可访问服务器机房
('G001', 'R003'),  -- 管理员可访问打印机
('G001', 'R004'),  -- 管理员可访问工程车间
('G001', 'R005'),  -- 管理员可访问HR办公室
('G001', 'R006'),  -- 管理员可访问高管笔记本
('G001', 'R007'),  -- 管理员可访问食堂门
('G001', 'R008'),  -- 管理员可访问停车场门
('G001', 'R009'),  -- 管理员可访问会议室A
('G001', 'R010'),  -- 管理员可访问财务保险箱
('G001', 'R011'),  -- 管理员可访问维护柜
('G001', 'R012'),  -- 管理员可访问紧急出口
('G002', 'R001'),  -- 工程组可访问主入口
('G002', 'R004'),  -- 工程组可访问工程车间
('G002', 'R003'),  -- 工程组可访问打印机
('G002', 'R009'),  -- 工程组可访问会议室A
('G003', 'R001'),  -- HR组可访问主入口
('G003', 'R005'),  -- HR组可访问HR办公室
('G003', 'R003'),  -- HR组可访问打印机
('G003', 'R009'),  -- HR组可访问会议室A
('G004', 'R001'),  -- IT组可访问主入口
('G004', 'R002'),  -- IT组可访问服务器机房
('G004', 'R006'),  -- IT组可访问高管笔记本
('G004', 'R009'),  -- IT组可访问会议室A
('G005', 'R001'),  -- 访客组可访问主入口
('G005', 'R007'),  -- 访客组可访问食堂门
('G005', 'R009'),  -- 访客组可访问会议室A
('G006', 'R001'),  -- 安全组可访问主入口
('G006', 'R002'),  -- 安全组可访问服务器机房
('G006', 'R008'),  -- 安全组可访问停车场门
('G006', 'R012'),  -- 安全组可访问紧急出口
('G007', 'R001'),  -- 财务组可访问主入口
('G007', 'R010'),  -- 财务组可访问财务保险箱
('G007', 'R009'),  -- 财务组可访问会议室A
('G008', 'R001'),  -- 维护组可访问主入口
('G008', 'R011'),  -- 维护组可访问维护柜
('G008', 'R012');  -- 维护组可访问紧急出口

-- 7. 插入读卡器数据
INSERT INTO badge_readers (reader_id, reader_name, location, status, resource_id, last_seen, badge_code_length, operation_mode) VALUES
('READ001', 'Front Door Reader', 'Main Entrance Lobby', 'ONLINE', 'R001', '2025-01-11 08:30:00', 6, 'SWIPE'),
('READ002', 'Server Room Reader', 'Data Center Room 101', 'ONLINE', 'R002', '2025-01-11 09:15:00', 6, 'SWIPE'),
('READ003', 'HR Office Reader', 'HR Department', 'ONLINE', 'R005', '2025-01-11 10:00:00', 6, 'SWIPE'),
('READ004', 'Engineering Reader', 'Engineering Wing', 'OFFLINE', 'R004', '2025-01-10 16:45:00', 6, 'SWIPE'),
('READ005', 'Cafeteria Reader', 'Cafeteria Entrance', 'ONLINE', 'R007', '2025-01-11 11:20:00', 6, 'SWIPE'),
('READ006', 'Parking Reader', 'Parking Gate 1', 'MAINTENANCE', 'R008', '2025-01-09 14:30:00', 6, 'SWIPE'),
('READ007', 'Conference Reader', 'Conference Room A', 'ONLINE', 'R009', '2025-01-11 13:45:00', 6, 'SWIPE'),
('READ008', 'Emergency Reader', 'Emergency Exit East', 'ONLINE', 'R012', '2025-01-11 07:50:00', 6, 'SWIPE');

-- 8. 插入配置文件数据
INSERT INTO profiles (profile_id, profile_name, description, max_daily_access, max_weekly_access, priority_level, is_active, created_at, updated_at) VALUES
('PROF001', 'Standard Employee', 'Standard access for regular employees', 10, 50, 5, TRUE, '2025-01-01 09:00:00', '2025-01-01 09:00:00'),
('PROF002', 'Administrator', 'Full system access for administrators', 100, 500, 1, TRUE, '2025-01-01 09:00:00', '2025-01-01 09:00:00'),
('PROF003', 'Visitor', 'Limited access for visitors', 5, 20, 10, TRUE, '2025-01-01 09:00:00', '2025-01-01 09:00:00'),
('PROF004', 'Security Staff', 'Enhanced access for security personnel', 20, 100, 2, TRUE, '2025-01-01 09:00:00', '2025-01-01 09:00:00'),
('PROF005', 'Maintenance', 'Access for maintenance staff', 15, 75, 6, TRUE, '2025-01-01 09:00:00', '2025-01-01 09:00:00'),
('PROF006', 'After Hours', 'Access outside normal business hours', 5, 25, 8, FALSE, '2025-01-01 09:00:00', '2025-01-01 09:00:00');

-- 9. 插入时间过滤器数据
INSERT INTO time_filters (time_filter_id, filter_name, raw_rule, year, months, days_of_month, days_of_week, start_time, end_time, time_ranges, excluded_months, excluded_days_of_week, excluded_time_ranges, is_recurring, description) VALUES
('TF001', 'Business Hours', '2025.January-December.Monday-Friday.9:00-17:00', 2025, 'January,February,March,April,May,June,July,August,September,October,November,December', '1-31', 'Monday,Tuesday,Wednesday,Thursday,Friday', '09:00:00', '17:00:00', '9:00-17:00', NULL, 'Saturday,Sunday', NULL, TRUE, 'Standard business hours'),
('TF002', 'Weekend Access', '2025.January-December.Saturday-Sunday.8:00-20:00', 2025, 'January,February,March,April,May,June,July,August,September,October,November,December', '1-31', 'Saturday,Sunday', '08:00:00', '20:00:00', '8:00-20:00', NULL, 'Monday,Tuesday,Wednesday,Thursday,Friday', NULL, TRUE, 'Weekend access hours'),
('TF003', 'Night Shift', '2025.January-December.Monday-Friday.22:00-6:00', 2025, 'January,February,March,April,May,June,July,August,September,October,November,December', '1-31', 'Monday,Tuesday,Wednesday,Thursday,Friday', '22:00:00', '06:00:00', '22:00-6:00', NULL, 'Saturday,Sunday', NULL, TRUE, 'Night shift hours'),
('TF004', 'Holiday Block', '2025.January-December.*.0:00-23:59', 2025, 'January,February,March,April,May,June,July,August,September,October,November,December', '1,15,25', '*', '00:00:00', '23:59:59', '0:00-23:59', NULL, NULL, NULL, FALSE, 'Block specific days of month'),
('TF005', 'Summer Hours', '2025.June-August.Monday-Friday.8:00-16:00', 2025, 'June,July,August', '1-31', 'Monday,Tuesday,Wednesday,Thursday,Friday', '08:00:00', '16:00:00', '8:00-16:00', NULL, 'Saturday,Sunday', NULL, TRUE, 'Summer business hours');

-- 10. 插入配置文件-时间过滤器关联数据
INSERT INTO profile_time_filters (profile_id, time_filter_id) VALUES
('PROF001', 'TF001'),  -- Standard Employee uses business hours
('PROF002', 'TF001'),  -- Administrator uses business hours
('PROF002', 'TF002'),  -- Administrator also has weekend access
('PROF002', 'TF003'),  -- Administrator also has night shift access
('PROF003', 'TF001'),  -- Visitor uses business hours
('PROF004', 'TF001'),  -- Security Staff uses business hours
('PROF004', 'TF002'),  -- Security Staff also has weekend access
('PROF004', 'TF003'),  -- Security Staff also has night shift access
('PROF005', 'TF001'),  -- Maintenance uses business hours
('PROF005', 'TF003'),  -- Maintenance also has night shift access
('PROF006', 'TF003');  -- After Hours profile uses night shift only

-- 11. 插入配置文件-权限组关联数据
INSERT INTO profile_groups (profile_id, group_id) VALUES
('PROF001', 'G002'),  -- Standard Employee profile linked to Engineering group
('PROF001', 'G003'),  -- Standard Employee profile linked to HR group
('PROF001', 'G004'),  -- Standard Employee profile linked to IT Support group
('PROF002', 'G001'),  -- Administrator profile linked to Administrators group
('PROF003', 'G005'),  -- Visitor profile linked to Visitors group
('PROF004', 'G006'),  -- Security Staff profile linked to Security group
('PROF005', 'G008'),  -- Maintenance profile linked to Maintenance group
('PROF006', 'G001'),  -- After Hours profile linked to Administrators group
('PROF006', 'G006');  -- After Hours profile also linked to Security group

-- 12. 插入资源依赖关系数据
INSERT INTO resource_dependencies (resource_id, required_resource_id, time_window_minutes, description) VALUES
('R002', 'R001', 5, 'Server room requires main entrance access within 5 minutes'),
('R004', 'R001', 10, 'Engineering workshop requires main entrance access within 10 minutes'),
('R010', 'R005', 2, 'Finance safe requires HR office access within 2 minutes'),
('R012', 'R001', 1, 'Emergency exit requires main entrance access within 1 minute');

-- 13. 插入访问日志数据（展示各种决策和原因代码）
INSERT INTO access_logs (timestamp, badge_id, employee_id, resource_id, decision, reason_code) VALUES
('2025-01-11 08:30:00', 'B001', 'E001', 'R001', 'ALLOW', 'ALLOW'),
('2025-01-11 08:45:00', 'B002', 'E002', 'R005', 'ALLOW', 'ALLOW'),
('2025-01-11 09:15:00', 'B003', 'E003', 'R004', 'DENY', 'BADGE_INACTIVE'),
('2025-01-11 10:00:00', 'B004', 'E004', 'R002', 'DENY', 'NO_PERMISSION'),
('2025-01-11 11:30:00', 'B005', 'E005', 'R001', 'DENY', 'BADGE_INACTIVE'),
('2025-01-11 13:45:00', 'B006', 'E006', 'R002', 'ALLOW', 'ALLOW'),
('2025-01-11 14:20:00', 'B007', 'E007', 'R004', 'DENY', 'NO_PERMISSION'),
('2025-01-11 15:50:00', 'B001', 'E001', 'R002', 'ALLOW', 'ALLOW'),
('2025-01-11 17:00:00', 'B002', 'E002', 'R007', 'ALLOW', 'ALLOW'),
('2025-01-11 18:30:00', 'B008', 'E008', 'R010', 'DENY', 'RESOURCE_LOCKED'),
('2025-01-11 20:15:00', 'B009', 'E009', 'R012', 'DENY', 'BADGE_INACTIVE'),
('2025-01-11 21:45:00', 'B010', 'E010', 'R011', 'ALLOW', 'ALLOW'),
('2025-01-11 22:10:00', 'B001', 'E001', 'R009', 'ALLOW', 'ALLOW'),
('2025-01-11 23:30:00', 'B002', 'E002', 'R003', 'DENY', 'RESOURCE_OCCUPIED'),
('2025-01-12 07:45:00', 'B006', 'E006', 'R008', 'DENY', 'RESOURCE_NOT_FOUND'),
('2025-01-12 09:00:00', 'B004', 'E004', 'R006', 'ALLOW', 'ALLOW');

-- 完成初始化