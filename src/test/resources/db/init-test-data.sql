-- Active: 1766819106838@@127.0.0.1@3306@access_control_db
-- init-test-data.sql
-- Test data for Access Control System
-- All names in English, person names in "John White" format

-- Clear existing data (if any) - delete in reverse order to respect foreign key constraints
DELETE FROM access_logs;
DELETE FROM resource_dependencies;
DELETE FROM profile_resource_limits;
DELETE FROM profile_badges;
DELETE FROM profile_employees;
DELETE FROM profile_groups;
DELETE FROM profile_time_filters;
DELETE FROM badge_readers;
DELETE FROM group_resources;
DELETE FROM employee_groups;
DELETE FROM profiles;
DELETE FROM time_filters;
DELETE FROM employees;
DELETE FROM badges;
DELETE FROM group_permissions;
DELETE FROM resources;

-- 1. Insert permission groups (group_permissions table) - no dependencies
INSERT INTO group_permissions (group_id, name) VALUES
('GROUP001', 'Administrators'),
('GROUP002', 'Engineering'),
('GROUP003', 'Finance'),
('GROUP004', 'Human Resources'),
('GROUP005', 'Facilities'),
('GROUP006', 'Security'),
('GROUP007', 'Guests');

-- 2. Insert resources (resources table) - no dependencies
INSERT INTO resources (resource_id, resource_name, resource_type, resource_state, is_controlled) VALUES
('RES001', 'Main Entrance Door', 'DOOR', 'AVAILABLE', TRUE),
('RES002', 'Server Room Door', 'DOOR', 'LOCKED', TRUE),
('RES003', 'Finance Office Door', 'DOOR', 'AVAILABLE', TRUE),
('RES004', 'Color Printer 3rd Floor', 'PRINTER', 'AVAILABLE', TRUE),
('RES005', 'Engineering Lab Computer', 'COMPUTER', 'OCCUPIED', TRUE),
('RES006', 'Conference Room A', 'ROOM', 'AVAILABLE', TRUE),
('RES007', 'Executive Office', 'ROOM', 'AVAILABLE', TRUE),
('RES008', 'Security Control Room', 'ROOM', 'OCCUPIED', TRUE),
('RES009', 'Document Printer', 'PRINTER', 'OFFLINE', TRUE),
('RES010', 'Guest WiFi Access', 'OTHER', 'AVAILABLE', FALSE),
('RES011', 'Parking Gate', 'DOOR', 'AVAILABLE', TRUE),
('RES012', 'Cafeteria Entrance', 'DOOR', 'AVAILABLE', TRUE);

-- Set resource locations (building/floor/coord)
UPDATE resources SET building = 'SITE', floor = 'G', coord_x = 180, coord_y = 140, location = 'Main Gate' WHERE resource_id = 'RES001';
UPDATE resources SET building = 'OFFICE', floor = '3F', coord_x = 1760, coord_y = 210, location = 'Server Room' WHERE resource_id = 'RES002';
UPDATE resources SET building = 'OFFICE', floor = '2F', coord_x = 1460, coord_y = 560, location = 'Finance Office' WHERE resource_id = 'RES003';
UPDATE resources SET building = 'OFFICE', floor = '3F', coord_x = 1180, coord_y = 230, location = 'Printer Area' WHERE resource_id = 'RES004';
UPDATE resources SET building = 'OFFICE', floor = '3F', coord_x = 680, coord_y = 220, location = 'Engineering Lab' WHERE resource_id = 'RES005';
UPDATE resources SET building = 'OFFICE', floor = '1F', coord_x = 740, coord_y = 610, location = 'Conference Room A' WHERE resource_id = 'RES006';
UPDATE resources SET building = 'OFFICE', floor = '2F', coord_x = 1240, coord_y = 540, location = 'Executive Office' WHERE resource_id = 'RES007';
UPDATE resources SET building = 'OFFICE', floor = '1F', coord_x = 420, coord_y = 720, location = 'Security Room' WHERE resource_id = 'RES008';
UPDATE resources SET building = 'OFFICE', floor = '1F', coord_x = 1580, coord_y = 700, location = 'Document Printer' WHERE resource_id = 'RES009';
UPDATE resources SET building = 'SITE', floor = 'G', coord_x = 980, coord_y = 180, location = 'Guest WiFi' WHERE resource_id = 'RES010';
UPDATE resources SET building = 'SITE', floor = 'G', coord_x = 260, coord_y = 260, location = 'Parking Gate' WHERE resource_id = 'RES011';
UPDATE resources SET building = 'SITE', floor = 'G', coord_x = 720, coord_y = 720, location = 'Cafeteria' WHERE resource_id = 'RES012';
-- 3. Insert badges (badges table) - simple insert, only required fields
INSERT INTO badges (badge_id, status) VALUES
('BADGE001', 'ACTIVE'),
('BADGE002', 'ACTIVE'),
('BADGE003', 'ACTIVE'),
('BADGE004', 'DISABLED'),
('BADGE005', 'LOST'),
('BADGE006', 'ACTIVE'),
('BADGE007', 'ACTIVE'),
('BADGE008', 'ACTIVE');

-- 4. Insert employees (employees table) - depends on badges
INSERT INTO employees (employee_id, employee_name, badge_id) VALUES
('EMP001', 'John White', 'BADGE001'),
('EMP002', 'Sarah Johnson', 'BADGE002'),
('EMP003', 'Michael Brown', 'BADGE003'),
('EMP004', 'Emily Davis', 'BADGE004'),
('EMP005', 'David Wilson', 'BADGE005'),
('EMP006', 'Jennifer Lee', 'BADGE006'),
('EMP007', 'Robert Taylor', 'BADGE007'),
('EMP008', 'Jessica Miller', 'BADGE008');

-- 5. Insert employee-group associations (employee_groups table)
INSERT INTO employee_groups (employee_id, group_id) VALUES
('EMP001', 'GROUP001'),
('EMP001', 'GROUP002'),
('EMP002', 'GROUP002'),
('EMP003', 'GROUP002'),
('EMP004', 'GROUP003'),
('EMP005', 'GROUP005'),
('EMP006', 'GROUP004'),
('EMP007', 'GROUP006'),
('EMP008', 'GROUP007');

-- 6. Insert group-resource associations (group_resources table)
-- Administrators have access to everything
INSERT INTO group_resources (group_id, resource_id) VALUES
('GROUP001', 'RES001'),
('GROUP001', 'RES002'),
('GROUP001', 'RES003'),
('GROUP001', 'RES004'),
('GROUP001', 'RES005'),
('GROUP001', 'RES006'),
('GROUP001', 'RES007'),
('GROUP001', 'RES008'),
('GROUP001', 'RES009'),
('GROUP001', 'RES010'),
('GROUP001', 'RES011'),
('GROUP001', 'RES012');

-- Engineering access
INSERT INTO group_resources (group_id, resource_id) VALUES
('GROUP002', 'RES001'),
('GROUP002', 'RES002'),
('GROUP002', 'RES004'),
('GROUP002', 'RES005'),
('GROUP002', 'RES006');

-- Finance access
INSERT INTO group_resources (group_id, resource_id) VALUES
('GROUP003', 'RES001'),
('GROUP003', 'RES003'),
('GROUP003', 'RES004'),
('GROUP003', 'RES006');

-- HR access
INSERT INTO group_resources (group_id, resource_id) VALUES
('GROUP004', 'RES001'),
('GROUP004', 'RES004'),
('GROUP004', 'RES006'),
('GROUP004', 'RES007');

-- Facilities access
INSERT INTO group_resources (group_id, resource_id) VALUES
('GROUP005', 'RES001'),
('GROUP005', 'RES002'),
('GROUP005', 'RES011'),
('GROUP005', 'RES012');

-- Security access
INSERT INTO group_resources (group_id, resource_id) VALUES
('GROUP006', 'RES001'),
('GROUP006', 'RES002'),
('GROUP006', 'RES008'),
('GROUP006', 'RES011');

-- Guests access
INSERT INTO group_resources (group_id, resource_id) VALUES
('GROUP007', 'RES001'),
('GROUP007', 'RES006'),
('GROUP007', 'RES010'),
('GROUP007', 'RES012');

-- 7. Insert badge readers (badge_readers table)
INSERT INTO badge_readers (reader_id, reader_name, location, status, resource_id, last_seen, badge_code_length, operation_mode) VALUES
('READER001', 'Main Entrance Reader', 'Building A Lobby', 'ONLINE', 'RES001', NOW(), 9, 'SWIPE'),
('READER002', 'Server Room Reader', 'Floor 3 Room 301', 'ONLINE', 'RES002', NOW(), 9, 'PROXIMITY'),
('READER003', 'Finance Office Reader', 'Floor 2 Room 205', 'ONLINE', 'RES003', NOW(), 9, 'SWIPE'),
('READER004', 'Conference Room Reader', 'Floor 1 Room 101', 'OFFLINE', 'RES006', DATE_SUB(NOW(), INTERVAL 1 HOUR), 9, 'SWIPE'),
('READER005', 'Parking Gate Reader', 'Parking Lot Entrance', 'ONLINE', 'RES011', NOW(), 9, 'PROXIMITY'),
('READER006', 'Cafeteria Reader', 'Cafeteria Entrance', 'MAINTENANCE', 'RES012', DATE_SUB(NOW(), INTERVAL 2 DAY), 9, 'SWIPE');

-- 8. Insert profiles (profiles table)
INSERT INTO profiles (profile_id, profile_name, description, max_daily_access, max_weekly_access, priority_level, is_active, created_at, updated_at) VALUES
('PROFILE001', 'Standard Employee Profile', 'Default profile for regular employees', 10, 50, 5, TRUE, NOW(), NOW()),
('PROFILE002', 'Admin Full Access', 'Administrators with full system access', NULL, NULL, 1, TRUE, NOW(), NOW()),
('PROFILE003', 'Time Restricted Access', 'Access limited to business hours', 5, 25, 10, TRUE, NOW(), NOW()),
('PROFILE004', 'Guest Limited Access', 'Limited access for visitors and guests', 3, 15, 20, TRUE, NOW(), NOW()),
('PROFILE005', 'After Hours Access', 'Access permitted outside normal hours', 2, 10, 15, TRUE, NOW(), NOW());

-- 9. Insert time filters (time_filters table)
INSERT INTO time_filters (time_filter_id, filter_name, raw_rule, year, months, days_of_month, days_of_week, start_time, end_time, time_ranges, excluded_months, excluded_days_of_week, excluded_time_ranges, is_recurring, description) VALUES
('TIMEFILTER001', 'Business Hours', '*.January-December.Monday-Friday.8:00-18:00', NULL, '1-12', NULL, '1-5', '08:00:00', '18:00:00', '08:00-18:00', NULL, NULL, NULL, TRUE, 'Standard business hours 8AM-6PM weekdays'),
('TIMEFILTER002', 'Weekend Access', '*.January-December.Saturday-Sunday.*', NULL, '1-12', NULL, '6-7', NULL, NULL, NULL, NULL, NULL, NULL, TRUE, 'Weekend access only'),
('TIMEFILTER003', 'After Hours', '*.January-December.Monday-Friday.18:00-22:00', NULL, '1-12', NULL, '1-5', '18:00:00', '22:00:00', '18:00-22:00', NULL, NULL, NULL, TRUE, 'Evening access after business hours'),
('TIMEFILTER004', 'Holiday Exclusion', '*.January-December.*.*', NULL, '1-12', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, TRUE, 'Holiday period exclusion'),
('TIMEFILTER005', 'Quarter End', '2025.March,June,September,December.1-10.Monday-Friday.6:00-20:00', 2025, '3,6,9,12', '1-10', '1-5', '06:00:00', '20:00:00', '06:00-20:00', NULL, NULL, NULL, FALSE, 'Extended hours during quarter end');

-- 10. Insert profile-time filter associations (profile_time_filters table)
INSERT INTO profile_time_filters (profile_id, time_filter_id) VALUES
('PROFILE001', 'TIMEFILTER001'),
('PROFILE003', 'TIMEFILTER001'),
('PROFILE003', 'TIMEFILTER004'),
('PROFILE004', 'TIMEFILTER001'),
('PROFILE005', 'TIMEFILTER003'),
('PROFILE005', 'TIMEFILTER002');

-- 11. Insert profile-group associations (profile_groups table)
INSERT INTO profile_groups (profile_id, group_id) VALUES
('PROFILE001', 'GROUP002'),
('PROFILE001', 'GROUP003'),
('PROFILE001', 'GROUP004'),
('PROFILE001', 'GROUP005'),
('PROFILE002', 'GROUP001'),
('PROFILE002', 'GROUP006'),
('PROFILE003', 'GROUP002'),
('PROFILE004', 'GROUP007'),
('PROFILE005', 'GROUP001'),
('PROFILE005', 'GROUP006');

-- 12. Insert access logs (access_logs table)
INSERT INTO access_logs (timestamp, badge_id, employee_id, resource_id, decision, reason_code) VALUES
(DATE_SUB(NOW(), INTERVAL 2 HOUR), 'BADGE001', 'EMP001', 'RES001', 'ALLOW', 'ALLOW'),
(DATE_SUB(NOW(), INTERVAL 3 HOUR), 'BADGE002', 'EMP002', 'RES002', 'DENY', 'NO_PERMISSION'),
(DATE_SUB(NOW(), INTERVAL 4 HOUR), 'BADGE003', 'EMP003', 'RES004', 'ALLOW', 'ALLOW'),
(DATE_SUB(NOW(), INTERVAL 5 HOUR), 'BADGE004', 'EMP004', 'RES003', 'DENY', 'BADGE_INACTIVE'),
(DATE_SUB(NOW(), INTERVAL 6 HOUR), 'BADGE005', 'EMP005', 'RES011', 'DENY', 'BADGE_INACTIVE'),
(DATE_SUB(NOW(), INTERVAL 7 HOUR), 'BADGE001', 'EMP001', 'RES006', 'ALLOW', 'ALLOW'),
(DATE_SUB(NOW(), INTERVAL 8 HOUR), 'BADGE002', 'EMP002', 'RES005', 'ALLOW', 'ALLOW'),
(DATE_SUB(NOW(), INTERVAL 9 HOUR), 'BADGE003', 'EMP003', 'RES002', 'DENY', 'RESOURCE_LOCKED'),
(DATE_SUB(NOW(), INTERVAL 10 HOUR), 'BADGE006', 'EMP006', 'RES007', 'ALLOW', 'ALLOW'),
(DATE_SUB(NOW(), INTERVAL 11 HOUR), 'BADGE007', 'EMP007', 'RES008', 'ALLOW', 'ALLOW'),
(DATE_SUB(NOW(), INTERVAL 12 HOUR), 'BADGE008', 'EMP008', 'RES010', 'ALLOW', 'ALLOW'),
(DATE_SUB(NOW(), INTERVAL 13 HOUR), 'BADGE008', 'EMP008', 'RES002', 'DENY', 'NO_PERMISSION');

-- 13. Insert resource dependencies (resource_dependencies table)
INSERT INTO resource_dependencies (resource_id, required_resource_id, time_window_minutes, description) VALUES
('RES002', 'RES001', 5, 'Must access main entrance within 5 minutes before server room'),
('RES007', 'RES001', 10, 'Executive office requires main entrance access within 10 minutes'),
('RES008', 'RES001', 2, 'Security room requires recent main entrance access'),
('RES005', 'RES001', 15, 'Engineering lab requires main entrance access within 15 minutes');

-- End of test data initialization
