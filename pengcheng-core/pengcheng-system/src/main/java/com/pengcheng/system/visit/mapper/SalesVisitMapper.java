package com.pengcheng.system.visit.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.pengcheng.common.annotation.DataScope;
import com.pengcheng.system.visit.entity.SalesVisit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 销售拜访记录 Mapper
 *
 * <p>V4.0 数据权限扩展：通过 {@link DataScope} 注解 + {@code DataPermissionInterceptor}
 * 自动注入 WHERE 条件（dept_id / user_id 别名）。
 */
@Mapper
public interface SalesVisitMapper extends BaseMapper<SalesVisit> {

    /** 分页查询拜访列表（带数据权限过滤） */
    @Select("SELECT * FROM sys_sales_visit ${ew.customSqlSegment}")
    @DataScope(deptAlias = "dept_id", userAlias = "user_id")
    IPage<SalesVisit> selectPageWithScope(IPage<SalesVisit> page,
                                          @Param(Constants.WRAPPER) Wrapper<SalesVisit> queryWrapper);

    /** 列表查询拜访（带数据权限过滤） */
    @Select("SELECT * FROM sys_sales_visit ${ew.customSqlSegment}")
    @DataScope(deptAlias = "dept_id", userAlias = "user_id")
    List<SalesVisit> selectListWithScope(@Param(Constants.WRAPPER) Wrapper<SalesVisit> queryWrapper);


    /** 统计用户拜访数据 */
    @Select("""
        SELECT visit_type, COUNT(*) AS cnt
        FROM sys_sales_visit
        WHERE user_id = #{userId} AND deleted = 0
          AND visit_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        GROUP BY visit_type
        """)
    List<Map<String, Object>> countByTypeLastMonth(Long userId);

    /** 团队拜访排行 */
    @Select("""
        SELECT user_id, COUNT(*) AS visit_count
        FROM sys_sales_visit
        WHERE dept_id = #{deptId} AND deleted = 0
          AND visit_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        GROUP BY user_id
        ORDER BY visit_count DESC
        LIMIT 10
        """)
    List<Map<String, Object>> teamRanking(Long deptId);
}
