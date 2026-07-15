SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER TABLE pms_connection_config
    MODIFY COLUMN ssh_port INT UNSIGNED NULL DEFAULT NULL COMMENT 'SSH端口';

UPDATE pms_connection_config
SET ssh_port = NULL
WHERE ssh_port = 22
  AND ssh_username IS NULL
  AND ssh_host IS NULL
  AND ssh_password IS NULL;