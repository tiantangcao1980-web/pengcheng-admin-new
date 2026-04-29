package com.pengcheng.system.dashboard.service;

import com.pengcheng.system.dashboard.entity.DashboardLayout;

import java.util.List;

/**
 * 看板布局服务接口。
 */
public interface DashboardLayoutService {

    /**
     * 获取指定归属者的默认布局。
     *
     * <p>若数据库中不存在默认布局，则自动生成内置默认：
     * 取前 6 个已启用卡片，按 2 列自动排列（每卡片 defaultCols×defaultRows）。
     *
     * @param ownerType USER / ROLE
     * @param ownerId   归属 ID
     * @return 默认布局（不为 null）
     */
    DashboardLayout getDefault(String ownerType, Long ownerId);

    /**
     * 保存（新增或更新）布局。
     *
     * <p>若 {@code layout.isDefault == 1}，需先将同 ownerType+ownerId 的其他布局
     * 的 isDefault 置 0，再保存，避免违反唯一约束。
     *
     * @param layout 布局实体
     */
    void saveLayout(DashboardLayout layout);

    /**
     * 列出指定归属者的所有布局。
     *
     * @param ownerType USER / ROLE
     * @param ownerId   归属 ID
     * @return 布局列表，按 id 升序
     */
    List<DashboardLayout> listByOwner(String ownerType, Long ownerId);
}
