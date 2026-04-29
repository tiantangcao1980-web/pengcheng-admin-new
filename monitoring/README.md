# MasterLife 可观测性栈

Prometheus + Loki + Grafana，通过 Docker Compose `observability` profile 可选启用。

## 快速启动

```bash
# 仅启动可观测性三件套（不重启主服务）
docker compose --profile observability up -d prometheus loki grafana

# 或连同主服务一起启动
docker compose --profile observability up -d
```

## 访问地址

| 服务       | 地址                    | 默认账号/密码              |
|------------|-------------------------|---------------------------|
| Grafana    | http://localhost:3000   | admin / masterlife2024    |
| Prometheus | http://localhost:9090   | 无需认证                  |
| Loki       | http://localhost:3100   | 无需认证（内部端口）       |

## 应用侧配置

- **指标端点**：`http://app:8080/actuator/prometheus`（每 15s 被 Prometheus 拉取）
- **日志推送**：生产环境（`spring.profiles.active=prod`）自动通过 Loki4j Appender 推送到 Loki
- **自定义环境变量**：`LOKI_URL`（默认 `http://loki:3100/loki/api/v1/push`）

## 目录结构

```
monitoring/
├── prometheus.yml                          # Prometheus scrape 配置
├── grafana-datasources/
│   └── datasources.yml                     # Prometheus + Loki 数据源 provision
├── grafana-dashboards/
│   ├── dashboards.yml                      # Dashboard 目录 provision 配置
│   └── v4-mvp-overview.json               # V4 MVP 核心看板
└── README.md
```

## Grafana 看板说明（v4-mvp-overview）

| Panel | 指标 | 说明 |
|-------|------|------|
| HTTP 请求 P95 响应时间 | `http_server_requests_seconds_bucket` | 全接口 P95 时延 |
| OpenAPI 调用 QPS | `pengcheng_openapi_requests_total` | 按 AK 分组 |
| Webhook 投递结果 | `pengcheng_webhook_delivery_total` | SUCCESS/FAILED/DEAD |
| AI Copilot 动作次数 | `pengcheng_ai_copilot_actions_total` | FOLLOW/TODO/APPROVAL |

## 日志查询示例（Loki）

```logql
# 查看所有 ERROR 日志
{application="MasterLife", level="ERROR"} |= ""

# 查看 OpenAPI 模块日志
{application="MasterLife"} |= "openapi"

# 最近 1h 的 Webhook 失败日志
{application="MasterLife"} |= "WEBHOOK" |= "FAILED" | json
```
