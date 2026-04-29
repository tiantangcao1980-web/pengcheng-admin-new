package com.pengcheng.bi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.bi.dto.DimensionDef;
import com.pengcheng.bi.dto.MetricDef;
import com.pengcheng.bi.model.entity.BiViewModel;
import com.pengcheng.bi.model.mapper.BiViewModelMapper;
import com.pengcheng.bi.service.BiViewModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * BI 视图模型服务实现。
 */
@Service
@RequiredArgsConstructor
public class BiViewModelServiceImpl implements BiViewModelService {

    private final BiViewModelMapper viewModelMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<BiViewModel> listEnabled() {
        return viewModelMapper.selectList(
                new LambdaQueryWrapper<BiViewModel>()
                        .eq(BiViewModel::getEnabled, 1)
                        .orderByAsc(BiViewModel::getId)
        );
    }

    @Override
    public BiViewModel findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return viewModelMapper.selectOne(
                new LambdaQueryWrapper<BiViewModel>()
                        .eq(BiViewModel::getCode, code)
                        .eq(BiViewModel::getEnabled, 1)
        );
    }

    @Override
    public List<DimensionDef> parseDimensions(BiViewModel viewModel) {
        if (viewModel == null || viewModel.getDimensionsJson() == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(viewModel.getDimensionsJson(),
                    new TypeReference<List<DimensionDef>>() {});
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse dimensions JSON for view: " + viewModel.getCode(), e);
        }
    }

    @Override
    public List<MetricDef> parseMetrics(BiViewModel viewModel) {
        if (viewModel == null || viewModel.getMetricsJson() == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(viewModel.getMetricsJson(),
                    new TypeReference<List<MetricDef>>() {});
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse metrics JSON for view: " + viewModel.getCode(), e);
        }
    }
}
