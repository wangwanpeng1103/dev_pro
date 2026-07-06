# devPro 生产发布说明

## 发布目标

生产环境使用 `docker-compose.prod.yml` 启动 MySQL、后端和前端服务。MySQL 容器首次初始化时会自动执行现有 SQL 脚本，后端默认连接生产编排内置的 MySQL 服务。

## 初始化脚本

生产 MySQL 首次创建数据卷时按以下顺序执行：

- `database/schema/20260629_001_create_ops_console_tables.sql`
- `database/seed/20260629_001_init_ops_console_seed.sql`
- `database/migration/20260702_001_add_cloud_checkin_project.sql`
- `database/migration/20260702_002_fix_ops_console_utf8mb4.sql`

这些脚本由 MySQL 官方镜像的 `/docker-entrypoint-initdb.d/` 机制触发。该机制只在 MySQL 数据目录为空、首次初始化时执行；已有数据卷不会重复执行初始化脚本。

MySQL 容器启动时显式使用 `utf8mb4` 和 `utf8mb4_0900_ai_ci`，数据库脚本也会先执行 `SET NAMES utf8mb4`，避免中文初始化数据乱码。

## 服务器环境变量

在服务器项目目录创建 `.env`，至少包含以下配置：

```properties
MYSQL_DATABASE=dev_pro
MYSQL_USER=devpro
MYSQL_PASSWORD=数据库用户密码
MYSQL_ROOT_PASSWORD=数据库 root 密码

SPRING_PROFILES_ACTIVE=docker
BACKEND_PORT=8080
FRONTEND_PORT=80
MYSQL_PORT=3306
```

后端默认连接 `jdbc:mysql://mysql:3306/${MYSQL_DATABASE}`。如确实需要覆盖连接地址，可额外配置：

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/dev_pro?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_0900_ai_ci&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=devpro
SPRING_DATASOURCE_PASSWORD=数据库用户密码
```

`mihotel` 缓存清理目标建议只在 `.env` 中配置编码和地址，展示名由后端按编码和端口生成，避免 `.env` 中文编码差异导致页面乱码：

```properties
MIHOTEL_CLEAR_CACHE_TRUNK_TARGETS=trunk-8104|http://example.com:8104,trunk-8106|http://example.com:8106
MIHOTEL_CLEAR_CACHE_LOCAL_TARGETS=local-8080|http://localhost:8080
MIHOTEL_CLEAR_CACHE_READ_TIMEOUT_SECONDS=180
```

## 发布命令

```bash
docker compose -f docker-compose.prod.yml up --build -d
```

## 验证命令

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs --tail=100 backend
curl http://127.0.0.1:8080/api/health
```

## 端口说明

- `FRONTEND_PORT`：前端宿主机端口，默认 `80`。
- `BACKEND_PORT`：后端宿主机端口，默认 `8080`。
- `MYSQL_PORT`：生产 MySQL 宿主机端口，默认 `3306`。
- 当前服务器的 `3305` 用于 SSH 登录，不能同时作为 MySQL 宿主机端口。
- 如果服务器内部验证正常，但本机无法访问上述端口，需要检查上游安全组、网络 ACL 或机房防火墙。

## 已有数据卷重新初始化

如果需要清空生产 MySQL 并重新执行初始化脚本，必须先确认数据可丢弃或已备份，再删除 `mysql-prod-data` 数据卷。该操作会删除生产数据库数据，不能作为常规发布步骤。

如果生产库已经初始化过，只需要修复字符集和已知种子数据中文乱码，不要删除数据卷；应备份后手动执行 `database/migration/20260702_002_fix_ops_console_utf8mb4.sql`。

## 安全约定

- 数据库密码、服务器密码和 root 权限信息不得写入 Git。
- 本次用户已明确授权：发布过程中如自动化工具无法交互输入密码，可以临时通过命令参数传入服务器密码；命令执行完成后不得把真实密码记录到文档、脚本或仓库文件。
- 生产 MySQL 如需对本地开放访问，应优先通过内网、VPN 或来源 IP 限制控制访问范围。
