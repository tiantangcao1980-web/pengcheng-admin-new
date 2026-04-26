package com.pengcheng.system.device.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.device.dto.DeviceRecordRequest;
import com.pengcheng.system.device.entity.UserLoginDevice;
import com.pengcheng.system.device.mapper.UserLoginDeviceMapper;
import com.pengcheng.system.device.service.UserLoginDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 用户登录设备服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginDeviceServiceImpl extends ServiceImpl<UserLoginDeviceMapper, UserLoginDevice>
        implements UserLoginDeviceService {

    @Override
    public UserLoginDevice recordLogin(DeviceRecordRequest request) {
        if (request == null || request.getUserId() == null || !StringUtils.hasText(request.getTokenValue())) {
            throw new BusinessException("登录设备记录参数缺失");
        }
        // 同 tokenValue 已存在则刷新最近活跃，否则插入新记录
        UserLoginDevice exist = getOne(new LambdaQueryWrapper<UserLoginDevice>()
                .eq(UserLoginDevice::getTokenValue, request.getTokenValue()));
        LocalDateTime now = LocalDateTime.now();
        if (exist != null) {
            exist.setLastActive(now);
            exist.setStatus(UserLoginDevice.STATUS_ONLINE);
            updateById(exist);
            return exist;
        }
        UserLoginDevice device = new UserLoginDevice();
        device.setUserId(request.getUserId());
        device.setTokenValue(request.getTokenValue());
        device.setClientType(StringUtils.hasText(request.getClientType()) ? request.getClientType() : "WEB");
        device.setDeviceId(request.getDeviceId());
        device.setDeviceName(request.getDeviceName());
        device.setOs(request.getOs());
        device.setBrowser(request.getBrowser());
        device.setIp(request.getIp());
        device.setLocation(request.getLocation());
        device.setLoginTime(now);
        device.setLastActive(now);
        device.setStatus(UserLoginDevice.STATUS_ONLINE);
        save(device);
        return device;
    }

    @Override
    public List<UserLoginDevice> listByUser(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<UserLoginDevice>()
                .eq(UserLoginDevice::getUserId, userId)
                .orderByDesc(UserLoginDevice::getLoginTime));
    }

    @Override
    public void kickoutDevice(Long deviceId, Long operatorId) {
        UserLoginDevice device = getById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        // 幂等：已离线/已踢则跳过 Sa-Token 调用，但仍刷新状态字段
        if (device.getStatus() != null && device.getStatus() == UserLoginDevice.STATUS_KICKED) {
            return;
        }
        try {
            StpUtil.kickoutByTokenValue(device.getTokenValue());
        } catch (Exception e) {
            // 已经过期/不存在的 token 可能抛异常，吞掉以保证幂等
            log.warn("Sa-Token kickoutByTokenValue 失败（可能已过期，可忽略）：tokenValue={}, msg={}",
                    device.getTokenValue(), e.getMessage());
        }
        device.setStatus(UserLoginDevice.STATUS_KICKED);
        updateById(device);
    }

    @Override
    public void markOffline(String tokenValue) {
        if (!StringUtils.hasText(tokenValue)) {
            return;
        }
        UserLoginDevice device = getOne(new LambdaQueryWrapper<UserLoginDevice>()
                .eq(UserLoginDevice::getTokenValue, tokenValue));
        if (device != null && device.getStatus() != null
                && device.getStatus() != UserLoginDevice.STATUS_OFFLINE) {
            device.setStatus(UserLoginDevice.STATUS_OFFLINE);
            updateById(device);
        }
    }
}
