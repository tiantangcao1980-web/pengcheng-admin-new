package com.pengcheng.wechat.subscribe;

import com.pengcheng.push.unified.ChannelSubscribeSender;
import com.pengcheng.wechat.WechatMiniProgramService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 微信小程序订阅消息发送器（E5）
 *
 * <p>实现 {@link ChannelSubscribeSender}，调用微信
 * {@code POST cgi-bin/message/subscribe/send} 接口。
 *
 * <p>Access Token 取自 {@link WechatMiniProgramService#getAccessToken()}，
 * 该方法内部已完成 Redis 缓存（V3.2 WP-S6-G 落地），提前 300 秒刷新。
 *
 * <p>Feature Flag {@code pengcheng.feature.wechat.mini=false} 时此 Bean 不被加载，
 * 由 {@code NoOpChannelSubscribeSender} 兜底（见 WechatSubscribeSenderConfiguration）。
 */
@Slf4j
public class WechatMpSubscribeSender implements ChannelSubscribeSender {

    public static final String CHANNEL_CODE = "wechat_subscribe";

    private static final String SUBSCRIBE_SEND_URL =
            "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";

    /** 从 WechatMiniProgramService 获取 access_token（走缓存，V3.2 WP-S6-G 已落地） */
    private final WechatMiniProgramService wechatService;
    private final WechatHttpClient httpClient;

    /** 生产构造函数 */
    public WechatMpSubscribeSender(WechatMiniProgramService wechatService) {
        this(wechatService, new HutoolWechatHttpClient());
    }

    /** 测试构造函数（可注入 mock HTTP 客户端） */
    WechatMpSubscribeSender(WechatMiniProgramService wechatService, WechatHttpClient httpClient) {
        this.wechatService = wechatService;
        this.httpClient = httpClient;
    }

    /** 渠道编码（非接口方法 — ChannelSubscribeSender 接口未约束）。 */
    public String channelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public boolean send(String openId, String templateId, Map<String, String> data, String page) {
        if (!wechatService.isConfigured()) {
            log.warn("[WechatSubscribe] 小程序未配置，跳过发送: openId={}, templateId={}", openId, templateId);
            return false;
        }

        String accessToken;
        try {
            accessToken = wechatService.getAccessToken();
        } catch (WechatTokenExpiredException e) {
            log.error("[WechatSubscribe] Access token 获取失败（token 失效）: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("[WechatSubscribe] Access token 获取异常: openId={}, error={}", openId, e.getMessage());
            return false;
        }

        try {
            return doSend(accessToken, openId, templateId, data, page);
        } catch (WechatTemplateRenderException e) {
            log.error("[WechatSubscribe] 模板渲染异常: templateId={}, error={}", templateId, e.getMessage());
            return false;
        } catch (WechatTokenExpiredException e) {
            log.error("[WechatSubscribe] access_token 已失效，需刷新: {}", e.getMessage());
            return false;
        } catch (WechatNetworkException e) {
            log.error("[WechatSubscribe] 网络请求失败: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("[WechatSubscribe] 发送订阅消息未知异常: openId={}, templateId={}", openId, templateId, e);
            return false;
        }
    }

    // ==================== 内部方法 ====================

    private boolean doSend(String accessToken, String openId, String templateId,
                           Map<String, String> data, String page) {
        String url = SUBSCRIBE_SEND_URL + "?access_token=" + accessToken;

        // 构建 data 字段：{"key": {"value": "xxx"}}
        if (data == null || data.isEmpty()) {
            throw new WechatTemplateRenderException("模板 data 不能为空: templateId=" + templateId);
        }

        cn.hutool.json.JSONObject dataJson = new cn.hutool.json.JSONObject();
        data.forEach((k, v) ->
                dataJson.set(k, new cn.hutool.json.JSONObject().set("value", v))
        );

        cn.hutool.json.JSONObject body = new cn.hutool.json.JSONObject()
                .set("touser", openId)
                .set("template_id", templateId)
                .set("data", dataJson);

        if (page != null && !page.isBlank()) {
            body.set("page", page);
        }

        String response = httpClient.postJson(url, body.toString());
        cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(response);
        int errcode = json.getInt("errcode", 0);

        if (errcode == 0) {
            log.info("[WechatSubscribe] 发送成功: openId={}, templateId={}", openId, templateId);
            return true;
        }

        // 40001 / 42001 表示 access_token 失效，需重新获取
        if (errcode == 40001 || errcode == 42001) {
            throw new WechatTokenExpiredException("access_token 失效 errcode=" + errcode);
        }

        String errmsg = json.getStr("errmsg", "unknown");
        log.error("[WechatSubscribe] 微信 API 错误: errcode={}, errmsg={}, openId={}, templateId={}",
                errcode, errmsg, openId, templateId);
        return false;
    }
}
