package com.pengcheng.system.doc.collab.service.impl;

import com.pengcheng.system.doc.collab.dto.ShareCreateDTO;
import com.pengcheng.system.doc.collab.entity.DocShare;
import com.pengcheng.system.doc.collab.mapper.DocShareMapper;
import com.pengcheng.system.doc.collab.service.DocShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文档分享权限服务实现
 *
 * 安全考量：
 * 1. LINK 分享码使用 UUID 生成（32位随机，熵值足够），不可预测
 * 2. expires_at 强制校验，过期链接立即拒绝
 * 3. checkPermission 按优先级判断，避免低权限覆盖高权限
 * 4. 公开端点 GET /shares/access?code=xxx 不需要登录，但只返回 docId + permission，
 *    前端凭此跳转后仍需 WebSocket 握手（token 校验），保证编辑鉴权
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocShareServiceImpl implements DocShareService {

    private final DocShareMapper shareMapper;

    /** 权限优先级：READ < COMMENT < EDIT */
    private static final java.util.List<String> PERMISSION_ORDER =
            java.util.List.of("READ", "COMMENT", "EDIT");

    @Override
    @Transactional
    public DocShare share(Long docId, Long createBy, ShareCreateDTO dto) {
        DocShare share = new DocShare();
        share.setDocId(docId);
        share.setTargetType(dto.getTargetType());
        share.setTargetId(dto.getTargetId());
        share.setPermission(dto.getPermission());
        share.setExpiresAt(dto.getExpiresAt());
        share.setCreateBy(createBy);
        share.setCreateTime(LocalDateTime.now());

        // LINK 类型：生成随机访问码
        if ("LINK".equalsIgnoreCase(dto.getTargetType())) {
            share.setShareCode(UUID.randomUUID().toString().replace("-", ""));
            share.setTargetId(null);
        }

        shareMapper.insert(share);
        log.info("[DocShare] 创建分享 docId={} type={} permission={} shareCode={}",
                docId, dto.getTargetType(), dto.getPermission(), share.getShareCode());
        return share;
    }

    @Override
    public DocShare accessByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("访问码不能为空");
        }
        DocShare share = shareMapper.selectByShareCode(code);
        if (share == null) {
            throw new IllegalArgumentException("无效的分享链接");
        }
        // 校验过期
        if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("分享链接已过期");
        }
        return share;
    }

    @Override
    public boolean checkPermission(Long docId, Long userId, Long deptId, String action) {
        int requiredLevel = permissionLevel(action);

        // 1. USER 类型分享
        DocShare userShare = shareMapper.selectUserShare(docId, userId);
        if (userShare != null && permissionLevel(userShare.getPermission()) >= requiredLevel) {
            return true;
        }

        // 2. DEPT 类型分享
        if (deptId != null) {
            DocShare deptShare = shareMapper.selectDeptShare(docId, deptId);
            if (deptShare != null && permissionLevel(deptShare.getPermission()) >= requiredLevel) {
                return true;
            }
        }

        // 3. LINK 分享（只提供 READ 级别，代码层面不参与 EDIT/COMMENT 鉴权）
        //    公开链接的访问通过 accessByCode 单独处理，这里不做额外判断

        return false;
    }

    @Override
    @Transactional
    public void cancelShare(Long shareId) {
        shareMapper.deleteById(shareId);
        log.info("[DocShare] 取消分享 shareId={}", shareId);
    }

    // ========== 私有方法 ==========

    private int permissionLevel(String permission) {
        int idx = PERMISSION_ORDER.indexOf(permission != null ? permission.toUpperCase() : "READ");
        return idx >= 0 ? idx : 0;
    }
}
