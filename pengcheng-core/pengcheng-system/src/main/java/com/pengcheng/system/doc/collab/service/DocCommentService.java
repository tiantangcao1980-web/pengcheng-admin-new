package com.pengcheng.system.doc.collab.service;

import com.pengcheng.system.doc.collab.dto.CommentCreateDTO;
import com.pengcheng.system.doc.collab.entity.DocComment;

import java.util.List;

/**
 * 文档评论服务接口（含 @ 通知）
 */
public interface DocCommentService {

    /**
     * 按 docId 获取评论树（parentId=null 的顶层评论，children 递归填充）
     *
     * @param docId 文档 ID
     * @return 顶层评论列表（已填充子评论）
     */
    List<DocComment> getCommentTree(Long docId);

    /**
     * 创建评论
     * - 解析 dto.mentionUserIds 和 content 中的 @{userId} 写入 mentions 字段
     * - 向被 @ 的用户发送通知
     *
     * @param docId   文档 ID
     * @param userId  评论用户 ID
     * @param userName 评论用户昵称
     * @param dto     评论内容 DTO
     * @return 创建后的评论实体
     */
    DocComment createComment(Long docId, Long userId, String userName, CommentCreateDTO dto);

    /**
     * 将评论标记为已解决
     *
     * @param commentId 评论 ID
     */
    void resolveComment(Long commentId);

    /**
     * 删除评论（软删除：若有子评论则清空 content，否则物理删除）
     *
     * @param commentId 评论 ID
     */
    void deleteComment(Long commentId);
}
