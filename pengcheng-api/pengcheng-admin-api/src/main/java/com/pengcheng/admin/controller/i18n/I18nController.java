package com.pengcheng.admin.controller.i18n;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.i18n.entity.I18nMessage;
import com.pengcheng.system.i18n.entity.UserLocalePreference;
import com.pengcheng.system.i18n.mapper.I18nMessageMapper;
import com.pengcheng.system.i18n.service.I18nMessageService;
import com.pengcheng.system.i18n.service.UserLocalePreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * i18n 词条 + 用户偏好 + 公开导出端点。
 */
@RestController
@RequiredArgsConstructor
public class I18nController {

    private final I18nMessageMapper messageMapper;
    private final I18nMessageService messageService;
    private final UserLocalePreferenceService prefService;

    /** 公开端点：前端启动时拉指定 locale 的全量词条（namespace.key → value）。 */
    @GetMapping("/api/i18n/{locale}.json")
    public Result<Map<String, String>> exportLocale(@PathVariable String locale) {
        return Result.ok(messageService.exportLocale(locale));
    }

    /* ========== 用户偏好（已登录） ========== */

    @GetMapping("/app/me/locale")
    public Result<UserLocalePreference> myPreference() {
        return Result.ok(prefService.get(StpUtil.getLoginIdAsLong()));
    }

    @PutMapping("/app/me/locale")
    public Result<Void> updateMyPreference(@RequestBody UserLocalePreference pref) {
        pref.setUserId(StpUtil.getLoginIdAsLong());
        prefService.upsert(pref);
        return Result.ok();
    }

    /* ========== 词条管理（管理员） ========== */

    @PostMapping("/admin/i18n/messages")
    @SaCheckPermission("system:i18n:manage")
    public Result<Long> createMessage(@RequestBody I18nMessage msg) {
        messageMapper.insert(msg);
        messageService.refresh();
        return Result.ok(msg.getId());
    }

    @PutMapping("/admin/i18n/messages/{id}")
    @SaCheckPermission("system:i18n:manage")
    public Result<Void> updateMessage(@PathVariable Long id, @RequestBody I18nMessage msg) {
        msg.setId(id);
        messageMapper.updateById(msg);
        messageService.refresh();
        return Result.ok();
    }

    @PostMapping("/admin/i18n/refresh-cache")
    @SaCheckPermission("system:i18n:manage")
    public Result<Void> refreshCache() {
        messageService.refresh();
        return Result.ok();
    }
}
