package com.pengcheng.integration.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.integration.config.IntegrationProviderConfig;
import com.pengcheng.integration.config.IntegrationProviderConfigMapper;
import com.pengcheng.integration.spi.ImApprovalService;
import com.pengcheng.integration.spi.dto.ApprovalSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业微信审批同步实现。
 * <p>
 * 调用 /cgi-bin/oa/applyevent 接口，将内部审批单推送到企业微信审批流程。
 * 需提前在企业微信管理台配置审批模板（template_id）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WecomApprovalServiceImpl implements ImApprovalService {

    private static final String APPLY_EVENT_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/oa/applyevent?access_token=%s";

    private final IntegrationProviderConfigMapper configMapper;
    private final WecomTokenCache                 tokenCache;
    private final WecomHttpClient                 httpClient;

    @Override
    public void pushApproval(Long tenantId, ApprovalSyncEvent event) {
        IntegrationProviderConfig cfg   = loadConfig(tenantId);
        String                    token = tokenCache.getToken(cfg.getCorpId(), cfg.getSecretRef());

        Map<String, Object> body = buildApplyBody(cfg, event);
        String url = String.format(APPLY_EVENT_URL, token);
        httpClient.post(url, body);
        log.info("[WecomApproval] pushed approvalId={} to wecom, tenantId={}", event.getApprovalId(), tenantId);
    }

    // ---- private ----

    /**
     * 构建企业微信 applyevent 请求体。
     * <p>
     * 参考文档：https://developer.work.weixin.qq.com/document/path/91853
     */
    private Map<String, Object> buildApplyBody(IntegrationProviderConfig cfg, ApprovalSyncEvent event) {
        Map<String, Object> body = new HashMap<>();
        body.put("creator_userid", event.getApplicantExternalId());
        body.put("template_id", event.getTemplateId());
        body.put("use_template_approver", 1); // 使用模板中配置的审批人

        // apply_data.contents：按字段构建控件列表
        List<Map<String, Object>> contents = new ArrayList<>();
        if (event.getFields() != null) {
            for (Map<String, String> field : event.getFields()) {
                String ctrlId    = field.get("ctrlId");
                String ctrlValue = field.get("ctrlValue");
                if (ctrlId == null) continue;

                Map<String, Object> valueMap = new HashMap<>();
                valueMap.put("text", ctrlValue != null ? ctrlValue : "");

                Map<String, Object> ctrl = new HashMap<>();
                ctrl.put("control", "Text");
                ctrl.put("id", ctrlId);
                ctrl.put("value", valueMap);
                contents.add(ctrl);
            }
        }
        body.put("apply_data", Map.of("contents", contents));

        // 汇总附言（用内部审批 ID 追溯）
        body.put("summary_list", List.of(
                Map.of("summary_info", List.of(
                        Map.of("text", "内部审批单号:" + event.getApprovalId(), "lang", "zh_CN")
                ))
        ));

        return body;
    }

    private IntegrationProviderConfig loadConfig(Long tenantId) {
        IntegrationProviderConfig cfg = configMapper.selectOne(
                new LambdaQueryWrapper<IntegrationProviderConfig>()
                        .eq(IntegrationProviderConfig::getTenantId, tenantId)
                        .eq(IntegrationProviderConfig::getProvider, "wecom"));
        if (cfg == null) {
            throw new IllegalStateException("No wecom config for tenantId=" + tenantId);
        }
        return cfg;
    }
}
