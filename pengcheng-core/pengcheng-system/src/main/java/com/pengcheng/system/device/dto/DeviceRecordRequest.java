package com.pengcheng.system.device.dto;

import lombok.Data;

/**
 * 注册一台登录设备
 */
@Data
public class DeviceRecordRequest {

    private Long userId;
    private String tokenValue;
    private String clientType;
    private String deviceId;
    private String deviceName;
    private String os;
    private String browser;
    private String ip;
    private String location;
}
