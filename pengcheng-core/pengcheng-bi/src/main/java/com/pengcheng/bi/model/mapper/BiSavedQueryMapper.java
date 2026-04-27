package com.pengcheng.bi.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.bi.model.entity.BiSavedQuery;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户保存查询 Mapper。
 */
@Mapper
public interface BiSavedQueryMapper extends BaseMapper<BiSavedQuery> {
}
