package com.pengcheng.system.doc.collab.service;

import com.pengcheng.system.doc.collab.entity.DocCollabState;

/**
 * Y.js 文档协作状态服务接口
 */
public interface DocCollabService {

    /**
     * 获取文档最新快照（初始连接时下发给客户端）
     *
     * @param docId 文档 ID
     * @return 快照实体，首次协作时返回 null
     */
    DocCollabState getSnapshot(Long docId);

    /**
     * 持久化最新 Y.js update blob（由定时任务触发）
     *
     * @param docId       文档 ID
     * @param stateVector Y.js stateVector binary
     * @param updateBlob  Y.js update binary（合并后的完整状态）
     * @param lastUpdater 触发持久化的用户 ID
     */
    void persistUpdate(Long docId, byte[] stateVector, byte[] updateBlob, Long lastUpdater);

    /**
     * 合并压缩（周期性调用，避免 update 链过长）
     * 当前实现：直接以最新 update 替换旧快照，版本号递增
     *
     * @param docId 文档 ID
     */
    void mergeAndCompact(Long docId);
}
