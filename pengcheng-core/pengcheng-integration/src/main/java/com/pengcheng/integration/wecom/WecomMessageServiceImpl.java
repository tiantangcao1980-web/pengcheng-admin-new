package com.pengcheng.integration.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.integration.config.IntegrationProviderConfig;
import com.pengcheng.integration.config.IntegrationProviderConfigMapper;
import com.pengcheng.integration.config.IntegrationUserMapping;
import com.pengcheng.integration.config.IntegrationUserMappingMapper;
import com.pengcheng.integration.spi.ImMessageService;
import com.pengcheng.integration.spi.dto.ImCardMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业微信消息发送实现。
 * <p>
 * 内部 userId → 通过 integration_user_mapping 解出企业微信 external_id，用 | 拼接。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WecomMessageServiceImpl implements ImMessageService {

    private static final String SEND_MSG_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s";

    private final IntegrationProviderConfigMapper configMapper;
    private final IntegrationUserMappingMapper    userMappingMapper;
    private final WecomTokenCache                 tokenCache;
    private final WecomHttpClient                 httpClient;

    @Override
    public void sendText(Long tenantId, List<Long> userIds, String content) {
        IntegrationProviderConfig cfg   = loadConfig(tenantId);
        String                    token = tokenCache.getToken(cfg.getCorpId(), cfg.getSecretRef());
        String                    toUser = resolveExternalIds(tenantId, userIds);

        Map<String, Object> body = new HashMap<>();
        body.put("touser", toUser);
        body.put("msgtype", "text");
        body.put("agentid", cfg.getAgentId());
        body.put("text", Map.of("content", content));
        body.put("safe", 0);

        String url = String.format(SEND_MSG_URL, token);
        httpClient.post(url, body);
        log.info("[WecomMsg] sendText to {} users, tenantId={}", userIds.size(), tenantId);
    }

    @Override
    public void sendCard(Long tenantId, List<Long> userIds, ImCardMessage card) {
        IntegrationProviderConfig cfg   = loadConfig(tenantId);
        String                    token = tokenCache.getToken(cfg.getCorpId(), cfg.getSecretRef());
        String                    toUser = resolveExternalIds(tenantId, userIds);

        Map<String, Object> textCard = new HashMap<>();
        textCard.put("title", card.getTitle());
        textCard.put("description", card.getDescription());
        textCard.put("url", card.getUrl());
        textCard.put("btntxt", card.getBtnTxt() != null ? card.getBtnTxt() : "详情");

        Map<String, Object> body = new HashMap<>();
        body.put("touser", toUser);
        body.put("msgtype", "textcard");
        body.put("agentid", cfg.getAgentId());
        body.put("textcard", textCard);
        body.put("safe", 0);

        String url = String.format(SEND_MSG_URL, token);
        httpClient.post(url, body);
        log.info("[WecomMsg] sendCard to {} users, tenantId={}", userIds.size(), tenantId);
    }

    // ---- private ----

    /**
     * 将内部 userIds 转换为 enterprise_wecom externalId 字符串（|分隔）。
     */
    private String resolveExternalIds(Long tenantId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return "";

        List<IntegrationUserMapping> mappings = userMappingMapper.selectList(
                new LambdaQueryWrapper<IntegrationUserMapping>()
                        .eq(IntegrationUserMapping::getTenantId, tenantId)
                        .eq(IntegrationUserMapping::getProvider, "wecom")
                        .in(IntegrationUserMapping::getUserId, userIds));

        return mappings.stream()
                .map(IntegrationUserMapping::getExternalId)
                .collect(Collectors.joining("|"));
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
