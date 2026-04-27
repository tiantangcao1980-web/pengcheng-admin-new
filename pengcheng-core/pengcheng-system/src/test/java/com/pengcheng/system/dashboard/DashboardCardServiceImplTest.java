package com.pengcheng.system.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.dashboard.dto.CardRenderResponse;
import com.pengcheng.system.dashboard.dto.RenderRequest;
import com.pengcheng.system.dashboard.entity.DashboardCardDef;
import com.pengcheng.system.dashboard.mapper.DashboardCardDefMapper;
import com.pengcheng.system.dashboard.registry.DashboardCardRegistry;
import com.pengcheng.system.dashboard.service.impl.DashboardCardServiceImpl;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardCardServiceImpl")
class DashboardCardServiceImplTest {

    @Mock
    private DashboardCardRegistry registry;

    @Mock
    private DashboardCardDefMapper cardDefMapper;

    private DashboardCardServiceImpl service;

    private DashboardCardDef salesDef;
    private DashboardCardProvider salesProvider;

    @BeforeEach
    void setUp() {
        service = new DashboardCardServiceImpl(registry, cardDefMapper);

        salesDef = new DashboardCardDef();
        salesDef.setId(1L);
        salesDef.setCode("sales.funnel");
        salesDef.setName("销售漏斗");
        salesDef.setEnabled(1);

        salesProvider = mock(DashboardCardProvider.class);
        when(salesProvider.code()).thenReturn("sales.funnel");
    }

    @Test
    @DisplayName("renderCard 正常：provider 返回业务数据，success=true")
    void should_render_card_successfully() {
        Map<String, Object> fakeData = Map.of("total", 100, "converted", 30);
        when(cardDefMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(salesDef);
        when(registry.findByCode("sales.funnel")).thenReturn(Optional.of(salesProvider));
        when(salesProvider.render(any())).thenReturn(fakeData);

        RenderRequest req = new RenderRequest();
        CardRenderResponse resp = service.renderCard("sales.funnel", req, 1L, 1L);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEqualTo(fakeData);
        assertThat(resp.getMeta().getCode()).isEqualTo("sales.funnel");
    }

    @Test
    @DisplayName("renderCard 异常隔离：provider 抛出 RuntimeException，success=false 含 error 信息")
    void should_isolate_provider_exception() {
        when(cardDefMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(salesDef);
        when(registry.findByCode("sales.funnel")).thenReturn(Optional.of(salesProvider));
        when(salesProvider.render(any())).thenThrow(new RuntimeException("数据库连接超时"));

        CardRenderResponse resp = service.renderCard("sales.funnel", null, 1L, 1L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getData()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> errMap = (Map<String, Object>) resp.getData();
        assertThat(errMap).containsKey("error");
        assertThat(errMap.get("error").toString()).contains("数据库连接超时");
    }

    @Test
    @DisplayName("renderCard 不存在的 code：def 为 null，返回 error 响应而非异常")
    void should_return_error_for_unknown_code() {
        when(cardDefMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        CardRenderResponse resp = service.renderCard("unknown.code", null, 1L, 1L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getData().toString()).contains("不存在或已禁用");
        verifyNoInteractions(registry);
    }

    @Test
    @DisplayName("renderCard provider 未注册：def 存在但 registry 无实现，返回 error")
    void should_return_error_when_provider_missing() {
        when(cardDefMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(salesDef);
        when(registry.findByCode("sales.funnel")).thenReturn(Optional.empty());

        CardRenderResponse resp = service.renderCard("sales.funnel", null, 1L, 1L);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getData().toString()).contains("provider 未注册");
    }
}
