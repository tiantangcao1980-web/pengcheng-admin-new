package com.pengcheng.admin.controller.oa;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.admin.controller.oa.OaApprovalFlowController.CancelInstanceRequest;
import com.pengcheng.admin.controller.oa.OaApprovalFlowController.FlowDefPayload;
import com.pengcheng.common.result.Result;
import com.pengcheng.oa.flow.dto.HandleApprovalDTO;
import com.pengcheng.oa.flow.dto.InstanceDetailVO;
import com.pengcheng.oa.flow.dto.StartInstanceDTO;
import com.pengcheng.oa.flow.entity.ApprovalFlowDef;
import com.pengcheng.oa.flow.entity.ApprovalFlowNode;
import com.pengcheng.oa.flow.entity.ApprovalInstance;
import com.pengcheng.oa.flow.service.ApprovalFlowDefService;
import com.pengcheng.oa.flow.service.ApprovalFlowEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OaApprovalFlowController 单元测试。
 * 使用 MockitoExtension，不启动 Spring 上下文。
 * {@link StpUtil#getLoginIdAsLong()} 通过 mockStatic(StpUtil.class) 隔离。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OaApprovalFlowController 单元测试")
class OaApprovalFlowControllerTest {

    @Mock
    private ApprovalFlowDefService defService;

    @Mock
    private ApprovalFlowEngine flowEngine;

    @InjectMocks
    private OaApprovalFlowController controller;

    // -------- 辅助工厂方法 --------

    private ApprovalFlowDef buildDef(Long id, String bizType) {
        ApprovalFlowDef def = new ApprovalFlowDef();
        def.setId(id);
        def.setBizType(bizType);
        def.setName(bizType + "_流程");
        def.setEnabled(1);
        def.setIsDefault(1);
        return def;
    }

    private ApprovalFlowNode buildNode(Long id, Long defId, int order) {
        ApprovalFlowNode node = new ApprovalFlowNode();
        node.setId(id);
        node.setFlowDefId(defId);
        node.setNodeOrder(order);
        node.setNodeName("审批节点" + order);
        node.setNodeType(ApprovalFlowNode.NODE_TYPE_USER);
        node.setApproverIds("1001");
        return node;
    }

    private ApprovalInstance buildInstance(Long id, Long defId, int state) {
        ApprovalInstance inst = new ApprovalInstance();
        inst.setId(id);
        inst.setFlowDefId(defId);
        inst.setBizType("leave");
        inst.setBizId(200L);
        inst.setApplicantId(500L);
        inst.setState(state);
        return inst;
    }

    // ======== 流程定义 CRUD ========

    @Test
    @DisplayName("listDefs — bizType=null 返回全部流程定义")
    void listDefs_noBizType_returnsAll() {
        List<ApprovalFlowDef> all = Arrays.asList(buildDef(1L, "leave"), buildDef(2L, "overtime"));
        when(defService.listAll()).thenReturn(all);

        Result<List<ApprovalFlowDef>> result = controller.listDefs(null);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(2);
        verify(defService).listAll();
        verify(defService, never()).listByBizType(any());
    }

    @Test
    @DisplayName("listDefs — 传入 bizType 按类型过滤")
    void listDefs_withBizType_filters() {
        List<ApprovalFlowDef> leave = Collections.singletonList(buildDef(1L, "leave"));
        when(defService.listByBizType("leave")).thenReturn(leave);

        Result<List<ApprovalFlowDef>> result = controller.listDefs("leave");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().get(0).getBizType()).isEqualTo("leave");
    }

    @Test
    @DisplayName("getDef — 返回流程定义及其节点列表")
    void getDef_returnsDefWithNodes() {
        ApprovalFlowDef def = buildDef(10L, "leave");
        List<ApprovalFlowNode> nodes = Arrays.asList(buildNode(1L, 10L, 1), buildNode(2L, 10L, 2));
        when(defService.getDef(10L)).thenReturn(def);
        when(defService.listNodes(10L)).thenReturn(nodes);

        Result<FlowDefPayload> result = controller.getDef(10L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getDef().getId()).isEqualTo(10L);
        assertThat(result.getData().getNodes()).hasSize(2);
    }

    @Test
    @DisplayName("createDef — 正常创建返回新 ID")
    void createDef_success_returnsNewId() {
        ApprovalFlowDef def = buildDef(null, "reimburse");
        List<ApprovalFlowNode> nodes = Collections.singletonList(buildNode(null, null, 1));
        FlowDefPayload payload = new FlowDefPayload(def, nodes);
        when(defService.createDef(def, nodes)).thenReturn(55L);

        Result<Long> result = controller.createDef(payload);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(55L);
    }

    @Test
    @DisplayName("updateDef — 更新成功返回 200，def.id 被注入")
    void updateDef_success_returnsOk() {
        ApprovalFlowDef def = buildDef(null, "leave");
        FlowDefPayload payload = new FlowDefPayload(def, Collections.emptyList());
        doNothing().when(defService).updateDef(any(), any());

        Result<Void> result = controller.updateDef(7L, payload);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(def.getId()).isEqualTo(7L);
        verify(defService).updateDef(def, Collections.emptyList());
    }

    @Test
    @DisplayName("deleteDef — 删除成功返回 200")
    void deleteDef_success_returnsOk() {
        doNothing().when(defService).deleteDef(9L);

        Result<Void> result = controller.deleteDef(9L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(defService).deleteDef(9L);
    }

    // ======== 流程实例 ========

    @Test
    @DisplayName("startInstance — dto 含 applicantId 直接使用，返回实例 ID")
    void startInstance_withApplicantId_returnsInstanceId() {
        StartInstanceDTO dto = StartInstanceDTO.builder()
                .flowDefId(1L).bizType("leave").bizId(100L)
                .applicantId(500L).summary("请假申请").build();
        when(flowEngine.start(dto)).thenReturn(1000L);

        Result<Long> result = controller.startInstance(dto);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("startInstance — applicantId=null 时从 StpUtil 获取")
    void startInstance_nullApplicantId_fillsFromStpUtil() {
        StartInstanceDTO dto = StartInstanceDTO.builder()
                .flowDefId(1L).bizType("leave").bizId(100L)
                .applicantId(null).build();

        try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
            mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(600L);
            when(flowEngine.start(dto)).thenReturn(1001L);

            Result<Long> result = controller.startInstance(dto);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(dto.getApplicantId()).isEqualTo(600L);
        }
    }

    @Test
    @DisplayName("listPending — approverId=null 时使用 StpUtil 查当前用户待办")
    void listPending_nullApproverId_usesStpUtil() {
        List<ApprovalInstance> pending = Collections.singletonList(
                buildInstance(10L, 1L, ApprovalInstance.STATE_RUNNING));

        try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
            mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(1001L);
            when(flowEngine.listPending(1001L)).thenReturn(pending);

            Result<List<ApprovalInstance>> result = controller.listPending(null);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData()).hasSize(1);
        }
    }

    @Test
    @DisplayName("getInstance — 返回实例详情（含审批记录）")
    void getInstance_returnsDetail() {
        ApprovalInstance inst = buildInstance(20L, 1L, ApprovalInstance.STATE_RUNNING);
        InstanceDetailVO detail = InstanceDetailVO.builder()
                .instance(inst).records(Collections.emptyList()).build();
        when(flowEngine.detail(20L)).thenReturn(detail);

        Result<InstanceDetailVO> result = controller.getInstance(20L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getInstance().getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("handle — approverId 已填，处理成功返回 200")
    void handle_withApproverId_returnsOk() {
        HandleApprovalDTO dto = HandleApprovalDTO.builder()
                .instanceId(10L).approverId(1001L).approved(true).remark("同意").build();
        doNothing().when(flowEngine).handle(dto);

        Result<Void> result = controller.handle(dto);

        assertThat(result.getCode()).isEqualTo(200);
        verify(flowEngine).handle(dto);
    }

    @Test
    @DisplayName("cancel — instanceId 不存在时 Service 抛异常，Controller 冒泡")
    void cancel_instanceNotFound_propagatesException() {
        CancelInstanceRequest req = new CancelInstanceRequest();
        req.setInstanceId(999L);
        req.setApplicantId(500L);

        doThrow(new IllegalArgumentException("审批实例不存在：999"))
                .when(flowEngine).cancel(999L, 500L);

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.cancel(req)
        );
        verify(flowEngine).cancel(999L, 500L);
    }

    @Test
    @DisplayName("cancel — applicantId=null 时从 StpUtil 获取，撤销成功返回 200")
    void cancel_nullApplicantId_usesStpUtil() {
        CancelInstanceRequest req = new CancelInstanceRequest();
        req.setInstanceId(30L);
        req.setApplicantId(null);

        try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
            mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(700L);
            doNothing().when(flowEngine).cancel(30L, 700L);

            Result<Void> result = controller.cancel(req);

            assertThat(result.getCode()).isEqualTo(200);
            verify(flowEngine).cancel(30L, 700L);
        }
    }
}
