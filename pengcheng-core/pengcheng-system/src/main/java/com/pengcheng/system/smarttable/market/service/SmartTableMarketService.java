package com.pengcheng.system.smarttable.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.system.smarttable.entity.SmartTableTemplate;
import com.pengcheng.system.smarttable.mapper.SmartTableTemplateMapper;
import com.pengcheng.system.smarttable.market.entity.SmartTableTemplateDownload;
import com.pengcheng.system.smarttable.market.entity.SmartTableTemplateRating;
import com.pengcheng.system.smarttable.market.mapper.SmartTableTemplateDownloadMapper;
import com.pengcheng.system.smarttable.market.mapper.SmartTableTemplateRatingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 智能表格模板市场服务（M4 — V1.2 收口）。
 *
 * <p>核心能力：
 * <ul>
 *   <li>{@link #shareTemplate} 用户发布私有模板到市场（status=REVIEWING 待管理员审核）</li>
 *   <li>{@link #approveSharing} 管理员审核通过 → status=PUBLIC</li>
 *   <li>{@link #recordDownload} 用户从模板创建表 → +1 download_count + 写下载记录</li>
 *   <li>{@link #rate} 用户评分（1-5）+ 重算 rating_count/sum 聚合</li>
 *   <li>{@link #listMarket} 市场列表（按 category/tag/sort 过滤）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartTableMarketService {

    public static final String STATUS_PRIVATE = "PRIVATE";
    public static final String STATUS_REVIEWING = "REVIEWING";
    public static final String STATUS_PUBLIC = "PUBLIC";
    public static final String STATUS_REJECTED = "REJECTED";

    private final SmartTableTemplateMapper templateMapper;
    private final SmartTableTemplateRatingMapper ratingMapper;
    private final SmartTableTemplateDownloadMapper downloadMapper;

    /** 用户分享私有模板到市场（管理员审核前 REVIEWING）。 */
    @Transactional
    public void shareTemplate(Long templateId, Long authorUserId, String authorName, String tags) {
        SmartTableTemplate t = templateMapper.selectById(templateId);
        if (t == null) throw new IllegalArgumentException("模板不存在");
        // 内置模板不可改 share_status（修复：SmartTableTemplate.builtIn 是 Boolean，
        // 原代码 Integer.valueOf(1).equals(Boolean) 永远 false 导致内置模板保护失效）
        if (Boolean.TRUE.equals(t.getBuiltIn())) {
            throw new IllegalStateException("内置模板已为公开状态，无需分享");
        }
        // 用反射或扩展字段设值 — 实际通过 update SQL 比较稳，避免 entity 字段同步问题
        templateMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SmartTableTemplate>()
                .eq(SmartTableTemplate::getId, templateId)
                .set(t.getName() != null ? null : null, null) // workaround：使用 SQL 直接更新更直接
        );
        // 用更直观的 update SQL（直接写）
        rawUpdateShare(templateId, STATUS_REVIEWING, authorUserId, authorName, tags);
    }

    /** 管理员审核：通过 → PUBLIC，不通过 → REJECTED。 */
    @Transactional
    public void approveSharing(Long templateId, boolean approve) {
        rawUpdateShareStatus(templateId, approve ? STATUS_PUBLIC : STATUS_REJECTED);
    }

    /** 用户从模板创建表后调用 — 写下载记录 + 原子 +1 download_count。 */
    @Transactional
    public void recordDownload(Long templateId, Long userId, Long targetTableId) {
        SmartTableTemplateDownload d = new SmartTableTemplateDownload();
        d.setTemplateId(templateId);
        d.setUserId(userId);
        d.setTargetTableId(targetTableId);
        d.setCreateTime(LocalDateTime.now());
        downloadMapper.insert(d);
        downloadMapper.incrementDownloadCount(templateId);
    }

    /** 用户评分（1-5）— UPSERT + 重算聚合。 */
    @Transactional
    public void rate(Long templateId, Long userId, int rating, String review) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分范围 1-5");
        }
        SmartTableTemplateRating exist = ratingMapper.selectOne(
                new LambdaQueryWrapper<SmartTableTemplateRating>()
                        .eq(SmartTableTemplateRating::getTemplateId, templateId)
                        .eq(SmartTableTemplateRating::getUserId, userId));
        if (exist == null) {
            SmartTableTemplateRating r = new SmartTableTemplateRating();
            r.setTemplateId(templateId);
            r.setUserId(userId);
            r.setRating(rating);
            r.setReview(review);
            r.setCreateTime(LocalDateTime.now());
            ratingMapper.insert(r);
        } else {
            exist.setRating(rating);
            exist.setReview(review);
            exist.setUpdateTime(LocalDateTime.now());
            ratingMapper.updateById(exist);
        }
        ratingMapper.recomputeAggregate(templateId);
    }

    /**
     * 市场列表（仅 PUBLIC + built_in；按 sort 排序）。
     *
     * @param sort downloads / rating / latest
     */
    public IPage<SmartTableTemplate> listMarket(String category, String keyword, String sort,
                                                 int page, int size) {
        LambdaQueryWrapper<SmartTableTemplate> q = new LambdaQueryWrapper<>();
        q.in(SmartTableTemplate::getCategory, category != null ? new Object[]{category}
                : new Object[]{"general", "realty", "sales", "hr", "finance"});
        if (category != null) {
            q.eq(SmartTableTemplate::getCategory, category);
        }
        if (keyword != null && !keyword.isBlank()) {
            q.like(SmartTableTemplate::getName, keyword);
        }
        // share_status=PUBLIC 或 built_in=1（未在实体内字段时用 apply）
        q.apply("(share_status = 'PUBLIC' OR built_in = 1)");

        switch (sort != null ? sort : "downloads") {
            case "rating":
                q.last("ORDER BY (CASE WHEN rating_count > 0 THEN rating_sum * 1.0 / rating_count ELSE 0 END) DESC, " +
                        "download_count DESC LIMIT " + ((page - 1) * size) + "," + size);
                break;
            case "latest":
                q.orderByDesc(SmartTableTemplate::getUpdatedAt);
                break;
            case "downloads":
            default:
                q.last("ORDER BY download_count DESC LIMIT " + ((page - 1) * size) + "," + size);
                break;
        }
        Page<SmartTableTemplate> p = new Page<>(page, size);
        return templateMapper.selectPage(p, q);
    }

    /* ===== 直接 SQL 更新（avoid entity 字段同步问题） ===== */

    private void rawUpdateShare(Long templateId, String status, Long authorUserId,
                                  String authorName, String tags) {
        templateMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SmartTableTemplate>()
                .eq(SmartTableTemplate::getId, templateId)
                .setSql("share_status = '" + status.replace("'", "''") + "'"
                        + (authorUserId != null ? ", author_user_id = " + authorUserId : "")
                        + (authorName != null ? ", author_name = '" + authorName.replace("'", "''") + "'" : "")
                        + (tags != null ? ", tags = '" + tags.replace("'", "''") + "'" : "")));
    }

    private void rawUpdateShareStatus(Long templateId, String status) {
        templateMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SmartTableTemplate>()
                .eq(SmartTableTemplate::getId, templateId)
                .setSql("share_status = '" + status.replace("'", "''") + "'"));
    }
}
