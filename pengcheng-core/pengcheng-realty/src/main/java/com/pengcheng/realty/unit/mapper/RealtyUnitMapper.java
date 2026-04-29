package com.pengcheng.realty.unit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.unit.entity.RealtyUnit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 房源 Mapper 接口
 */
@Mapper
public interface RealtyUnitMapper extends BaseMapper<RealtyUnit> {

    /**
     * 原子加锁：仅在 AVAILABLE 且未被锁定（或锁已过期）时才更新。
     * 返回影响行数：1 成功 / 0 失败（已被锁）
     */
    @Update("UPDATE realty_unit " +
            "SET locked_by = #{userId}, locked_until = #{lockedUntil} " +
            "WHERE id = #{unitId} " +
            "  AND status = 'AVAILABLE' " +
            "  AND (locked_until IS NULL OR locked_until < NOW())")
    int tryLock(@Param("unitId") Long unitId,
                @Param("userId") Long userId,
                @Param("lockedUntil") java.time.LocalDateTime lockedUntil);

    /**
     * 解锁房源
     */
    @Update("UPDATE realty_unit SET locked_by = NULL, locked_until = NULL WHERE id = #{unitId}")
    int unlock(@Param("unitId") Long unitId);
}
