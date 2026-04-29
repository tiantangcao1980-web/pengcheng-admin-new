package com.pengcheng.oa.shift.service;

import com.pengcheng.oa.shift.entity.AttendanceShift;

import java.util.List;

/**
 * 班次模板 CRUD 服务。
 */
public interface AttendanceShiftService {

    Long createShift(AttendanceShift shift);

    void updateShift(AttendanceShift shift);

    void deleteShift(Long id);

    AttendanceShift getById(Long id);

    List<AttendanceShift> listEnabled();

    List<AttendanceShift> listAll();
}
