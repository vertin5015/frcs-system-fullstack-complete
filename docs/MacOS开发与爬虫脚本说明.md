# MacOS 开发与爬虫脚本说明

## 1. MacOS 后端环境

推荐使用 Java 21，与项目 `pom.xml` 中的 `java.version=21` 保持一致。

```bash
brew install openjdk@21 maven mysql redis
```

配置 Java：

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

启动基础服务：

```bash
brew services start mysql
brew services start redis
```

初始化数据库时，执行 `legal_cases-master/src/main/resources/sql/` 下的 SQL：

1. `schema.sql`
2. `alter_extensions_2026.sql`
3. `recharge_order.sql`
4. 旧库缺少额度字段时再执行 `alter_users_summary_credits.sql`

## 2. 后端启动

进入后端目录：

```bash
cd legal_cases-master
```

使用本地 Maven：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

或使用 Maven wrapper：

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

本地配置建议复制：

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

然后修改 MySQL 密码、Redis 地址和大模型配置。

## 3. 大模型配置

本周只需要跑通 RAG 问答流程，不做模型微调训练。

可选方案：

1. 本地 Ollama：默认配置指向 `http://127.0.0.1:11434`，模型可用 `qwen2.5:7b-instruct`。
2. DeepSeek/OpenAI 兼容接口：使用 `application-deepseek.yml` 或环境变量配置 API Key。

示例：

```bash
export SPRING_AI_OPENAI_API_KEY="你的 key"
export SPRING_AI_OPENAI_BASE_URL="https://api.deepseek.com"
export SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL="deepseek-chat"
```

## 4. 爬虫端口

当前后端配置涉及这些爬虫端口：

| 用途 | 地址 |
| --- | --- |
| 案例详情 | `http://127.0.0.1:9001/crawl.json` |
| EU 列表 | `http://127.0.0.1:9002/crawl.json`，当前也有后端内置 bridge |
| US 列表 | `http://127.0.0.1:9003/crawl.json` |
| JPN 列表 | `http://127.0.0.1:9004/crawl.json`，当前也有后端内置 bridge |

MacOS 检查端口：

```bash
nc -vz 127.0.0.1 9001
nc -vz 127.0.0.1 9002
nc -vz 127.0.0.1 9003
nc -vz 127.0.0.1 9004
```

或：

```bash
lsof -iTCP:9001 -sTCP:LISTEN
```

## 5. Windows PowerShell 脚本在 MacOS 上的处理

后端 `scripts/` 目录主要是 PowerShell 脚本：

- `verify-crawler-ports.ps1`：检查 9001-9004 端口。
- `search-stress-25.ps1`：搜索接口压力冒烟。
- `batch-summary-kb.ps1`：批量搜索、摘要、知识库入库辅助。
- `batch-summary-kb-region.ps1`：按 EU/JPN 区域批量处理。

MacOS 上有两种方式：

1. 安装 PowerShell Core 后运行：

```bash
brew install --cask powershell
pwsh ./scripts/verify-crawler-ports.ps1
```

2. 将常用脚本改写为 bash。端口检查可直接使用 `nc`，接口测试可用 `curl`。

注意：前端论文/Word 相关脚本里如果使用 Windows COM 自动化，MacOS 无法直接运行。

## 6. 本周演示建议

MacOS 侧只需保证这些命令或说明可走通：

1. Java 21、MySQL、Redis 可启动。
2. 后端能编译和运行。
3. 爬虫端口能检测。
4. `/api/kb/query` 能查询本地 KB。
5. `/api/agent/ask` 能返回 RAG 问答和 trace。
