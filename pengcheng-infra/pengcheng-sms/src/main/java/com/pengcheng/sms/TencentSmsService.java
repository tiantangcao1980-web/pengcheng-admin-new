package com.pengcheng.sms;

import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 腾讯云短信服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentSmsService implements SmsService {

    private final SystemConfigHelper configHelper;

    @Override
    public boolean sendCode(String phone, String code) {
        String secretId = configHelper.getSmsTencentSecretId();
        String secretKey = configHelper.getSmsTencentSecretKey();
        String appId = configHelper.getSmsTencentAppId();
        String signName = configHelper.getSmsTencentSignName();
        String templateId = configHelper.getSmsTencentTemplateId();

        if (secretId.isEmpty() || secretKey.isEmpty()) {
            log.warn("腾讯云短信配置不完整，使用控制台打印模式");
            log.info("============================================");
            log.info("【短信验证码 - 腾讯云(未配置)】");
            log.info("手机号: {}", phone);
            log.info("验证码: {}", code);
            log.info("有效期: 5分钟");
            log.info("============================================");
            return true;
        }

        try {
            // TODO: 实际调用腾讯云短信API
            // 参考文档: https://cloud.tencent.com/document/product/382/43194
            // 1. 引入依赖 tencentcloud-sdk-java
            // 2. 构建请求发送短信

            /*
            Credential cred = new Credential(secretId, secretKey);
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");

            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            SmsClient client = new SmsClient(cred, "ap-guangzhou", clientProfile);

            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(appId);
            req.setSignName(signName);
            req.setTemplateId(templateId);
            req.setPhoneNumberSet(new String[]{"+86" + phone});
            req.setTemplateParamSet(new String[]{code});

            SendSmsResponse resp = client.SendSms(req);
            if ("Ok".equals(resp.getSendStatusSet()[0].getCode())) {
                log.info("腾讯云短信发送成功: phone={}", phone);
                return true;
            } else {
                log.error("腾讯云短信发送失败: {}", resp.getSendStatusSet()[0].getMessage());
                return false;
            }
            */

            log.info("============================================");
            log.info("【短信验证码 - 腾讯云(待实现)】");
            log.info("手机号: {}", phone);
            log.info("验证码: {}", code);
            log.info("AppId: {}", appId);
            log.info("签名: {}", signName);
            log.info("模板: {}", templateId);
            log.info("有效期: 5分钟");
            log.info("============================================");
            return true;

        } catch (Exception e) {
            log.error("腾讯云短信发送异常", e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "tencent";
    }
}
