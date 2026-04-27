package com.pengcheng.realty.unit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.unit.entity.HouseType;
import com.pengcheng.realty.unit.mapper.HouseTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 户型管理服务
 */
@Service
@RequiredArgsConstructor
public class HouseTypeService {

    private final HouseTypeMapper houseTypeMapper;

    /**
     * 创建户型
     *
     * @return 新增记录 ID
     */
    @Transactional
    public Long create(HouseType houseType) {
        // 同一楼盘内 code 唯一性由 DB UK 保障，此处仅校验必填
        if (houseType.getProjectId() == null) {
            throw new IllegalArgumentException("楼盘 ID 不能为空");
        }
        if (houseType.getCode() == null || houseType.getCode().isBlank()) {
            throw new IllegalArgumentException("户型代码不能为空");
        }
        houseTypeMapper.insert(houseType);
        return houseType.getId();
    }

    /**
     * 更新户型
     */
    @Transactional
    public void update(HouseType houseType) {
        if (houseType.getId() == null) {
            throw new IllegalArgumentException("户型 ID 不能为空");
        }
        houseTypeMapper.updateById(houseType);
    }

    /**
     * 删除户型（物理删除，调用前由业务层确认无关联房源）
     */
    @Transactional
    public void delete(Long id) {
        houseTypeMapper.deleteById(id);
    }

    /**
     * 获取户型详情
     */
    public HouseType getById(Long id) {
        return houseTypeMapper.selectById(id);
    }

    /**
     * 查询楼盘下所有户型（不过滤 enabled）
     */
    public List<HouseType> listByProject(Long projectId) {
        LambdaQueryWrapper<HouseType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HouseType::getProjectId, projectId)
               .orderByAsc(HouseType::getCode);
        return houseTypeMapper.selectList(wrapper);
    }

    /**
     * 查询楼盘下启用的户型
     */
    public List<HouseType> listEnabled(Long projectId) {
        LambdaQueryWrapper<HouseType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HouseType::getProjectId, projectId)
               .eq(HouseType::getEnabled, 1)
               .orderByAsc(HouseType::getCode);
        return houseTypeMapper.selectList(wrapper);
    }
}
