package com.pengcheng.finance.contract.sign.esign;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.finance.contract.entity.Contract;
import com.pengcheng.finance.contract.entity.ContractSignRecord;
import com.pengcheng.finance.contract.mapper.ContractMapper;
import com.pengcheng.finance.contract.mapper.ContractSignRecordMapper;
import com.pengcheng.finance.contract.mapper.ContractVersionMapper;
import com.pengcheng.finance.contract.service.impl.ContractServiceImpl;
import com.pengcheng.finance.contract.sign.esign.dto.EsignCallback;
import com.pengcheng.finance.contract.sign.esign.dto.EsignSignFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ContractServiceImpl} e签宝相关逻辑单元测试。
 *
 * <p>全量 Mock 数据库层，纯 POJO 测试，无 Spring 上下文。
 */
@ExtendWith(MockitoExtension.class)
class ContractSignServiceImplTest {

    @Mock
    private ContractMapper contractMapper;
    @Mock
    private ContractVersionMapper contractVersionMapper;
    @Mock
    private ContractSignRecordMapper contractSignRecordMapper;
    @Mock
    private EsignHttpClient esignHttpClient;

    @InjectMocks
    private ContractServiceImpl service;

    private Contract sampleContract;

    @BeforeEach
    void setUp() {
        sampleContract = new Contract();
        sampleContract.setId(100L);
        sampleContract.setTitle("测试合同");
        sampleContract.setExternalSignId("esign-flow-abc");
        sampleContract.setStatus(Contract.STATUS_APPROVED);
    }

    // ===================================================================
    // 用例 1：initiateOnlineSign 成功写入 externalSignId
    // ===================================================================
    @Test
    @DisplayName("initiateOnlineSign：e签宝调用成功后应回写 externalSignId 和 STATUS_SIGNING")
    void testInitiateOnlineSign_writesExternalSignId() {
        // Arrange
        when(contractMapper.selectById(100L)).thenReturn(sampleContract);
        when(esignHttpClient.createSignFlow(any(EsignSignFlowRequest.class))).thenReturn("esign-flow-xyz");

        // 注入 esignHttpClient（@InjectMocks 已注入，但需确保 esignHttpClient 非 null）
        // (Mock 已由 Mockito 注入)

        // Act
        service.initiateOnlineSign(100L, "esign", 1L);

        // Assert：updateById 被调用，且 Contract 携带 externalSignId=esign-flow-xyz
        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).updateById(captor.capture());
        Contract updated = captor.getValue();
        assertThat(updated.getExternalSignId()).isEqualTo("esign-flow-xyz");
        assertThat(updated.getStatus()).isEqualTo(Contract.STATUS_SIGNING);
        assertThat(updated.getSignProvider()).isEqualTo("esign");
    }

    // ===================================================================
    // 用例 2：Feature Flag 关闭时（esignHttpClient=null）抛出 IllegalStateException
    // ===================================================================
    @Test
    @DisplayName("initiateOnlineSign：Feature Flag 关闭（esignHttpClient=null）时抛出 IllegalStateException")
    void testInitiateOnlineSign_featureFlagOff_throwsIllegalState() {
        // 将 esignHttpClient 置为 null，模拟 Feature Flag 关闭
        ReflectionTestUtils.setField(service, "esignHttpClient", null);

        assertThatThrownBy(() -> service.initiateOnlineSign(100L, "esign", 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pengcheng.feature.esign=true");
    }

    // ===================================================================
    // 用例 3：Webhook 幂等 — 相同 eventId 第二次调用不更新数据库
    //         （幂等由 Controller Redis 保证，此处测试 handleSignCallback 本身
    //           在找不到合同时安全退出，不抛异常）
    // ===================================================================
    @Test
    @DisplayName("handleSignCallback：signFlowId 无对应合同时静默返回，不抛异常")
    void testHandleSignCallback_noContractFound_noException() {
        // Arrange：查不到合同
        when(contractMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        EsignCallback callback = new EsignCallback();
        callback.setAction("FlowFinish");
        callback.setSignFlowId("unknown-flow-id");
        callback.setEventId("evt-001");

        // Act & Assert：不抛异常
        assertThatCode(() -> service.handleSignCallback(callback)).doesNotThrowAnyException();

        // 数据库 update 不被调用
        verify(contractMapper, never()).updateById(any());
    }

    // ===================================================================
    // 用例 4：状态机 PROCESSING → SIGNED（FlowFinish 回调）
    // ===================================================================
    @Test
    @DisplayName("handleSignCallback：FlowFinish 时合同状态应更新为 STATUS_SIGNED + SIGN_STATUS_ALL")
    void testHandleSignCallback_flowFinish_contractSigned() {
        // Arrange
        when(contractMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(sampleContract);

        EsignCallback callback = new EsignCallback();
        callback.setAction("FlowFinish");
        callback.setSignFlowId("esign-flow-abc");
        callback.setEventId("evt-002");

        // Act
        service.handleSignCallback(callback);

        // Assert
        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).updateById(captor.capture());
        Contract updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo(Contract.STATUS_SIGNED);
        assertThat(updated.getSignStatus()).isEqualTo(Contract.SIGN_STATUS_ALL);
    }

    // ===================================================================
    // 用例 5：FlowReject 时签署记录标记拒签，合同状态回到 STATUS_APPROVED
    // ===================================================================
    @Test
    @DisplayName("handleSignCallback：FlowReject 时待签 sign_record 标记 RESULT_REFUSED，合同回 STATUS_APPROVED")
    void testHandleSignCallback_flowReject_recordRefused() {
        // Arrange：两条待签记录
        ContractSignRecord rec1 = new ContractSignRecord();
        rec1.setId(1L);
        rec1.setSignResult(ContractSignRecord.RESULT_PENDING);

        ContractSignRecord rec2 = new ContractSignRecord();
        rec2.setId(2L);
        rec2.setSignResult(ContractSignRecord.RESULT_PENDING);

        when(contractMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(sampleContract);
        when(contractSignRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.List.of(rec1, rec2));

        EsignCallback callback = new EsignCallback();
        callback.setAction("FlowReject");
        callback.setSignFlowId("esign-flow-abc");
        callback.setReason("客户拒签");
        callback.setEventId("evt-003");

        // Act
        service.handleSignCallback(callback);

        // Assert：两条记录都被更新为 RESULT_REFUSED
        ArgumentCaptor<ContractSignRecord> recCaptor = ArgumentCaptor.forClass(ContractSignRecord.class);
        verify(contractSignRecordMapper, times(2)).updateById(recCaptor.capture());
        recCaptor.getAllValues().forEach(r ->
                assertThat(r.getSignResult()).isEqualTo(ContractSignRecord.RESULT_REFUSED));

        // 合同状态回到 APPROVED
        ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).updateById(contractCaptor.capture());
        assertThat(contractCaptor.getValue().getStatus()).isEqualTo(Contract.STATUS_APPROVED);
    }
}
