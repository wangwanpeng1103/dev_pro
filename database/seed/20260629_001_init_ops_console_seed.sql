INSERT INTO sys_user (username, display_name, password_hash, user_type, enabled)
VALUES ('admin', '系统管理员', '{noop}admin', 'ADMIN', 1);

INSERT INTO ops_project (project_code, project_name, description, icon_text, sort_order, enabled)
VALUES
    ('user-admin', '用户管理', '管理用户、临时用户、项目授权和功能树配置', 'UA', 10, 1),
    ('mihotel', 'mihotel', 'mihotel 运维项目模块', 'MI', 20, 1),
    ('ihotel', 'ihotel', 'ihotel 运维项目模块', 'IH', 30, 1);

INSERT INTO ops_user_project_permission (user_id, project_id)
SELECT u.id, p.id
FROM sys_user u
JOIN ops_project p ON p.project_code IN ('user-admin', 'mihotel', 'ihotel')
WHERE u.username = 'admin';

INSERT INTO ops_function_node (project_id, parent_id, node_code, node_name, node_type, route_path, external_url, sso_enabled, sort_order, enabled)
SELECT p.id, NULL, 'overview', '首页', 'MENU', CONCAT('/projects/', p.project_code, '/overview'), NULL, 0, 10, 1
FROM ops_project p
WHERE p.project_code IN ('mihotel', 'ihotel');

INSERT INTO ops_function_node (project_id, parent_id, node_code, node_name, node_type, route_path, external_url, sso_enabled, sort_order, enabled)
SELECT p.id, NULL, 'external-tool', '外部工具入口', 'EXTERNAL_LINK', NULL, 'https://example.com', 0, 20, 1
FROM ops_project p
WHERE p.project_code IN ('mihotel', 'ihotel');

