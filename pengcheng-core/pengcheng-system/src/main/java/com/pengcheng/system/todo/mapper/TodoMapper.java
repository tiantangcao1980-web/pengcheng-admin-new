package com.pengcheng.system.todo.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.pengcheng.common.annotation.DataScope;
import com.pengcheng.system.todo.entity.Todo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 待办 Mapper
 *
 * <p>V4.0 数据权限扩展：通过 {@link DataScope} 注解使待办列表支持
 * 仅本人 / 本部门 / 本部门及下级 / 全部四档过滤。
 */
@Mapper
public interface TodoMapper extends BaseMapper<Todo> {

    @Select("SELECT COUNT(*) FROM sys_todo WHERE user_id = #{userId} AND status IN (0, 1)")
    int countPending(Long userId);

    /** 分页查询待办（带数据权限过滤） */
    @Select("SELECT * FROM sys_todo ${ew.customSqlSegment}")
    @DataScope(userAlias = "user_id")
    IPage<Todo> selectPageWithScope(IPage<Todo> page, @Param(Constants.WRAPPER) Wrapper<Todo> queryWrapper);

    /** 列表查询待办（带数据权限过滤） */
    @Select("SELECT * FROM sys_todo ${ew.customSqlSegment}")
    @DataScope(userAlias = "user_id")
    List<Todo> selectListWithScope(@Param(Constants.WRAPPER) Wrapper<Todo> queryWrapper);
}
