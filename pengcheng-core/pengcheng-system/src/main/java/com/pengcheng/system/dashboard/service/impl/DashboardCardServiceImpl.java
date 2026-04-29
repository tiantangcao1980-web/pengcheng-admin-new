package com.pengcheng.system.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.dashboard.dto.CardRenderResponse;
import com.pengcheng.system.dashboard.dto.RenderRequest;
import com.pengcheng.system.dashboard.entity.DashboardCardDef;
import com.pengcheng.system.dashboard.mapper.DashboardCardDefMapper;
import com.pengcheng.system.dashboard.registry.DashboardCardRegistry;
import com.pengcheng.system.dashboard.service.DashboardCardService;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 看板卡片服务实现。
 *
 * <h3>异常隔离机制</h3>
 * {@link #renderCard} 内部用 try-catch 包裹 provider.render() 调用，
 * 捕获 {@code Throwable}（含 Error 子类）后记录 WARN 日志，
 * 返回 {@code CardRenderResponse.error(meta, message)} 而非抛出异常，
 * 保证单张卡片故障不传染整个看板页面。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardCardServiceImpl implements DashboardCardService {

    private final DashboardCardRegistry registry;
    private final DashboardCardDefMapper cardDefMapper;

    @Override
    public CardRenderResponse renderCard(String code, RenderRequest request,
                                         Long userId, Long tenantId) {
        // 1. 查库获取 meta
        DashboardCardDef def = cardDefMapper.selectOne(
                new LambdaQueryWrapper<DashboardCardDef>()
                        .eq(DashboardCardDef::getCode, code)
                        .eq(DashboardCardDef::getEnabled, 1)
        );
        if (def == null) {
            log.warn("[DashboardCard] 卡片 [{}] 不存在或已禁用", code);
            DashboardCardDef placeholder = new DashboardCardDef();
            placeholder.setCode(code);
            placeholder.setName(code);
            return CardRenderResponse.error(placeholder, "卡片不存在或已禁用: " + code);
        }

        // 2. 查找 provider
        Optional<DashboardCardProvider> providerOpt = registry.findByCode(code);
        if (providerOpt.isEmpty()) {
            log.warn("[DashboardCard] 卡片 [{}] 没有对应的 provider 实现", code);
            return CardRenderResponse.error(def, "卡片 provider 未注册: " + code);
        }

        // 3. 角色校验：若 provider 要求特定角色，此处检查在 Controller 层已处理，
        //    Service 层做二次保险（调用方应传 roles，此处略，role 校验在 listForRoles）

        // 4. 构建上下文
        DashboardCardProvider.DashboardCardContext ctx = buildContext(request, userId, tenantId);

        // 5. 调用 render，异常隔离
        try {
            Object data = providerOpt.get().render(ctx);
            return CardRenderResponse.ok(def, data);
        } catch (Throwable t) {
            log.warn("[DashboardCard] 卡片 [{}] 渲染异常，已降级: {}", code, t.getMessage(), t);
            return CardRenderResponse.error(def, t.getMessage());
        }
    }

    @Override
    public List<DashboardCardDef> listAvailable() {
        return cardDefMapper.selectList(
                new LambdaQueryWrapper<DashboardCardDef>()
                        .eq(DashboardCardDef::getEnabled, 1)
                        .orderByAsc(DashboardCardDef::getId)
        );
    }

    @Override
    public List<DashboardCardDef> listForRoles(Set<String> roles) {
        // 先获取所有可见的 provider code（registry 做 role 过滤）
        List<DashboardCardProvider> applicable = registry.applicableForRoles(roles);
        Set<String> applicableCodes = applicable.stream()
                .map(DashboardCardProvider::code)
                .collect(Collectors.toSet());
        if (applicableCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return cardDefMapper.selectList(
                new LambdaQueryWrapper<DashboardCardDef>()
                        .eq(DashboardCardDef::getEnabled, 1)
                        .in(DashboardCardDef::getCode, applicableCodes)
                        .orderByAsc(DashboardCardDef::getId)
        );
    }

    // ---------------------------------------------------------------- 私有工具

    private DashboardCardProvider.DashboardCardContext buildContext(
            RenderRequest request, Long userId, Long tenantId) {
        LocalDateTime start = request != null ? request.getWindowStart() : null;
        LocalDateTime end = request != null ? request.getWindowEnd() : null;
        Map<String, Object> params = request != null ? request.getParams() : Collections.emptyMap();
        return new DashboardCardProvider.DashboardCardContext() {
            @Override public Long userId()   { return userId; }
            @Override public Long tenantId() { return tenantId; }
            @Override public LocalDateTime windowStart() { return start; }
            @Override public LocalDateTime windowEnd()   { return end; }
            @Override public Map<String, Object> params() {
                return params != null ? params : Collections.emptyMap();
            }
        };
    }
}
