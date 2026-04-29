package com.pengcheng.oa.flow;

import com.pengcheng.oa.flow.entity.ApprovalFlowDef;
import com.pengcheng.oa.flow.entity.ApprovalFlowNode;
import com.pengcheng.oa.flow.mapper.ApprovalFlowDefMapper;
import com.pengcheng.oa.flow.mapper.ApprovalFlowNodeMapper;
import com.pengcheng.oa.flow.service.impl.ApprovalFlowDefServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ApprovalFlowDefServiceImpl")
class ApprovalFlowDefServiceImplTest {

    private ApprovalFlowDefMapper defMapper;
    private ApprovalFlowNodeMapper nodeMapper;
    private ApprovalFlowDefServiceImpl service;
    private final AtomicLong idSeq = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        defMapper = mock(ApprovalFlowDefMapper.class);
        nodeMapper = mock(ApprovalFlowNodeMapper.class);
        doAnswer(inv -> {
            ApprovalFlowDef d = inv.getArgument(0);
            d.setId(idSeq.getAndIncrement());
            return 1;
        }).when(defMapper).insert(any(ApprovalFlowDef.class));
        doAnswer(inv -> {
            ApprovalFlowNode n = inv.getArgument(0);
            n.setId(idSeq.getAndIncrement());
            return 1;
        }).when(nodeMapper).insert(any(ApprovalFlowNode.class));
        when(defMapper.selectList(any())).thenReturn(new ArrayList<>());

        service = new ApprovalFlowDefServiceImpl(defMapper, nodeMapper);
    }

    @Test
    @DisplayName("createDef：补全 nodeOrder，并按顺序插入节点")
    void create_normalizeNodeOrder() {
        ApprovalFlowDef def = new ApprovalFlowDef();
        def.setBizType("leave");
        def.setName("请假流程");

        List<ApprovalFlowNode> nodes = new ArrayList<>();
        ApprovalFlowNode a = new ApprovalFlowNode();
        a.setNodeName("主管");
        ApprovalFlowNode b = new ApprovalFlowNode();
        b.setNodeName("经理");
        nodes.add(a);
        nodes.add(b);

        Long id = service.createDef(def, nodes);
        assertThat(id).isNotNull();

        ArgumentCaptor<ApprovalFlowNode> captor = ArgumentCaptor.forClass(ApprovalFlowNode.class);
        verify(nodeMapper, times(2)).insert(captor.capture());
        List<ApprovalFlowNode> inserted = captor.getAllValues();
        assertThat(inserted.get(0).getNodeOrder()).isEqualTo(1);
        assertThat(inserted.get(1).getNodeOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("createDef：标记默认流程会取消同 bizType 旧默认")
    void create_unsetOtherDefaults() {
        ApprovalFlowDef old = new ApprovalFlowDef();
        old.setId(99L);
        old.setBizType("leave");
        old.setIsDefault(1);
        old.setName("旧流程");
        old.setEnabled(1);
        when(defMapper.selectList(any())).thenReturn(List.of(old));

        ApprovalFlowDef next = new ApprovalFlowDef();
        next.setBizType("leave");
        next.setName("新流程");
        next.setIsDefault(1);

        service.createDef(next, List.of());

        verify(defMapper, atLeastOnce()).updateById(any(ApprovalFlowDef.class));
    }

    @Test
    @DisplayName("校验：bizType 空抛异常")
    void validate_emptyBizType() {
        ApprovalFlowDef def = new ApprovalFlowDef();
        def.setName("x");
        assertThatThrownBy(() -> service.createDef(def, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("校验：name 空抛异常")
    void validate_emptyName() {
        ApprovalFlowDef def = new ApprovalFlowDef();
        def.setBizType("leave");
        assertThatThrownBy(() -> service.createDef(def, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateDef：删旧节点并写入新节点")
    void updateDef_replacesNodes() {
        ApprovalFlowDef def = new ApprovalFlowDef();
        def.setId(7L);
        def.setBizType("leave");
        def.setName("流程");

        ApprovalFlowNode n = new ApprovalFlowNode();
        n.setNodeOrder(1);
        n.setNodeName("主管");
        service.updateDef(def, List.of(n));

        verify(nodeMapper).delete(any());
        verify(nodeMapper, times(1)).insert(any(ApprovalFlowNode.class));
        verify(defMapper).updateById(any(ApprovalFlowDef.class));
    }

    @Test
    @DisplayName("updateDef：缺 ID 抛异常")
    void updateDef_missingId() {
        ApprovalFlowDef def = new ApprovalFlowDef();
        def.setBizType("leave");
        def.setName("x");
        assertThatThrownBy(() -> service.updateDef(def, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deleteDef：节点与定义同时被删")
    void deleteDef_cascadeNodes() {
        service.deleteDef(5L);
        verify(nodeMapper).delete(any());
        verify(defMapper).deleteById(5L);
    }

    @Test
    @DisplayName("listByBizType：未传 bizType 时返回所有")
    void listByBizType_all() {
        when(defMapper.selectList(any())).thenReturn(List.of(new ApprovalFlowDef()));
        assertThat(service.listByBizType(null)).hasSize(1);
        assertThat(service.listAll()).hasSize(1);
    }
}
