package com.pengcheng.system.meeting.booking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.meeting.booking.entity.MeetingBooking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议预订 Mapper（Phase 4 J5）
 */
@Mapper
public interface MeetingBookingMapper extends BaseMapper<MeetingBooking> {

    /**
     * 检测时间冲突：同一会议室，状态非取消，时段重叠
     * 排除 excludeId（编辑场景，新建传 null）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM meeting_booking " +
            "WHERE room_id = #{roomId} AND status &lt;&gt; 3 " +
            "AND NOT (end_time &lt;= #{start} OR start_time &gt;= #{end})" +
            "<if test='excludeId != null'> AND id &lt;&gt; #{excludeId}</if>" +
            "</script>")
    int countConflict(@Param("roomId") Long roomId,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end,
                      @Param("excludeId") Long excludeId);

    /**
     * 按会议室 + 时段查询预订列表
     */
    @Select("SELECT * FROM meeting_booking WHERE room_id = #{roomId} AND status &lt;&gt; 3 " +
            "AND NOT (end_time &lt;= #{start} OR start_time &gt;= #{end}) ORDER BY start_time")
    List<MeetingBooking> listByRoomAndTimeRange(@Param("roomId") Long roomId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);
}
