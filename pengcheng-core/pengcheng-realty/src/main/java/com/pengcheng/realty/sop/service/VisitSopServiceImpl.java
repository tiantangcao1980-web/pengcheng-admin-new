package com.pengcheng.realty.sop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.finance.contract.sign.esign.EsignCallException;
import com.pengcheng.finance.contract.sign.esign.EsignHttpClient;
import com.pengcheng.finance.contract.sign.esign.dto.EsignSignFlowRequest;
import com.pengcheng.finance.contract.sign.esign.dto.EsignSigner;
import com.pengcheng.realty.sop.dto.VisitSopCreateDTO;
import com.pengcheng.realty.sop.entity.RealtySopTemplate;
import com.pengcheng.realty.sop.entity.RealtyVisitSop;
import com.pengcheng.realty.sop.mapper.RealtySopTemplateMapper;
import com.pengcheng.realty.sop.mapper.RealtyVisitSopMapper;
import com.pengcheng.realty.sop.pdf.PdfGenerator;
import com.pengcheng.realty.sop.template.SopTemplateRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 带看 SOP 服务实现
 * <p>
 * EsignHttpClient 使用 {@code @Autowired(required=false)}：
 * 当 e签宝未配置（Feature Flag 关闭）时 Bean 为 null，相关方法降级抛出
 * {@link IllegalStateException} 提示管理员先配置 e签宝。
 */
@Slf4j
@Service
public class VisitSopServiceImpl implements VisitSopService {

    private static final String TEMPLATE_CODE = "visit_confirm";
    private static final int DEFAULT_VALID_DAYS = 14;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RealtyVisitSopMapper visitSopMapper;
    private final RealtySopTemplateMapper templateMapper;
    private final SopTemplateRenderer renderer;
    private final PdfGenerator pdfGenerator;

    /**
     * required=false：e签宝未配置时不抛 Bean 注入异常，方法内判断 null 时抛业务异常。
     */
    @Autowired(required = false)
    private EsignHttpClient esignHttpClient;

    public VisitSopServiceImpl(RealtyVisitSopMapper visitSopMapper,
                                RealtySopTemplateMapper templateMapper,
                                SopTemplateRenderer renderer,
                                PdfGenerator pdfGenerator) {
        this.visitSopMapper = visitSopMapper;
        this.templateMapper = templateMapper;
        this.renderer = renderer;
        this.pdfGenerator = pdfGenerator;
    }

    @Override
    @Transactional
    public Long initiate(VisitSopCreateDTO dto) {
        requireEsign();

        int validDays = dto.getValidDays() != null ? dto.getValidDays() : DEFAULT_VALID_DAYS;
        LocalDateTime visitTime = dto.getVisitTime();
        LocalDateTime expiresAt = visitTime.plusDays(validDays);

        // 1. 查询模板
        RealtySopTemplate template = templateMapper.selectByCode(TEMPLATE_CODE);
        if (template == null) {
            throw new IllegalStateException("未找到带看确认书模板（code=visit_confirm），请先初始化模板数据");
        }

        // 2. 渲染模板
        Map<String, String> vars = new HashMap<>();
        vars.put("customer_name", nvl(dto.getCustomerName()));
        vars.put("customer_phone", nvl(dto.getCustomerPhone()));
        vars.put("project_name", nvl(dto.getProjectName()));
        vars.put("visit_time", visitTime.format(DT_FMT));
        vars.put("salesperson_name", nvl(dto.getSalespersonName()));
        vars.put("alliance_name", nvl(dto.getAllianceName()));
        vars.put("expires_at", expiresAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        String htmlContent = renderer.render(template.getContentHtml(), vars);

        // 3. 生成文档（简化版：HTML 字节，TODO: 替换为 PDF）
        byte[] docBytes = pdfGenerator.generate(htmlContent, "带看确认书-" + dto.getCustomerName());
        // TODO: 调用 OSS 上传 docBytes，获取访问 URL
        String docUrl = "oss://realty/sop/visit_confirm/" + System.currentTimeMillis()
                + pdfGenerator.getFileExtension();

        // 4. 创建 SOP 记录
        RealtyVisitSop sop = RealtyVisitSop.builder()
                .customerId(dto.getCustomerId())
                .projectId(dto.getProjectId())
                .salespersonId(dto.getSalespersonId())
                .allianceId(dto.getAllianceId())
                .visitTime(visitTime)
                .visitUnitId(dto.getVisitUnitId())
                .durationMin(dto.getDurationMin())
                .status("PENDING_CONFIRM")
                .confirmDocUrl(docUrl)
                .expiresAt(expiresAt)
                .build();
        visitSopMapper.insert(sop);

        // 5. 创建 e签宝签署流
        try {
            EsignSignFlowRequest signRequest = EsignSignFlowRequest.builder()
                    .signFlowTitle("带看确认书-" + nvl(dto.getCustomerName()))
                    .signFlowExpireTime(toUnixMs(expiresAt))
                    .notifyCallbackUrl(dto.getCallbackUrl())
                    .signers(List.of(
                            EsignSigner.builder()
                                    .signerType("PERSONAL")
                                    .psnName(nvl(dto.getCustomerName()))
                                    .psnMobile(nvl(dto.getCustomerPhone()))
                                    .signOrder(1)
                                    .signerRole("partyB")
                                    .build()
                    ))
                    .build();

            String signFlowId = esignHttpClient.createSignFlow(signRequest);

            // 6. 写入 sign_flow_id
            sop.setConfirmSignId(signFlowId);
            visitSopMapper.updateById(sop);
            log.info("[VisitSop] SOP={} 签署流创建成功 signFlowId={}", sop.getId(), signFlowId);
        } catch (EsignCallException e) {
            log.error("[VisitSop] SOP={} 创建签署流失败: {}", sop.getId(), e.getMessage());
            throw new RuntimeException("e签宝签署流创建失败：" + e.getMessage(), e);
        }

        return sop.getId();
    }

    @Override
    public String getSignUrl(Long sopId, String signerId) {
        requireEsign();
        RealtyVisitSop sop = requireSop(sopId);
        if (sop.getConfirmSignId() == null) {
            throw new IllegalStateException("带看 SOP 尚未创建签署流，请先发起签署");
        }
        return esignHttpClient.getSignUrl(sop.getConfirmSignId(), signerId);
    }

    @Override
    @Transactional
    public void onSigned(Long sopId) {
        RealtyVisitSop sop = requireSop(sopId);
        sop.setStatus("CONFIRMED");
        sop.setConfirmedAt(LocalDateTime.now());
        visitSopMapper.updateById(sop);
        log.info("[VisitSop] SOP={} 签署完成，状态变更为 CONFIRMED", sopId);
    }

    @Override
    public boolean isCovered(Long customerId, Long allianceId, LocalDateTime time) {
        if (customerId == null || allianceId == null || time == null) {
            return false;
        }
        RealtyVisitSop sop = visitSopMapper.findCoveredSop(customerId, allianceId, time);
        return sop != null;
    }

    @Override
    public RealtyVisitSop getById(Long id) {
        return visitSopMapper.selectById(id);
    }

    @Override
    public List<RealtyVisitSop> listByCustomer(Long customerId) {
        return visitSopMapper.selectList(
                new LambdaQueryWrapper<RealtyVisitSop>()
                        .eq(RealtyVisitSop::getCustomerId, customerId)
                        .orderByDesc(RealtyVisitSop::getVisitTime));
    }

    // ======================== 私有工具方法 ========================

    private void requireEsign() {
        if (esignHttpClient == null) {
            throw new IllegalStateException("e签宝未配置或 Feature Flag 已关闭，请先在系统配置中启用 e签宝集成");
        }
    }

    private RealtyVisitSop requireSop(Long sopId) {
        RealtyVisitSop sop = visitSopMapper.selectById(sopId);
        if (sop == null) {
            throw new IllegalArgumentException("带看 SOP 记录不存在：id=" + sopId);
        }
        return sop;
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static long toUnixMs(LocalDateTime dt) {
        return dt.toInstant(java.time.ZoneOffset.ofHours(8)).toEpochMilli();
    }
}
