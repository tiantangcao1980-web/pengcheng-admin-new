package com.pengcheng.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤打卡记录实体（公司级假勤）
 *
 * <p>V1.0 Sprint C 增量字段：
 * <ul>
 *   <li>{@code clockInLng/clockInLat/clockInPhotoUrl/clockInFieldWork} 上班 GPS+照片+外勤标记</li>
 *   <li>{@code clockOutLng/clockOutLat/clockOutPhotoUrl/clockOutFieldWork} 下班 GPS+照片+外勤标记</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("attendance_record")
public class AttendanceRecord extends BaseEntity {

    private Long userId;
    private LocalDate attendanceDate;
    private LocalDateTime clockInTime;
    private String clockInLocation;
    /** 1-正常 2-迟到 */
    private Integer clockInStatus;
    private LocalDateTime clockOutTime;
    private String clockOutLocation;
    /** 1-正常 2-早退 */
    private Integer clockOutStatus;

    /** 上班打卡经度 */
    private BigDecimal clockInLng;
    /** 上班打卡纬度 */
    private BigDecimal clockInLat;
    /** 上班打卡照片URL */
    private String clockInPhotoUrl;
    /** 上班打卡是否外勤 0=内勤 1=外勤 */
    private Integer clockInFieldWork;

    /** 下班打卡经度 */
    private BigDecimal clockOutLng;
    /** 下班打卡纬度 */
    private BigDecimal clockOutLat;
    /** 下班打卡照片URL */
    private String clockOutPhotoUrl;
    /** 下班打卡是否外勤 0=内勤 1=外勤 */
    private Integer clockOutFieldWork;
}
