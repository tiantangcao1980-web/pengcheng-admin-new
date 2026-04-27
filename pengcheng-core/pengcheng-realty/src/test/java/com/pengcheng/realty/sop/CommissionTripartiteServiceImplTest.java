package com.pengcheng.realty.sop;

import com.pengcheng.finance.contract.sign.esign.EsignHttpClient;
import com.pengcheng.finance.contract.sign.esign.dto.EsignSignFlowRequest;
import com.pengcheng.realty.sop.dto.CommissionInitiateDTO;
import com.pengcheng.realty.sop.entity.RealtyCommissionTripartite;
import com.pengcheng.realty.sop.entity.RealtySopTemplate;
import com.pengcheng.realty.sop.mapper.RealtyCommissionTripartiteMapper;
import com.pengcheng.realty.sop.mapper.RealtySopTemplateMapper;
import com.pengcheng.realty.sop.pdf.PdfGenerator;
import com.pengcheng.realty.sop.service.CommissionTripartiteServiceImpl;
import com.pengcheng.realty.sop.template.SopTemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * CommissionTripartiteServiceImpl 单元测试（4 用例）
 */
@ExtendWith(MockitoExtension.class)
class CommissionTripartiteServiceImplTest {

    @Mock
    private RealtyCommissionTripartiteMapper tripartiteMapper;
    @Mock
    private RealtySopTemplateMapper templateMapper;
    @Mock
    private SopTemplateRenderer renderer;
    @Mock
    private PdfGenerator pdfGenerator;
    @Mock
    private EsignHttpClient esignHttpClient;

    private CommissionTripartiteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommissionTripartiteServiceImpl(tripartiteMapper, templateMapper, renderer, pdfGenerator);
        ReflectionTestUtils.setField(service, "esignHttpClient", esignHttpClient);
    }

    // ===== 用例 1：成交触发 initiate 完整流程 =====

    @Test
    @DisplayName("用例1: initiate - 新 dealId 完整流程：计算佣金、渲染模板、e签宝三方签署流")
    void testInitiateNewDeal() {
        CommissionInitiateDTO dto = buildDto();

        given(tripartiteMapper.selectByDealId(dto.getDealId())).willReturn(null);
        RealtySopTemplate template = buildTemplate();
        given(templateMapper.selectByCode("commission_tripartite")).willReturn(template);
        given(renderer.render(anyString(), anyMap())).willReturn("<h2>佣金三方协议</h2>");
        given(pdfGenerator.generate(anyString(), anyString())).willReturn(new byte[0]);
        given(pdfGenerator.getFileExtension()).willReturn(".html");
        given(esignHttpClient.createSignFlow(any(EsignSignFlowRequest.class))).willReturn("FLOW-COM-001");
        doAnswer(inv -> {
            RealtyCommissionTripartite t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", 300L);
            return 1;
        }).when(tripartiteMapper).insert(any(RealtyCommissionTripartite.class));

        Long id = service.initiate(dto);

        assertThat(id).isEqualTo(300L);

        // 验证 e签宝签署流包含三方签署人
        ArgumentCaptor<EsignSignFlowRequest> flowCaptor = ArgumentCaptor.forClass(EsignSignFlowRequest.class);
        verify(esignHttpClient).createSignFlow(flowCaptor.capture());
        EsignSignFlowRequest flow = flowCaptor.getValue();
        assertThat(flow.getSigners()).hasSize(3);
        assertThat(flow.getSigners().get(0).getSignerRole()).isEqualTo("partyA");
        assertThat(flow.getSigners().get(1).getSignerRole()).isEqualTo("partyB");
        assertThat(flow.getSigners().get(2).getSignerRole()).isEqualTo("partyC");

        // 验证 signStatus 变为 SIGNING
        ArgumentCaptor<RealtyCommissionTripartite> updateCaptor = ArgumentCaptor.forClass(RealtyCommissionTripartite.class);
        verify(tripartiteMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getSignStatus()).isEqualTo("SIGNING");
        assertThat(updateCaptor.getValue().getSignFlowId()).isEqualTo("FLOW-COM-001");

        // 验证佣金金额计算：3500000 × 0.015 = 52500
        ArgumentCaptor<RealtyCommissionTripartite> insertCaptor = ArgumentCaptor.forClass(RealtyCommissionTripartite.class);
        verify(tripartiteMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getCommissionAmount())
                .isEqualByComparingTo(new BigDecimal("52500.00"));
    }

    // ===== 用例 2：Webhook 回调状态机 =====

    @Test
    @DisplayName("用例2: onSigned - 状态从 SIGNING 变更为 SIGNED")
    void testOnSignedStateTransition() {
        RealtyCommissionTripartite tripartite = RealtyCommissionTripartite.builder()
                .id(300L)
                .signStatus("SIGNING")
                .signFlowId("FLOW-COM-001")
                .build();
        given(tripartiteMapper.selectById(300L)).willReturn(tripartite);

        service.onSigned(300L);

        ArgumentCaptor<RealtyCommissionTripartite> captor = ArgumentCaptor.forClass(RealtyCommissionTripartite.class);
        verify(tripartiteMapper).updateById(captor.capture());
        assertThat(captor.getValue().getSignStatus()).isEqualTo("SIGNED");
    }

    // ===== 用例 3：UNIQUE deal_id 拒绝重复发起（幂等） =====

    @Test
    @DisplayName("用例3: 重复 dealId 幂等 - 已存在的 dealId 直接返回已有记录 ID，不重复创建")
    void testInitiateIdempotentForDuplicateDealId() {
        CommissionInitiateDTO dto = buildDto();

        RealtyCommissionTripartite existing = RealtyCommissionTripartite.builder()
                .id(999L)
                .dealId(dto.getDealId())
                .signStatus("SIGNING")
                .build();
        given(tripartiteMapper.selectByDealId(dto.getDealId())).willReturn(existing);

        Long id = service.initiate(dto);

        assertThat(id).isEqualTo(999L);
        // 不应调用 insert 或 e签宝
        verify(tripartiteMapper, never()).insert(any());
        verify(esignHttpClient, never()).createSignFlow(any());
    }

    // ===== 用例 4：getSignUrl 正常返回 H5 链接 =====

    @Test
    @DisplayName("用例4: getSignUrl - 正常返回 e签宝 H5 签署链接")
    void testGetSignUrl() {
        RealtyCommissionTripartite tripartite = RealtyCommissionTripartite.builder()
                .id(300L)
                .signFlowId("FLOW-COM-001")
                .signStatus("SIGNING")
                .build();
        given(tripartiteMapper.selectById(300L)).willReturn(tripartite);
        given(esignHttpClient.getSignUrl("FLOW-COM-001", "SIGNER-A")).willReturn("https://esign.example.com/h5/xxx");

        String url = service.getSignUrl(300L, "SIGNER-A");

        assertThat(url).isEqualTo("https://esign.example.com/h5/xxx");
        verify(esignHttpClient).getSignUrl("FLOW-COM-001", "SIGNER-A");
    }

    // ======================== 工具方法 ========================

    private CommissionInitiateDTO buildDto() {
        CommissionInitiateDTO dto = new CommissionInitiateDTO();
        dto.setDealId(1001L);
        dto.setCustomerId(1L);
        dto.setProjectId(10L);
        dto.setAllianceId(30L);
        dto.setDealAmount(new BigDecimal("3500000.00"));
        dto.setCommissionRate(new BigDecimal("0.0150"));
        dto.setPartyAName("XX 房地产开发有限公司");
        dto.setPartyBName("链家地产中介");
        dto.setPartyCName("张三");
        dto.setCustomerName("张三");
        dto.setProjectName("江湾华庭");
        dto.setFullNo("1-3-0501");
        dto.setPartyAMobile("13900001111");
        dto.setPartyBMobile("13900002222");
        dto.setPartyCMobile("13800001234");
        return dto;
    }

    private RealtySopTemplate buildTemplate() {
        RealtySopTemplate t = new RealtySopTemplate();
        t.setId(2L);
        t.setCode("commission_tripartite");
        t.setName("佣金三方协议");
        t.setContentHtml("<h2>佣金三方协议</h2><p>甲方：{{party_a}}</p>");
        t.setEnabled(1);
        return t;
    }
}
