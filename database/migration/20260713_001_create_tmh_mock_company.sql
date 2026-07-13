SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ihotel_tmh_mock_company (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模拟协议单位ID',
    enterprise_code VARCHAR(64) NOT NULL COMMENT '企业编码',
    enterprise_name VARCHAR(200) NOT NULL COMMENT '企业名称',
    open_status TINYINT NOT NULL DEFAULT 1 COMMENT '开放状态：1启用，0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_tmh_mock_company_code (enterprise_code),
    KEY idx_tmh_mock_company_updated_at (updated_at),
    KEY idx_tmh_mock_company_status_id (open_status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='天目湖接口模拟协议单位表';

INSERT INTO ihotel_tmh_mock_company (enterprise_code, enterprise_name, open_status)
VALUES
    ('KHCSZQL10FXS', '测试中青旅10分销商', 1),
    ('KHCSZQL09FXS', '测试中青旅09分销商', 1)
ON DUPLICATE KEY UPDATE
    enterprise_name = VALUES(enterprise_name),
    open_status = VALUES(open_status);
