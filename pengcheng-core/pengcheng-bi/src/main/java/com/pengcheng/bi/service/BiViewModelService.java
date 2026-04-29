package com.pengcheng.bi.service;

import com.pengcheng.bi.dto.DimensionDef;
import com.pengcheng.bi.dto.MetricDef;
import com.pengcheng.bi.model.entity.BiViewModel;

import java.util.List;

/**
 * BI 视图模型服务接口。
 */
public interface BiViewModelService {

    /**
     * 查询所有已启用的视图模型列表。
     *
     * @return 启用的视图模型列表
     */
    List<BiViewModel> listEnabled();

    /**
     * 按 code 查找视图模型。
     *
     * @param code 视图编码
     * @return 视图模型实体；不存在时返回 null
     */
    BiViewModel findByCode(String code);

    /**
     * 解析视图模型的维度定义列表。
     *
     * @param viewModel 视图模型实体
     * @return 维度定义列表
     */
    List<DimensionDef> parseDimensions(BiViewModel viewModel);

    /**
     * 解析视图模型的指标定义列表。
     *
     * @param viewModel 视图模型实体
     * @return 指标定义列表
     */
    List<MetricDef> parseMetrics(BiViewModel viewModel);
}
