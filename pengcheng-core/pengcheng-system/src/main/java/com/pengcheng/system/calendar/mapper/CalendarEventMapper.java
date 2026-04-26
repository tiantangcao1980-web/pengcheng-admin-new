package com.pengcheng.system.calendar.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.pengcheng.common.annotation.DataScope;
import com.pengcheng.system.calendar.entity.CalendarEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日历事件 Mapper
 *
 * <p>V4.0 数据权限扩展：通过 {@link DataScope}（userAlias=user_id）。
 */
@Mapper
public interface CalendarEventMapper extends BaseMapper<CalendarEvent> {

    /** 分页查询日历事件（带数据权限过滤） */
    @Select("SELECT * FROM sys_calendar_event ${ew.customSqlSegment}")
    @DataScope(userAlias = "user_id")
    IPage<CalendarEvent> selectPageWithScope(IPage<CalendarEvent> page,
                                             @Param(Constants.WRAPPER) Wrapper<CalendarEvent> queryWrapper);

    /** 列表查询日历事件（带数据权限过滤） */
    @Select("SELECT * FROM sys_calendar_event ${ew.customSqlSegment}")
    @DataScope(userAlias = "user_id")
    List<CalendarEvent> selectListWithScope(@Param(Constants.WRAPPER) Wrapper<CalendarEvent> queryWrapper);


    @Select("SELECT * FROM sys_calendar_event WHERE user_id = #{userId} AND status = 1 " +
            "AND start_time >= #{rangeStart} AND start_time < #{rangeEnd} ORDER BY start_time")
    List<CalendarEvent> findByUserAndRange(Long userId, LocalDateTime rangeStart, LocalDateTime rangeEnd);

    @Select("SELECT * FROM sys_calendar_event WHERE reminder_sent = 0 AND status = 1 " +
            "AND start_time <= DATE_ADD(NOW(), INTERVAL reminder_minutes MINUTE) AND start_time > NOW()")
    List<CalendarEvent> findPendingReminders();
}
