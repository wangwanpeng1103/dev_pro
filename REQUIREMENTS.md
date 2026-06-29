# 需求变更记录

本文档用于记录本项目每次需求、约定和实现变更的背景，方便后续回看当时的业务逻辑和技术决策。

## 记录规则

- 只要用户提出新需求、调整需求、技术选型变化或影响代码结构的改动，都要同步更新本文档。
- 记录内容使用中文，包含日期、需求来源、变更摘要、影响范围和后续待办。
- 不记录数据库密码、令牌、生产地址等敏感信息；如需说明配置，仅描述配置项名称和用途。

## 2026-06-26 初始化项目规范

### 需求来源

用户要求生成仓库贡献指南，并在当前项目中引入 Java 开发规范约束。

### 变更摘要

- 创建项目级 `AGENTS.md`，记录项目结构、开发命令、编码风格、测试规范和提交规范。
- 当前项目 Java 开发参考 `D:\download\阿里巴巴Java开发手册（公开版）.pdf`。
- 约定 Markdown 文档默认使用中文。

### 影响范围

- 后续 Java、MySQL、ORM、异常日志、安全和代码评审都需要参考项目级 `AGENTS.md`。

## 2026-06-26 初始化全栈项目骨架

### 需求来源

用户指定技术栈：JDK 21 LTS、Spring Boot 3.5.16、Vue 3.x、Node.js 22 LTS、MySQL 8.4 LTS、Docker Compose。

### 变更摘要

- 创建 `backend/` Spring Boot 后端骨架。
- 创建 `frontend/` Vue 3 + Vite + TypeScript 前端骨架。
- 创建 `docker-compose.yml`、`.env.example`、`.gitignore`、`.editorconfig` 和 `README.md`。
- 后端提供 `/api/health` 健康检查接口，前端首页调用该接口展示状态。

### 影响范围

- 项目采用前后端分离结构。
- 本地真实配置写入 `.env`，不提交到 Git。

## 2026-06-29 数据库脚本目录约定

### 需求来源

用户要求在后端和前端同级创建专门存放建表语句和初始化数据脚本的数据库文件夹。

### 变更摘要

- 创建 `database/README.md`。
- 在项目级 `AGENTS.md` 中约定后续建表语句、结构变更、索引调整和初始化数据脚本默认放入 `database/`。
- 建议按 `database/schema/`、`database/seed/`、`database/migration/` 拆分脚本。

### 影响范围

- 后续数据库相关 SQL 文件都应进入 `database/`，避免散落在后端、文档或临时目录。

## 2026-06-29 配置测试数据库并推送 GitHub

### 需求来源

用户提供测试数据库连接信息，并创建 GitHub 仓库 `wangwanpeng1103/dev_pro.git`。

### 变更摘要

- 新增 `backend/src/main/resources/application-local.yml`，通过环境变量读取本地数据库连接。
- 创建本地 `.env` 保存真实测试数据库配置，`.env` 被 `.gitignore` 排除。
- 将项目初始代码提交并推送到 GitHub `main` 分支。

### 影响范围

- 后端本地运行使用 `SPRING_PROFILES_ACTIVE=local`。
- 真实数据库密码只保存在本地 `.env`，不进入 Git 仓库。

## 2026-06-29 建立需求记录机制

### 需求来源

用户要求创建一个全局 Markdown 文件，用于保存每次需求汇总记录，并在项目规则中约定后续需求和改动都要同步记录。

### 变更摘要

- 创建 `REQUIREMENTS.md` 作为项目需求变更记录。
- 在项目级 `AGENTS.md` 中新增需求记录规则。

### 影响范围

- 后续每次用户提出需求、需求调整或代码改动，都要同步更新 `REQUIREMENTS.md`。
