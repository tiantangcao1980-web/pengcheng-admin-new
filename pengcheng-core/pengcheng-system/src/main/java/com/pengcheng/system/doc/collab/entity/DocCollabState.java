package com.pengcheng.system.doc.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Y.js 文档协作状态快照
 * 服务端只保存 binary blob，不解析 CRDT 内容
 */
@Data
@TableName("sys_doc_collab_state")
public class DocCollabState {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对应 sys_doc.id */
    private Long docId;

    /** Y.js encodeStateVector() 结果 */
    private byte[] stateVector;

    /** Y.js encodeStateAsUpdate() 结果（合并后最新状态） */
    private byte[] updateBlob;

    /** 快照版本号，每次持久化 +1 */
    private Long version;

    /** 最后触发持久化的用户 */
    private Long lastUpdater;

    private LocalDateTime updateTime;
}
