package com.pengcheng.hr.performance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pengcheng.common.result.PageResult;
import com.pengcheng.hr.performance.dto.*;
import com.pengcheng.hr.performance.entity.KpiReview360;
import com.pengcheng.hr.performance.mapper.KpiReview360Mapper;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 360 度绩效评估服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KpiReview360Service {

    private final KpiReview360Mapper review360Mapper;
    private final SysUserMapper userMapper;

    /** 评估类型常量 */
    public static final int REVIEW_TYPE_SELF = 1;
    public static final int REVIEW_TYPE_MANAGER = 2;
    public static final int REVIEW_TYPE_PEER = 3;
    public static final int REVIEW_TYPE_SUBORDINATE = 4;

    /**
     * 创建或更新 360 度评估
     */
    @Transactional
    public Long createOrUpdateReview(KpiReview360DTO dto) {
        // 检查是否已存在评估
        LambdaQueryWrapper<KpiReview360> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KpiReview360::getPeriodId, dto.getPeriodId())
                .eq(KpiReview360::getUserId, dto.getUserId())
                .eq(KpiReview360::getReviewerId, getCurrentUserId());

        KpiReview360 existing = review360Mapper.selectOne(wrapper);

        LocalDateTime now = LocalDateTime.now();

        if (existing != null) {
            // 更新现有评估
            existing.setTotalScore(dto.getTotalScore());
            existing.setComment(dto.getComment());
            existing.setStrengths(dto.getStrengths());
            existing.setImprovements(dto.getImprovements());
            existing.setStatus(2); // 已完成
            existing.setUpdateTime(now);
            review360Mapper.updateById(existing);
            log.info("360 度评估已更新：reviewId={}", existing.getId());
            return existing.getId();
        } else {
            // 创建新评估
            KpiReview360 review = new KpiReview360();
            review.setPeriodId(dto.getPeriodId());
            review.setUserId(dto.getUserId());
            review.setReviewerId(getCurrentUserId());
            review.setReviewType(dto.getReviewType());
            review.setTotalScore(dto.getTotalScore());
            review.setComment(dto.getComment());
            review.setStrengths(dto.getStrengths());
            review.setImprovements(dto.getImprovements());
            review.setStatus(2); // 已完成
            review.setCreateTime(now);
            review.setUpdateTime(now);
            review360Mapper.insert(review);
            log.info("360 度评估已创建：reviewId={}", review.getId());
            return review.getId();
        }
    }

    /**
     * 获取用户的待评估列表
     */
    public PageResult<KpiReview360DTO> getPendingReviews(Long userId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<KpiReview360> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KpiReview360::getReviewerId, userId)
                .eq(KpiReview360::getStatus, 1); // 待评估

        IPage<KpiReview360> pageResult = review360Mapper.selectPage(
                new Page<>(page, pageSize), wrapper);

        List<KpiReview360DTO> dtoList = pageResult.getRecords().stream()
                .map(r -> KpiReview360DTO.builder()
                        .periodId(r.getPeriodId())
                        .userId(r.getUserId())
                        .reviewType(r.getReviewType())
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(dtoList, pageResult.getTotal(), (long) page, (long) pageSize);
    }

    /**
     * 获取 360 度评估结果
     */
    public KpiReview360ResultVO getReviewResult(Long periodId, Long userId) {
        // 查询所有评估记录
        LambdaQueryWrapper<KpiReview360> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KpiReview360::getPeriodId, periodId)
                .eq(KpiReview360::getUserId, userId)
                .eq(KpiReview360::getStatus, 2); // 已完成

        List<KpiReview360> reviews = review360Mapper.selectList(wrapper);

        if (reviews.isEmpty()) {
            return null;
        }

        // 按评估类型分组
        Map<Integer, List<KpiReview360>> byType = reviews.stream()
                .collect(Collectors.groupingBy(KpiReview360::getReviewType));

        // 计算各类型平均分
        BigDecimal selfScore = calculateAverage(byType.get(REVIEW_TYPE_SELF));
        BigDecimal managerScore = calculateAverage(byType.get(REVIEW_TYPE_MANAGER));
        BigDecimal peerScore = calculateAverage(byType.get(REVIEW_TYPE_PEER));
        BigDecimal subordinateScore = calculateAverage(byType.get(REVIEW_TYPE_SUBORDINATE));

        // 获取权重配置
        Kpi360WeightConfig config = getWeightConfig();

        // 计算加权平均分
        BigDecimal finalScore = selfScore.multiply(BigDecimal.valueOf(config.getSelfWeight()))
                .add(managerScore.multiply(BigDecimal.valueOf(config.getManagerWeight())))
                .add(peerScore.multiply(BigDecimal.valueOf(config.getPeerWeight())))
                .add(subordinateScore.multiply(BigDecimal.valueOf(config.getSubordinateWeight())))
                .setScale(2, RoundingMode.HALF_UP);

        // 确定等级
        String grade = calculateGrade(finalScore);

        // 构建统计信息
        ReviewCountStats stats = ReviewCountStats.builder()
                .selfCount(getListSize(byType.get(REVIEW_TYPE_SELF)))
                .managerCount(getListSize(byType.get(REVIEW_TYPE_MANAGER)))
                .peerCount(getListSize(byType.get(REVIEW_TYPE_PEER)))
                .subordinateCount(getListSize(byType.get(REVIEW_TYPE_SUBORDINATE)))
                .totalCount(reviews.size())
                .build();

        // 获取被评估人姓名
        SysUser user = userMapper.selectById(userId);
        String userName = user != null ? user.getNickname() : "未知用户";

        // 构建详细评价列表
        List<ReviewDetail> reviewDetails = reviews.stream()
                .map(r -> {
                    SysUser reviewer = userMapper.selectById(r.getReviewerId());
                    String reviewerName = config.getAnonymous() ? "***" :
                            (reviewer != null ? reviewer.getNickname() : "未知");
                    return ReviewDetail.builder()
                            .reviewType(r.getReviewType())
                            .reviewTypeName(getReviewTypeName(r.getReviewType()))
                            .reviewerName(reviewerName)
                            .score(r.getTotalScore())
                            .comment(r.getComment())
                            .strengths(r.getStrengths())
                            .improvements(r.getImprovements())
                            .build();
                })
                .collect(Collectors.toList());

        return KpiReview360ResultVO.builder()
                .userId(userId)
                .userName(userName)
                .selfScore(selfScore)
                .managerScore(managerScore)
                .peerScore(peerScore)
                .subordinateScore(subordinateScore)
                .finalScore(finalScore)
                .grade(grade)
                .stats(stats)
                .reviews(reviewDetails)
                .build();
    }

    /**
     * 批量创建评估任务（为周期内所有用户创建待评估记录）
     */
    @Transactional
    public int createReviewTasks(Long periodId, List<Long> userIds) {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Long userId : userIds) {
            // 为每个用户创建自评任务
            KpiReview360 selfReview = new KpiReview360();
            selfReview.setPeriodId(periodId);
            selfReview.setUserId(userId);
            selfReview.setReviewerId(userId);
            selfReview.setReviewType(REVIEW_TYPE_SELF);
            selfReview.setStatus(1); // 待评估
            selfReview.setCreateTime(now);
            selfReview.setUpdateTime(now);
            review360Mapper.insert(selfReview);
            count++;

            // TODO: 创建上级、同事、下级评估任务（需要评估关系配置）
        }

        log.info("已创建 {} 个 360 度评估任务", count);
        return count;
    }

    /**
     * 获取 360 度评估权重配置
     */
    public Kpi360WeightConfig getWeightConfig() {
        try {
            // 从 sys_config_group 读取配置
            // 这里简化处理，返回默认配置
            return Kpi360WeightConfig.builder()
                    .selfWeight(0.1)
                    .managerWeight(0.4)
                    .peerWeight(0.3)
                    .subordinateWeight(0.2)
                    .minReviewers(3)
                    .anonymous(true)
                    .build();
        } catch (Exception e) {
            log.error("获取 360 度评估权重配置失败", e);
            return Kpi360WeightConfig.builder().build();
        }
    }

    /**
     * 更新 360 度评估权重配置
     */
    public void updateWeightConfig(Kpi360WeightConfig config) {
        // TODO: 保存到 sys_config_group
        log.info("360 度评估权重配置已更新：{}", config);
    }

    // ========== 辅助方法 ==========

    private BigDecimal calculateAverage(List<KpiReview360> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = reviews.stream()
                .map(KpiReview360::getTotalScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);
    }

    private int getListSize(List<KpiReview360> list) {
        return list != null ? list.size() : 0;
    }

    private String getReviewTypeName(Integer type) {
        switch (type) {
            case REVIEW_TYPE_SELF: return "自评";
            case REVIEW_TYPE_MANAGER: return "上级评价";
            case REVIEW_TYPE_PEER: return "同事评价";
            case REVIEW_TYPE_SUBORDINATE: return "下级评价";
            default: return "未知";
        }
    }

    private String calculateGrade(BigDecimal score) {
        if (score == null) return "D";
        if (score.compareTo(BigDecimal.valueOf(90)) >= 0) return "A";
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) return "B";
        if (score.compareTo(BigDecimal.valueOf(70)) >= 0) return "C";
        return "D";
    }

    private Long getCurrentUserId() {
        // TODO: 从安全上下文获取当前用户 ID
        return 1L;
    }
}
