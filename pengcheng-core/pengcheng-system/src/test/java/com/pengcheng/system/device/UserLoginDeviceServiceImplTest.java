package com.pengcheng.system.device;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.device.dto.DeviceRecordRequest;
import com.pengcheng.system.device.entity.UserLoginDevice;
import com.pengcheng.system.device.mapper.UserLoginDeviceMapper;
import com.pengcheng.system.device.service.impl.UserLoginDeviceServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserLoginDeviceServiceImpl")
class UserLoginDeviceServiceImplTest {

    @Mock
    private UserLoginDeviceMapper deviceMapper;

    private UserLoginDeviceServiceImpl service;

    private MockedStatic<StpUtil> stpStatic;

    @BeforeEach
    void setUp() {
        service = spy(new UserLoginDeviceServiceImpl());
        ReflectionTestUtils.setField(service, "baseMapper", deviceMapper);
        stpStatic = Mockito.mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpStatic.close();
    }

    @Test
    @DisplayName("recordLogin：tokenValue 不存在则插入新记录")
    void recordLogin_insertNew() {
        lenient().when(deviceMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(inv -> {
            UserLoginDevice d = inv.getArgument(0);
            d.setId(1L);
            return 1;
        }).when(deviceMapper).insert(any(UserLoginDevice.class));

        DeviceRecordRequest req = new DeviceRecordRequest();
        req.setUserId(100L);
        req.setTokenValue("token-A");
        req.setClientType("ADMIN");
        req.setOs("macOS");
        req.setBrowser("Chrome");

        UserLoginDevice d = service.recordLogin(req);
        assertThat(d.getId()).isEqualTo(1L);
        assertThat(d.getStatus()).isEqualTo(UserLoginDevice.STATUS_ONLINE);
        verify(deviceMapper, times(1)).insert(any(UserLoginDevice.class));
    }

    @Test
    @DisplayName("recordLogin：tokenValue 已存在则刷新 lastActive 不重复插入")
    void recordLogin_updateExisting() {
        UserLoginDevice exist = new UserLoginDevice();
        exist.setId(2L);
        exist.setUserId(100L);
        exist.setTokenValue("token-B");
        exist.setStatus(UserLoginDevice.STATUS_OFFLINE);
        when(deviceMapper.selectOne(any(Wrapper.class))).thenReturn(exist);

        DeviceRecordRequest req = new DeviceRecordRequest();
        req.setUserId(100L);
        req.setTokenValue("token-B");
        req.setClientType("WEB");

        UserLoginDevice d = service.recordLogin(req);

        assertThat(d.getId()).isEqualTo(2L);
        assertThat(d.getStatus()).isEqualTo(UserLoginDevice.STATUS_ONLINE);
        verify(deviceMapper, never()).insert(any(UserLoginDevice.class));
        verify(deviceMapper).updateById(any(UserLoginDevice.class));
    }

    @Test
    @DisplayName("recordLogin：tokenValue 缺失抛业务异常")
    void recordLogin_missingToken() {
        DeviceRecordRequest req = new DeviceRecordRequest();
        req.setUserId(100L);
        assertThatThrownBy(() -> service.recordLogin(req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("kickoutDevice：调用 Sa-Token kickoutByTokenValue 并写入 KICKED 状态")
    void kickoutDevice_success() {
        UserLoginDevice device = new UserLoginDevice();
        device.setId(1L);
        device.setUserId(100L);
        device.setTokenValue("token-K");
        device.setStatus(UserLoginDevice.STATUS_ONLINE);
        when(deviceMapper.selectById(1L)).thenReturn(device);

        service.kickoutDevice(1L, 100L);

        stpStatic.verify(() -> StpUtil.kickoutByTokenValue("token-K"));
        assertThat(device.getStatus()).isEqualTo(UserLoginDevice.STATUS_KICKED);
        verify(deviceMapper).updateById(device);
    }

    @Test
    @DisplayName("kickoutDevice：已被踢的设备幂等处理（不再调用 Sa-Token）")
    void kickoutDevice_idempotent() {
        UserLoginDevice device = new UserLoginDevice();
        device.setId(1L);
        device.setUserId(100L);
        device.setTokenValue("token-K");
        device.setStatus(UserLoginDevice.STATUS_KICKED);
        when(deviceMapper.selectById(1L)).thenReturn(device);

        service.kickoutDevice(1L, 100L);

        stpStatic.verifyNoInteractions();
        verify(deviceMapper, never()).updateById(any(UserLoginDevice.class));
    }

    @Test
    @DisplayName("kickoutDevice：Sa-Token 抛异常时仍标记为 KICKED（容错幂等）")
    void kickoutDevice_satokenFailure() {
        UserLoginDevice device = new UserLoginDevice();
        device.setId(1L);
        device.setUserId(100L);
        device.setTokenValue("token-K");
        device.setStatus(UserLoginDevice.STATUS_ONLINE);
        when(deviceMapper.selectById(1L)).thenReturn(device);
        stpStatic.when(() -> StpUtil.kickoutByTokenValue("token-K"))
                .thenThrow(new RuntimeException("token expired"));

        service.kickoutDevice(1L, 100L);

        assertThat(device.getStatus()).isEqualTo(UserLoginDevice.STATUS_KICKED);
        verify(deviceMapper).updateById(device);
    }

    @Test
    @DisplayName("kickoutDevice：设备不存在抛业务异常")
    void kickoutDevice_notFound() {
        when(deviceMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.kickoutDevice(99L, 100L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("markOffline：在线设备置为离线")
    void markOffline_setsOffline() {
        UserLoginDevice device = new UserLoginDevice();
        device.setId(2L);
        device.setStatus(UserLoginDevice.STATUS_ONLINE);
        device.setTokenValue("t");
        when(deviceMapper.selectOne(any(Wrapper.class))).thenReturn(device);
        service.markOffline("t");
        assertThat(device.getStatus()).isEqualTo(UserLoginDevice.STATUS_OFFLINE);
        verify(deviceMapper).updateById(device);
    }

    @Test
    @DisplayName("listByUser：null userId 返回空列表")
    void listByUser_null() {
        assertThat(service.listByUser(null)).isEmpty();
        verify(deviceMapper, never()).selectList(any(Wrapper.class));
    }
}
