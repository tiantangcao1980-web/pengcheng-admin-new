package com.pengcheng.realty.sop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.finance.contract.sign.esign.EsignCallException;
import com.pengcheng.finance.contract.sign.esign.EsignHttpClient;
import com.pengcheng.finance.contract.sign.esign.dto.EsignSignFlowRequest;
import com.pengcheng.finance.contract.sign.esign.dto.EsignSigner;
import com.pengcheng.realty.sop.dto.CommissionInitiateDTO;
import com.pengcheng.realty.sop.entity.RealtyCommissionTripartite;
import com.pengcheng.realty.sop.entity.RealtySopTemplate;
import com.pengcheng.realty.sop.mapper.RealtyCommissionTripartiteMapper;
import com.pengcheng.realty.sop.mapper.RealtySopTemplateMapper;
import com.pengcheng.realty.sop.pdf.PdfGenerator;
import com.pengcheng.realty.sop.template.SopTemplateRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 佣金三方协议服务实现
 * <p>
 * EsignHttpClient 使用 {@code @Autowired(required=false)}：
 * 未配置时 Bean 为 null，调用签署相关方法会抛出 {@link IllegalStateException}。
 */
@Slf4j
@Service
public class CommissionTripartiteServiceImpl implements CommissionTripartiteService {

    private static final String TEMPLATE_CODE = "commission_tripartite";

    private final RealtyCommissionTripartiteMapper tripartiteMapper;
    private final RealtySopTemplateMapper templateMapper;
    private final SopTemplateRenderer renderer;
    private final PdfGenerator pdfGenerator;

    @Autowired(required = false)
    private EsignHttpClient esignHttpClient;

    public CommissionTripartiteServiceImpl(RealtyCommissionTripartiteMapper tripartiteMapper,
                                           RealtySopTemplateMapper templateMapper,
                                           SopTemplateRenderer renderer,
                                           PdfGenerator pdfGenerator) {
        this.tripartiteMapper = tripartiteMapper;
        this.templateMapper = templateMapper;
        this.renderer = renderer;
        this.pdfGenerator = pdfGenerator;
    }

    @Override
    @Transactional
    public Long initiate(CommissionInitiateDTO dto) {
        requireEsign();

        // 1. 幂等检查：同一 dealId 已存在直接返回
        RealtyCommissionTripartite existing = tripartiteMapper.selectByDealId(dto.getDealId());
        if (existing != null) {
            log.info("[CommissionTripartite] dealId={} 已存在三方协议 id={}，直接返回", dto.getDealId(), existing.getId());
            return existing.getId();
        }

        // 2. 计算佣金金额
        BigDecimal commissionAmount = dto.getDealAmount()
                .multiply(dto.getCommissionRate())
                .setScale(2, RoundingMode.HALF_UP);

        // 3. 查询模板
        RealtySopTemplate template = templateMapper.selectByCode(TEMPLATE_CODE);
        if (template == null) {
            throw new IllegalStateException("未找到佣金三方协议模板（code=commission_tripartite），请先初始化模板数据");
        }

        // 4. 渲染模板
        Map<String, String> vars = new HashMap<>();
        vars.put("party_a", nvl(dto.getPartyAName()));
        vars.put("party_b", nvl(dto.getPartyBName()));
        vars.put("party_c", nvl(dto.getPartyCName()));
        vars.put("customer_name", nvl(dto.getCustomerName()));
        vars.put("project_name", nvl(dto.getProjectName()));
        vars.put("full_no", nvl(dto.getFullNo()));
        vars.put("deal_amount", dto.getDealAmount().toPlainString());
        vars.put("commission_rate", dto.getCommissionRate().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).toPlainString());
        vars.put("commission_amount", commissionAmount.toPlainString());

        String htmlContent = renderer.render(template.getContentHtml(), vars);

        // 5. 生成文档（简化版：HTML，TODO: 替换为 PDF）
        byte[] docBytes = pdfGenerator.generate(htmlContent, "佣金三方协议-" + dto.getCustomerName());
        // TODO: 上传 docBytes 至 OSS
        String docUrl = "oss://realty/sop/commission_tripartite/" + System.currentTimeMillis()
                + pdfGenerator.getFileExtension();

        // 6. 创建三方协议记录
        RealtyCommissionTripartite tripartite = RealtyCommissionTripartite.builder()
                .dealId(dto.getDealId())
                .visitSopId(dto.getVisitSopId())
                .customerId(dto.getCustomerId())
                .projectId(dto.getProjectId())
                .allianceId(dto.getAllianceId())
                .dealAmount(dto.getDealAmount())
                .commissionRate(dto.getCommissionRate())
                .commissionAmount(commissionAmount)
                .partyAName(nvl(dto.getPartyAName()))
                .partyBName(nvl(dto.getPartyBName()))
                .partyCName(nvl(dto.getPartyCName()))
                .docUrl(docUrl)
                .signStatus("DRAFT")
                .build();
        tripartiteMapper.insert(tripartite);

        // 7. 创建 e签宝签署流（三方：甲乙丙）
        try {
            // 签署有效期：30 天
            long expireMs = LocalDateTime.now().plusDays(30).toInstant(ZoneOffset.ofHours(8)).toEpochMilli();

            EsignSignFlowRequest signRequest = EsignSignFlowRequest.builder()
                    .signFlowTitle("佣金三方协议-" + nvl(dto.getCustomerName()))
                    .signFlowExpireTime(expireMs)
                    .notifyCallbackUrl(dto.getCallbackUrl())
                    .signers(List.of(
                            EsignSigner.builder()
                                    .signerType("ORGANIZATION")
                                    .orgName(nvl(dto.getPartyAName()))
                                    .operatorMobile(nvl(dto.getPartyAMobile()))
                                    .signOrder(1)
                                    .signerRole("partyA")
                                    .build(),
                            EsignSigner.builder()
                                    .signerType("ORGANIZATION")
                                    .orgName(nvl(dto.getPartyBName()))
                                    .operatorMobile(nvl(dto.getPartyBMobile()))
                                    .signOrder(2)
                                    .signerRole("partyB")
                                    .build(),
                            EsignSigner.builder()
                                    .signerType("PERSONAL")
                                    .psnName(nvl(dto.getPartyCName()))
                                    .psnMobile(nvl(dto.getPartyCMobile()))
                                    .signOrder(3)
                                    .signerRole("partyC")
                                    .build()
                    ))
                    .build();

            String signFlowId = esignHttpClient.createSignFlow(signRequest);

            tripartite.setSignFlowId(signFlowId);
            tripartite.setSignStatus("SIGNING");
            tripartiteMapper.updateById(tripartite);
            log.info("[CommissionTripartite] id={} 签署流创建成功 signFlowId={}", tripartite.getId(), signFlowId);
        } catch (EsignCallException e) {
            log.error("[CommissionTripartite] id={} 创建签署流失败: {}", tripartite.getId(), e.getMessage());
            throw new RuntimeException("e签宝签署流创建失败：" + e.getMessage(), e);
        }

        return tripartite.getId();
    }

    @Override
    @Transactional
    public void onSigned(Long id) {
        RealtyCommissionTripartite tripartite = requireTripartite(id);
        tripartite.setSignStatus("SIGNED");
        tripartiteMapper.updateById(tripartite);
        log.info("[CommissionTripartite] id={} 签署完成，状态变更为 SIGNED", id);
    }

    @Override
    public String getSignUrl(Long id, String signerId) {
        requireEsign();
        RealtyCommissionTripartite tripartite = requireTripartite(id);
        if (tripartite.getSignFlowId() == null) {
            throw new IllegalStateException("三方协议尚未创建签署流，请先发起签署");
        }
        return esignHttpClient.getSignUrl(tripartite.getSignFlowId(), signerId);
    }

    @Override
    public RealtyCommissionTripartite getById(Long id) {
        return tripartiteMapper.selectById(id);
    }

    @Override
    public RealtyCommissionTripartite getByDealId(Long dealId) {
        return tripartiteMapper.selectByDealId(dealId);
    }

    @Override
    public List<RealtyCommissionTripartite> listByAlliance(Long allianceId) {
        return tripartiteMapper.selectList(
                new LambdaQueryWrapper<RealtyCommissionTripartite>()
                        .eq(RealtyCommissionTripartite::getAllianceId, allianceId)
                        .orderByDesc(RealtyCommissionTripartite::getCreateTime));
    }

    // ======================== 私有工具方法 ========================

    private void requireEsign() {
        if (esignHttpClient == null) {
            throw new IllegalStateException("e签宝未配置或 Feature Flag 已关闭，请先在系统配置中启用 e签宝集成");
        }
    }

    private RealtyCommissionTripartite requireTripartite(Long id) {
        RealtyCommissionTripartite tripartite = tripartiteMapper.selectById(id);
        if (tripartite == null) {
            throw new IllegalArgumentException("三方协议记录不存在：id=" + id);
        }
        return tripartite;
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
