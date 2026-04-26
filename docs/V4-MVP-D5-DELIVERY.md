# V4.0 MVP - D5 交付清单（移动办公闭环⑤）

> 角色：D5 - 移动办公（APP 打包 + 原生能力 + 三通道推送）
> 范围：约 29 人日（PRD §四 闭环⑤）

## 一、交付总览

| # | 任务 | 状态 | 主要文件 |
|---|------|------|---------|
| 1 | uni-app APP 打包流水线 | ✓ 脚本 | `pengcheng-uniapp/scripts/build-{app,h5,mp}.sh` |
| 2 | iOS 企业签 / TestFlight 文档 | ✓ 占位 | 本文 §五 |
| 3 | Android 多渠道打包 | ✓ 脚本 | `pengcheng-uniapp/scripts/build-app.sh` |
| 4 | 原生推送插件（极光） | ✓ 桥接 | `pengcheng-uniapp/utils/native-bridge.js` + `manifest.json` |
| 5 | 定位/相机/扫一扫 业务组件 | ✓ | `pengcheng-uniapp/components/native/{Geolocation,Camera,Scan}.vue` |
| 6 | OCR 名片识别（百度/腾讯任选） | ✓ 后端 | `pengcheng-core/pengcheng-system/src/main/java/com/pengcheng/system/ocr/` |
| 7 | 小程序订阅消息模板 + 调度 Service | ✓ 后端 | `pengcheng-core/pengcheng-message/src/main/java/com/pengcheng/message/subscribe/` |
| 8 | 离线缓存统一方案（白名单 + TTL + 合并） | ✓ | `pengcheng-uniapp/utils/offline.js` + `scripts/test-offline.js` |
| 9 | 三通道统一调度（APP/订阅/站内信） | ✓ 后端 | `pengcheng-infra/pengcheng-push/.../unified/` + `pengcheng-core/pengcheng-message/.../channel/` |
| 10 | APP 启动页/引导页/版本更新 | ✓ 配置 | `manifest.json` `splashscreen` 节点（已存在）+ 极光占位 |

## 二、新增/修改的文件清单

### 后端 - pengcheng-infra/pengcheng-push（unified-dispatcher 子包）
- `src/main/java/com/pengcheng/push/unified/PushChannel.java`
- `src/main/java/com/pengcheng/push/unified/PushTarget.java`
- `src/main/java/com/pengcheng/push/unified/PushPayload.java`
- `src/main/java/com/pengcheng/push/unified/PushDispatchResult.java`
- `src/main/java/com/pengcheng/push/unified/PushDecisionRule.java`
- `src/main/java/com/pengcheng/push/unified/UnifiedPushDispatcher.java`
- `src/main/java/com/pengcheng/push/unified/ChannelSubscribeSender.java`
- `src/main/java/com/pengcheng/push/unified/ChannelInboxSender.java`
- `src/test/java/com/pengcheng/push/unified/UnifiedPushDispatcherTest.java`
- 修改 `pom.xml`：新增 `spring-boot-starter-test` 测试依赖

### 后端 - pengcheng-core/pengcheng-message（channel + subscribe 子包）
- `src/main/java/com/pengcheng/message/channel/PushChannelLog.java`
- `src/main/java/com/pengcheng/message/channel/UserChannelProfile.java`
- `src/main/java/com/pengcheng/message/channel/UserChannelResolver.java`
- `src/main/java/com/pengcheng/message/channel/PushChannelLogStore.java`
- `src/main/java/com/pengcheng/message/channel/ChannelPushService.java`
- `src/main/java/com/pengcheng/message/channel/ChannelPushRequest.java`
- `src/main/java/com/pengcheng/message/subscribe/SubscribeMsgTemplate.java`
- `src/main/java/com/pengcheng/message/subscribe/SubscribeMessageRequest.java`
- `src/main/java/com/pengcheng/message/subscribe/SubscribeMessageRenderer.java`
- `src/main/java/com/pengcheng/message/subscribe/SubscribeMsgTemplateRepository.java`
- `src/main/java/com/pengcheng/message/subscribe/SubscribeMessageService.java`
- `src/test/java/com/pengcheng/message/channel/ChannelPushServiceTest.java`
- `src/test/java/com/pengcheng/message/subscribe/SubscribeMessageServiceTest.java`

### 后端 - pengcheng-core/pengcheng-system（ocr 子包，红线例外许可）
- `src/main/java/com/pengcheng/system/ocr/BusinessCardData.java`
- `src/main/java/com/pengcheng/system/ocr/OcrProvider.java`
- `src/main/java/com/pengcheng/system/ocr/BusinessCardParser.java`
- `src/main/java/com/pengcheng/system/ocr/BusinessCardOcrService.java`
- `src/test/java/com/pengcheng/system/ocr/BusinessCardOcrServiceTest.java`

### 数据库 Migration
- `sql/V22__push_channel_log.sql`
- `sql/V23__subscribe_msg_template.sql`

### Web 前端
- `pengcheng-ui/src/api/pushChannel.ts`（新建）
- `pengcheng-ui/src/views/system/push-channel/index.vue`（新建）

### uniapp
- `pengcheng-uniapp/scripts/build-app.sh`（新建）
- `pengcheng-uniapp/scripts/build-h5.sh`（新建）
- `pengcheng-uniapp/scripts/build-mp.sh`（新建）
- `pengcheng-uniapp/scripts/test-offline.js`（新建）
- `pengcheng-uniapp/utils/native-bridge.js`（新建）
- `pengcheng-uniapp/utils/offline.js`（新建）
- `pengcheng-uniapp/components/native/Geolocation.vue`（新建）
- `pengcheng-uniapp/components/native/Camera.vue`（新建）
- `pengcheng-uniapp/components/native/Scan.vue`（新建）
- `pengcheng-uniapp/manifest.json`（修改 `app-plus.modules` / `distribute.android.permissions` / `distribute.ios.privacyDescription` / `distribute.sdkConfigs`）

## 三、运行测试

### 后端
```bash
# 通道调度 + 订阅消息（含 ChannelPushService / SubscribeMessageService / Renderer）
mvn -pl pengcheng-core/pengcheng-message -am test -DskipITs

# 三通道决策规则 + UnifiedPushDispatcher
mvn -pl pengcheng-infra/pengcheng-push -am test -DskipITs

# OCR 子包（system 模块整体跑较慢；仅跑 ocr 测试用 -Dtest=）
mvn -pl pengcheng-core/pengcheng-system -am test -DskipITs -Dtest='BusinessCardOcrServiceTest'
```

> 不需要 `mvn install`；按上述命令即可拉起依赖（`-am`）并执行单测。
> 测试目标：`channel-dispatch`、`subscribe-msg`、`unified-dispatcher` 行覆盖率 ≥ 60%（覆盖三通道决策、降级链、模板渲染、OCR 字段映射）。

### uniapp 离线缓存
```bash
node pengcheng-uniapp/scripts/test-offline.js
```
预期：所有 suite 全 PASS，退出码 0。

### uniapp 打包脚本（mock 干跑）
```bash
chmod +x pengcheng-uniapp/scripts/*.sh
HBX_CLI=mock-hbx ./pengcheng-uniapp/scripts/build-app.sh android huawei
HBX_CLI=mock-hbx ./pengcheng-uniapp/scripts/build-app.sh ios appstore
./pengcheng-uniapp/scripts/build-h5.sh prod
./pengcheng-uniapp/scripts/build-mp.sh weixin
```
没有真实 HBuilderX CLI 时，脚本会写出占位 `build-info.json`，便于 CI 流水线 verify。

## 四、需要用户提供的密钥（占位）

> **重要：以下凭据请通过 CI Secret 注入，切勿硬编码到仓库。**

| 用途 | 密钥变量 | 说明 |
|------|---------|------|
| 极光推送 AppKey | `JPUSH_APP_KEY` | 在 `manifest.json` `app-plus.distribute.sdkConfigs.push.jpush.appkey` 中填写；为空时本地构建会跳过 |
| 极光推送 channel | `JPUSH_CHANNEL` | 默认 `developer-default` |
| iOS 企业签 .p12 | `IOS_CERT_PATH` + `IOS_CERT_PASSWORD` | iOS 打包时由 `build-app.sh ios` 读取，缺失时仅生成 mock 占位 |
| iOS 描述文件 | `IOS_PROVISION_PATH` | `.mobileprovision` |
| iOS Team ID | `IOS_TEAM_ID` | Apple Developer 后台获取 |
| 百度 / 腾讯 OCR | `OCR_PROVIDER` / `OCR_API_KEY` / `OCR_SECRET_KEY` | 后端 `OcrProvider` 实现类启动时读取 SystemConfig（`ocr` 分组）；预留接入点 |
| 微信小程序订阅模板 ID | 通过 `subscribe_msg_template` 表配置 | 由后台运营人员在公众平台申请后录入 |

## 五、iOS 企业签 / TestFlight 发布流程

### 5.1 准备 Apple Developer 账号
- 企业账号：Apple Enterprise Developer Program（$299/年），适合内部分发
- App Store 账号：Apple Developer Program（$99/年），适合 TestFlight + App Store

### 5.2 证书与 provisioning profile
1. 登录 https://developer.apple.com → Certificates, Identifiers & Profiles
2. **Certificates**：
   - 企业签：创建 `iOS Distribution (In-House and Ad Hoc)`
   - TestFlight/App Store：创建 `Apple Distribution`
3. **Identifiers**：注册 App ID（与 `manifest.json` 中 iOS bundleId 一致；`ios.bundleId` 一般为 `com.<your-org>.masterlife`）
4. **Profiles**：
   - 企业签：`In-House Distribution Profile`
   - TestFlight：`App Store Distribution Profile`
5. 导出 `.p12` 私钥（含密码）和 `.mobileprovision` 文件，放进 CI Secret 仓库。

### 5.3 在 manifest.json 中填写 iOS bundleId
```jsonc
"app-plus": {
  "distribute": {
    "ios": {
      "appid": "your.bundle.id",
      "privacyDescription": { ... }   // 已配置
    }
  }
}
```

### 5.4 打包步骤
```bash
export IOS_CERT_PATH=/path/to/dist.p12
export IOS_CERT_PASSWORD=...
export IOS_PROVISION_PATH=/path/to/profile.mobileprovision
export IOS_TEAM_ID=ABCDE12345
export HBX_CLI=/Applications/HBuilderX.app/Contents/MacOS/cli
./pengcheng-uniapp/scripts/build-app.sh ios appstore
```

### 5.5 上传 TestFlight
1. 打包产物：`dist/build/app-plus/ios/<profile>/MasterLife.ipa`
2. 通过 Transporter（Mac App Store 应用）上传到 App Store Connect
3. 在 App Store Connect → My Apps → TestFlight 中：
   - 内部测试：直接添加内部测试员（最多 100 人）
   - 外部测试：提交 Beta 审核（1-2 个工作日）
4. 测试员安装 TestFlight App，扫码或邮件链接安装
5. 反馈通过 TestFlight App 内置反馈渠道收集

## 六、Android 多渠道打包

### 6.1 渠道列表
- `huawei` 华为应用市场
- `xiaomi` 小米应用商店
- `oppo` OPPO 软件商店
- `vivo` vivo 应用商店
- `default` 通用渠道（官网下载）

### 6.2 打包命令
```bash
# 单渠道
./pengcheng-uniapp/scripts/build-app.sh android huawei

# 全渠道一次出
./pengcheng-uniapp/scripts/build-app.sh all release
```

### 6.3 渠道号注入
脚本会调用 `jq` 把 `channel` 写入 `manifest.json` 的 `app-plus.distribute.android.channel` 节点。
没有 `jq` 时回退到 mock 模式（仅写 build-info.json 占位）。

### 6.4 各应用市场上架要点
| 渠道 | 关键差异 |
|------|--------|
| 华为 | 需 HMS Core 集成；推送可走极光适配 |
| 小米 | 需小米推送 SDK；隐私政策必填 |
| OPPO | 应用启动 ≤ 5s，否则审核驳回 |
| vivo | 需开放平台备案（30 天审核） |

## 七、三通道决策规则速查

```
hasApp(registrationId 非空) && appOnline  → APP_PUSH
hasApp(registrationId 非空) && !appOnline → APP_PUSH（厂商通道唤醒）
!hasApp && hasMpOpenId && subscribed       → MP_SUBSCRIBE
否则 webInboxEnabled                       → WEB_INBOX
全部不具备                                  → NONE（记录失败）
```

降级链：
- 主通道 APP 失败 → MP_SUBSCRIBE → WEB_INBOX
- 主通道 MP_SUBSCRIBE 失败 → WEB_INBOX
- WEB_INBOX 失败：终止

`PushDecisionRule.decideStrict()` 用于"离线必达"业务消息：APP 离线时直接跳到订阅消息，不走 APP 厂商通道。

## 八、装配指引（供其他 Agent 接力）

### 装配 ChannelPushService（建议在 starter 模块或独立 @Configuration 中）
```java
@Configuration
public class ChannelPushAutoConfig {

    @Bean
    public UnifiedPushDispatcher unifiedPushDispatcher(
            PushServiceFactory pushFactory,
            ChannelSubscribeSender subscribeSender,
            ChannelInboxSender inboxSender) {
        return new UnifiedPushDispatcher(pushFactory, subscribeSender, inboxSender);
    }

    @Bean
    public ChannelPushService channelPushService(
            UserChannelResolver resolver,
            UnifiedPushDispatcher dispatcher,
            PushChannelLogStore logStore) {
        return new ChannelPushService(resolver, dispatcher, logStore);
    }
}
```
对应的 `UserChannelResolver` / `ChannelSubscribeSender` / `ChannelInboxSender` / `PushChannelLogStore` 由其他 Agent 在 system / message / wechat 模块下实现并以 `@Component` 注册。

### 已有 NotificationServiceImpl 增量改造（建议路径）
原 `NotificationServiceImpl.pushSafe(...)` 仅调用 APP 通道；后续可改为：
```java
channelPushService.push(userId, ChannelPushRequest.builder()
    .title(title).content(content)
    .bizType("approval").bizId(requestId)
    .subscribeTemplateId("...")  // 由 SubscribeMsgTemplateRepository 查得
    .build());
```
此改造留给后续 Agent，不在 D5 范围内（避免冲突 D2/D4）。

## 九、红线问题与限制

1. Java 包名不能含连字符，`channel-dispatch` / `subscribe-msg` / `unified-dispatcher` 物理目录在 Java 中映射为 `channel` / `subscribe` / `unified` 包。等价语义保持。
2. `pengcheng-core/pengcheng-system/ocr/` 是任务红线明确许可的例外，未触碰其它 system 子包。
3. **未运行 maven 与 node 测试**：本 worktree 沙箱拒绝 `mvn` / `node` 调用；测试代码已写完，请用户在主仓库环境执行 §三 的命令复核。
4. uniapp 脚本的可执行权限（`chmod +x`）需在用户主机首次执行：
   ```bash
   chmod +x pengcheng-uniapp/scripts/*.sh
   ```
5. 极光推送 SDK 实体包未下载——只在 `manifest.json.app-plus.distribute.sdkConfigs.push.jpush` 占位；用户在 HBuilderX 中"App 离线 SDK 配置"对话框补齐 AppKey 即可生效。
6. iOS / Android 真实证书与 AppKey 全部走环境变量 / SystemConfig，未硬编码 fake key。

## 十、自验证 checklist

- [ ] 后端：`mvn -pl pengcheng-core/pengcheng-message -am test -DskipITs` 全绿
- [ ] 后端：`mvn -pl pengcheng-infra/pengcheng-push -am test -DskipITs` 全绿
- [ ] uniapp：`node pengcheng-uniapp/scripts/test-offline.js` 全绿
- [ ] manifest.json：`app-plus.modules` 含 `Push / Geolocation / Camera / Barcode`
- [ ] SQL：V22 / V23 已加入并被 Flyway 识别
- [ ] Web：`pengcheng-ui/src/views/system/push-channel/index.vue` 路由可挂（路由表由 D2 / D3 维护，本 PR 不直接动）

