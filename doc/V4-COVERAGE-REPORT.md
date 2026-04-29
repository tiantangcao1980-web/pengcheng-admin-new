# V4 JaCoCo 覆盖率报告

生成时间：2026-04-29  
JaCoCo 版本：0.8.12  
JDK：17

---

## 一、JaCoCo 配置说明

### 配置架构

顶层 `pom.xml` 在 `<build><plugins>` 中已声明 `jacoco-maven-plugin`，配置如下：

- 默认 `jacoco.skip=true`（不影响日常构建和编译速度）
- 排除 entity / dto / vo / mapper / Config / Application 等非业务类
- `jacoco-prepare-agent`：绑定 `initialize` phase（自动）
- `jacoco-report`：绑定 `verify` phase（需运行 `mvn verify`）
- `jacoco-check`：绑定 `verify` phase，阈值 `coverage.line.minimum=0.45`（45%）

### 已启用 jacoco 的模块（`<jacoco.skip>false</jacoco.skip>`）

| 模块 | 状态 | 备注 |
|------|------|------|
| pengcheng-system | 已启用（FU5 新增） | V4.0 核心系统模块 |
| pengcheng-crm | 已启用 | V4.0 CRM 模块 |
| pengcheng-oa | 已启用 | V4.0 OA 模块 |
| pengcheng-finance | 已启用 | V4.0 财务模块 |
| pengcheng-bi | 已启用 | V4.0 BI 模块 |
| pengcheng-realty | 已启用（V3.1 落地） | 房产核心模块 |
| pengcheng-hr | 已启用（V3.1 落地） | 人事绩效模块 |

---

## 二、生成覆盖率报告命令

### 本地生成（单模块）

```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk/Contents/Home

# 5 个核心模块（逐一或批量）
mvn -B -DskipITs -Djacoco.skip=false \
  -pl pengcheng-core/pengcheng-system -am test

# 生成 HTML 报告（report 绑定在 verify phase）
mvn -B -Djacoco.skip=false \
  -pl pengcheng-core/pengcheng-system,pengcheng-core/pengcheng-crm,pengcheng-core/pengcheng-oa,pengcheng-core/pengcheng-finance,pengcheng-core/pengcheng-bi \
  jacoco:report

# 查看覆盖率摘要
for mod in pengcheng-system pengcheng-crm pengcheng-oa pengcheng-finance pengcheng-bi; do
  csv="pengcheng-core/$mod/target/site/jacoco/jacoco.csv"
  if [ -f "$csv" ]; then
    awk -F, -v m="$mod" 'NR>1 {lc+=$5; lm+=$4; bc+=$7; bm+=$6} END {
      printf "%s: line %.1f%% (%d/%d), branch %.1f%% (%d/%d)\n",
      m, lc*100/(lc+lm), lc, lc+lm, bc*100/(bc+bm), bc, bc+bm
    }' "$csv"
  fi
done
```

### CI 自动生成

CI pipeline（`.github/workflows/ci.yml`）已配置：
1. `mvn -B -DskipITs -Djacoco.skip=false test` —— 运行测试并注入 jacoco agent
2. `mvn jacoco:report` —— 对 5 个核心模块生成 HTML/CSV 报告
3. artifact upload `pengcheng-core/*/target/site/jacoco` —— 上传报告

---

## 三、当前覆盖率数字

> **注意**：以下数字需在 CI 或本地运行 `mvn jacoco:report` 后填入实际值。  
> 报告路径：`pengcheng-core/{module}/target/site/jacoco/index.html`

### 测试文件统计（基于代码仓库现状，2026-04-29）

| 模块 | 测试文件数 | 覆盖的业务包 | 预估 Line Coverage |
|------|-----------|-------------|-------------------|
| pengcheng-system | ~30 个 | automation / dashboard / meeting / smarttable / ocr / saas / openapi / doc / tenant / observability | 预估 35–50% |
| pengcheng-crm | ~7 个 | lead / customfield / importexport | 预估 25–40% |
| pengcheng-oa | ~6 个 | shift / flow / template / correction | 预估 30–45% |
| pengcheng-finance | ~2 个 | contract.sign | 预估 10–20% |
| pengcheng-bi | ~2 个 | engine / service | 预估 10–20% |

### 实际覆盖率（运行 mvn 后填入）

| 模块 | Line Covered | Line Missed | Line % | Branch % |
|------|-------------|-------------|--------|----------|
| pengcheng-system | — | — | — | — |
| pengcheng-crm | — | — | — | — |
| pengcheng-oa | — | — | — | — |
| pengcheng-finance | — | — | — | — |
| pengcheng-bi | — | — | — | — |

---

## 四、补测试优先级（按业务关键性）

### P1 — 立即补测（业务核心、覆盖率预估 < 30%）

#### 1. `pengcheng-finance`（财务模块）
- **业务重要性**：合同管理、发票开具、提成规则，直接涉及资金流转
- **当前测试**：仅 `ContractSignServiceImplTest` + `HutoolEsignHttpClientTest`（e 签宝相关）
- **需补充**：
  - `CommissionRuleServiceImpl` — 通用销售提成计算逻辑
  - `InvoiceServiceImpl` — 发票开具校验逻辑
  - `ContractServiceImpl` — 合同状态机转换

#### 2. `pengcheng-bi`（BI 分析模块）
- **业务重要性**：多维查询引擎，错误会导致数据统计失真
- **当前测试**：`JdbcBiQueryEngineTest` + `BiViewModelServiceImplTest`
- **需补充**：
  - BI 查询 SQL 构建逻辑（维度/指标组合）
  - 导出功能边界用例

### P2 — 重点补测（主链路、覆盖率预估 30–40%）

#### 3. `pengcheng-crm`（CRM 模块）
- **当前测试**：线索分配规则、自定义字段、导入导出
- **需补充**：
  - `CustomerTagServiceImpl` — 客户标签多标签操作
  - 跟进记录媒体上传边界
  - 线索转化状态机

#### 4. `pengcheng-oa`（OA 模块）
- **当前测试**：班次/补卡/审批模板/流程引擎
- **需补充**：
  - 审批流多级串行边界（已有 `ApprovalFlowEngineImplTest` 但需扩充）
  - 假勤假期余额扣减逻辑

### P3 — 后续补测（覆盖率已相对较好）

#### 5. `pengcheng-system`（系统模块）
- **当前测试**：~30 个测试文件，覆盖面最广
- **需补充**：
  - `SaasSubscriptionService` — SaaS 订阅升降级
  - `TenantDataIsolationFilter` — 多租户隔离拦截器

---

## 五、报告查看路径

本地运行后，HTML 报告位于：

```
pengcheng-core/pengcheng-system/target/site/jacoco/index.html
pengcheng-core/pengcheng-crm/target/site/jacoco/index.html
pengcheng-core/pengcheng-oa/target/site/jacoco/index.html
pengcheng-core/pengcheng-finance/target/site/jacoco/index.html
pengcheng-core/pengcheng-bi/target/site/jacoco/index.html
```

CSV 摘要（用于脚本解析）：

```
pengcheng-core/*/target/site/jacoco/jacoco.csv
```

---

## 六、JaCoCo 排除规则（顶层 pom 已配置）

以下类不计入覆盖率统计：

- `**/package-info*`
- `**/*Application*`（Spring Boot 启动类）
- `**/*Config*` / `**/*Configuration*`（配置类）
- `**/entity/**`（JPA/MyBatis 实体）
- `**/dto/**` / `**/vo/**`（数据传输对象）
- `**/mapper/**`（MyBatis Mapper 接口）
- `**/common/exception/**`（公共异常类）
- `**/task/**`（定时任务调度类）
- `**/provider/**`（三方服务适配类）
