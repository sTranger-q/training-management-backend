# IDEA 运行配置说明

本项目已预置两套 IDEA Run Configuration，拉取代码后会自动出现在 IDEA 顶部运行配置下拉框中。

## 1. TrainingBackend Dev (H2) — 日常开发用

**启动前无需准备**，直接运行即可。

| 环境变量 | 值 | 说明 |
|---------|----|------|
| `SPRING_PROFILES_ACTIVE` | `dev` | 启用开发配置 |
| `SERVER_PORT` | `8080` | 服务端口 |

- 数据库：H2 内存库，自动建表 + 灌入示例数据
- H2 Console：http://localhost:8080/h2-console（JDBC URL: `jdbc:h2:mem:training`，用户名 sa，密码空）
- Swagger：http://localhost:8080/swagger-ui.html
- SQL 日志：控制台打印 MyBatis SQL

---

## 2. TrainingBackend Prod (MySQL) — 生产配置调试用

**启动前请确保：**
1. 本地/远程 MySQL 已启动
2. 已创建数据库 `training`
3. 已修改下面的 `DB_PASSWORD` 为你的实际密码

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` | 启用生产配置 |
| `SERVER_PORT` | `8080` | 服务端口 |
| `DB_HOST` | `localhost` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `training` | 数据库名 |
| `DB_USERNAME` | `root` | 用户名 |
| `DB_PASSWORD` | （空，必填） | 数据库密码 |

首次运行需要执行 schema.sql 建表：
- 在 application-prod.yml 中临时把 `sql.init.mode` 改为 `always`，启动建表后再改回 `never`

---

## 如何在 IDEA 中编辑环境变量

1. 顶部运行配置下拉框 → **Edit Configurations**
2. 选中 `TrainingBackend Prod (MySQL)`
3. 找到 **Environment variables**，点击右侧「=」按钮即可编辑
4. 将 `DB_PASSWORD` 改为你的实际 MySQL 密码
5. 点 OK → 运行

配置文件位置：`.idea/runConfigurations/TrainingBackend_Prod_MySQL.xml`
