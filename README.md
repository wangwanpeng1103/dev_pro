# devPro 项目骨架

## 技术栈

- JDK：21 LTS
- Spring Boot：3.5.16
- Vue：3.x
- Node.js：22 LTS
- 数据库：MySQL 8.4 LTS
- 部署：Docker Compose

## 目录结构

- `backend/`：Spring Boot 后端服务。
- `frontend/`：Vue 3 前端应用。
- `docker-compose.yml`：本地联调与部署编排。
- `.env.example`：本地环境变量示例。

## 本地开发

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

Docker Compose：

```bash
copy .env.example .env
docker compose up --build
```

数据库连接信息后续补充到 `.env`，不要提交真实密码。

