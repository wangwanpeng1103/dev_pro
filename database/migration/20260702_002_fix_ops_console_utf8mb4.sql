SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE sys_user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE ops_project CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE ops_user_project_permission CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

UPDATE sys_user
SET display_name = '系统管理员'
WHERE username = 'admin';

UPDATE sys_user
SET display_name = CONCAT('模拟用户', LPAD(SUBSTRING(username, -3), 3, '0'))
WHERE username LIKE 'demo-user-%';

UPDATE ops_project
SET project_name = '用户管理',
    description = '管理用户、临时用户和项目授权'
WHERE project_code = 'user-admin';

UPDATE ops_project
SET project_name = 'mihotel',
    description = 'mihotel 运维项目模块'
WHERE project_code = 'mihotel';

UPDATE ops_project
SET project_name = 'ihotel',
    description = 'ihotel 运维项目模块'
WHERE project_code = 'ihotel';

UPDATE ops_project
SET project_name = '云入住',
    description = '云入住运维项目模块'
WHERE project_code = 'cloud-checkin';
