package com.pengcheng.hr.okr.service;

import com.pengcheng.hr.okr.entity.OkrPeriod;

import java.util.List;

/**
 * OKR 周期服务
 */
public interface OkrPeriodService {

    /**
     * 查询所有进行中（status=1）的周期
     */
    List<OkrPeriod> listActive();

    /**
     * 查询所有周期
     */
    List<OkrPeriod> listAll();

    /**
     * 创建周期（status 默认草稿）
     */
    Long create(OkrPeriod period);

    /**
     * 将周期状态推进为「进行中」
     */
    void activatePeriod(Long id);

    /**
     * 关闭周期（status = 2），已结束后目标不允许再写入
     */
    void closePeriod(Long id);
}
