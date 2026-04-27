package com.pengcheng.system.doc.collab;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.message.entity.Notification;
import com.pengcheng.message.service.NotificationService;
import com.pengcheng.system.doc.collab.dto.CommentCreateDTO;
import com.pengcheng.system.doc.collab.entity.DocComment;
import com.pengcheng.system.doc.collab.mapper.DocCommentMapper;
import com.pengcheng.system.doc.collab.service.impl.DocCommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocCommentServiceImpl")
class DocCommentServiceImplTest {

    @Mock
    private DocCommentMapper commentMapper;

    @Mock
    private NotificationService notificationService;

    private DocCommentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocCommentServiceImpl(commentMapper, notificationService);
    }

    @Test
    @DisplayName("1. createComment：基础创建，mentions 字段正确写入并触发通知")
    void should_create_comment_and_notify_mentions() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("你好 @{2} 请看这里");
        dto.setAnchorPath("eyJjbGllbnRJRCI6..."); // base64 示例
        dto.setParentId(null);
        dto.setMentionUserIds(List.of(3L)); // 前端额外传入

        when(commentMapper.insert(any())).thenReturn(1);

        DocComment result = service.createComment(10L, 1L, "Alice", dto);

        assertThat(result.getDocId()).isEqualTo(10L);
        assertThat(result.getUserId()).isEqualTo(1L);
        // mentions 应包含从 content 解析的 2 和前端传入的 3
        assertThat(result.getMentions()).contains("2").contains("3");
        // 通知：userId=2 和 userId=3（共 2 条，均不是自己 userId=1）
        verify(notificationService, times(2)).createNotification(any(Notification.class));
    }

    @Test
    @DisplayName("2. getCommentTree：评论树形组装（顶层 + 子评论）")
    void should_build_comment_tree() {
        DocComment root = new DocComment();
        root.setId(1L);
        root.setDocId(10L);
        root.setParentId(null);
        root.setContent("顶层评论");

        DocComment child = new DocComment();
        child.setId(2L);
        child.setDocId(10L);
        child.setParentId(1L);
        child.setContent("子评论");

        when(commentMapper.selectByDocId(10L)).thenReturn(List.of(root, child));

        List<DocComment> tree = service.getCommentTree(10L);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getContent()).isEqualTo("子评论");
    }

    @Test
    @DisplayName("3. @ 解析：content 中 @{userId} 格式和前端 mentionUserIds 合并去重")
    void should_merge_mentions_from_content_and_frontend() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("@{5} 和 @{5} 重复测试，还有 @{6}"); // userId 5 出现两次
        dto.setMentionUserIds(List.of(5L, 7L)); // 前端传 5 和 7
        dto.setParentId(null);

        when(commentMapper.insert(any())).thenReturn(1);

        DocComment result = service.createComment(10L, 1L, "Alice", dto);

        // mentions 去重后应包含 5、6、7（不含自己 1）
        String mentions = result.getMentions();
        assertThat(mentions).contains("5").contains("6").contains("7");
        // 不应有重复：按逗号分割后 "5" 只出现一次
        long count5 = List.of(mentions.split(",")).stream().filter("5"::equals).count();
        assertThat(count5).isEqualTo(1);
    }

    @Test
    @DisplayName("4. notifyMentions：被 @ 用户收到通知，@ 自己不产生通知")
    void should_not_notify_self_mention() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("@{1} 自己 @{2} 他人"); // userId=1 是自己
        dto.setMentionUserIds(null);
        dto.setParentId(null);

        when(commentMapper.insert(any())).thenReturn(1);

        service.createComment(10L, 1L, "Alice", dto);

        // 只应通知 userId=2，不通知 userId=1
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, times(1)).createNotification(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("5. 树深度限制：回复的回复（parentId 的 parent 非 null）应抛异常")
    void should_reject_over_depth_reply() {
        // parent 评论自身有 parentId（即二级评论），再回复就超限
        DocComment parent = new DocComment();
        parent.setId(99L);
        parent.setParentId(50L); // 已经是子评论

        when(commentMapper.selectById(99L)).thenReturn(parent);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("三级嵌套回复");
        dto.setParentId(99L);

        assertThatThrownBy(() -> service.createComment(10L, 1L, "Alice", dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("最大深度限制");
    }
}
