SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS pms_connection_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    hotel_code VARCHAR(32) NOT NULL COMMENT '集团或酒店代码',
    hotel_name VARCHAR(200) NULL COMMENT '集团或酒店名称',
    entity_type VARCHAR(16) NULL COMMENT '类型：GROUP集团，HOTEL酒店',
    address_config VARCHAR(500) NULL COMMENT '集团或酒店地址服务配置',
    database_username VARCHAR(128) NULL COMMENT '数据库用户名',
    database_host VARCHAR(255) NULL COMMENT '数据库地址',
    database_password VARCHAR(255) NULL COMMENT '数据库明文密码',
    database_port INT UNSIGNED NULL DEFAULT 3306 COMMENT '数据库端口',
    ssh_username VARCHAR(128) NULL COMMENT 'SSH用户名',
    ssh_host VARCHAR(255) NULL COMMENT 'SSH地址',
    ssh_password VARCHAR(255) NULL COMMENT 'SSH明文密码',
    ssh_port INT UNSIGNED NULL DEFAULT 22 COMMENT 'SSH端口',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pms_connection_config_hotel_code (hotel_code),
    KEY idx_pms_connection_config_entity_type (entity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PMS地址服务与MySQL连接共享配置表';

INSERT INTO ops_project (project_code, project_name, description, icon_text, sort_order, enabled)
VALUES ('group-hotel-management', '集团酒店管理', '维护集团酒店地址服务及MySQL连接配置', 'GH', 50, 1)
ON DUPLICATE KEY UPDATE
    project_name = VALUES(project_name),
    description = VALUES(description),
    icon_text = VALUES(icon_text),
    sort_order = VALUES(sort_order),
    enabled = VALUES(enabled);

INSERT IGNORE INTO ops_user_project_permission (user_id, project_id)
SELECT u.id, p.id FROM sys_user u
JOIN ops_project p ON p.project_code = 'group-hotel-management'
WHERE u.username = 'admin';

INSERT IGNORE INTO ops_user_project_permission (user_id, project_id)
SELECT u.id, p.id FROM sys_user u
JOIN ops_project p ON p.project_code = 'group-hotel-management'
WHERE u.username LIKE 'demo-user-%';