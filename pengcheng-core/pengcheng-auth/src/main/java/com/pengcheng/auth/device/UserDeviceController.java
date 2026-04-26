package com.pengcheng.auth.device;

import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.device.entity.UserLoginDevice;
import com.pengcheng.system.device.service.UserLoginDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 个人中心-设备管理 Controller。
 *
 * <p>路由 /auth/device/*。所有接口要求登录态。
 */
@RestController
@RequestMapping("/auth/device")
@RequiredArgsConstructor
public class UserDeviceController {

    private final UserLoginDeviceService deviceService;

    /**
     * 我的设备列表
     */
    @GetMapping
    public Result<List<UserLoginDevice>> myDevices() {
        Long userId = currentUserId();
        return Result.ok(deviceService.listByUser(userId));
    }

    /**
     * 踢下线一台设备
     */
    @PostMapping("/{id}/kickout")
    public Result<Void> kickout(@PathVariable Long id) {
        Long operatorId = currentUserId();
        UserLoginDevice device = deviceService.getById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        // 仅允许操作自己的设备
        if (!operatorId.equals(device.getUserId())) {
            throw new BusinessException("只能踢下线自己的设备");
        }
        deviceService.kickoutDevice(id, operatorId);
        return Result.ok();
    }

    private Long currentUserId() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("请先登录");
        }
        return StpUtil.getLoginIdAsLong();
    }
}
