-- 初始化测试数据库数据
USE access_control_db;

-- 清空现有数据（按依赖反向顺序）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE access_logs;
TRUNCATE group_resources;
TRUNCATE employee_groups;
ALTER TABLE employees DROP FOREIGN KEY fk_employee_badge;
TRUNCATE badges;
TRUNCATE employees;
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
('G005', 'Visitors');

-- 2. 插入资源数据
INSERT INTO resources (resource_id, resource_name, resource_type, resource_state) VALUES
('R001', 'Main Entrance', 'DOOR', 'AVAILABLE'),
('R002', 'Server Room', 'ROOM', 'LOCKED'),
('R003', 'Office Printer 1', 'PRINTER', 'AVAILABLE'),
('R004', 'Engineering Workshop', 'ROOM', 'OCCUPIED'),
('R005', 'HR Office', 'ROOM', 'AVAILABLE'),
('R006', 'Executive Laptop', 'COMPUTER', 'AVAILABLE'),
('R007', 'Cafeteria Door', 'DOOR', 'AVAILABLE');

-- 3. 插入员工数据
INSERT INTO employees (employee_id, employee_name, badge_id) VALUES
('E001', 'John Doe', NULL),
('E002', 'Jane Smith', NULL),
('E003', 'Robert Johnson', NULL),
('E004', 'Emily Davis', NULL),
('E005', 'Michael Brown', NULL),
('E006', 'Sarah Wilson', NULL),
('E007', 'David Miller', NULL);

-- 4. 插入徽章数据
INSERT INTO badges (badge_id, status, employee_id) VALUES
('B001', 'ACTIVE', 'E001'),
('B002', 'ACTIVE', 'E002'),
('B003', 'DISABLED', 'E003'),
('B004', 'ACTIVE', 'E004'),
('B005', 'LOST', 'E005'),
('B006', 'ACTIVE', 'E006'),
('B007', 'ACTIVE', 'E007');

-- 5. 更新员工表的badge_id关联
UPDATE employees SET badge_id = 'B001' WHERE employee_id = 'E001';
UPDATE employees SET badge_id = 'B002' WHERE employee_id = 'E002';
UPDATE employees SET badge_id = 'B003' WHERE employee_id = 'E003';
UPDATE employees SET badge_id = 'B004' WHERE employee_id = 'E004';
UPDATE employees SET badge_id = 'B005' WHERE employee_id = 'E005';
UPDATE employees SET badge_id = 'B006' WHERE employee_id = 'E006';
UPDATE employees SET badge_id = 'B007' WHERE employee_id = 'E007';

-- 6. 插入员工-组关联数据
INSERT INTO employee_groups (employee_id, group_id) VALUES
('E001', 'G001'),  -- John Doe 属于管理员组
('E002', 'G003'),  -- Jane Smith 属于HR组
('E003', 'G002'),  -- Robert Johnson 属于工程组
('E004', 'G002'),  -- Emily Davis 属于工程组
('E005', 'G004'),  -- Michael Brown 属于IT组
('E006', 'G004'),  -- Sarah Wilson 属于IT组
('E007', 'G005');  -- David Miller 属于访客组

-- 7. 插入组-资源关联数据
INSERT INTO group_resources (group_id, resource_id) VALUES
('G001', 'R001'),  -- 管理员可访问主入口
('G001', 'R002'),  -- 管理员可访问服务器机房
('G001', 'R003'),  -- 管理员可访问打印机
('G001', 'R004'),  -- 管理员可访问工程车间
('G001', 'R005'),  -- 管理员可访问HR办公室
('G001', 'R006'),  -- 管理员可访问高管笔记本
('G001', 'R007'),  -- 管理员可访问食堂门
('G002', 'R001'),  -- 工程组可访问主入口
('G002', 'R004'),  -- 工程组可访问工程车间
('G002', 'R003'),  -- 工程组可访问打印机
('G003', 'R001'),  -- HR组可访问主入口
('G003', 'R005'),  -- HR组可访问HR办公室
('G003', 'R003'),  -- HR组可访问打印机
('G004', 'R001'),  -- IT组可访问主入口
('G004', 'R002'),  -- IT组可访问服务器机房
('G004', 'R006'),  -- IT组可访问高管笔记本
('G005', 'R001'),  -- 访客组可访问主入口
('G005', 'R007');  -- 访客组可访问食堂门

-- 8. 插入访问日志数据
INSERT INTO access_logs (timestamp, badge_id, employee_id, resource_id, decision, reason_code) VALUES
('2024-05-01 08:30:00', 'B001', 'E001', 'R001', 'ALLOW', 'ALLOW'),
('2024-05-01 08:45:00', 'B002', 'E002', 'R005', 'ALLOW', 'ALLOW'),
('2024-05-01 09:15:00', 'B003', 'E003', 'R004', 'DENY', 'BADGE_INACTIVE'),
('2024-05-01 10:00:00', 'B004', 'E004', 'R002', 'DENY', 'NO_PERMISSION'),
('2024-05-01 11:30:00', 'B005', 'E005', 'R001', 'DENY', 'BADGE_INACTIVE'),
('2024-05-01 13:45:00', 'B006', 'E006', 'R002', 'ALLOW', 'ALLOW'),
('2024-05-01 14:20:00', 'B007', 'E007', 'R004', 'DENY', 'NO_PERMISSION'),
('2024-05-01 15:50:00', 'B001', 'E001', 'R002', 'ALLOW', 'ALLOW'),
('2024-05-01 17:00:00', 'B002', 'E002', 'R007', 'ALLOW', 'ALLOW');