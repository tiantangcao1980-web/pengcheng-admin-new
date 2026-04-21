package com.pengcheng.sms;

import com.pengcheng.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 阿里云短信服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunSmsService implements SmsService {

    private final SystemConfigHelper configHelper;

    @Override
    public boolean sendCode(String phone, String code) {
        String accessKeyId = configHelper.getSmsAliyunAccessKeyId();
        String accessKeySecret = configHelper.getSmsAliyunAccessKeySecret();
        String signName = configHelper.getSmsAliyunSignName();
        String templateCode = configHelper.getSmsAliyunTemplateCode();

        if (accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
            log.warn("阿里云短信配置不完整，使用控制台打印模式");
            log.info("============================================");
            log.info("【短信验证码 - 阿里云(未配置)】");
            log.info("手机号: {}", phone);
            log.info("验证码: {}", code);
            log.info("有效期: 5分钟");
            log.info("============================================");
            return true;
        }

        try {
            // TODO: 实际调用阿里云短信API
            // 参考文档: https://help.aliyun.com/document_detail/101414.html
            // 1. 引入依赖 aliyun-java-sdk-core 和 aliyun-java-sdk-dysmsapi
            // 2. 构建请求发送短信

            /*
            DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
            IAcsClient client = new DefaultAcsClient(profile);

            SendSmsRequest request = new SendSmsRequest();
            request.setPhoneNumbers(phone);
            request.setSignName(signName);
            request.setTemplateCode(templateCode);
            request.setTemplateParam("{\"code\":\"" + code + "\"}");

            SendSmsResponse response = client.getAcsResponse(request);
            if ("OK".equals(response.getCode())) {
                log.info("阿里云短信发送成功: phone={}", phone);
                return true;
            } else {
                log.error("阿里云短信发送失败: {}", response.getMessage());
                return false;
            }
            */

            log.info("============================================");
            log.info("【短信验证码 - 阿里云(待实现)】");
            log.info("手机号: {}", phone);
            log.info("验证码: {}", code);
            log.info("签名: {}", signName);
            log.info("模板: {}", templateCode);
            log.info("有效期: 5分钟");
            log.info("============================================");
            return true;

        } catch (Exception e) {
            log.error("阿里云短信发送异常", e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "aliyun";
    }
}
