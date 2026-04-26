package com.pengcheng.system.smarttable.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.pengcheng.common.annotation.DataScope;
import com.pengcheng.system.smarttable.entity.SmartTableRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 智能表格记录 Mapper
 *
 * <p>V4.0 数据权限扩展：通过 {@link DataScope}（userAlias=create_by）。
 */
@Mapper
public interface SmartTableRecordMapper extends BaseMapper<SmartTableRecord> {

    /** 分页查询智能表格记录（带数据权限过滤） */
    @Select("SELECT * FROM smart_table_record ${ew.customSqlSegment}")
    @DataScope(userAlias = "create_by")
    IPage<SmartTableRecord> selectPageWithScope(IPage<SmartTableRecord> page,
                                                @Param(Constants.WRAPPER) Wrapper<SmartTableRecord> queryWrapper);

    /** 列表查询智能表格记录（带数据权限过滤） */
    @Select("SELECT * FROM smart_table_record ${ew.customSqlSegment}")
    @DataScope(userAlias = "create_by")
    List<SmartTableRecord> selectListWithScope(@Param(Constants.WRAPPER) Wrapper<SmartTableRecord> queryWrapper);
}
