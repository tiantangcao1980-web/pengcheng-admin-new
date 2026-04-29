package com.pengcheng.system.doc.onlyoffice;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.pengcheng.system.doc.entity.Doc;
import com.pengcheng.system.doc.mapper.DocMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OnlyOffice 业务 Service：拼 docKey、处理保存回调、抓 url 内容覆盖原文件。
 *
 * <p>注意：抓取 OnlyOffice 给出的 url 内容并写入本地存储（OSS）的代码仅做骨架展示，
 * 真实环境应注入 {@code FileStorageService} 替换 TODO 注释。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pengcheng.feature.onlyoffice", havingValue = "true")
public class OnlyOfficeService {

    private final DocMapper docMapper;

    /** 计算 docKey：同一个文档同一个版本 key 必须一样；内容变了 key 必须变化（OnlyOffice 缓存依据）。 */
    public String resolveDocKey(Doc doc) {
        if (doc.getOoDocKey() != null && !doc.getOoDocKey().isBlank()) {
            return doc.getOoDocKey();
        }
        String key = "doc-" + doc.getId() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        doc.setOoDocKey(key);
        docMapper.updateById(doc);
        return key;
    }

    /** 编辑结束生成新 docKey（避免 OnlyOffice Server 缓存旧版本）。 */
    public void rotateDocKey(Long docId) {
        Doc doc = docMapper.selectById(docId);
        if (doc == null) return;
        doc.setOoDocKey("doc-" + doc.getId() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        docMapper.updateById(doc);
    }

    /**
     * 处理 OnlyOffice 服务端的保存回调。
     *
     * @return OnlyOffice 协议要求的响应：成功返回 {"error":0}；失败返回 {"error":N}
     */
    @Transactional
    public int handleCallback(OnlyOfficeCallback cb) {
        Integer status = cb.getStatus();
        if (status == null) return 1;

        log.info("[OnlyOffice] callback key={} status={} url={}", cb.getKey(), status, cb.getUrl());

        // status=1 仅表示编辑中，无操作（OnlyOffice 期望 200 即可）
        if (status == 1 || status == 4) return 0;

        // status=2/3/6/7 → 抓 url 覆盖原文件
        if (cb.getUrl() != null) {
            try {
                Doc doc = findByDocKey(cb.getKey());
                if (doc == null) {
                    log.warn("[OnlyOffice] 找不到 docKey 对应文档: {}", cb.getKey());
                    return 1;
                }
                downloadAndSave(cb.getUrl(), doc);
                doc.setOoLastSave(LocalDateTime.now());
                docMapper.updateById(doc);
            } catch (Exception e) {
                log.error("[OnlyOffice] 保存失败 key={}", cb.getKey(), e);
                return 1;
            }
        }

        // 编辑会话结束 → 轮换 docKey
        if (status == 2 || status == 3) {
            Doc doc = findByDocKey(cb.getKey());
            if (doc != null) rotateDocKey(doc.getId());
        }
        return 0;
    }

    private Doc findByDocKey(String docKey) {
        // 简化版：用 LambdaQueryWrapper 按 oo_doc_key 查
        return docMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Doc>()
                .eq(Doc::getOoDocKey, docKey).last("LIMIT 1"));
    }

    private void downloadAndSave(String url, Doc doc) {
        try (HttpResponse resp = HttpRequest.get(url).timeout(30_000).execute()) {
            if (resp.getStatus() != 200) {
                throw new IllegalStateException("download failed: " + resp.getStatus());
            }
            byte[] content = resp.bodyBytes();
            // TODO 注入 FileStorageService 把 content 写入 OSS 覆盖 doc.fileUrl 对应路径
            log.info("[OnlyOffice] 下载完成 docId={} bytes={}", doc.getId(), content.length);
        }
    }
}
