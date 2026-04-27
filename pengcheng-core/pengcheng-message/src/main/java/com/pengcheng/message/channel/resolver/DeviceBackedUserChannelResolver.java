package com.pengcheng.message.channel.resolver;

import com.pengcheng.system.device.entity.UserLoginDevice;
import com.pengcheng.system.device.service.UserLoginDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 基于 {@code user_login_device} 表 + 心跳时间的用户三通道画像解析器。
 *
 * <p>替换 V4MvpAutoConfiguration 中的站内信兜底实现（@ConditionalOnMissingBean 机制）。
 *
 * <h3>规则</h3>
 * <ul>
 *   <li><b>appOnline</b>：取 clientType=APP 且 lastActive > now-60s 的最新设备；若存在则在线。</li>
 *   <li><b>appRegistrationId</b>：取所有设备中 lastActive 最新的 APP 设备的 tokenValue。
 *       <br>TODO: user_login_device 表暂无独立的 push device_token 字段（如极光/个推 registrationId），
 *       此处以 tokenValue 占位；待表增加 {@code device_push_token} 列后替换。</li>
 *   <li><b>miniProgramSubscribed</b>：始终 false（mp_user_subscribe 表尚未建立，留 TODO）。</li>
 *   <li><b>miniProgramOpenId</b>：始终 null（同上 TODO）。</li>
 *   <li><b>webInboxEnabled</b>：始终 true。</li>
 *   <li>用户无任何登录设备时返回最小画像（仅站内信）。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class DeviceBackedUserChannelResolver implements UserChannelResolver {

    /** APP 心跳超时阈值（秒）：60 秒内有 lastActive 即认为在线 */
    static final long APP_ONLINE_THRESHOLD_SECONDS = 60L;

    private final UserLoginDeviceService userLoginDeviceService;

    @Override
    public UserChannelProfile resolve(Long userId) {
        if (userId == null) {
            return minimalProfile(null);
        }

        List<UserLoginDevice> devices = userLoginDeviceService.listByUser(userId);
        if (devices == null || devices.isEmpty()) {
            return minimalProfile(userId);
        }

        // 只关注 APP 客户端设备
        Optional<UserLoginDevice> latestApp = devices.stream()
                .filter(d -> "APP".equalsIgnoreCase(d.getClientType()))
                .filter(d -> d.getLastActive() != null)
                .max(Comparator.comparing(UserLoginDevice::getLastActive));

        boolean appOnline = false;
        String appRegistrationId = null;

        if (latestApp.isPresent()) {
            UserLoginDevice appDevice = latestApp.get();
            appOnline = isOnline(appDevice.getLastActive());
            // TODO: 替换为 device_push_token 字段（极光/个推 registrationId）；暂用 tokenValue 占位
            appRegistrationId = appDevice.getTokenValue();
        }

        // TODO: 查询 mp_user_subscribe 表获取 miniProgramSubscribed 和 miniProgramOpenId
        //       mp_user_subscribe 表尚未建立，暂默认 false / null

        return UserChannelProfile.builder()
                .userId(userId)
                .appOnline(appOnline)
                .appRegistrationId(appRegistrationId)
                .miniProgramSubscribed(false)
                .miniProgramOpenId(null)
                .webInboxEnabled(true)
                .build();
    }

    /**
     * 判断设备是否在线：lastActive 在 {@link #APP_ONLINE_THRESHOLD_SECONDS} 秒以内。
     */
    private boolean isOnline(LocalDateTime lastActive) {
        return lastActive.isAfter(LocalDateTime.now().minusSeconds(APP_ONLINE_THRESHOLD_SECONDS));
    }

    /**
     * 无设备时的最小画像：仅启用站内信。
     */
    private UserChannelProfile minimalProfile(Long userId) {
        return UserChannelProfile.builder()
                .userId(userId)
                .appOnline(false)
                .appRegistrationId(null)
                .miniProgramSubscribed(false)
                .miniProgramOpenId(null)
                .webInboxEnabled(true)
                .build();
    }
}
