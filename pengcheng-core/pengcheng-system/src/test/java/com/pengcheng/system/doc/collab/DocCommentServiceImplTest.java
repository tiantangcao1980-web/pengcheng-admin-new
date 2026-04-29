package com.pengcheng.system.doc.collab;

import com.pengcheng.system.doc.collab.dto.CommentCreateDTO;
import com.pengcheng.system.doc.collab.entity.DocComment;
import com.pengcheng.system.doc.collab.mapper.DocCommentMapper;
import com.pengcheng.system.doc.collab.service.impl.DocCommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DocCommentServiceImpl 单测。
 *
 * <p>NotificationService 在 pengcheng-message 模块（system 不能反向依赖以避免循环），
 * Service 改用 ApplicationContext 反射软依赖。本单测仅校验：
 * <ul>
 *   <li>评论树形组装（核心数据模型）</li>
 *   <li>@ 解析（content + frontend mentions 合并去重）</li>
 *   <li>深度限制（不允许嵌套二级回复）</li>
 *   <li>通知降级（NotificationService 不在 classpath 时静默不抛）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocCommentServiceImpl")
class DocCommentServiceImplTest {

    @Mock
    private DocCommentMapper commentMapper;

    @Mock
    private ApplicationContext applicationContext;

    private DocCommentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocCommentServiceImpl(commentMapper, applicationContext);
    }

    @Test
    @DisplayName("1. createComment：基础创建，mentions 字段正确写入；通知降级不抛")
    void should_create_comment_with_mentions_softly() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("你好 @{2} 请看这里");
        dto.setAnchorPath("eyJjbGllbnRJRCI6...");
        dto.setParentId(null);
        dto.setMentionUserIds(List.of(3L));

        when(commentMapper.insert(any())).thenReturn(1);
        org.mockito.Mockito.lenient().when(applicationContext.getBean(any(Class.class)))
                .thenThrow(new RuntimeException("NotificationService 不在 classpath"));

        DocComment result = service.createComment(10L, 1L, "Alice", dto);

        assertThat(result.getDocId()).isEqualTo(10L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getMentions()).contains("2").contains("3");
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
    @DisplayName("3. @ 解析：content 中 @{userId} 与前端 mentionUserIds 合并去重")
    void should_merge_mentions_from_content_and_frontend() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("@{5} 和 @{5} 重复测试，还有 @{6}");
        dto.setMentionUserIds(List.of(5L, 7L));
        dto.setParentId(null);

        when(commentMapper.insert(any())).thenReturn(1);
        org.mockito.Mockito.lenient().when(applicationContext.getBean(any(Class.class)))
                .thenThrow(new RuntimeException("not loaded"));

        DocComment result = service.createComment(10L, 1L, "Alice", dto);

        String mentions = result.getMentions();
        assertThat(mentions).contains("5").contains("6").contains("7");
        long count5 = List.of(mentions.split(",")).stream().filter("5"::equals).count();
        assertThat(count5).isEqualTo(1);
    }

    @Test
    @DisplayName("4. 深度限制：回复的回复（parentId 的 parent 非 null）应抛异常")
    void should_reject_over_depth_reply() {
        DocComment parent = new DocComment();
        parent.setId(99L);
        parent.setParentId(50L);

        when(commentMapper.selectById(99L)).thenReturn(parent);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setContent("三级嵌套回复");
        dto.setParentId(99L);

        assertThatThrownBy(() -> service.createComment(10L, 1L, "Alice", dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("最大深度限制");
    }
}
