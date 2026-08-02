#!/usr/bin/env bash
# ===========================================================
# 后端一键部署脚本
# 用法：
#   bash deploy.sh                # 默认部署（拉代码 + Maven 打包 + 重启服务）
#   bash deploy.sh --skip-pull    # 跳过拉代码，仅用本地源码
#   bash deploy.sh --skip-build   # 跳过构建，直接用 target/ 现有 jar 重启
# ===========================================================
set -euo pipefail

APP_NAME="training-backend"
APP_JAR="target/training-backend-1.0.0.jar"
PROFILE="${SPRING_PROFILES_ACTIVE:-prod}"
JAR_OPTS="${JAVA_OPTS:--Xms512m -Xmx1024m}"
LOG_DIR="$(pwd)/logs"
PID_FILE="$(pwd)/.app.pid"

SKIP_PULL=0
SKIP_BUILD=0
for arg in "$@"; do
  case "$arg" in
    --skip-pull)   SKIP_PULL=1 ;;
    --skip-build)  SKIP_BUILD=1 ;;
    *) echo "未知参数: $arg"; exit 1 ;;
  esac
done

log()  { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }
die()  { log "❌ $*"; exit 1; }

# --- 环境检查 ---
command -v java   >/dev/null 2>&1 || die "未找到 java，安装 JDK 17+"
command -v mvn    >/dev/null 2>&1 || die "未找到 mvn，安装 Maven 3.9+"
java -version 2>&1 | grep -q "17\|18\|19\|20\|21" || die "需要 JDK 17+，当前 $(java -version 2>&1 | head -1)"

mkdir -p "$LOG_DIR"

# --- 1. 拉取最新代码 ---
if [ "$SKIP_PULL" -eq 0 ]; then
  log "拉取最新代码 ..."
  git fetch origin || die "git fetch 失败"
  git reset --hard origin/main || die "git reset 失败"
fi

# --- 2. Maven 打包 ---
if [ "$SKIP_BUILD" -eq 0 ]; then
  log "Maven 构建（跳过测试）..."
  if [ -f maven-settings.xml ]; then
    mvn clean package -DskipTests -s maven-settings.xml -B -q || die "构建失败"
  else
    mvn clean package -DskipTests -B -q || die "构建失败"
  fi
fi

[ -f "$APP_JAR" ] || die "未找到 $APP_JAR，构建未成功"

# --- 3. 停止旧进程 ---
if [ -f "$PID_FILE" ]; then
  OLD_PID="$(cat "$PID_FILE" 2>/dev/null || true)"
  if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
    log "停止旧进程 PID=$OLD_PID ..."
    kill "$OLD_PID"
    for _ in {1..30}; do
      kill -0 "$OLD_PID" 2>/dev/null || break
      sleep 1
    done
    kill -9 "$OLD_PID" 2>/dev/null || true
  fi
fi

# --- 4. 启动新进程 ---
log "启动 $APP_NAME (profile=$PROFILE) ..."
nohup java $JAR_OPTS \
  -Dspring.profiles.active="$PROFILE" \
  -jar "$APP_JAR" \
  > "$LOG_DIR/app.stdout.log" \
  2> "$LOG_DIR/app.stderr.log" &
NEW_PID=$!
echo "$NEW_PID" > "$PID_FILE"

# --- 5. 等待启动 ---
log "等待启动 (PID=$NEW_PID) ..."
for i in {1..60}; do
  if ! kill -0 "$NEW_PID" 2>/dev/null; then
    die "进程已退出，查看 $LOG_DIR/app.stderr.log"
  fi
  if curl -sf http://localhost:${SERVER_PORT:-8080}/actuator/health >/dev/null 2>&1 ||
     curl -sf http://localhost:${SERVER_PORT:-8080}/v3/api-docs >/dev/null 2>&1; then
    log "✅ 启动成功！PID=$NEW_PID"
    echo "  服务地址:   http://$(hostname -I | awk '{print $1}'):${SERVER_PORT:-8080}"
    echo "  日志目录:   $LOG_DIR"
    echo "  停止服务:   kill \$(cat $PID_FILE)"
    exit 0
  fi
  sleep 1
done

log "⚠️ 启动超时，请检查日志: $LOG_DIR/app.stderr.log"
echo "最新错误："
tail -30 "$LOG_DIR/app.stderr.log"
exit 1
