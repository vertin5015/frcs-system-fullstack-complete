# 多项目部署手册（law1~law4.hexilab.cn）

**本项目 = 第三组 lawsys1 = `law3.hexilab.cn` → `127.0.0.1:8003` → `/var/www/law3-web`**

```
外网用户
  └─ https://lawN.hexilab.cn → DNS A 记录 → 云服务器 8.129.108.145
       └─ frps（7000 / 80 / 443）
            └─ frp 隧道 → 内网 Ubuntu 宿主机 frpc
                 └─ 宿主机 Nginx 443（*.hexilab.cn 泛域名证书）
                      ├─ 静态文件 /var/www/lawN-web          ← 前端 dist
                      └─ /api/ → 127.0.0.1:800N → 容器 8000  ← 各项目后端
```

## 一、四个项目的对应关系

| 域名 | 项目 | 宿主机端口 | 前端目录 | 数据/日志目录 |
| --- | --- | --- | --- | --- |
| law1.hexilab.cn | lawllm1（其他组） | 127.0.0.1:8001 | /var/www/law1-web | /opt/projects/law1 |
| law2.hexilab.cn | lawllm2（其他组） | 127.0.0.1:8002 | /var/www/law2-web | /opt/projects/law2 |
| **law3.hexilab.cn** | **lawsys1（本项目）** | **127.0.0.1:8003** | **/var/www/law3-web** | **/opt/projects/law3** |
| law4.hexilab.cn | lawsys2（其他组） | 127.0.0.1:8004 | /var/www/law4-web | /opt/projects/law4 |

## 二、本目录的文件

| 文件 | 在虚拟机上的位置 | 对应老师的步骤 |
| --- | --- | --- |
| `nginx/law-proj-all.conf` | `/etc/nginx/sites-available/` | 阶段2-4 写 Nginx 配置 |
| `docker-compose.yml` | `/opt/projects/docker-compose.yml` | 阶段3-2 填镜像 tag 和环境变量 |
| `env.example` | `/opt/projects/.env` | 阶段3-2 环境变量 |
| `scripts/bootstrap-host.sh` | 仓库内执行 | 阶段2-1/2/4 建目录、装配置 |
| `scripts/deploy-frcs.sh` | 仓库内执行 | 阶段3-1~3-4 部署本项目 |
| `scripts/enable-project.sh` | `/opt/projects/` | 阶段3 其他组交付后接入 |
| `frp/*.toml` | 仅供参考 | 阶段1/2-3（老师已做，留作排查用） |

## 三、本项目按交付规范做的改动

| 改动 | 原因 |
| --- | --- |
| 容器内端口 8122 → 8000，映射 `127.0.0.1:8003:8000` | 规范硬性要求 |
| 后端新增 `GET /api/health` | 规范要求健康检查接口 |
| 前端 `VUE_APP_BASE_URL` 改为空 | 接口走相对路径 `/api/xxx`，规范禁止写死域名 |
| 爬虫内置 bridge 地址改为 `http://127.0.0.1:8000/...` | 容器名/端口变了，不改会导致检索兜底爬虫全部失败 |
| 小程序默认域名改为 `https://law3.hexilab.cn/api` | 换到新域名入口 |

关于健康检查的一个说明：规范里是 `GET /health`，本项目后端有
`server.servlet.context-path=/api`，容器内实际是 `/api/health`。
Nginx 上把 `https://law3.hexilab.cn/health` 转发到了 `/api/health`，对外仍是规范的 `/health`；
在虚拟机上自检用 `curl http://127.0.0.1:8003/api/health`。

## 四、虚拟机上的操作步骤

### 第 1 步：拿代码

```bash
cd ~
git clone https://github.com/vertin5015/frcs-system-fullstack-complete.git
cd frcs-system-fullstack-complete
```

### 第 2 步：一次性初始化（阶段2-1/2/4）

```bash
sudo bash deploy/scripts/bootstrap-host.sh
```

建 `/var/www/law1~4-web`、`/opt/projects/law1~4/{logs,data}`、`/etc/nginx/ssl`，
写入 Nginx 配置并 `nginx -t` 后 reload，生成 `/opt/projects/.env`。
如果提示缺少证书，先用脚本里打印的 acme.sh 命令申请 `*.hexilab.cn` 泛域名证书。

### 第 3 步：填环境变量（阶段3-2）

```bash
sudo nano /opt/projects/.env
```

只改模型三行：虚拟机上装了 Ollama 就保持默认；用云端模型就填
`SPRING_AI_OPENAI_API_KEY` / `SPRING_AI_OPENAI_BASE_URL`（**不要带 `/v1`**）/
`SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL`。
没有可用模型时网页和检索仍可用，但 AI 摘要、RAG 助手会失败。

### 第 4 步：部署本项目（阶段3-1~3-4）

```bash
sudo bash deploy/scripts/deploy-frcs.sh
```

依次做：克隆代码到 `/opt/projects/law3/src` → 构建前端 dist 并发布到 `/var/www/law3-web`
→ 构建后端镜像 `frcs-backend:v1.0.0` → 起 `law3-mysql / law3-redis / law3-api`
→ 健康检查 → 注入演示知识库。首次构建要下载 Maven/npm 依赖，约 5~20 分钟。

### 第 5 步：验收（阶段4）

```bash
curl -fsS http://127.0.0.1:8003/api/health                        # 后端容器
curl -fsS -H 'Host: law3.hexilab.cn' http://127.0.0.1/health      # Nginx
systemctl status frpc --no-pager | head -5                        # frp 隧道
```

浏览器打开 `https://law3.hexilab.cn`，注册登录、案例检索、AI 摘要、Agent 问答、
收藏/历史/充值各走一遍。

## 五、其他组交付后（阶段3）

对方给出「镜像名:tag + 环境变量清单 + 持久化目录 + dist.zip」后：

```bash
sudo bash /opt/projects/enable-project.sh law1 lawllm1-api:v1.0.0 /tmp/law1-dist.zip
```

脚本会解压前端到 `/var/www/law1-web`、生成 `/opt/projects/compose/law1.yml`、
起 `law1-api`（映射 `127.0.0.1:8001:8000`）并 `curl 127.0.0.1:8001/health`。
law2、law4 同理。

如果对方还要额外环境变量，把参数加在 `/opt/projects/compose/law1.yml` 的 `environment` 下再重跑一次即可。
也可以手工按老师模板把 `docker-compose.yml` 里的占位注释放开、填镜像 tag，两种方式二选一。

发给其他组的交付清单模板：

```
项目名称：lawN
镜像名:tag：
容器内监听端口：8000
健康检查：GET /health 返回 200（若带 context-path，请写完整路径）
环境变量清单：
持久化目录：/app/data、/app/logs
前端：dist.zip（history 路由，接口一律相对路径 /api/xxx，不要写死域名）
```

## 六、迭代更新（阶段5）

```bash
# 本项目后端 + 前端整体更新
sudo bash /opt/projects/law3/src/deploy/scripts/deploy-frcs.sh

# 只更新后端（跳过前端）
sudo bash /opt/projects/law3/src/deploy/scripts/deploy-frcs.sh --skip-frontend

# 只替换某个组的前端
sudo rm -rf /var/www/law1-web/* && sudo unzip -q /tmp/law1-dist.zip -d /var/www/law1-web
sudo chown -R www-data:www-data /var/www/law1-web && sudo nginx -t && sudo systemctl reload nginx
```

## 七、排查顺序

| 现象 | 大概率原因 | 怎么查 |
| --- | --- | --- |
| 域名打不开、超时 | 443 未放行 / frpc 掉线 | 云服务器安全组；`systemctl status frpc` |
| 出现阿里云备案提示页 | 走的是 80 端口 HTTP | 用 `https://` 访问（未备案域名 80 必被拦） |
| 打开是占位页 | 前端没发布到 `/var/www/lawN-web` | `ls /var/www/lawN-web` |
| 页面能开、接口 502 | 后端容器没起 / 端口不对 | `docker ps`、`curl 127.0.0.1:800N/api/health` |
| 接口 504、AI 摘要失败 | Nginx 超时太短 | 确认用的是本目录的 conf（1800s + 关 buffering） |
| 页面是旧版本 | `index.html` 被缓存 | 本配置已设 no-cache，硬刷新一次 |
| 容器反复重启 | 数据库没连上 / 环境变量缺失 | `docker logs law3-api` |
| 表不存在 | MySQL 数据卷早于 init 脚本建立 | 清掉 `/opt/projects/law3/data/mysql` 重启（会清库） |

常用命令：

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
docker logs -f law3-api
docker compose -f /opt/projects/docker-compose.yml ps
sudo nginx -t && sudo systemctl reload nginx
sudo tail -f /var/log/nginx/error.log
```

## 八、2026-09-12 实测现状（需要老师先解决）

| 检查项 | 结果 |
| --- | --- |
| law1/law2 的 DNS | 已解析到 8.129.108.145 |
| law3/law4 的 DNS | 无 A 记录，需要补 |
| 云服务器 7000 | 通，frps 在运行 |
| 云服务器 80 | 返回阿里云 `403 Non-compliance ICP Filing`，域名未备案 |
| 云服务器 443 | 连接失败：frps 未监听 443 或安全组未放行 |

结论：**443 不通的话四个域名都打不开**，必须先让老师在云服务器上确认
`frps.toml` 有 `vhostHTTPSPort = 443` 并在安全组放行 443；同时补齐 law3/law4 的 DNS。
域名未备案时 80 端口一定被拦，只能走 HTTPS。
