# 后端打包部署完整指南

本项目提供 **4 种部署方案**，按「最快 → 最自动化」排序：

| 方案 | 适用场景 | 复杂度 |
|------|---------|:-----:|
| [方案一：Shell 脚本（JDK+Maven 在服务器上直接跑）](#方案一shell-脚本部署服务器装-jdk--maven) | 小服务器、快速上线 | ⭐ |
| [方案二：Docker Compose（推荐）](#方案二docker-compose部署) | 标准生产环境 | ⭐⭐ |
| [方案三：GitHub Actions 全自动 CI/CD](#方案三github-actions-cicd-全自动) | push 代码自动构建发布 | ⭐⭐⭐ |
| [方案四：本地打包 + 上传 jar 包](#方案四本地打包手动上传-jar) | 服务器不能外网拉源码 | ⭐ |

---

## 方案一：Shell 脚本部署（服务器装 JDK + Maven）

**服务器环境要求：JDK 17、Maven 3.9、Git、MySQL 8**

### 1. 服务器准备

```bash
# 1) 安装 JDK 17（Ubuntu/Debian）
sudo apt update && sudo apt install -y openjdk-17-jdk-headless

# 2) 安装 Maven 3.9
wget https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
sudo tar -xzf apache-maven-3.9.9-bin.tar.gz -C /opt
sudo ln -s /opt/apache-maven-3.9.9 /opt/maven
echo 'export PATH=/opt/maven/bin:$PATH' >> ~/.bashrc && source ~/.bashrc

# 3) 安装 MySQL 8 + 创建库
sudo apt install -y mysql-server
sudo mysql -e "CREATE DATABASE training DEFAULT CHARACTER SET utf8mb4; CREATE USER 'training'@'%' IDENTIFIED BY '你的密码'; GRANT ALL ON training.* TO 'training'@'%'; FLUSH PRIVILEGES;"

# 4) 拉项目代码
cd /opt
git clone https://github.com/sTranger-q/training-management-backend.git
cd training-management-backend
```

### 2. 首次启动：执行建表 SQL

```bash
mysql -uroot -p training < src/main/resources/schema.sql
mysql -uroot -p training < src/main/resources/data.sql
```

### 3. 一键部署

```bash
# 写入环境变量（生产 DB 密码等）
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=training
export DB_USERNAME=training
export DB_PASSWORD=你的密码
export SERVER_PORT=8080
export JAVA_OPTS="-Xms512m -Xmx1024m"

# 拉代码 + 打包 + 重启
bash deploy.sh
```

后续更新代码：
```bash
cd /opt/training-management-backend
bash deploy.sh
```

### 常用操作

```bash
# 查进程
ps -ef | grep training-backend
cat .app.pid

# 查日志
tail -100f logs/app.stdout.log
tail -100f logs/app.stderr.log

# 停止
kill $(cat .app.pid)
```

---

## 方案二：Docker Compose 部署（推荐）

**服务器环境要求：Docker、Docker Compose**

### 1. 服务器准备

```bash
# 安装 Docker（脚本一键装）
curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
sudo systemctl enable --now docker
sudo usermod -aG docker $USER  # 退出再重新登录，避免每次 sudo docker

# 验证
docker -v
docker compose version

# 创建部署目录
sudo mkdir -p /opt/training-backend
sudo chown -R $USER:$USER /opt/training-backend
cd /opt/training-backend
```

### 2. 拷贝文件到服务器

从项目中上传这 5 个文件到服务器 `/opt/training-backend/`：

- `Dockerfile`
- `docker-compose.prod.yml`
- `.env.example`
- `pom.xml`
- `src/`（整个源码目录）

```bash
# 本地上传（任选）
scp -r training-management-backend/* root@你的服务器IP:/opt/training-backend/
# 或直接 git clone
git clone https://github.com/sTranger-q/training-management-backend.git .
```

### 3. 配置并启动

```bash
cd /opt/training-backend

# 复制环境变量模板并修改密码
cp .env.example .env
nano .env   # 把 DB_PASSWORD 改成强密码

# 重命名 compose 文件
cp docker-compose.prod.yml docker-compose.yml

# 首次构建 + 启动（会自动建库、建表、灌示例数据）
docker compose up -d --build

# 查看日志
docker compose logs -f
docker compose logs -f backend
docker compose logs -f mysql

# 健康检查
docker compose ps
```

### 4. 更新代码后重启

```bash
cd /opt/training-backend
git pull origin main
docker compose up -d --build      # 重新构建镜像 + 重启后端
```

### 5. 常用操作

```bash
# 服务启停
docker compose stop          # 停止
docker compose start         # 启动
docker compose restart       # 重启
docker compose down          # 停 + 删除容器（保留卷，数据不丢）

# 看日志
docker logs -f --tail 300 training-backend
docker logs -f --tail 300 training-mysql

# 进入 MySQL
docker exec -it training-mysql mysql -uroot -p training

# 备份数据库
docker exec training-mysql mysqldump -uroot -p"DB密码" training > backup-$(date +%Y%m%d).sql

# 恢复数据库
docker exec -i training-mysql mysql -uroot -p"DB密码" training < backup-xxx.sql
```

### 6. 开放防火墙端口

```bash
# Ubuntu/Debian (UFW)
sudo ufw allow 8080/tcp
sudo ufw reload

# CentOS (firewalld)
sudo firewall-cmd --add-port=8080/tcp --permanent
sudo firewall-cmd --reload
```

---

## 方案三：GitHub Actions CI/CD 全自动（push 即发布）

### 流程

```
本地 push 代码 → GitHub Actions 自动
  ├─ 1) Checkout 代码
  ├─ 2) Maven 打包
  ├─ 3) 构建 Docker 镜像
  ├─ 4) 推送到 GitHub 容器仓库 (GHCR.io)
  └─ 5) SSH 到服务器 → docker compose 拉镜像重启
```

### 1. 准备服务器

参考方案二装好 Docker 就行。

### 2. GitHub 仓库配置 Secrets

打开 `https://github.com/sTranger-q/training-management-backend/settings/secrets/actions`

点 **New repository secret** 依次新建：

| Secret Name | 值 | 示例 |
|-------------|----|------|
| `SERVER_HOST` | 服务器公网 IP | `47.xxx.xxx.xxx` |
| `SERVER_PORT` | SSH 端口 | `22` |
| `SERVER_USER` | SSH 用户名 | `root` |
| `SERVER_SSH_KEY` | 服务器私钥（id_rsa 全部内容） | 见下方生成 |
| `DB_PASSWORD` | 生产 MySQL 密码 | 自设强密码 |

**生成 SSH 密钥对（在你本地或服务器执行）：**
```bash
ssh-keygen -t ed25519 -C "github-ci" -f ~/.ssh/github_ci
# 一路回车，不设密码
```

- `SERVER_SSH_KEY` 填 **私钥**：`cat ~/.ssh/github_ci` 的全部内容
- 服务器 **authorized_keys** 写入公钥：
  ```bash
  cat ~/.ssh/github_ci.pub >> ~/.ssh/authorized_keys
  chmod 600 ~/.ssh/authorized_keys
  ```

### 3. 推代码触发

```bash
git push origin main
```

然后到 `GitHub → Actions` 里看进度，绿色 ✅ 代表成功。

### 4. 手动触发

`GitHub → Actions → Build & Deploy to Server → Run workflow → Run workflow`

---

## 方案四：本地打包，手动上传 jar

服务器不能拉代码 / 不上 GitHub 时用这个。

### 1. 本地打包

```powershell
# Windows IDEA 终端或命令行
cd training-management-backend
mvn clean package -DskipTests
```

产物：`target/training-backend-1.0.0.jar`

### 2. 上传到服务器

```powershell
scp target/training-backend-1.0.0.jar root@服务器IP:/opt/training-backend/app.jar
scp src/main/resources/schema.sql root@服务器IP:/opt/training-backend/
scp src/main/resources/data.sql root@服务器IP:/opt/training-backend/
```

### 3. 服务器上启动

```bash
cd /opt/training-backend

# 首次：执行建表 SQL
mysql -uroot -p training < schema.sql
mysql -uroot -p training < data.sql

# 启动（写入环境变量后用 nohup）
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=training
export DB_USERNAME=root
export DB_PASSWORD=你的密码

mkdir -p logs
nohup java -Xms512m -Xmx1024m -Dspring.profiles.active=prod \
  -jar app.jar > logs/app.log 2>&1 &
echo $! > .app.pid
```

---

## 上线检查清单

| 检查项 | 验证命令 | 预期 |
|--------|---------|------|
| 服务是否启动 | `docker compose ps` / `curl http://127.0.0.1:8080/v3/api-docs` | 非空 JSON |
| 数据库可连 | `docker exec training-mysql mysql -uroot -p -e "show databases;"` | 有 training 库 |
| 数据是否初始化 | `mysql training -e "select count(*) from student;"` | 4 条初始学员 |
| 端口可访问 | `http://服务器IP:8080` | 浏览器/外部 curl 能通 |
| 小程序接口 | `POST http://服务器IP:8080/api/wx/login` body `{"code":"1"}` | 返回 token |
| 日志是否正常 | `tail -f logs/app.log` | 无 ERROR |

---

## 文件清单

| 文件 | 作用 |
|------|------|
| [deploy.sh](file:///workspace/training-management-backend/deploy.sh) | 方案一：Shell 一键部署脚本（JDK+Maven 部署） |
| [Dockerfile](file:///workspace/training-management-backend/Dockerfile) | 方案二/三：多阶段构建 Docker 镜像 |
| [docker-compose.prod.yml](file:///workspace/training-management-backend/docker-compose.prod.yml) | 方案二：MySQL + Backend 容器编排 |
| [.env.example](file:///workspace/training-management-backend/.env.example) | docker-compose 环境变量模板 |
| [.github/workflows/deploy.yml](file:///workspace/training-management-backend/.github/workflows/deploy.yml) | 方案三：GitHub Actions 全自动 CI/CD 流水线 |
