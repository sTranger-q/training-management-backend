# 培训班管理系统 - 后端服务

> Spring Boot 3.x + MyBatis-Plus + H2/MySQL，为管理后台前端与微信小程序端提供 RESTful API。

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.5.3 |
| ORM | MyBatis-Plus | 3.5.12 |
| 数据库 | H2（开发）/ MySQL 8.0（生产） | — |
| API 文档 | springdoc-openapi (Swagger UI) | 2.6.0 |
| 构建 | Maven | 3.9+ |
| JDK | Java | 17+ |

## 目录结构

```
src/main/java/com/training/system/
├── TrainingApplication.java       # 启动类
├── common/                        # 公共组件
│   ├── Result.java                # 统一响应体 {code, message, data}
│   ├── BusinessException.java     # 业务异常
│   ├── GlobalExceptionHandler.java# 全局异常处理
│   └── PageQuery.java             # 分页查询基类
├── config/                        # 配置
│   ├── CorsConfig.java            # 跨域配置
│   ├── MybatisPlusConfig.java     # MyBatis-Plus 配置
│   ├── MetaObjectFillHandler.java # 自动填充 create/update 字段
│   └── OpenApiConfig.java         # Swagger 配置
├── controller/                    # 接口控制器
│   ├── AuthController.java        # 管理后台登录
│   ├── DashboardController.java   # 经营看板统计
│   ├── StudentController.java     # 学员管理 CRUD
│   ├── CourseController.java      # 课程管理 CRUD
│   ├── ClassInfoController.java   # 班级管理 CRUD
│   ├── TeacherController.java     # 教师管理 CRUD
│   └── WxMiniController.java      # 微信小程序端接口
├── entity/                        # 实体类（对应数据库表）
├── mapper/                        # MyBatis-Plus Mapper
├── service/                       # 业务逻辑层
│   └── impl/
└── vo/                            # 视图对象
    ├── ClassInfoVO.java
    ├── WxClassVO.java             # 小程序-班级
    ├── WxScheduleVO.java          # 小程序-课表
    ├── WxAttendanceVO.java        # 小程序-考勤
    └── WxHomeworkVO.java          # 小程序-作业

src/main/resources/
├── application.yml                # 应用配置
├── schema.sql                     # 建表脚本（H2/MySQL 兼容）
└── data.sql                       # 初始化示例数据
```

## 数据库表

| 表名 | 说明 |
|------|------|
| `student` | 学员信息（姓名/性别/电话/家长/课时余额等） |
| `course` | 课程信息（科目/适用年龄/总课时/单价） |
| `teacher` | 教师信息（科目/资质/课时薪资） |
| `class_info` | 班级信息（关联课程和教师/班型/容量/周期） |
| `attendance` | 考勤记录（班级/学员/课次时间/出勤状态） |
| `order_info` | 订单信息（学员/班级/课时数/金额） |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+

### 启动

```bash
cd training-management-backend
mvn spring-boot:run
```

服务启动在 `http://localhost:8080`，H2 内存库自动建表并加载示例数据。

### 访问 API 文档

启动后打开：http://localhost:8080/swagger-ui.html

### H2 控制台

开发阶段可访问 http://localhost:8080/h2-console 查看数据：
- JDBC URL: `jdbc:h2:mem:training`
- 用户名: `sa`
- 密码: （空）

## API 概览

### 管理后台接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 管理后台登录 |
| GET | `/api/dashboard/stats` | 经营看板统计数据 |
| GET/POST | `/api/students` | 学员列表/新增 |
| GET/PUT/DELETE | `/api/students/{id}` | 学员详情/修改/删除 |
| GET/POST | `/api/courses` | 课程列表/新增 |
| GET/POST | `/api/classes` | 班级列表/新增 |
| GET/POST | `/api/teachers` | 教师列表/新增 |

### 微信小程序接口（`/api/wx`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/wx/login` | 小程序登录（模拟 code2session） |
| GET | `/api/wx/dashboard` | 首页统计卡 |
| GET | `/api/wx/student/me` | 当前学员信息 |
| GET | `/api/wx/classes` | 我的班级列表 |
| GET | `/api/wx/schedule` | 我的周课表 |
| GET | `/api/wx/today-lessons` | 今日课程 |
| POST | `/api/wx/attendance/checkin` | 扫码签到 |
| GET | `/api/wx/attendance/list` | 考勤记录 |
| GET | `/api/wx/homework` | 作业/成绩 |

> 小程序接口鉴权：通过 Header `x-student-id` 或 Query 参数 `studentId` 传入学员 ID。

## 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "traceId": null
}
```

- `code = 0` 表示成功
- `code != 0` 表示业务错误

## 切换到 MySQL（生产环境）

1. `pom.xml` 中将 H2 依赖 scope 改为 test，添加 MySQL 驱动：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

2. 修改 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://host:3306/training?useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: your_password
```

## 关联项目

| 项目 | 仓库 |
|------|------|
| 管理后台前端 | [training-management-frontend](https://github.com/sTranger-q/training-management-frontend) |
| 微信小程序端 | [training-miniprogram](https://github.com/sTranger-q/training-miniprogram) |
