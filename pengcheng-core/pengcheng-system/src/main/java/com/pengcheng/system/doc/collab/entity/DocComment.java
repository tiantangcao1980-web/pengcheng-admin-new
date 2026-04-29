package com.pengcheng.system.doc.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档评论实体（含 @ 提及和锚点）
 */
@Data
@TableName("sys_doc_comment")
public class DocComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docId;

    /** 回复目标评论 ID，顶层评论为 null */
    private Long parentId;

    private Long userId;

    private String userName;

    private String content;

    /**
     * Y.js RelativePosition 序列化为 base64 字符串
     * 用于在文档中定位评论锚点
     */
    private String anchorPath;

    /** 逗号分隔的被 @ 用户 ID 列表，如 "1,2,3" */
    private String mentions;

    /** 0=未解决 1=已解决 */
    private Integer resolved;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 子评论列表（非数据库字段，树形组装时填充） */
    @TableField(exist = false)
    private List<DocComment> children;
}
