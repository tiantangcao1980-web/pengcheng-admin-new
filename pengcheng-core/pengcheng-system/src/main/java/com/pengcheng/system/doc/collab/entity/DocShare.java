package com.pengcheng.system.doc.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档分享权限实体
 */
@Data
@TableName("sys_doc_share")
public class DocShare {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docId;

    /**
     * 分享目标类型：USER / DEPT / LINK
     */
    private String targetType;

    /**
     * 分享目标 ID（USER 时为用户 ID，DEPT 时为部门 ID，LINK 时为空）
     */
    private Long targetId;

    /**
     * 权限：READ / COMMENT / EDIT
     */
    private String permission;

    /**
     * 链接分享访问码（targetType=LINK 时生成，UUID 截取 32 位）
     */
    private String shareCode;

    /**
     * 链接过期时间（null 表示永不过期）
     */
    private LocalDateTime expiresAt;

    private Long createBy;

    private LocalDateTime createTime;
}
