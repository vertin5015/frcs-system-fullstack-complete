#!/usr/bin/env bash
# ============================================================================
#  其他组（law1 / law2 / law4）交付后，一条命令接入：
#      sudo bash /opt/projects/enable-project.sh law1 lawllm1-api:v1.0.0 /tmp/law1-dist.zip
#
#  参数：<law1|law2|law4> <镜像名:tag> [前端dist压缩包，可选]
#
#  Nginx 站点、8001/8002/8004 端口映射、静态目录、proj_net 网络都已提前配好，
#  这里只做三件事：
#    1. 前端 dist 解压到 /var/www/lawN-web，chown + reload nginx
#    2. 生成 /opt/projects/compose/lawN.yml（把对方镜像挂进同一个 network）
#    3. 起容器并 curl 127.0.0.1:800N/health
#
#  也可以不用这个脚本：按老师模板把 /opt/projects/docker-compose.yml 里对应的
#  占位注释放开、填上镜像 tag，再 docker compose up -d lawN-api。两种方式二选一。
# ============================================================================
set -Eeuo pipefail

SLOT="${1:-}"
IMAGE="${2:-}"
DIST_ZIP="${3:-}"
PROJECTS_DIR="/opt/projects"

log()  { printf '\033[1;32m[enable]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[enable][warn]\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m[enable][error]\033[0m %s\n' "$*" >&2; exit 1; }

[[ $EUID -eq 0 ]] || die "请用 root 运行：sudo bash $0 <law1|law2|law4> <镜像:tag> [dist.zip]"
case "$SLOT" in
  law1) PORT=8001 ;;
  law2) PORT=8002 ;;
  law4) PORT=8004 ;;
  *) die "第一个参数只能是 law1 / law2 / law4（law3 是本项目，用 deploy-frcs.sh）" ;;
esac
[[ -n "$IMAGE" ]] || die "缺少镜像名:tag，例如 lawllm1-api:v1.0.0"

WEB_ROOT="/var/www/${SLOT}-web"
[[ -d "$WEB_ROOT" ]] || die "缺少目录 $WEB_ROOT，请先运行 deploy/scripts/bootstrap-host.sh"
mkdir -p "$PROJECTS_DIR/compose"

# ---------- 1. 前端静态文件 ----------
if [[ -n "$DIST_ZIP" ]]; then
  [[ -f "$DIST_ZIP" ]] || die "找不到前端包：$DIST_ZIP"
  log "解压前端包到 $WEB_ROOT ..."
  tmp="$(mktemp -d)"
  unzip -q "$DIST_ZIP" -d "$tmp"
  if [[ -f "$tmp/dist/index.html" ]]; then src="$tmp/dist"; else src="$tmp"; fi
  [[ -f "$src/index.html" ]] || die "压缩包里没有 index.html：$DIST_ZIP"
  find "$WEB_ROOT" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
  cp -a "$src/." "$WEB_ROOT/"
  chown -R www-data:www-data "$WEB_ROOT"
  rm -rf "$tmp"
  nginx -t && systemctl reload nginx
else
  warn "未提供前端包，只启动后端；前端目录保持原样"
fi

# ---------- 2. 生成 compose 叠加文件 ----------
log "生成 $PROJECTS_DIR/compose/${SLOT}.yml ..."
cat > "$PROJECTS_DIR/compose/${SLOT}.yml" <<YML
# 由 enable-project.sh 自动生成：${SLOT} 的服务定义
# 对方还需要额外环境变量时，直接加在 environment 下，再重跑本脚本
services:
  ${SLOT}-api:
    image: ${IMAGE}
    container_name: ${SLOT}-api
    restart: always
    ports:
      - "127.0.0.1:${PORT}:8000"
    volumes:
      - ./${SLOT}/logs:/app/logs
      - ./${SLOT}/data:/app/data
    environment:
      TZ: Asia/Shanghai
    networks:
      - proj_net

networks:
  proj_net:
    name: proj_net
    external: true
YML

# ---------- 3. 起容器 ----------
log "启动 ${SLOT}-api ..."
docker compose -f "$PROJECTS_DIR/docker-compose.yml" -f "$PROJECTS_DIR/compose/${SLOT}.yml" \
  --env-file "$PROJECTS_DIR/.env" up -d "${SLOT}-api"

# ---------- 4. 健康检查 ----------
log "健康检查（按《交付规范》：GET /health）..."
ok=0
for _ in $(seq 1 40); do
  if curl -fsS "http://127.0.0.1:${PORT}/health" >/dev/null 2>&1; then ok=1; break; fi
  sleep 3
done
if [[ "$ok" == "1" ]]; then
  log "${SLOT}-api 健康检查通过，https://${SLOT}.hexilab.cn"
else
  warn "http://127.0.0.1:${PORT}/health 探活失败，最近日志："
  docker logs --tail 60 "${SLOT}-api" || true
  cat <<EOT

可能原因：
  1) 对方健康检查不是 GET /health，或后端接口没带 /api 前缀
     如果他们也有 context-path=/api，把 /etc/nginx/sites-available/law-proj-all.conf 里
     ${SLOT} 的 /health 改成 http://127.0.0.1:${PORT}/api/health，再 nginx -t && systemctl reload nginx
  2) 环境变量没给全，或镜像内监听端口不是 8000（改上面生成的文件后重跑 up -d）
  3) 前端接口写死绝对域名、不是相对路径 /api，需要让他们重新打包
EOT
fi
