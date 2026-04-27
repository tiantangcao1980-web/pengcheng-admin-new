package com.pengcheng.system.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pengcheng.system.dashboard.entity.DashboardCardDef;
import com.pengcheng.system.dashboard.entity.DashboardLayout;
import com.pengcheng.system.dashboard.mapper.DashboardCardDefMapper;
import com.pengcheng.system.dashboard.mapper.DashboardLayoutMapper;
import com.pengcheng.system.dashboard.service.DashboardLayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 看板布局服务实现。
 *
 * <h3>默认布局 Fallback 策略</h3>
 * 当数据库中不存在对应 owner 的默认布局时，自动生成：
 * 取前 6 个已启用卡片，按 2 列排列，每卡片使用 defaultCols×defaultRows 尺寸。
 * Fallback 布局不写库，仅内存生成，下次仍走同样逻辑（直到用户主动保存布局）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardLayoutServiceImpl implements DashboardLayoutService {

    private final DashboardLayoutMapper layoutMapper;
    private final DashboardCardDefMapper cardDefMapper;

    @Override
    public DashboardLayout getDefault(String ownerType, Long ownerId) {
        DashboardLayout layout = layoutMapper.selectOne(
                new LambdaQueryWrapper<DashboardLayout>()
                        .eq(DashboardLayout::getOwnerType, ownerType)
                        .eq(DashboardLayout::getOwnerId, ownerId)
                        .eq(DashboardLayout::getIsDefault, 1)
        );
        if (layout != null) {
            return layout;
        }
        // Fallback：生成内置默认布局
        return buildDefaultLayout(ownerType, ownerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLayout(DashboardLayout layout) {
        if (Integer.valueOf(1).equals(layout.getIsDefault())) {
            // 先将同 owner 其他布局的 isDefault 置 0
            layoutMapper.update(null,
                    new LambdaUpdateWrapper<DashboardLayout>()
                            .eq(DashboardLayout::getOwnerType, layout.getOwnerType())
                            .eq(DashboardLayout::getOwnerId, layout.getOwnerId())
                            .set(DashboardLayout::getIsDefault, 0)
            );
        }
        if (layout.getId() == null) {
            layoutMapper.insert(layout);
        } else {
            layoutMapper.updateById(layout);
        }
    }

    @Override
    public List<DashboardLayout> listByOwner(String ownerType, Long ownerId) {
        return layoutMapper.selectList(
                new LambdaQueryWrapper<DashboardLayout>()
                        .eq(DashboardLayout::getOwnerType, ownerType)
                        .eq(DashboardLayout::getOwnerId, ownerId)
                        .orderByAsc(DashboardLayout::getId)
        );
    }

    // ---------------------------------------------------------------- 私有工具

    /**
     * 内置默认布局：前 6 个已启用卡片，2 列排列。
     */
    private DashboardLayout buildDefaultLayout(String ownerType, Long ownerId) {
        List<DashboardCardDef> cards = cardDefMapper.selectList(
                new LambdaQueryWrapper<DashboardCardDef>()
                        .eq(DashboardCardDef::getEnabled, 1)
                        .orderByAsc(DashboardCardDef::getId)
                        .last("LIMIT 6")
        );

        StringBuilder json = new StringBuilder("[");
        int col = 0;
        int rowOffset = 0;
        for (int i = 0; i < cards.size(); i++) {
            DashboardCardDef card = cards.get(i);
            int w = card.getDefaultCols() != null ? card.getDefaultCols() : 4;
            int h = card.getDefaultRows() != null ? card.getDefaultRows() : 3;
            int x = col * 6;   // 2 列布局，每列宽 6
            int y = rowOffset;
            if (i > 0) json.append(",");
            json.append(String.format(
                    "{\"cardCode\":\"%s\",\"x\":%d,\"y\":%d,\"w\":%d,\"h\":%d}",
                    card.getCode(), x, y, w, h));
            col++;
            if (col >= 2) {
                col = 0;
                rowOffset += h;
            }
        }
        json.append("]");

        DashboardLayout fallback = new DashboardLayout();
        fallback.setOwnerType(ownerType);
        fallback.setOwnerId(ownerId);
        fallback.setName("默认");
        fallback.setLayoutJson(json.toString());
        fallback.setIsDefault(1);
        // fallback 不持久化，直接返回内存对象
        return fallback;
    }
}
