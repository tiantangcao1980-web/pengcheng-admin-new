package com.pengcheng.realty.sop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.realty.sop.entity.RealtySopTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 带看 SOP 模板 Mapper
 */
@Mapper
public interface RealtySopTemplateMapper extends BaseMapper<RealtySopTemplate> {

    /**
     * 按 code 查询模板（UNIQUE 字段）
     */
    @Select("SELECT * FROM realty_sop_template WHERE code = #{code} AND enabled = 1 LIMIT 1")
    RealtySopTemplate selectByCode(@Param("code") String code);
}
