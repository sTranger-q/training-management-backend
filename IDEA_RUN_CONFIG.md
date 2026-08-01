# IDEA 运行配置指南

本项目有两套运行配置，使用方式任选其一。

---

## 方式一：自动生成（推荐）

项目根目录执行：

```bash
python setup_idea_run_configs.py
```

或者 Windows PowerShell：
```powershell
python .\setup_idea_run_configs.py
```

执行完后在 IDEA 中：**File → Reload All from Disk**（或重启 IDEA），顶部运行配置下拉框就会自动出现两套配置。

---

## 方式二：手动配置（按下面变量表抄）

如果不想用脚本，直接在 IDEA 新建 Run Configuration：

### ① TrainingBackend Dev (H2) — 日常开发用

**Run → Edit Configurations → + → Application**

| 字段 | 值 |
|------|----|
| **Name** | `TrainingBackend Dev (H2)` |
| **Main class** | `com.training.system.TrainingApplication` |
| **Use classpath of module** | `training-backend.main` |
| **Working directory** | `$PROJECT_DIR$` |
| **VM options** | `-Dspring.profiles.active=dev -Dfile.encoding=UTF-8` |

点击 **Environment variables**，填入：

| 环境变量 | 值 |
|---------|----|
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `SERVER_PORT` | `8080` |

> 直接点 Run 即可启动，H2 内存库自动建表+灌入示例数据。

---

### ② TrainingBackend Prod (MySQL) — 生产配置调试用

启动前请先确保本地 MySQL 已启动，并创建 `training` 数据库：

```sql
CREATE DATABASE training DEFAULT CHARACTER SET utf8mb4;
```

**Run → Edit Configurations → + → Application**

| 字段 | 值 |
|------|----|
| **Name** | `TrainingBackend Prod (MySQL)` |
| **Main class** | `com.training.system.TrainingApplication` |
| **Use classpath of module** | `training-backend.main` |
| **Working directory** | `$PROJECT_DIR$` |
| **VM options** | `-Dspring.profiles.active=prod -Dfile.encoding=UTF-8` |

点击 **Environment variables**，填入：

| 环境变量 | 默认值 | 是否必填 | 说明 |
|---------|--------|:-------:|------|
| `SPRING_PROFILES_ACTIVE` | `prod` | ✅ | 启用生产配置 |
| `SERVER_PORT` | `8080` | ❌ | 服务端口 |
| `DB_HOST` | `localhost` | ❌ | MySQL 主机 |
| `DB_PORT` | `3306` | ❌ | MySQL 端口 |
| `DB_NAME` | `training` | ❌ | 数据库名 |
| `DB_USERNAME` | `root` | ❌ | 用户名 |
| `DB_PASSWORD` | （空） | ✅ | 改成你自己的 MySQL 密码 |

---

## 启动后访问地址

| 组件 | Dev 环境（H2） | Prod 环境（MySQL） |
|------|:--------------:|:------------------:|
| 服务根路径 | http://localhost:8080 | http://localhost:8080 |
| Swagger 文档 | http://localhost:8080/swagger-ui.html | ❌ 生产已关闭 |
| H2 控制台 | http://localhost:8080/h2-console（`jdbc:h2:mem:training`，用户名 `sa`，密码空） | ❌ 生产已关闭 |

---

## 首次用 MySQL 启动要先建表

application-prod.yml 中：
```yaml
spring:
  sql:
    init:
      mode: always   # 首次启动改为 always 执行 schema.sql 建表
```

建表完成后改回 `never`，避免重复初始化。
