#!/usr/bin/env bash
# ============================================================================
#  内网 Ubuntu 宿主机【一次性】初始化脚本
#
#  用法（在仓库根目录执行）：
#      sudo bash deploy/scripts/bootstrap-host.sh
#
#  做什么：
#    1. 检查 nginx / docker / docker compose 是否安装
#    2. 建好 4 个前端目录、4 个项目的数据/日志目录
#    3. 安装 Nginx 站点配置并 reload（已存在的配置先备份）
#    4. 把 compose 文件放到 /opt/projects
#    5. 检查泛域名证书，缺失时打印申请命令
#  不会做：启动任何业务容器、删除任何数据
# ============================================================================
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

NGINX_CONF_SRC="${DEPLOY_DIR}/nginx/law-proj-all.conf"
NGINX_CONF_DST="/etc/nginx/sites-available/law-proj-all.conf"
NGINX_ENABLED="/etc/nginx/sites-enabled/law-proj-all.conf"
CERT_DIR="/etc/nginx/ssl"
PROJECTS_DIR="/opt/projects"

log()  { printf '\033[1;32m[bootstrap]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[bootstrap][warn]\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m[bootstrap][error]\033[0m %s\n' "$*" >&2; exit 1; }

[[ $EUID -eq 0 ]] || die "请用 root 运行：sudo bash $0"
[[ -f "$NGINX_CONF_SRC" ]] || die "找不到 $NGINX_CONF_SRC"

# ---------- 1. 依赖检查 ----------
log "检查依赖 ..."
if ! command -v nginx >/dev/null 2>&1; then
  warn "未检测到 nginx，尝试安装 ..."
  apt-get update && apt-get install -y nginx
fi
command -v nginx >/dev/null 2>&1 || die "nginx 安装失败，请手动安装后重试"
nginx -v 2>&1 | sed 's/^/  /'

if ! command -v docker >/dev/null 2>&1; then
  warn "未检测到 docker：可执行 apt-get install -y docker.io docker-compose-v2，或按官方文档安装"
else
  docker --version | sed 's/^/  /'
  if docker compose version >/dev/null 2>&1; then
    docker compose version | sed 's/^/  /'
  else
    warn "docker compose v2 插件不可用（需要 'docker compose' 而不是老版 docker-compose）"
  fi
fi

# ---------- 2. 目录 ----------
log "创建前端目录 /var/www/law{1..4}-web ..."
for n in 1 2 3 4; do
  mkdir -p "/var/www/law${n}-web"
  if [[ ! -f "/var/www/law${n}-web/index.html" ]]; then
    cat > "/var/www/law${n}-web/index.html" <<HTML
<!doctype html>
<meta charset="utf-8">
<title>law${n}.hexilab.cn</title>
<h1>law${n}.hexilab.cn 已就绪，等待部署项目前端</h1>
HTML
  fi
  chown -R www-data:www-data "/var/www/law${n}-web"
done

log "创建项目目录 ${PROJECTS_DIR}/law{1..4}/{data,logs} ..."
for n in 1 2 3 4; do
  mkdir -p "${PROJECTS_DIR}/law${n}/data" "${PROJECTS_DIR}/law${n}/logs"
done
mkdir -p "${PROJECTS_DIR}/compose"

# ---------- 3. Nginx 站点 ----------
log "安装 Nginx 站点配置 ..."
if [[ -f "$NGINX_CONF_DST" ]] && ! cmp -s "$NGINX_CONF_SRC" "$NGINX_CONF_DST"; then
  backup="${NGINX_CONF_DST}.bak.$(date +%Y%m%d%H%M%S)"
  cp -a "$NGINX_CONF_DST" "$backup"
  warn "已备份原配置到 $backup"
fi
cp -a "$NGINX_CONF_SRC" "$NGINX_CONF_DST"
ln -sfn "$NGINX_CONF_DST" "$NGINX_ENABLED"

# 默认站点会抢 80 端口的默认 server，停用它（只改名，可随时改回来）
if [[ -e /etc/nginx/sites-enabled/default ]]; then
  mv /etc/nginx/sites-enabled/default /etc/nginx/sites-enabled/default.disabled
  warn "已停用默认站点（改名 /etc/nginx/sites-enabled/default.disabled）"
fi

# ---------- 4. compose ----------
log "同步 compose 文件到 ${PROJECTS_DIR} ..."
cp -a "${DEPLOY_DIR}/docker-compose.yml" "${PROJECTS_DIR}/docker-compose.yml"
cp -a "${DEPLOY_DIR}/env.example" "${PROJECTS_DIR}/env.example"
if [[ ! -f "${PROJECTS_DIR}/.env" ]]; then
  cp -a "${DEPLOY_DIR}/env.example" "${PROJECTS_DIR}/.env"
  warn "已生成 ${PROJECTS_DIR}/.env，请按需修改（模型 key、数据库密码）"
fi

# ---------- 5. 证书 ----------
log "检查证书 ..."
mkdir -p "$CERT_DIR"
if [[ -s "${CERT_DIR}/fullchain.crt" && -s "${CERT_DIR}/privkey.key" ]]; then
  openssl x509 -in "${CERT_DIR}/fullchain.crt" -noout -subject -dates | sed 's/^/  /'
else
  warn "缺少 ${CERT_DIR}/fullchain.crt 或 privkey.key，HTTPS 站点会启动失败！"
  cat <<'TIP'
  申请 *.hexilab.cn 泛域名证书（DNS 方式，需要阿里云 DNS API Key）：
    curl https://get.acme.sh | sh -s email=你的邮箱
    export Ali_Key="<阿里云 AccessKeyId>"
    export Ali_Secret="<阿里云 AccessKeySecret>"
    ~/.acme.sh/acme.sh --issue --dns dns_ali -d hexilab.cn -d '*.hexilab.cn'
    ~/.acme.sh/acme.sh --install-cert -d hexilab.cn --key-file /etc/nginx/ssl/privkey.key \
      --fullchain-file /etc/nginx/ssl/fullchain.crt \
      --reloadcmd "systemctl reload nginx"
TIP
fi

# ---------- 6. 校验并生效 ----------
log "校验 Nginx 配置 ..."
if nginx -t; then
  systemctl enable nginx >/dev/null 2>&1 || true
  systemctl reload nginx
  log "Nginx 已 reload"
else
  die "nginx -t 失败：请先解决上面的报错（证书缺失也会失败），再执行 sudo nginx -t && sudo systemctl reload nginx"
fi

cat <<'EOF'

[bootstrap] 完成。下一步：
  1) 确认 /opt/projects/.env 里填好模型 key（或宿主机装好 Ollama）
  2) 部署本项目：sudo bash deploy/scripts/deploy-frcs.sh
  3) 其他组交付后：docker compose -f docker-compose.yml -f compose/lawN.yml up -d
EOF
