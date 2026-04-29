package com.pengcheng.admin.controller.onlyoffice;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.doc.entity.Doc;
import com.pengcheng.system.doc.mapper.DocMapper;
import com.pengcheng.system.doc.onlyoffice.OnlyOfficeCallback;
import com.pengcheng.system.doc.onlyoffice.OnlyOfficeConfigBuilder;
import com.pengcheng.system.doc.onlyoffice.OnlyOfficeProperties;
import com.pengcheng.system.doc.onlyoffice.OnlyOfficeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OnlyOffice 编辑器配置 + 回调 Controller。
 *
 * <p>前端流程：
 * <ol>
 *   <li>用户点击文档"在线编辑"按钮 → 调 {@code GET /admin/onlyoffice/config?docId=...&mode=edit}
 *       拿到完整 config（含 JWT token）；</li>
 *   <li>前端把 config 传给 {@code DocsAPI.DocEditor(elementId, config)} 渲染 iframe；</li>
 *   <li>OnlyOffice Server 在保存时回调 {@code POST /api/onlyoffice/callback}（**公开端点**，依靠 JWT 校验）。</li>
 * </ol>
 *
 * <p>Feature Flag: {@code pengcheng.feature.onlyoffice=true} 开启；关时本 Controller 不注册。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pengcheng.feature.onlyoffice", havingValue = "true")
public class OnlyOfficeController {

    private final DocMapper docMapper;
    private final OnlyOfficeService onlyOfficeService;
    private final OnlyOfficeConfigBuilder configBuilder;
    private final OnlyOfficeProperties props;

    /** 返回 OnlyOffice editor config（含 JWT），前端直接传给 DocsAPI.DocEditor。 */
    @GetMapping("/admin/onlyoffice/config")
    @SaCheckLogin
    @SaCheckPermission("doc:edit")
    public Result<Map<String, Object>> getConfig(@RequestParam Long docId,
                                                  @RequestParam(defaultValue = "edit") String mode,
                                                  HttpServletRequest req) {
        Doc doc = docMapper.selectById(docId);
        if (doc == null) {
            return Result.fail("文档不存在");
        }
        // 不支持的文档类型（如纯 markdown）走旧编辑器
        if (doc.getFileType() == null || !isOfficeType(doc.getFileType())) {
            return Result.fail("此文档类型不支持 OnlyOffice 编辑：" + doc.getFileType());
        }

        Long userId = StpUtil.getLoginIdAsLong();
        String docKey = onlyOfficeService.resolveDocKey(doc);
        // fileUrl：OnlyOffice Server 必须能从外网访问；建议在生成时拼上下载签名
        String fileUrl = req.getScheme() + "://" + req.getServerName()
                + (req.getServerPort() == 80 || req.getServerPort() == 443 ? "" : ":" + req.getServerPort())
                + "/api/onlyoffice/file/" + docId + "?token=" + docKey;
        String fileName = doc.getTitle() + "." + doc.getFileType();

        Map<String, Object> config = configBuilder.build(
                docKey, fileUrl, fileName, doc.getFileType(),
                userId, "User-" + userId, mode);

        // 让前端知道 server-url（前端 <script src="${serverUrl}/web-apps/apps/api/documents/api.js">）
        Map<String, Object> result = new HashMap<>();
        result.put("config", config);
        result.put("serverUrl", props.getServerUrl());
        return Result.ok(result);
    }

    /**
     * OnlyOffice Server 保存回调（公开端点，依靠 JWT 校验）。
     *
     * <p>必须严格按 OnlyOffice 协议返回 {@code {"error":N}} JSON 格式。
     */
    @PostMapping("/api/onlyoffice/callback")
    public Map<String, Integer> callback(@RequestBody OnlyOfficeCallback cb) {
        Map<String, Integer> resp = new HashMap<>();
        try {
            if (props.isJwtEnabled() && !configBuilder.verifyJwt(cb.getToken())) {
                log.warn("[OnlyOffice] callback JWT 校验失败 key={}", cb.getKey());
                resp.put("error", 1);
                return resp;
            }
            int code = onlyOfficeService.handleCallback(cb);
            resp.put("error", code);
        } catch (Exception e) {
            log.error("[OnlyOffice] callback 异常 key={}", cb.getKey(), e);
            resp.put("error", 1);
        }
        return resp;
    }

    private boolean isOfficeType(String type) {
        switch (type.toLowerCase()) {
            case "docx": case "doc":
            case "xlsx": case "xls": case "csv":
            case "pptx": case "ppt":
            case "odt": case "ods": case "odp":
            case "rtf":
                return true;
            default:
                return false;
        }
    }
}
