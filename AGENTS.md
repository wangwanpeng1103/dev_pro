# Repository Guidelines

## 项目结构与模块组织

本项目采用前后端分离结构：

- `backend/`：Spring Boot 3.5.16 后端服务，源码在 `src/main/java`，配置在 `src/main/resources`，测试在 `src/test/java`。
- `frontend/`：Vue 3 + Vite 前端应用，入口在 `src/main.ts`，页面组件在 `src/`。
- `database/`：数据库脚本目录，用于存放建表语句、结构变更脚本和初始化数据脚本。
- `REQUIREMENTS.md`：需求变更记录，用于保存每次需求、约定和实现变更的中文汇总。
- `docker-compose.yml`：本地联调与部署编排，包含 MySQL、后端、前端。
- `.env.example`：环境变量示例，复制为 `.env` 后填写本地配置。
- `README.md`：本地启动与项目说明。

## 构建、测试与开发命令

后端：

- `cd backend && mvn spring-boot:run`：启动 Spring Boot 服务。
- `cd backend && mvn test`：运行后端测试。
- `cd backend && mvn package`：打包后端应用。

前端：

- `cd frontend && npm install`：安装依赖，要求 Node.js 22 LTS。
- `cd frontend && npm run dev`：启动 Vite 开发服务。
- `cd frontend && npm run build`：执行类型检查并构建前端产物。

部署：

- `copy .env.example .env`：创建本地环境变量文件。
- `docker compose up --build`：构建并启动完整本地环境。

## Java 开发标准

后续 Java 开发必须遵循 `D:\download\阿里巴巴Java开发手册（公开版）.pdf`。如本指南与手册冲突，以手册为准；如项目另有明确约定，以项目约定为准。

命名、常量、格式、OOP、集合、并发、控制语句、注释、异常日志、MySQL、ORM、工程分层、依赖、服务器与安全规则都应纳入评审范围。新增 `.java` 文件后，立即执行 `git add <path>` 纳入 Git 跟踪，除非任务明确要求不要暂存。

## 编码风格与命名约定

Java 类名使用 `UpperCamelCase`；方法、参数、局部变量使用 `lowerCamelCase`；常量使用 `UPPER_SNAKE_CASE`；包名使用小写单词。避免无意义缩写、拼音与英文混用、魔法值和误导性布尔命名。

控制语句必须使用大括号。集合使用前明确容量、空值、遍历和并发访问语义；不要在 foreach 中直接增删集合元素。并发代码必须说明线程安全边界。

Vue 组件使用 `PascalCase` 命名，组合式 API 优先使用 `<script setup>`。TypeScript 开启严格模式，接口和类型名称应表达业务含义。

## 异常、日志与安全

异常处理应保留上下文，不吞异常，不用异常控制正常流程。日志使用统一框架和参数化写法，记录关键业务字段、失败原因和排查线索。

禁止输出或提交敏感信息，包括密码、令牌、完整手机号、身份证号、数据库连接凭据和生产环境配置。数据库连接信息只写入本地 `.env`，不要提交。

后续新增或调整后端 Controller 接口时，方法级路由注解默认只允许使用 `@GetMapping` 和 `@PostMapping`。查询类接口可使用 `@GetMapping`；新增、修改、删除、批量操作、状态变更等会改变服务端状态的接口统一使用 `@PostMapping`，不再新增 `@PutMapping`、`@DeleteMapping`、`@PatchMapping` 等其它方法级 Mapping。类级路径前缀可继续使用 `@RequestMapping`。

## MySQL 与 ORM 规范

数据库版本目标为 MySQL 8.4 LTS。表、字段、索引和 SQL 必须遵循手册 MySQL 规约：字段含义明确，类型选择匹配业务，索引服务查询场景，避免无条件全表扫描。

后续后端数据库访问统一使用 MyBatis-Plus，不再新增 `JdbcTemplate` 或手写 JDBC DAO。简单单表或条件查询优先使用 MyBatis-Plus 的 `LambdaQueryWrapper`、`LambdaUpdateWrapper`、`BaseMapper` 等类型安全 API；遇到多表关联、复杂动态 SQL、批量统计、复杂排序分页或需要精细调优的 SQL，统一放到 MyBatis XML Mapper 中实现。除非用户明确提出特殊要求，后续所有 SQL 和 ORM 代码都按此约定编写。

后续只要新增建表语句、表结构变更、索引调整或初始化数据脚本，都默认放到 `database/` 目录。建议按用途拆分为 `database/schema/`、`database/seed/`、`database/migration/`，脚本命名包含日期、序号和业务含义，例如 `20260629_001_create_user_table.sql`。

需要测试或使用 MySQL 连接时，优先使用 `D:\dev\mysql-shell-9.7.0\bin\mysqlsh.exe`，并采用安全密码输入方式。

## 需求记录规范

后续只要用户提出新需求、调整需求、确认技术约定，或项目发生代码、配置、数据库脚本、文档等改动，都必须同步更新 `REQUIREMENTS.md`。记录使用中文，说明日期、需求来源、变更摘要、影响范围和后续待办。

不要在 `REQUIREMENTS.md` 中记录数据库密码、令牌、生产地址等敏感信息；如需说明配置，仅记录配置项名称和用途。

## 核心产品文档规则

`REQUIREMENTS.md` 是本项目唯一核心产品文档和需求事实来源。后续开始任何需求分析、方案设计、代码实现、UI 调整、接口变更、数据库脚本或文档更新前，都必须先参考 `REQUIREMENTS.md`，确认当前任务是否符合已记录的产品逻辑、业务边界和历史约定。

如果用户新需求、现有代码实现、设计判断或其它文档内容与 `REQUIREMENTS.md` 存在不一致、冲突或含义不清，必须先向用户说明差异并确认后再继续实现。确认后的结论也要同步写回 `REQUIREMENTS.md`，保证该文档持续作为后续开发的唯一核心依据。

## 测试规范

新增功能应包含对应测试；修复缺陷优先添加回归测试。后端测试命名示例：`UserServiceTest.java`。前端后续引入测试框架后，测试文件建议使用 `*.spec.ts`。

无法自动化验证时，在 PR 或变更说明中写明原因和手工验证步骤。

## 提交与 Pull Request 规范

建议使用简洁祈使句提交信息，例如：

- `Add user import validation`
- `Fix order total rounding`
- `Document local setup`

PR 应包含变更摘要、验证命令、关联 issue 或需求编号。涉及 UI 请附截图或录屏；涉及配置、权限、数据库或批量数据变更时，必须说明安全影响和回滚方案。

## Lombok 使用规范

本项目后端统一启用 Lombok 作为 Java 对象样板代码工具。实体类、领域模型、DTO/VO 等普通对象需要优先使用 Lombok 的 `@Getter`、`@Setter`、`@NoArgsConstructor`、`@AllArgsConstructor`、`@Builder` 等注解减少重复 getter、setter、构造器和构建代码。

使用 Lombok 时应按对象语义选择注解：有继承、集合字段、业务方法或 `equals/hashCode` 风险的对象，不默认使用 `@Data`；优先使用更精确的 `@Getter`、`@Setter`。Java `record` 已具备简洁不可变数据载体能力时，可以保留 `record`，不强行改为 Lombok 类。后续新增或改造 Java 对象时，除非用户有特殊要求，默认遵循本约定。
