package com.pengcheng.system.doc.collab.dto;

import lombok.Data;

/**
 * 创建评论请求 DTO
 */
@Data
public class CommentCreateDTO {

    /** 评论正文（支持 @ 语法，前端已转义 HTML） */
    private String content;

    /**
     * Y.js RelativePosition 序列化字符串
     * 前端通过 Y.encodeRelativePosition(pos) → base64 传入
     * 用于将评论锚定到文档中特定位置
     */
    private String anchorPath;

    /** 回复目标评论 ID，顶层评论为 null */
    private Long parentId;

    /**
     * 被 @ 的用户 ID 列表（前端解析 @ 语法后传入）
     * 服务端也会从 content 中解析 @{userId} 作为补充
     */
    private java.util.List<Long> mentionUserIds;
}
