package com.pengcheng.system.smarttable.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.smarttable.market.entity.SmartTableTemplateRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SmartTableTemplateRatingMapper extends BaseMapper<SmartTableTemplateRating> {

    /** 同步聚合到 smart_table_template (rating_count + rating_sum)。 */
    @Update("UPDATE smart_table_template t " +
            "SET rating_count = (SELECT COUNT(*) FROM smart_table_template_rating WHERE template_id = #{tplId}), " +
            "    rating_sum   = (SELECT COALESCE(SUM(rating),0) FROM smart_table_template_rating WHERE template_id = #{tplId}) " +
            "WHERE t.id = #{tplId}")
    int recomputeAggregate(@Param("tplId") Long templateId);
}
