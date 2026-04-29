package com.pengcheng.system.openapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.openapi.entity.OpenapiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OpenapiKeyMapper extends BaseMapper<OpenapiKey> {

    @Select("SELECT * FROM openapi_key WHERE access_key = #{ak} AND enabled = 1 LIMIT 1")
    OpenapiKey findByAccessKey(@Param("ak") String accessKey);
}
