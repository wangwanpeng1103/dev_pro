SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE pms_connection_config
    MODIFY COLUMN hotel_name VARCHAR(200) NULL COMMENT '集团或酒店名称',
    MODIFY COLUMN entity_type VARCHAR(16) NULL COMMENT '类型：GROUP集团，HOTEL酒店',
    MODIFY COLUMN address_config VARCHAR(500) NULL COMMENT '集团或酒店地址服务配置',
    MODIFY COLUMN database_username VARCHAR(128) NULL COMMENT '数据库用户名',
    MODIFY COLUMN database_host VARCHAR(255) NULL COMMENT '数据库地址',
    MODIFY COLUMN database_password VARCHAR(255) NULL COMMENT '数据库明文密码',
    MODIFY COLUMN database_port INT UNSIGNED NULL DEFAULT 3306 COMMENT '数据库端口';
