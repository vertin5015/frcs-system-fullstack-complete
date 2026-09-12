#!/usr/bin/env bash
# ============================================================================
#  本项目（第三组 lawsys1 -> law3.hexilab.cn）部署脚本，在内网 Ubuntu 宿主机上运行：
#      sudo bash /opt/projects/law3/src/deploy/scripts/deploy-frcs.sh
#
#  可选参数：
#      --repo <git地址>   默认用下面的 REPO_URL
#      --tag  <镜像tag>   默认 v1.0.0
#      --skip-pull        不拉代码，直接用本地已检出的代码
#      --skip-frontend    只更新后端
#
#  做的事：拉代码 -> 构建后端镜像 -> 构建前端 dist -> 发布静态文件
#          -> 起/更新容器 -> 健康检查 -> 注入演示知识库
# ============================================================================
set -Eeuo pipefail

REPO_URL="https://github.com/vertin5015/frcs-system-fullstack-complete.git"
IMAGE_TAG="v1.0.0"
SRC_DIR="/opt/projects/law3/src"
WEB_ROOT="/var/www/law3-web"
COMPOSE_FILE="/opt/projects/docker-compose.yml"
ENV_FILE="/opt/projects/.env"
HOST_PORT=8003
DOMAIN="law3.hexilab.cn"
SKIP_PULL=0
SKIP_FRONTEND=0

log()  { printf '\033[1;32m[deploy]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[deploy][warn]\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m[deploy][error]\033[0m %s\n' "$*" >&2; exit 1; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo) REPO_URL="$2"; shift 2 ;;
    --tag) IMAGE_TAG="$2"; shift 2 ;;
    --skip-pull) SKIP_PULL=1; shift ;;
    --skip-frontend) SKIP_FRONTEND=1; shift ;;
    -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
    *) die "未知参数：$1" ;;
  esac
done

[[ $EUID -eq 0 ]] || die "请用 root 运行：sudo bash $0"
command -v docker >/dev/null 2>&1 || die "未安装 docker"
docker compose version >/dev/null 2>&1 || die "缺少 docker compose v2 插件"
[[ -f "$COMPOSE_FILE" ]] || die "找不到 $COMPOSE_FILE，请先运行 deploy/scripts/bootstrap-host.sh"

if [[ ! -f "$ENV_FILE" ]]; then
  cp -a /opt/projects/env.example "$ENV_FILE"
  warn "已从 env.example 生成 $ENV_FILE，请检查模型 key / 数据库密码"
fi

# 安全护栏：只允许操作 /var/www/lawN-web
[[ "$WEB_ROOT" =~ ^/var/www/law[1-4]-web$ ]] || die "WEB_ROOT 不合法：$WEB_ROOT"

# ---------- 1. 取代码 ----------
if [[ ! -d "$SRC_DIR/.git" ]]; then
  log "克隆代码到 $SRC_DIR ..."
  mkdir -p "$(dirname "$SRC_DIR")"
  git clone "$REPO_URL" "$SRC_DIR"
elif [[ "$SKIP_PULL" != "1" ]]; then
  log "更新代码 ..."
  git -C "$SRC_DIR" fetch --prune origin
  git -C "$SRC_DIR" pull --ff-only
fi

# ---------- 2. 构建前端静态包 ----------
if [[ "$SKIP_FRONTEND" != "1" ]]; then
  log "用 node:20-alpine 构建前端（首次会下载依赖，约 1-3 分钟）..."
  docker run --rm \
    -v "$SRC_DIR/frcs-frontend":/app \
    -w /app \
    -e VUE_APP_BASE_URL= \
    -e NODE_OPTIONS=--max-old-space-size=2048 \
    node:20-alpine \
    sh -c "npm ci --no-audit --no-fund && npm run build"
  [[ -f "$SRC_DIR/frcs-frontend/dist/index.html" ]] || die "前端构建产物缺失：$SRC_DIR/frcs-frontend/dist/index.html"

  log "发布静态文件到 $WEB_ROOT ..."
  find "$WEB_ROOT" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
  cp -a "$SRC_DIR/frcs-frontend/dist/." "$WEB_ROOT/"
  chown -R www-data:www-data "$WEB_ROOT"
else
  log "按要求跳过前端发布"
fi

# ---------- 3. 构建后端镜像 ----------
log "构建后端镜像 frcs-backend:${IMAGE_TAG} ..."
docker build -t "frcs-backend:${IMAGE_TAG}" "$SRC_DIR/legal_cases-master"
docker tag "frcs-backend:${IMAGE_TAG}" frcs-backend:latest

# ---------- 4. 启动/更新容器 ----------
log "启动容器 ..."
LAW3_IMAGE_TAG="$IMAGE_TAG" docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" \
  up -d law3-mysql law3-redis law3-api

# ---------- 5. 健康检查 ----------
log "等待后端就绪（最多 180 秒）..."
ok=0
for _ in $(seq 1 60); do
  if curl -fsS "http://127.0.0.1:${HOST_PORT}/api/health" >/tmp/law3-health.json 2>/dev/null; then ok=1; break; fi
  sleep 3
done
if [[ "$ok" != "1" ]]; then
  echo "----- 最近 80 行后端日志 -----"
  docker logs --tail 80 law3-api || true
  die "后端健康检查失败：curl http://127.0.0.1:${HOST_PORT}/api/health"
fi
log "后端健康检查通过：$(cat /tmp/law3-health.json)"

if command -v nginx >/dev/null 2>&1; then
  nginx -t && systemctl reload nginx
  if curl -fsS -H "Host: ${DOMAIN}" http://127.0.0.1/health >/dev/null 2>&1; then
    log "Nginx 探活通过：http://127.0.0.1/health (Host: ${DOMAIN})"
  else
    warn "Nginx /health 探活失败，检查 deploy/nginx/law-proj-all.conf 是否已生效"
  fi
fi

# ---------- 6. 演示知识库 ----------
if [[ -f "$SRC_DIR/scripts/seed-kb.sh" ]]; then
  log "注入演示知识库（已有内容会自动跳过）..."
  bash "$SRC_DIR/scripts/seed-kb.sh" "http://127.0.0.1:${HOST_PORT}" || warn "知识库注入失败，不影响主流程"
fi

cat <<EOF

[deploy] 完成
  网页：   https://${DOMAIN}
  接口：   https://${DOMAIN}/api/cases/search
  探活：   https://${DOMAIN}/health   （容器内：curl http://127.0.0.1:${HOST_PORT}/api/health）
  日志：   docker logs -f law3-api
  重启：   docker compose -f ${COMPOSE_FILE} restart law3-api

如果公网打不开，按此顺序排查：
  1) curl -fsS http://127.0.0.1:${HOST_PORT}/api/health       容器本身是否正常
  2) curl -fsS -H 'Host: ${DOMAIN}' http://127.0.0.1/health   Nginx 是否正常
  3) systemctl status frpc                                    frp 隧道是否在线
  4) 云服务器安全组 443、DNS A 记录、证书是否有效
EOF
