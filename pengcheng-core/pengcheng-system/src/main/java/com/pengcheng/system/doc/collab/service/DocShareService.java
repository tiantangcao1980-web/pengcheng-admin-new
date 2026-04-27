package com.pengcheng.system.doc.collab.service;

import com.pengcheng.system.doc.collab.dto.ShareCreateDTO;
import com.pengcheng.system.doc.collab.entity.DocShare;

/**
 * 文档分享权限服务接口
 */
public interface DocShareService {

    /**
     * 创建分享（USER / DEPT / LINK 三种类型）
     * LINK 类型自动生成 shareCode（UUID 去横线取 32 位）
     *
     * @param docId     文档 ID
     * @param createBy  操作用户 ID
     * @param dto       分享参数
     * @return 创建后的分享记录（含 shareCode，LINK 类型时有值）
     */
    DocShare share(Long docId, Long createBy, ShareCreateDTO dto);

    /**
     * 通过访问码访问分享文档
     * - 校验 shareCode 是否存在
     * - 校验 expires_at 是否过期
     * - 返回 docId 供前端跳转
     *
     * @param code 访问码
     * @return 分享记录（含 docId / permission）
     * @throws IllegalArgumentException 访问码不存在或已过期
     */
    DocShare accessByCode(String code);

    /**
     * 校验用户对文档的操作权限
     * 判断优先级：文档 owner > USER 分享 > DEPT 分享 > LINK 分享（只读）
     *
     * @param docId   文档 ID
     * @param userId  操作用户 ID
     * @param deptId  用户所属部门 ID（用于 DEPT 分享匹配，可为 null）
     * @param action  所需权限：READ / COMMENT / EDIT
     * @return 是否有权限
     */
    boolean checkPermission(Long docId, Long userId, Long deptId, String action);

    /**
     * 取消分享（删除分享记录）
     *
     * @param shareId 分享记录 ID
     */
    void cancelShare(Long shareId);
}
