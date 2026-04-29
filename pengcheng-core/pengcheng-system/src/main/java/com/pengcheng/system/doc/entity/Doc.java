package com.pengcheng.system.doc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档实体（支持 Markdown 内容和目录树结构）
 */
@Data
@TableName("sys_doc")
public class Doc {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spaceId;
    private Long parentId;
    private String title;
    private String content;
    /** markdown / folder */
    private String docType;
    private Integer sortOrder;
    private Long creatorId;
    private Long lastEditorId;
    private Integer wordCount;
    private Integer version;
    private Integer pinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;

    /* ===== M1 OnlyOffice 集成（V75）===== */
    /** 文件扩展名（不带点）：docx/xlsx/pptx/txt/md，markdown 文档为 null。 */
    private String fileType;
    /** OnlyOffice 编辑会话 key（同一版本 key 一致；新版本必须新 key）。 */
    private String ooDocKey;
    /** 最近 OnlyOffice 保存时间。 */
    private LocalDateTime ooLastSave;
    /** 最近 OnlyOffice 编辑者 user_id。 */
    private Long ooLastUser;
}
