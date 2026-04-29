package com.pengcheng.oa.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.oa.template.entity.ApprovalTemplate;
import com.pengcheng.oa.template.mapper.ApprovalTemplateMapper;
import com.pengcheng.oa.template.service.ApprovalTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalTemplateServiceImpl implements ApprovalTemplateService {

    private final ApprovalTemplateMapper templateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(ApprovalTemplate template) {
        validate(template);
        if (template.getEnabled() == null) {
            template.setEnabled(1);
        }
        if (getByCode(template.getCode()) != null) {
            throw new IllegalArgumentException("审批模板编码已存在: " + template.getCode());
        }
        templateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(ApprovalTemplate template) {
        if (template.getId() == null) {
            throw new IllegalArgumentException("模板ID不能为空");
        }
        validate(template);
        templateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        templateMapper.deleteById(id);
    }

    @Override
    public ApprovalTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    @Override
    public ApprovalTemplate getByCode(String code) {
        if (code == null) return null;
        LambdaQueryWrapper<ApprovalTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalTemplate::getCode, code).last("LIMIT 1");
        return templateMapper.selectOne(wrapper);
    }

    @Override
    public List<ApprovalTemplate> listEnabled() {
        LambdaQueryWrapper<ApprovalTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalTemplate::getEnabled, 1)
                .orderByAsc(ApprovalTemplate::getCategory)
                .orderByAsc(ApprovalTemplate::getId);
        return templateMapper.selectList(wrapper);
    }

    @Override
    public List<ApprovalTemplate> listAll() {
        return templateMapper.selectList(new LambdaQueryWrapper<>());
    }

    private void validate(ApprovalTemplate template) {
        if (template.getCode() == null || template.getCode().isBlank()) {
            throw new IllegalArgumentException("模板编码不能为空");
        }
        if (template.getName() == null || template.getName().isBlank()) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
        if (template.getCategory() == null) {
            throw new IllegalArgumentException("模板分类不能为空");
        }
    }
}
