INSERT INTO ops_project (project_code, project_name, description, icon_text, sort_order, enabled)
VALUES ('cloud-checkin', '云入住', '云入住运维项目模块', 'CI', 40, 1)
ON DUPLICATE KEY UPDATE
    project_name = VALUES(project_name),
    description = VALUES(description),
    icon_text = VALUES(icon_text),
    sort_order = VALUES(sort_order),
    enabled = VALUES(enabled);

INSERT IGNORE INTO ops_user_project_permission (user_id, project_id)
SELECT u.id, p.id
FROM sys_user u
JOIN ops_project p ON p.project_code = 'cloud-checkin'
WHERE u.username = 'admin';

INSERT IGNORE INTO ops_user_project_permission (user_id, project_id)
SELECT u.id, p.id
FROM sys_user u
JOIN ops_project p ON p.project_code = 'cloud-checkin'
WHERE u.username LIKE 'demo-user-%';
