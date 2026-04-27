package com.pengcheng.system.doc.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.message.entity.Notification;
import com.pengcheng.message.service.NotificationService;
import com.pengcheng.system.doc.collab.dto.CommentCreateDTO;
import com.pengcheng.system.doc.collab.entity.DocComment;
import com.pengcheng.system.doc.collab.mapper.DocCommentMapper;
import com.pengcheng.system.doc.collab.service.DocCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文档评论服务实现
 * - 支持树形结构（最多两层：顶层评论 + 回复）
 * - @ 解析：合并前端传入的 mentionUserIds 和 content 中的 @{userId} 格式
 * - @ 通知：通过 NotificationService 向被 @ 用户推送系统通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocCommentServiceImpl implements DocCommentService {

    private final DocCommentMapper commentMapper;
    private final NotificationService notificationService;

    /** 匹配 content 中 @{123} 格式的 userId */
    private static final Pattern AT_PATTERN = Pattern.compile("@\\{(\\d+)}");

    /** 树形深度限制：只允许一层回复（顶层 + 回复，不再嵌套） */
    private static final int MAX_DEPTH = 1;

    @Override
    public List<DocComment> getCommentTree(Long docId) {
        List<DocComment> all = commentMapper.selectByDocId(docId);
        return buildTree(all);
    }

    @Override
    @Transactional
    public DocComment createComment(Long docId, Long userId, String userName, CommentCreateDTO dto) {
        // 深度校验：回复只允许挂在顶层评论下
        if (dto.getParentId() != null) {
            DocComment parent = commentMapper.selectById(dto.getParentId());
            if (parent != null && parent.getParentId() != null) {
                throw new IllegalArgumentException("评论嵌套超过最大深度限制（" + MAX_DEPTH + " 层）");
            }
        }

        // 解析 @ 提及：合并前端传入列表 + content 中的 @{userId}
        Set<Long> mentionIds = extractMentions(dto.getContent(), dto.getMentionUserIds());

        DocComment comment = new DocComment();
        comment.setDocId(docId);
        comment.setParentId(dto.getParentId());
        comment.setUserId(userId);
        comment.setUserName(userName);
        comment.setContent(dto.getContent());
        comment.setAnchorPath(dto.getAnchorPath());
        comment.setMentions(mentionIds.isEmpty() ? null :
                mentionIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        comment.setResolved(0);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());

        commentMapper.insert(comment);

        // 向被 @ 的用户发送通知
        if (!mentionIds.isEmpty()) {
            sendMentionNotifications(mentionIds, userId, userName, docId, comment.getId());
        }

        log.info("[DocComment] 用户 {} 在文档 {} 创建评论 id={}, mentions={}",
                userId, docId, comment.getId(), mentionIds);
        return comment;
    }

    @Override
    @Transactional
    public void resolveComment(Long commentId) {
        DocComment update = new DocComment();
        update.setId(commentId);
        update.setResolved(1);
        update.setUpdateTime(LocalDateTime.now());
        commentMapper.updateById(update);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        // 检查是否有子评论
        long childCount = commentMapper.selectCount(
                new LambdaQueryWrapper<DocComment>().eq(DocComment::getParentId, commentId));
        if (childCount > 0) {
            // 有子评论：清空内容（保留占位）
            DocComment update = new DocComment();
            update.setId(commentId);
            update.setContent("[已删除]");
            update.setUpdateTime(LocalDateTime.now());
            commentMapper.updateById(update);
        } else {
            commentMapper.deleteById(commentId);
        }
    }

    // ========== 私有方法 ==========

    /**
     * 从 content 中解析 @{userId} 并合并前端传入的列表
     */
    private Set<Long> extractMentions(String content, List<Long> frontendMentions) {
        Set<Long> ids = new HashSet<>();
        if (frontendMentions != null) {
            ids.addAll(frontendMentions);
        }
        if (content != null) {
            Matcher m = AT_PATTERN.matcher(content);
            while (m.find()) {
                try {
                    ids.add(Long.parseLong(m.group(1)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ids;
    }

    /**
     * 向被 @ 的用户发送系统通知
     */
    private void sendMentionNotifications(Set<Long> mentionIds, Long fromUserId,
                                          String fromUserName, Long docId, Long commentId) {
        for (Long targetUserId : mentionIds) {
            if (targetUserId.equals(fromUserId)) continue; // 不通知自己
            try {
                Notification n = Notification.builder()
                        .userId(targetUserId)
                        .title("文档评论 @ 提醒")
                        .content(fromUserName + " 在文档中 @ 了你")
                        .type(4) // 文档 @ 通知（新增类型）
                        .bizType("doc_comment")
                        .bizId(commentId)
                        .readStatus(0)
                        .createTime(LocalDateTime.now())
                        .build();
                notificationService.createNotification(n);
            } catch (Exception e) {
                log.warn("[DocComment] 发送 @ 通知失败 targetUserId={}", targetUserId, e);
            }
        }
    }

    /**
     * 将评论列表组装成树形结构（顶层 + 子评论）
     */
    private List<DocComment> buildTree(List<DocComment> all) {
        Map<Long, DocComment> map = all.stream()
                .collect(Collectors.toMap(DocComment::getId, c -> {
                    c.setChildren(new ArrayList<>());
                    return c;
                }));

        List<DocComment> roots = new ArrayList<>();
        for (DocComment c : all) {
            if (c.getParentId() == null) {
                roots.add(c);
            } else {
                DocComment parent = map.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(c);
                }
            }
        }
        return roots;
    }
}
