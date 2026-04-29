package com.pengcheng.integration.wecom;

import com.pengcheng.integration.spi.ImApprovalService;
import com.pengcheng.integration.spi.ImAuthService;
import com.pengcheng.integration.spi.ImContactService;
import com.pengcheng.integration.spi.ImMessageService;
import com.pengcheng.integration.spi.ImProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 企业微信 IM Provider 实现（provider="wecom"）。
 * <p>
 * 聚合 4 个 Service，作为工厂/注册中心的路由入口。
 */
@Component
@RequiredArgsConstructor
public class WecomImProvider implements ImProvider {

    private final WecomAuthServiceImpl     authService;
    private final WecomContactServiceImpl  contactService;
    private final WecomMessageServiceImpl  messageService;
    private final WecomApprovalServiceImpl approvalService;

    @Override
    public String provider() {
        return "wecom";
    }

    @Override
    public String providerName() {
        return "企业微信";
    }

    public ImAuthService auth() {
        return authService;
    }

    public ImContactService contact() {
        return contactService;
    }

    public ImMessageService message() {
        return messageService;
    }

    public ImApprovalService approval() {
        return approvalService;
    }
}
