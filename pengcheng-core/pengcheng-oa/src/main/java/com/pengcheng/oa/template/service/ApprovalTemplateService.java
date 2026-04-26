package com.pengcheng.oa.template.service;

import com.pengcheng.oa.template.entity.ApprovalTemplate;

import java.util.List;

public interface ApprovalTemplateService {

    Long createTemplate(ApprovalTemplate template);

    void updateTemplate(ApprovalTemplate template);

    void deleteTemplate(Long id);

    ApprovalTemplate getById(Long id);

    ApprovalTemplate getByCode(String code);

    List<ApprovalTemplate> listEnabled();

    List<ApprovalTemplate> listAll();
}
