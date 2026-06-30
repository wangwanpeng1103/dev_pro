CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    display_name VARCHAR(64) NOT NULL COMMENT '展示名称',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    user_type VARCHAR(16) NOT NULL COMMENT '用户类型：ADMIN、PERMANENT、TEMPORARY',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    expires_at DATETIME NULL COMMENT '临时用户过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_username (username)
) COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS ops_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目模块ID',
    project_code VARCHAR(64) NOT NULL COMMENT '项目编码',
    project_name VARCHAR(128) NOT NULL COMMENT '项目名称',
    description VARCHAR(255) NULL COMMENT '项目描述',
    icon_text VARCHAR(16) NULL COMMENT '项目图标文本',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_ops_project_code (project_code)
) COMMENT='运维项目模块表';

CREATE TABLE IF NOT EXISTS ops_user_project_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '授权ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    project_id BIGINT NOT NULL COMMENT '项目模块ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_project_permission (user_id, project_id)
) COMMENT='用户项目模块授权表';
