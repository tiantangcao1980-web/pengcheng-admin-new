package com.pengcheng.system.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pengcheng.system.device.dto.DeviceRecordRequest;
import com.pengcheng.system.device.entity.UserLoginDevice;

import java.util.List;

/**
 * 用户登录设备服务
 */
public interface UserLoginDeviceService extends IService<UserLoginDevice> {

    /**
     * 登录后写入或刷新一条设备记录（按 tokenValue 唯一）。
     */
    UserLoginDevice recordLogin(DeviceRecordRequest request);

    /**
     * 列出某用户的全部设备（含离线）
     */
    List<UserLoginDevice> listByUser(Long userId);

    /**
     * 踢下线一台设备：调用 Sa-Token kickoutByTokenValue + 标记 status=2。幂等。
     */
    void kickoutDevice(Long deviceId, Long operatorId);

    /**
     * 主动登出（用户自己点退出）：标记 status=0
     */
    void markOffline(String tokenValue);
}
