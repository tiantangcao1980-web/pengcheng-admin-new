package com.pengcheng.system.doc.collab.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建文档分享请求 DTO
 */
@Data
public class ShareCreateDTO {

    /** USER / DEPT / LINK */
    private String targetType;

    /** targetType=USER 时为用户 ID，targetType=DEPT 时为部门 ID，LINK 时为 null */
    private Long targetId;

    /** READ / COMMENT / EDIT */
    private String permission;

    /** targetType=LINK 时的链接过期时间，null 表示永不过期 */
    private LocalDateTime expiresAt;
}
