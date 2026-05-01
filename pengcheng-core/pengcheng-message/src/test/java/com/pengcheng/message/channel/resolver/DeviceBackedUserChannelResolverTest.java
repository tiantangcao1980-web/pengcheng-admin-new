package com.pengcheng.message.channel.resolver;

import com.pengcheng.message.channel.UserChannelProfile;
import com.pengcheng.message.subscribe.auth.MpUserSubscribe;
import com.pengcheng.message.subscribe.auth.MpUserSubscribeService;
import com.pengcheng.system.device.entity.UserLoginDevice;
import com.pengcheng.system.device.service.UserLoginDeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DeviceBackedUserChannelResolver")
class DeviceBackedUserChannelResolverTest {

    @Mock
    private UserLoginDeviceService userLoginDeviceService;

    @Mock
    private MpUserSubscribeService mpUserSubscribeService;

    private DeviceBackedUserChannelResolver resolver;

    @BeforeEach
    void setUp() {
        // 默认：用户无有效订阅（未授权状态）
        when(mpUserSubscribeService.findLatestActive(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(Optional.empty());
        resolver = new DeviceBackedUserChannelResolver(userLoginDeviceService, mpUserSubscribeService);
    }

    // -----------------------------------------------------------------------
    // 辅助方法
    // -----------------------------------------------------------------------

    private UserLoginDevice appDevice(Long userId, LocalDateTime lastActive, String tokenValue) {
        UserLoginDevice d = new UserLoginDevice();
        d.setUserId(userId);
        d.setClientType("APP");
        d.setLastActive(lastActive);
        d.setTokenValue(tokenValue);
        d.setStatus(UserLoginDevice.STATUS_ONLINE);
        return d;
    }

    // -----------------------------------------------------------------------
    // 测试用例
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("① 30 秒前有心跳 → appOnline=true，appRegistrationId 来自该设备 tokenValue")
    void resolve_appOnline_when_lastActive_within_30s() {
        LocalDateTime recentActive = LocalDateTime.now().minusSeconds(30);
        UserLoginDevice device = appDevice(1L, recentActive, "reg-token-abc");

        when(userLoginDeviceService.listByUser(1L)).thenReturn(List.of(device));

        UserChannelProfile profile = resolver.resolve(1L);

        assertThat(profile.getUserId()).isEqualTo(1L);
        assertThat(profile.isAppOnline()).isTrue();
        assertThat(profile.getAppRegistrationId()).isEqualTo("reg-token-abc");
        assertThat(profile.isMiniProgramSubscribed()).isFalse();
        assertThat(profile.getMiniProgramOpenId()).isNull();
        assertThat(profile.isWebInboxEnabled()).isTrue();
    }

    @Test
    @DisplayName("② 90 秒前有心跳 → appOnline=false（超过 60 秒阈值）")
    void resolve_appOffline_when_lastActive_90s_ago() {
        LocalDateTime staleActive = LocalDateTime.now().minusSeconds(90);
        UserLoginDevice device = appDevice(2L, staleActive, "reg-token-xyz");

        when(userLoginDeviceService.listByUser(2L)).thenReturn(List.of(device));

        UserChannelProfile profile = resolver.resolve(2L);

        assertThat(profile.isAppOnline()).isFalse();
        // appRegistrationId 仍取最新 APP 设备的 tokenValue（即使离线）
        assertThat(profile.getAppRegistrationId()).isEqualTo("reg-token-xyz");
        assertThat(profile.isWebInboxEnabled()).isTrue();
    }

    @Test
    @DisplayName("③ 用户无任何登录设备 → 最小画像，仅站内信")
    void resolve_minimalProfile_when_no_devices() {
        when(userLoginDeviceService.listByUser(3L)).thenReturn(Collections.emptyList());

        UserChannelProfile profile = resolver.resolve(3L);

        assertThat(profile.getUserId()).isEqualTo(3L);
        assertThat(profile.isAppOnline()).isFalse();
        assertThat(profile.getAppRegistrationId()).isNull();
        assertThat(profile.isMiniProgramSubscribed()).isFalse();
        assertThat(profile.getMiniProgramOpenId()).isNull();
        assertThat(profile.isWebInboxEnabled()).isTrue();
    }

    @Test
    @DisplayName("④ 多设备时取 lastActive 最新的 APP 设备决定在线状态和 registrationId")
    void resolve_picks_latest_app_device_among_multiple() {
        // older device: 120 秒前，已超时
        UserLoginDevice older = appDevice(4L, LocalDateTime.now().minusSeconds(120), "old-token");
        // newer device: 10 秒前，在线
        UserLoginDevice newer = appDevice(4L, LocalDateTime.now().minusSeconds(10), "new-token");
        // WEB 设备：不应影响 appOnline 判断
        UserLoginDevice webDevice = new UserLoginDevice();
        webDevice.setUserId(4L);
        webDevice.setClientType("WEB");
        webDevice.setLastActive(LocalDateTime.now().minusSeconds(5));
        webDevice.setTokenValue("web-token");

        when(userLoginDeviceService.listByUser(4L)).thenReturn(List.of(older, webDevice, newer));

        UserChannelProfile profile = resolver.resolve(4L);

        assertThat(profile.isAppOnline()).isTrue();
        assertThat(profile.getAppRegistrationId()).isEqualTo("new-token");
    }

    @Test
    @DisplayName("⑤ 设备列表中没有 APP 类型设备 → appOnline=false，appRegistrationId=null")
    void resolve_no_app_type_device() {
        UserLoginDevice webDevice = new UserLoginDevice();
        webDevice.setUserId(5L);
        webDevice.setClientType("WEB");
        webDevice.setLastActive(LocalDateTime.now().minusSeconds(5));
        webDevice.setTokenValue("web-token");

        when(userLoginDeviceService.listByUser(5L)).thenReturn(List.of(webDevice));

        UserChannelProfile profile = resolver.resolve(5L);

        assertThat(profile.isAppOnline()).isFalse();
        assertThat(profile.getAppRegistrationId()).isNull();
        assertThat(profile.isWebInboxEnabled()).isTrue();
    }

    @Test
    @DisplayName("⑥ 用户有有效订阅记录 → miniProgramSubscribed=true，miniProgramOpenId 来自记录")
    void resolve_miniProgramSubscribed_when_active_subscription_exists() {
        UserLoginDevice device = appDevice(6L, LocalDateTime.now().minusSeconds(10), "reg-token-6");
        when(userLoginDeviceService.listByUser(6L)).thenReturn(List.of(device));

        MpUserSubscribe sub = MpUserSubscribe.builder()
                .userId(6L)
                .openId("wx-open-id-6")
                .templateId("tpl-001")
                .quota(2)
                .used(0)
                .revoked(0)
                .lastSubscribeTime(LocalDateTime.now())
                .build();
        when(mpUserSubscribeService.findLatestActive(6L)).thenReturn(Optional.of(sub));

        UserChannelProfile profile = resolver.resolve(6L);

        assertThat(profile.isMiniProgramSubscribed()).isTrue();
        assertThat(profile.getMiniProgramOpenId()).isEqualTo("wx-open-id-6");
    }

    @Test
    @DisplayName("⑦ 用户订阅已撤销或配额耗尽（findLatestActive 返回 empty）→ miniProgramSubscribed=false")
    void resolve_miniProgramNotSubscribed_when_no_active_subscription() {
        UserLoginDevice device = appDevice(7L, LocalDateTime.now().minusSeconds(10), "reg-token-7");
        when(userLoginDeviceService.listByUser(7L)).thenReturn(List.of(device));

        // mpUserSubscribeService mock 默认已在 setUp 中返回 empty，此处无需重复 stub

        UserChannelProfile profile = resolver.resolve(7L);

        assertThat(profile.isMiniProgramSubscribed()).isFalse();
        assertThat(profile.getMiniProgramOpenId()).isNull();
    }
}
