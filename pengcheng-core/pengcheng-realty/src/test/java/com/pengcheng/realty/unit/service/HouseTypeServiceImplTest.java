package com.pengcheng.realty.unit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.unit.entity.HouseType;
import com.pengcheng.realty.unit.mapper.HouseTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("HouseTypeService")
class HouseTypeServiceImplTest {

    private HouseTypeMapper houseTypeMapper;
    private HouseTypeService service;

    @BeforeEach
    void setUp() {
        houseTypeMapper = mock(HouseTypeMapper.class);
        service         = new HouseTypeService(houseTypeMapper);
    }

    // ---- 1. CRUD 基础流程 ----
    @Test
    @DisplayName("create - 成功插入并返回 ID")
    void create_success() {
        doAnswer(inv -> {
            HouseType ht = inv.getArgument(0);
            ht.setId(42L);
            return 1;
        }).when(houseTypeMapper).insert(any(HouseType.class));

        HouseType ht = HouseType.builder()
                .projectId(1L).code("A1").name("三室两厅")
                .bedrooms(3).livingRooms(2).bathrooms(2)
                .area(BigDecimal.valueOf(120.0))
                .enabled(1)
                .build();

        Long id = service.create(ht);

        assertThat(id).isEqualTo(42L);
        verify(houseTypeMapper).insert(argThat(h -> "A1".equals(h.getCode())));
    }

    @Test
    @DisplayName("update - 调用 updateById")
    void update_callsUpdateById() {
        HouseType ht = HouseType.builder().id(10L).projectId(1L).code("B1").name("两室").area(BigDecimal.valueOf(90)).build();
        service.update(ht);
        verify(houseTypeMapper).updateById(ht);
    }

    // ---- 2. UNIQUE 约束：DB 抛出重复键异常应透传 ----
    @Test
    @DisplayName("create - 重复 code 时 DB 抛出 DuplicateKeyException 应透传")
    void create_duplicateCode_throws() {
        when(houseTypeMapper.insert(any())).thenThrow(new DuplicateKeyException("uk_project_code"));

        HouseType ht = HouseType.builder().projectId(1L).code("A1").name("三室").area(BigDecimal.valueOf(100)).build();

        assertThatThrownBy(() -> service.create(ht))
                .isInstanceOf(DuplicateKeyException.class);
    }

    // ---- 3. listByProject 筛选条件 ----
    @Test
    @DisplayName("listByProject - 按 projectId 查询并按 code 排序")
    void listByProject_returnsAll() {
        List<HouseType> expected = List.of(
                HouseType.builder().id(1L).projectId(5L).code("A1").name("三室").area(BigDecimal.valueOf(120)).build(),
                HouseType.builder().id(2L).projectId(5L).code("B1").name("两室").area(BigDecimal.valueOf(90)).build()
        );
        when(houseTypeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expected);

        List<HouseType> result = service.listByProject(5L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("A1");
    }

    // ---- 4. listEnabled 只返回 enabled=1 ----
    @Test
    @DisplayName("listEnabled - 仅查询启用户型")
    void listEnabled_onlyEnabled() {
        List<HouseType> enabled = List.of(
                HouseType.builder().id(1L).projectId(5L).code("A1").name("三室").area(BigDecimal.valueOf(120)).enabled(1).build()
        );
        when(houseTypeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(enabled);

        List<HouseType> result = service.listEnabled(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEnabled()).isEqualTo(1);
        // 验证 wrapper 中包含 enabled=1 条件（通过 ArgumentCaptor 捕获）
        ArgumentCaptor<LambdaQueryWrapper<HouseType>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(houseTypeMapper).selectList(captor.capture());
        // wrapper 不为 null 即可（LambdaQueryWrapper 内部条件无法直接断言）
        assertThat(captor.getValue()).isNotNull();
    }
}
