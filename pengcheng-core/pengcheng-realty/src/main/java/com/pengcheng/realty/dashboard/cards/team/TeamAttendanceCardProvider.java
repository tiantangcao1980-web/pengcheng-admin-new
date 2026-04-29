package com.pengcheng.realty.dashboard.cards.team;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.hr.attendance.entity.AttendanceRecord;
import com.pengcheng.hr.attendance.mapper.AttendanceRecordMapper;
import com.pengcheng.system.dashboard.spi.DashboardCardProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 今日考勤打卡率卡片
 * <p>打卡率 = 今日已打卡记录数 / 全体员工数（近似：取近 30 天有记录的不重复用户数）
 */
@Component
@RequiredArgsConstructor
public class TeamAttendanceCardProvider implements DashboardCardProvider {

    private final AttendanceRecordMapper attendanceRecordMapper;

    @Override
    public String code() {
        return "team.attendance";
    }

    @Override
    public DashboardCardMetadata metadata() {
        return new DashboardCardMetadata() {
            public String name()           { return "今日考勤打卡率"; }
            public String category()       { return "team"; }
            public Set<String> applicableRoles() { return Set.of("manager", "admin"); }
            public int defaultCols()       { return 4; }
            public int defaultRows()       { return 2; }
            public String suggestedChart() { return "number"; }
            public String description()    { return "今日已打卡人数及打卡率"; }
        };
    }

    @Override
    public Object render(DashboardCardContext ctx) {
        LocalDate today = LocalDate.now();

        // 今日打卡数
        long todayCheckedIn = attendanceRecordMapper.selectCount(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getAttendanceDate, today)
                .isNotNull(AttendanceRecord::getClockInTime));

        // 活跃员工基数：取近 30 天有考勤记录的不重复人数（近似分母）
        long activeEmployees = attendanceRecordMapper.selectCount(new LambdaQueryWrapper<AttendanceRecord>()
                .ge(AttendanceRecord::getAttendanceDate, today.minusDays(30)));

        // 去重近似：直接用 todayCheckedIn 展示，率用 min(1.0) 处理
        double rate = activeEmployees == 0 ? 0.0
                : Math.min(100.0, Math.round(todayCheckedIn * 10000.0 / activeEmployees) / 100.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedIn", todayCheckedIn);
        result.put("rate", rate);
        result.put("unit", "%");
        return result;
    }
}
