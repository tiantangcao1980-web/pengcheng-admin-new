package com.pengcheng.app.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.message.subscribe.auth.MpUserSubscribeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序订阅消息授权回调 Controller
 *
 * <p>客户端调用 {@code wx.requestSubscribeMessage} 成功后，
 * 需回调 {@link #record} 接口将授权记录落库；
 * 当用户在小程序"订阅消息管理"中关闭授权时，
 * 需回调 {@link #revoke} 接口标记撤销。</p>
 *
 * <p>两个接口均需登录态（Sa-Token），userId 从 Token 中获取，
 * 避免客户端伪造 userId 参数。</p>
 */
@Slf4j
@RestController
@RequestMapping("/app/mp/subscribe")
@RequiredArgsConstructor
public class MpSubscribeController {

    private final MpUserSubscribeService mpUserSubscribeService;

    // -------------------------------------------------------------------------
    // 记录授权
    // -------------------------------------------------------------------------

    /**
     * 记录用户订阅授权
     *
     * <p>小程序端 {@code wx.requestSubscribeMessage} 成功后调用。
     * 每次用户同意授权对应 quotaIncrement=1。</p>
     *
     * @param req body：templateId（模板 ID）、openId（用户 openId）
     */
    @SaCheckLogin
    @PostMapping("/record")
    public Result<Void> record(@RequestBody SubscribeRecordReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[MpSubscribe] 记录授权: userId={}, templateId={}, openId={}",
                userId, req.getTemplateId(), req.getOpenId());
        mpUserSubscribeService.recordSubscribe(userId, req.getOpenId(), req.getTemplateId(), 1);
        return Result.ok();
    }

    // -------------------------------------------------------------------------
    // 撤销授权
    // -------------------------------------------------------------------------

    /**
     * 撤销订阅授权
     *
     * <p>用户在小程序"订阅消息管理"中关闭某模板后，
     * 客户端检测到变化时回调此接口。</p>
     *
     * @param req body：templateId（模板 ID）
     */
    @SaCheckLogin
    @PostMapping("/revoke")
    public Result<Void> revoke(@RequestBody SubscribeRevokeReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[MpSubscribe] 撤销授权: userId={}, templateId={}", userId, req.getTemplateId());
        mpUserSubscribeService.revoke(userId, req.getTemplateId());
        return Result.ok();
    }

    // -------------------------------------------------------------------------
    // 内部 DTO
    // -------------------------------------------------------------------------

    @Data
    public static class SubscribeRecordReq {
        /** 订阅消息模板 ID */
        private String templateId;
        /** 微信小程序 openId */
        private String openId;
    }

    @Data
    public static class SubscribeRevokeReq {
        /** 订阅消息模板 ID */
        private String templateId;
    }
}
