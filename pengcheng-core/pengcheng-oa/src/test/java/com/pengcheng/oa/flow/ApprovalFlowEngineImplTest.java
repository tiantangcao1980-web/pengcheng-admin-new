package com.pengcheng.oa.flow;

import com.pengcheng.oa.flow.dto.HandleApprovalDTO;
import com.pengcheng.oa.flow.dto.InstanceDetailVO;
import com.pengcheng.oa.flow.dto.StartInstanceDTO;
import com.pengcheng.oa.flow.entity.ApprovalFlowDef;
import com.pengcheng.oa.flow.entity.ApprovalFlowNode;
import com.pengcheng.oa.flow.entity.ApprovalInstance;
import com.pengcheng.oa.flow.entity.ApprovalRecord;
import com.pengcheng.oa.flow.mapper.ApprovalFlowDefMapper;
import com.pengcheng.oa.flow.mapper.ApprovalFlowNodeMapper;
import com.pengcheng.oa.flow.mapper.ApprovalInstanceMapper;
import com.pengcheng.oa.flow.mapper.ApprovalRecordMapper;
import com.pengcheng.oa.flow.service.ApprovalFlowCallback;
import com.pengcheng.oa.flow.service.impl.ApprovalFlowEngineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ApprovalFlowEngineImpl - 轻量串行流程引擎")
class ApprovalFlowEngineImplTest {

    private ApprovalFlowDefMapper defMapper;
    private ApprovalFlowNodeMapper nodeMapper;
    private ApprovalInstanceMapper instanceMapper;
    private ApprovalRecordMapper recordMapper;
    private ApprovalFlowEngineImpl engine;
    private RecordingCallback callback;

    private final Map<Long, ApprovalFlowDef> defStore = new HashMap<>();
    private final Map<Long, ApprovalFlowNode> nodeStore = new HashMap<>();
    private final Map<Long, ApprovalInstance> instanceStore = new HashMap<>();
    private final List<ApprovalRecord> recordStore = new ArrayList<>();

    private final AtomicLong defIdSeq = new AtomicLong(1);
    private final AtomicLong nodeIdSeq = new AtomicLong(1);
    private final AtomicLong instIdSeq = new AtomicLong(1);
    private final AtomicLong recIdSeq = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        defStore.clear();
        nodeStore.clear();
        instanceStore.clear();
        recordStore.clear();
        defMapper = mock(ApprovalFlowDefMapper.class);
        nodeMapper = mock(ApprovalFlowNodeMapper.class);
        instanceMapper = mock(ApprovalInstanceMapper.class);
        recordMapper = mock(ApprovalRecordMapper.class);

        // ===== def =====
        when(defMapper.selectById(any())).thenAnswer(inv -> defStore.get((Long) inv.getArgument(0)));
        when(defMapper.selectOne(any())).thenAnswer(inv ->
                defStore.values().stream()
                        .filter(d -> Integer.valueOf(1).equals(d.getEnabled()))
                        .filter(d -> Integer.valueOf(1).equals(d.getIsDefault()))
                        .findFirst().orElse(null));

        // ===== node =====
        when(nodeMapper.selectById(any())).thenAnswer(inv -> nodeStore.get((Long) inv.getArgument(0)));
        when(nodeMapper.selectList(any())).thenAnswer(inv -> {
            // 因测试构造下我们知道 wrapper 是按 flowDefId 过滤
            // 取第一个 def 的所有 node 排序返回
            if (defStore.isEmpty()) return new ArrayList<>();
            Long defId = defStore.keySet().iterator().next();
            return nodeStore.values().stream()
                    .filter(n -> defId.equals(n.getFlowDefId()))
                    .sorted((a, b) -> Integer.compare(a.getNodeOrder(), b.getNodeOrder()))
                    .toList();
        });

        // ===== instance =====
        doAnswer(inv -> {
            ApprovalInstance i = inv.getArgument(0);
            i.setId(instIdSeq.getAndIncrement());
            i.setCreateTime(LocalDateTime.now());
            instanceStore.put(i.getId(), copyOf(i));
            return 1;
        }).when(instanceMapper).insert(any(ApprovalInstance.class));
        doAnswer(inv -> {
            ApprovalInstance i = inv.getArgument(0);
            instanceStore.put(i.getId(), copyOf(i));
            return 1;
        }).when(instanceMapper).updateById(any(ApprovalInstance.class));
        when(instanceMapper.selectById(any())).thenAnswer(inv -> {
            ApprovalInstance stored = instanceStore.get((Long) inv.getArgument(0));
            return stored == null ? null : copyOf(stored);
        });
        when(instanceMapper.selectList(any())).thenAnswer(inv -> {
            // 模拟 wrapper 语义：只返回 state=RUNNING 且 currentNodeDeadline<=now 的实例。
            // listPending 的 wrapper 仅过滤 state；sweepTimeouts 的 wrapper 还会过滤 deadline。
            // 这里取所有 state=RUNNING 的实例（额外的 deadline 过滤交给引擎来做也没关系，
            // 因为 sweepTimeouts 内部循环会再次访问 deadline；为简化调用方覆盖范围，我们也支持只按 state 过滤）。
            return instanceStore.values().stream()
                    .filter(i -> Integer.valueOf(ApprovalInstance.STATE_RUNNING).equals(i.getState()))
                    .map(ApprovalFlowEngineImplTest::copyOf)
                    .toList();
        });

        // ===== record =====
        doAnswer(inv -> {
            ApprovalRecord r = inv.getArgument(0);
            r.setId(recIdSeq.getAndIncrement());
            r.setCreateTime(LocalDateTime.now());
            recordStore.add(r);
            return 1;
        }).when(recordMapper).insert(any(ApprovalRecord.class));
        when(recordMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(recordStore));

        callback = new RecordingCallback("correction");
        engine = new ApprovalFlowEngineImpl(defMapper, nodeMapper, instanceMapper, recordMapper, List.of(callback));
    }

    // ===== 单级通过 =====
    @Test
    @DisplayName("单级流程：审批人通过 → 终态 APPROVED 并触发 callback")
    void singleNode_pass() {
        ApprovalFlowDef def = createDef("correction", true);
        ApprovalFlowNode n1 = createNode(def.getId(), 1, "主管审批", "100");

        Long instId = engine.start(buildStart(def.getId(), 999L));
        engine.handle(HandleApprovalDTO.builder()
                .instanceId(instId).approverId(100L).approved(true).build());

        ApprovalInstance inst = instanceStore.get(instId);
        assertThat(inst.getState()).isEqualTo(ApprovalInstance.STATE_APPROVED);
        assertThat(inst.getEndTime()).isNotNull();
        assertThat(callback.completedBizIds).hasSize(1);
        assertThat(callback.lastApproved).isTrue();
        assertThat(recordStore).hasSize(1);
        assertThat(recordStore.get(0).getResult()).isEqualTo(ApprovalRecord.RESULT_APPROVED);
    }

    // ===== 多级串行 =====
    @Test
    @DisplayName("多级串行：3 级审批，每级通过后推进到下一级，最后触发 callback")
    void multiNode_serialPass() {
        ApprovalFlowDef def = createDef("correction", true);
        createNode(def.getId(), 1, "主管", "100");
        createNode(def.getId(), 2, "经理", "200");
        createNode(def.getId(), 3, "总监", "300");

        Long instId = engine.start(buildStart(def.getId(), 999L));
        // L1
        engine.handle(HandleApprovalDTO.builder().instanceId(instId).approverId(100L).approved(true).build());
        ApprovalInstance afterL1 = instanceStore.get(instId);
        assertThat(afterL1.getState()).isEqualTo(ApprovalInstance.STATE_RUNNING);
        assertThat(afterL1.getCurrentNodeOrder()).isEqualTo(2);

        // L2
        engine.handle(HandleApprovalDTO.builder().instanceId(instId).approverId(200L).approved(true).build());
        ApprovalInstance afterL2 = instanceStore.get(instId);
        assertThat(afterL2.getCurrentNodeOrder()).isEqualTo(3);

        // L3
        engine.handle(HandleApprovalDTO.builder().instanceId(instId).approverId(300L).approved(true).build());
        ApprovalInstance afterL3 = instanceStore.get(instId);
        assertThat(afterL3.getState()).isEqualTo(ApprovalInstance.STATE_APPROVED);
        assertThat(callback.completedBizIds).containsExactly(999L);
    }

    // ===== 多级中间驳回 =====
    @Test
    @DisplayName("多级串行：第二级驳回 → 立即终态 REJECTED，callback approved=false")
    void multiNode_rejectAtMiddle() {
        ApprovalFlowDef def = createDef("correction", true);
        createNode(def.getId(), 1, "主管", "100");
        createNode(def.getId(), 2, "经理", "200");

        Long instId = engine.start(buildStart(def.getId(), 12345L));
        engine.handle(HandleApprovalDTO.builder().instanceId(instId).approverId(100L).approved(true).build());
        engine.handle(HandleApprovalDTO.builder().instanceId(instId).approverId(200L).approved(false).remark("不同意").build());

        ApprovalInstance inst = instanceStore.get(instId);
        assertThat(inst.getState()).isEqualTo(ApprovalInstance.STATE_REJECTED);
        assertThat(callback.lastApproved).isFalse();
        assertThat(recordStore).hasSize(2);
    }

    // ===== 取消 =====
    @Test
    @DisplayName("申请人撤销：流程进入 CANCELLED")
    void cancel_byApplicant() {
        ApprovalFlowDef def = createDef("correction", true);
        createNode(def.getId(), 1, "主管", "100");

        Long instId = engine.start(buildStart(def.getId(), 1L, 50L));
        engine.cancel(instId, 50L);

        assertThat(instanceStore.get(instId).getState()).isEqualTo(ApprovalInstance.STATE_CANCELLED);
    }

    @Test
    @DisplayName("非申请人撤销抛异常")
    void cancel_byNonApplicant() {
        ApprovalFlowDef def = createDef("correction", true);
        createNode(def.getId(), 1, "主管", "100");

        Long instId = engine.start(buildStart(def.getId(), 1L, 50L));

        assertThatThrownBy(() -> engine.cancel(instId, 999L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ===== 超时自动通过 =====
    @Test
    @DisplayName("超时扫描：节点已过期且 timeoutAction=1 → 自动推进到下一节点")
    void sweepTimeouts_autoPass() {
        ApprovalFlowDef def = createDef("correction", true);
        ApprovalFlowNode n1 = createNode(def.getId(), 1, "主管", "100");
        n1.setTimeoutHours(2);
        n1.setTimeoutAction(1);
        ApprovalFlowNode n2 = createNode(def.getId(), 2, "经理", "200");

        Long instId = engine.start(buildStart(def.getId(), 1L));
        // 强制把 deadline 调到过去
        ApprovalInstance inst = instanceStore.get(instId);
        inst.setCurrentNodeDeadline(LocalDateTime.now().minusHours(1));
        instanceStore.put(instId, inst);

        int affected = engine.sweepTimeouts();
        assertThat(affected).isEqualTo(1);

        ApprovalInstance after = instanceStore.get(instId);
        assertThat(after.getState()).isEqualTo(ApprovalInstance.STATE_RUNNING);
        assertThat(after.getCurrentNodeOrder()).isEqualTo(2);
        assertThat(recordStore).hasSize(1);
        assertThat(recordStore.get(0).getResult()).isEqualTo(ApprovalRecord.RESULT_TIMEOUT_PASS);
    }

    @Test
    @DisplayName("超时扫描：timeoutAction=2 → 自动驳回")
    void sweepTimeouts_autoReject() {
        ApprovalFlowDef def = createDef("correction", true);
        ApprovalFlowNode n1 = createNode(def.getId(), 1, "主管", "100");
        n1.setTimeoutHours(1);
        n1.setTimeoutAction(2);

        Long instId = engine.start(buildStart(def.getId(), 1L));
        ApprovalInstance inst = instanceStore.get(instId);
        inst.setCurrentNodeDeadline(LocalDateTime.now().minusMinutes(1));
        instanceStore.put(instId, inst);

        engine.sweepTimeouts();
        assertThat(instanceStore.get(instId).getState()).isEqualTo(ApprovalInstance.STATE_REJECTED);
        assertThat(callback.lastApproved).isFalse();
    }

    @Test
    @DisplayName("超时扫描：timeoutAction=3 跳过 → 进入下一节点")
    void sweepTimeouts_skip() {
        ApprovalFlowDef def = createDef("correction", true);
        ApprovalFlowNode n1 = createNode(def.getId(), 1, "主管", "100");
        n1.setTimeoutHours(1);
        n1.setTimeoutAction(3);
        createNode(def.getId(), 2, "经理", "200");

        Long instId = engine.start(buildStart(def.getId(), 1L));
        ApprovalInstance inst = instanceStore.get(instId);
        inst.setCurrentNodeDeadline(LocalDateTime.now().minusMinutes(5));
        instanceStore.put(instId, inst);

        engine.sweepTimeouts();
        ApprovalInstance after = instanceStore.get(instId);
        assertThat(after.getState()).isEqualTo(ApprovalInstance.STATE_RUNNING);
        assertThat(after.getCurrentNodeOrder()).isEqualTo(2);
        assertThat(recordStore.get(0).getResult()).isEqualTo(ApprovalRecord.RESULT_TIMEOUT_SKIP);
    }

    @Test
    @DisplayName("超时扫描：未过期实例不受影响")
    void sweepTimeouts_noop() {
        ApprovalFlowDef def = createDef("correction", true);
        ApprovalFlowNode n1 = createNode(def.getId(), 1, "主管", "100");
        n1.setTimeoutHours(48);
        n1.setTimeoutAction(1);

        engine.start(buildStart(def.getId(), 1L));

        int affected = engine.sweepTimeouts();
        assertThat(affected).isEqualTo(0);
    }

    // ===== 空流程 =====
    @Test
    @DisplayName("启动空流程（无节点）→ 直接终态通过")
    void startEmptyFlow_directApprove() {
        ApprovalFlowDef def = createDef("correction", true);

        Long instId = engine.start(buildStart(def.getId(), 1L));

        assertThat(instanceStore.get(instId).getState()).isEqualTo(ApprovalInstance.STATE_APPROVED);
        assertThat(callback.completedBizIds).hasSize(1);
    }

    // ===== 待办列表 =====
    @Test
    @DisplayName("listPending：返回当前节点 approver 命中的实例")
    void listPending_filter() {
        ApprovalFlowDef def = createDef("correction", true);
        createNode(def.getId(), 1, "主管", "100,200");

        engine.start(buildStart(def.getId(), 1L));

        List<ApprovalInstance> pending = engine.listPending(100L);
        assertThat(pending).hasSize(1);

        List<ApprovalInstance> none = engine.listPending(999L);
        assertThat(none).isEmpty();
    }

    @Test
    @DisplayName("无审批权限的处理人抛异常")
    void handle_byNonApprover_throws() {
        ApprovalFlowDef def = createDef("correction", true);
        createNode(def.getId(), 1, "主管", "100");

        Long instId = engine.start(buildStart(def.getId(), 1L));
        assertThatThrownBy(() -> engine.handle(HandleApprovalDTO.builder()
                .instanceId(instId).approverId(999L).approved(true).build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("处理已终结实例抛异常")
    void handle_finished_throws() {
        ApprovalFlowDef def = createDef("correction", true);
        createNode(def.getId(), 1, "主管", "100");

        Long instId = engine.start(buildStart(def.getId(), 1L));
        engine.handle(HandleApprovalDTO.builder().instanceId(instId).approverId(100L).approved(true).build());

        assertThatThrownBy(() -> engine.handle(HandleApprovalDTO.builder()
                .instanceId(instId).approverId(100L).approved(true).build()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ===== 详情 =====
    @Test
    @DisplayName("detail：返回实例与全部记录")
    void detail_returnsRecords() {
        ApprovalFlowDef def = createDef("correction", true);
        createNode(def.getId(), 1, "主管", "100");
        createNode(def.getId(), 2, "经理", "200");

        Long instId = engine.start(buildStart(def.getId(), 1L));
        engine.handle(HandleApprovalDTO.builder().instanceId(instId).approverId(100L).approved(true).build());
        engine.handle(HandleApprovalDTO.builder().instanceId(instId).approverId(200L).approved(true).build());

        InstanceDetailVO vo = engine.detail(instId);
        assertThat(vo.getInstance()).isNotNull();
        assertThat(vo.getRecords()).hasSize(2);
    }

    // ===== 参数校验 =====
    @Test
    @DisplayName("start 参数校验：bizType 空")
    void start_invalidBizType() {
        StartInstanceDTO bad = StartInstanceDTO.builder().bizId(1L).applicantId(1L).build();
        assertThatThrownBy(() -> engine.start(bad)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("start 找不到默认流程抛异常")
    void start_noDefaultDef() {
        StartInstanceDTO dto = StartInstanceDTO.builder()
                .bizType("nonexistent").bizId(1L).applicantId(1L).build();
        assertThatThrownBy(() -> engine.start(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    // ===== helpers =====

    private ApprovalFlowDef createDef(String bizType, boolean isDefault) {
        ApprovalFlowDef def = new ApprovalFlowDef();
        def.setId(defIdSeq.getAndIncrement());
        def.setBizType(bizType);
        def.setName("default-" + bizType);
        def.setEnabled(1);
        def.setIsDefault(isDefault ? 1 : 0);
        defStore.put(def.getId(), def);
        return def;
    }

    private ApprovalFlowNode createNode(Long defId, int order, String name, String approverIds) {
        ApprovalFlowNode n = new ApprovalFlowNode();
        n.setId(nodeIdSeq.getAndIncrement());
        n.setFlowDefId(defId);
        n.setNodeOrder(order);
        n.setNodeName(name);
        n.setNodeType(ApprovalFlowNode.NODE_TYPE_USER);
        n.setApproverIds(approverIds);
        nodeStore.put(n.getId(), n);
        return n;
    }

    private static StartInstanceDTO buildStart(Long defId, Long bizId) {
        return buildStart(defId, bizId, 1L);
    }

    private static StartInstanceDTO buildStart(Long defId, Long bizId, Long applicantId) {
        return StartInstanceDTO.builder()
                .flowDefId(defId)
                .bizType("correction")
                .bizId(bizId)
                .applicantId(applicantId)
                .summary("test")
                .build();
    }

    private static ApprovalInstance copyOf(ApprovalInstance src) {
        ApprovalInstance c = new ApprovalInstance();
        c.setId(src.getId());
        c.setFlowDefId(src.getFlowDefId());
        c.setBizType(src.getBizType());
        c.setBizId(src.getBizId());
        c.setApplicantId(src.getApplicantId());
        c.setCurrentNodeOrder(src.getCurrentNodeOrder());
        c.setCurrentNodeId(src.getCurrentNodeId());
        c.setState(src.getState());
        c.setSummary(src.getSummary());
        c.setCurrentNodeDeadline(src.getCurrentNodeDeadline());
        c.setEndTime(src.getEndTime());
        c.setCreateTime(src.getCreateTime());
        return c;
    }

    /** 用于校验 callback 触发的 spy。 */
    static class RecordingCallback implements ApprovalFlowCallback {
        final String supportType;
        final List<Long> completedBizIds = new ArrayList<>();
        Boolean lastApproved;

        RecordingCallback(String supportType) {
            this.supportType = supportType;
        }

        @Override
        public String supportBizType() {
            return supportType;
        }

        @Override
        public void onComplete(Long bizId, boolean approved) {
            completedBizIds.add(bizId);
            lastApproved = approved;
        }
    }
}
