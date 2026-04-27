package com.pengcheng.system.doc.collab.service.impl;

import com.pengcheng.system.doc.collab.entity.DocCollabState;
import com.pengcheng.system.doc.collab.mapper.DocCollabStateMapper;
import com.pengcheng.system.doc.collab.service.DocCollabService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Y.js 文档协作状态服务实现
 * 服务端不解码 CRDT，仅做"读快照 / 写快照"操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocCollabServiceImpl implements DocCollabService {

    private final DocCollabStateMapper collabStateMapper;

    @Override
    public DocCollabState getSnapshot(Long docId) {
        return collabStateMapper.selectByDocId(docId);
    }

    @Override
    @Transactional
    public void persistUpdate(Long docId, byte[] stateVector, byte[] updateBlob, Long lastUpdater) {
        int updated = collabStateMapper.updateBlob(docId, stateVector, updateBlob, lastUpdater);
        if (updated == 0) {
            // 首次写入：INSERT
            DocCollabState state = new DocCollabState();
            state.setDocId(docId);
            state.setStateVector(stateVector);
            state.setUpdateBlob(updateBlob);
            state.setVersion(1L);
            state.setLastUpdater(lastUpdater);
            collabStateMapper.insert(state);
            log.info("[DocCollabService] 首次写入快照 docId={}", docId);
        } else {
            log.debug("[DocCollabService] 更新快照 docId={}", docId);
        }
    }

    @Override
    @Transactional
    public void mergeAndCompact(Long docId) {
        // 当前策略：以最新 update blob 直接作为合并后的全量状态写入
        // 无需引入 Java Yjs 库，合并由前端 Y.js 完成后 push 的 update 即为全量状态
        DocCollabState existing = collabStateMapper.selectByDocId(docId);
        if (existing == null) {
            log.debug("[DocCollabService] mergeAndCompact: docId={} 无快照，跳过", docId);
            return;
        }
        // 递增版本号，标记已 compact
        collabStateMapper.updateBlob(docId,
                existing.getStateVector(),
                existing.getUpdateBlob(),
                existing.getLastUpdater());
        log.info("[DocCollabService] compact 完成 docId={} version={}", docId, existing.getVersion() + 1);
    }
}
