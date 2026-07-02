SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

INSERT INTO sys_user (username, display_name, password_hash, user_type, enabled)
VALUES ('admin', '系统管理员', '{noop}admin', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    user_type = VALUES(user_type),
    enabled = VALUES(enabled);

INSERT INTO ops_project (project_code, project_name, description, icon_text, sort_order, enabled)
VALUES
    ('user-admin', '用户管理', '管理用户、临时用户和项目授权', 'UA', 10, 1),
    ('mihotel', 'mihotel', 'mihotel 运维项目模块', 'MI', 20, 1),
    ('ihotel', 'ihotel', 'ihotel 运维项目模块', 'IH', 30, 1),
    ('cloud-checkin', '云入住', '云入住运维项目模块', 'CI', 40, 1)
ON DUPLICATE KEY UPDATE
    project_name = VALUES(project_name),
    description = VALUES(description),
    icon_text = VALUES(icon_text),
    sort_order = VALUES(sort_order),
    enabled = VALUES(enabled);

INSERT IGNORE INTO ops_user_project_permission (user_id, project_id)
SELECT u.id, p.id
FROM sys_user u
JOIN ops_project p ON p.project_code IN ('user-admin', 'mihotel', 'ihotel', 'cloud-checkin')
WHERE u.username = 'admin';

INSERT INTO sys_user (username, display_name, password_hash, user_type, enabled)
VALUES
    ('demo-user-001', '模拟用户001', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-002', '模拟用户002', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-003', '模拟用户003', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-004', '模拟用户004', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-005', '模拟用户005', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-006', '模拟用户006', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-007', '模拟用户007', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-008', '模拟用户008', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-009', '模拟用户009', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-010', '模拟用户010', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-011', '模拟用户011', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-012', '模拟用户012', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-013', '模拟用户013', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-014', '模拟用户014', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-015', '模拟用户015', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-016', '模拟用户016', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-017', '模拟用户017', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-018', '模拟用户018', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-019', '模拟用户019', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-020', '模拟用户020', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-021', '模拟用户021', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-022', '模拟用户022', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-023', '模拟用户023', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-024', '模拟用户024', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-025', '模拟用户025', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-026', '模拟用户026', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-027', '模拟用户027', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-028', '模拟用户028', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-029', '模拟用户029', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-030', '模拟用户030', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-031', '模拟用户031', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-032', '模拟用户032', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-033', '模拟用户033', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-034', '模拟用户034', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-035', '模拟用户035', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-036', '模拟用户036', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-037', '模拟用户037', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-038', '模拟用户038', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-039', '模拟用户039', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-040', '模拟用户040', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-041', '模拟用户041', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-042', '模拟用户042', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-043', '模拟用户043', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-044', '模拟用户044', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-045', '模拟用户045', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-046', '模拟用户046', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-047', '模拟用户047', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-048', '模拟用户048', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-049', '模拟用户049', '{noop}1234', 'PERMANENT', 1),
    ('demo-user-050', '模拟用户050', '{noop}1234', 'PERMANENT', 1)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    password_hash = VALUES(password_hash),
    user_type = VALUES(user_type),
    enabled = VALUES(enabled);

INSERT IGNORE INTO ops_user_project_permission (user_id, project_id)
SELECT u.id, p.id
FROM sys_user u
JOIN ops_project p ON p.project_code IN ('mihotel', 'ihotel', 'cloud-checkin')
WHERE u.username LIKE 'demo-user-%';
