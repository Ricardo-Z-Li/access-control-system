-- Active: 1767078462975@@127.0.0.1@3306@mysql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS access_control_db;
USE access_control_db;

-- 1. 先创建无循环依赖的基础表：权限组表
CREATE TABLE IF NOT EXISTS group_permissions (
    group_id VARCHAR(50) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- 2. 资源表（无外键依赖，优先创建）
CREATE TABLE IF NOT EXISTS resources (
    resource_id VARCHAR(50) NOT NULL PRIMARY KEY,
    resource_name VARCHAR(100) NOT NULL,
    resource_type ENUM('PENDING','DOOR', 'PRINTER', 'COMPUTER', 'ROOM', 'OTHER') NOT NULL,
    resource_state ENUM('PENDING','AVAILABLE', 'OCCUPIED', 'LOCKED', 'OFFLINE') NOT NULL
);

-- 3. 先创建 employees 表（无外键，后续追加 badge_id 外键）
CREATE TABLE IF NOT EXISTS employees (
    employee_id VARCHAR(50) NOT NULL PRIMARY KEY,
    employee_name VARCHAR(100) NOT NULL,
    badge_id VARCHAR(50) -- 先不建外键
);

-- 4. 创建 badges 表（引用 employees 的外键，此时 employees 已存在）
CREATE TABLE IF NOT EXISTS badges (
    badge_id VARCHAR(50) NOT NULL PRIMARY KEY,
    status ENUM('ACTIVE', 'DISABLED', 'LOST') NOT NULL
);

-- 5. 给 employees 表追加 badge_id 外键（解决循环依赖）
ALTER TABLE employees 
ADD CONSTRAINT fk_employee_badge 
FOREIGN KEY (badge_id) REFERENCES badges(badge_id) ON DELETE CASCADE;

-- 6. 员工-组关联表（多对多，引用已存在的 group_permissions）
CREATE TABLE IF NOT EXISTS employee_groups (
    employee_id VARCHAR(50) NOT NULL,
    group_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (employee_id, group_id),
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES group_permissions(group_id) ON DELETE CASCADE
);

-- 7. 组-资源关联表（多对多，引用已存在的 group_permissions）
CREATE TABLE IF NOT EXISTS group_resources (
    group_id VARCHAR(50) NOT NULL,
    resource_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (group_id, resource_id),
    FOREIGN KEY (group_id) REFERENCES group_permissions(group_id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES resources(resource_id) ON DELETE CASCADE
);

-- 8. 访问日志表
CREATE TABLE IF NOT EXISTS access_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    badge_id VARCHAR(50),
    employee_id VARCHAR(50),
    resource_id VARCHAR(50),
    decision ENUM('PENDING', 'ALLOW', 'DENY') NOT NULL,
    reason_code ENUM(
        'PENDING',
        'ALLOW', 
        'BADGE_NOT_FOUND', 
        'BADGE_INACTIVE', 
        'EMPLOYEE_NOT_FOUND', 
        'RESOURCE_NOT_FOUND', 
        'RESOURCE_LOCKED', 
        'RESOURCE_OCCUPIED', 
        'NO_PERMISSION', 
        'INVALID_REQUEST', 
        'SYSTEM_ERROR'
    ) NOT NULL,
    FOREIGN KEY (badge_id) REFERENCES badges(badge_id) ON DELETE SET NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE SET NULL,
    FOREIGN KEY (resource_id) REFERENCES resources(resource_id) ON DELETE CASCADE
 );

-- 9. 新增表：badge_readers（读卡器表）
CREATE TABLE IF NOT EXISTS badge_readers (
    reader_id VARCHAR(50) NOT NULL PRIMARY KEY,
    reader_name VARCHAR(100) NOT NULL,
    location VARCHAR(200),
    status VARCHAR(20) NOT NULL,
    resource_id VARCHAR(50),
    last_seen DATETIME,
    badge_code_length INT,
    FOREIGN KEY (resource_id) REFERENCES resources(resource_id) ON DELETE SET NULL
);

-- 10. 新增表：profiles（配置文件表）
CREATE TABLE IF NOT EXISTS profiles (
    profile_id VARCHAR(50) NOT NULL PRIMARY KEY,
    profile_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    max_daily_access INT,
    max_weekly_access INT,
    priority_level INT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- 11. 新增表：time_filters（时间过滤器表）
CREATE TABLE IF NOT EXISTS time_filters (
    time_filter_id VARCHAR(50) NOT NULL PRIMARY KEY,
    filter_name VARCHAR(100) NOT NULL,
    raw_rule VARCHAR(500),
    year INT,
    months VARCHAR(100),
    days_of_month VARCHAR(100),
    days_of_week VARCHAR(50),
    start_time TIME,
    end_time TIME,
    is_recurring BOOLEAN DEFAULT FALSE,
    description VARCHAR(500)
);

-- 12. 新增关联表：profile_time_filters（配置文件-时间过滤器多对多）
CREATE TABLE IF NOT EXISTS profile_time_filters (
    profile_id VARCHAR(50) NOT NULL,
    time_filter_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (profile_id, time_filter_id),
    FOREIGN KEY (profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
    FOREIGN KEY (time_filter_id) REFERENCES time_filters(time_filter_id) ON DELETE CASCADE
);

-- 13. 新增关联表：profile_groups（配置文件-权限组多对多）
CREATE TABLE IF NOT EXISTS profile_groups (
    profile_id VARCHAR(50) NOT NULL,
    group_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (profile_id, group_id),
    FOREIGN KEY (profile_id) REFERENCES profiles(profile_id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES group_permissions(group_id) ON DELETE CASCADE
);

-- 14. 修改现有表：添加新字段

-- 为badges表添加新字段
ALTER TABLE badges 
ADD COLUMN expiration_date DATE,
ADD COLUMN badge_code VARCHAR(100),
ADD COLUMN last_updated DATETIME;

-- 为resources表添加新字段
ALTER TABLE resources 
ADD COLUMN is_controlled BOOLEAN DEFAULT TRUE;

-- 为employees表添加新字段
ALTER TABLE employees 
ADD COLUMN user_type ENUM('EMPLOYEE', 'CONTRACTOR', 'VISITOR', 'ADMIN', 'GUEST') NOT NULL DEFAULT 'EMPLOYEE';

-- 创建索引（仅保留非主键的有效索引）
CREATE INDEX idx_group_id ON group_permissions(group_id); 
CREATE INDEX idx_log_badge ON access_logs(badge_id);
CREATE INDEX idx_log_employee ON access_logs(employee_id);
CREATE INDEX idx_log_resource ON access_logs(resource_id);
CREATE INDEX idx_log_timestamp ON access_logs(timestamp);
CREATE INDEX idx_log_decision ON access_logs(decision);

-- 新增表：resource_dependencies（资源依赖关系表）
CREATE TABLE IF NOT EXISTS resource_dependencies (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resource_id VARCHAR(50) NOT NULL,
    required_resource_id VARCHAR(50) NOT NULL,
    time_window_minutes INT,
    description VARCHAR(500),
    FOREIGN KEY (resource_id) REFERENCES resources(resource_id) ON DELETE CASCADE,
    FOREIGN KEY (required_resource_id) REFERENCES resources(resource_id) ON DELETE CASCADE
);

-- 为resource_dependencies表创建索引
CREATE INDEX idx_dep_resource ON resource_dependencies(resource_id);
CREATE INDEX idx_dep_required ON resource_dependencies(required_resource_id);