package com.pengcheng.admin.controller.doc;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.doc.collab.dto.CommentCreateDTO;
import com.pengcheng.system.doc.collab.dto.ShareCreateDTO;
import com.pengcheng.system.doc.collab.entity.DocCollabState;
import com.pengcheng.system.doc.collab.entity.DocComment;
import com.pengcheng.system.doc.collab.entity.DocShare;
import com.pengcheng.system.doc.collab.service.DocCollabService;
import com.pengcheng.system.doc.collab.service.DocCommentService;
import com.pengcheng.system.doc.collab.service.DocShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档实时协作 REST 接口
 * - snapshot：WebSocket 连接前先拉一次初始状态（也可由 WS sync_init 触发，此端点作为备用）
 * - comments：评论 CRUD
 * - shares：权限分享
 */
@RestController
@RequestMapping("/admin/doc")
@RequiredArgsConstructor
public class DocCollabController {

    private final DocCollabService docCollabService;
    private final DocCommentService docCommentService;
    private final DocShareService docShareService;

    // ========== Y.js 快照 ==========

    /**
     * GET /admin/doc/{docId}/collab/snapshot
     * 获取文档 Y.js 初始状态（stateVector + updateBlob 均以 base64 返回）
     */
    @GetMapping("/{docId}/collab/snapshot")
    @SaCheckLogin
    public Result<Map<String, Object>> getSnapshot(@PathVariable Long docId) {
        DocCollabState state = docCollabService.getSnapshot(docId);
        Map<String, Object> data = new HashMap<>();
        if (state != null) {
            data.put("stateVector", state.getStateVector() != null
                    ? Base64.getEncoder().encodeToString(state.getStateVector()) : "");
            data.put("updateBlob", state.getUpdateBlob() != null
                    ? Base64.getEncoder().encodeToString(state.getUpdateBlob()) : "");
            data.put("version", state.getVersion());
        } else {
            data.put("stateVector", "");
            data.put("updateBlob", "");
            data.put("version", 0);
        }
        return Result.ok(data);
    }

    // ========== 评论 ==========

    /**
     * GET /admin/doc/{docId}/comments
     * 获取文档评论树（顶层评论 + 子评论）
     */
    @GetMapping("/{docId}/comments")
    @SaCheckLogin
    public Result<List<DocComment>> getComments(@PathVariable Long docId) {
        return Result.ok(docCommentService.getCommentTree(docId));
    }

    /**
     * POST /admin/doc/{docId}/comments
     * 创建评论（支持 anchorPath 锚点、parentId 回复、mentionUserIds @提及）
     */
    @PostMapping("/{docId}/comments")
    @SaCheckLogin
    public Result<DocComment> createComment(@PathVariable Long docId,
                                             @RequestBody CommentCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        String userName = StpUtil.getTokenInfo().getLoginId().toString();
        return Result.ok(docCommentService.createComment(docId, userId, userName, dto));
    }

    /**
     * PUT /admin/doc/comments/{id}/resolve
     * 将评论标记为已解决
     */
    @PutMapping("/comments/{id}/resolve")
    @SaCheckLogin
    public Result<Void> resolveComment(@PathVariable Long id) {
        docCommentService.resolveComment(id);
        return Result.ok();
    }

    /**
     * DELETE /admin/doc/comments/{id}
     * 删除评论
     */
    @DeleteMapping("/comments/{id}")
    @SaCheckLogin
    public Result<Void> deleteComment(@PathVariable Long id) {
        docCommentService.deleteComment(id);
        return Result.ok();
    }

    // ========== 分享 ==========

    /**
     * POST /admin/doc/{docId}/shares
     * 创建分享（USER / DEPT / LINK）
     */
    @PostMapping("/{docId}/shares")
    @SaCheckLogin
    public Result<DocShare> createShare(@PathVariable Long docId,
                                         @RequestBody ShareCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(docShareService.share(docId, userId, dto));
    }

    /**
     * DELETE /admin/doc/shares/{id}
     * 取消分享
     */
    @DeleteMapping("/shares/{id}")
    @SaCheckLogin
    public Result<Void> cancelShare(@PathVariable Long id) {
        docShareService.cancelShare(id);
        return Result.ok();
    }

    /**
     * GET /admin/doc/shares/access?code=xxx
     * 公开端点（无需登录），通过访问码获取分享文档信息
     * 只返回 docId + permission，不返回文档内容，内容通过 WebSocket 鉴权后获取
     */
    @GetMapping("/shares/access")
    public Result<Map<String, Object>> accessByCode(@RequestParam String code) {
        DocShare share = docShareService.accessByCode(code);
        Map<String, Object> data = new HashMap<>();
        data.put("docId", share.getDocId());
        data.put("permission", share.getPermission());
        data.put("expiresAt", share.getExpiresAt());
        return Result.ok(data);
    }
}
