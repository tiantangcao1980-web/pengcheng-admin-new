package com.pengcheng.crm.lead.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.exception.BizErrorCode;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.crm.lead.dto.LeadCreateDTO;
import com.pengcheng.crm.lead.dto.PublicLeadSubmitDTO;
import com.pengcheng.crm.lead.entity.CrmLead;
import com.pengcheng.crm.lead.entity.CrmLeadForm;
import com.pengcheng.crm.lead.mapper.CrmLeadFormMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 线索采集表单 Service：管理后台 CRUD + 公开提交
 */
@Service
public class LeadFormService {

    @Autowired
    private CrmLeadFormMapper formMapper;

    @Autowired
    private LeadService leadService;

    public CrmLeadForm createForm(CrmLeadForm form) {
        if (form.getFormCode() == null || form.getFormCode().isBlank()) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "formCode 不能为空");
        }
        if (formMapper.selectCount(
                new LambdaQueryWrapper<CrmLeadForm>().eq(CrmLeadForm::getFormCode, form.getFormCode())) > 0) {
            throw new BusinessException(BizErrorCode.BUSINESS_ERROR, "formCode 已存在");
        }
        if (form.getEnabled() == null) form.setEnabled(1);
        if (form.getSubmitCount() == null) form.setSubmitCount(0);
        if (form.getDefaultSource() == null) form.setDefaultSource("form");
        formMapper.insert(form);
        return form;
    }

    public CrmLeadForm getByCode(String code) {
        CrmLeadForm form = formMapper.selectOne(
                new LambdaQueryWrapper<CrmLeadForm>().eq(CrmLeadForm::getFormCode, code));
        if (form == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "表单不存在");
        }
        return form;
    }

    /**
     * 公开提交：根据 formCode 创建一条线索（来源 = qrcode/form）。
     */
    @Transactional(rollbackFor = Exception.class)
    public CrmLead submit(PublicLeadSubmitDTO dto) {
        if (dto == null || dto.getFormCode() == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "formCode 不能为空");
        }
        CrmLeadForm form = getByCode(dto.getFormCode());
        if (form.getEnabled() == null || form.getEnabled() != 1) {
            throw new BusinessException(BizErrorCode.BUSINESS_ERROR, "表单已停用");
        }

        Map<String, Object> fields = dto.getFields();
        if (fields == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "fields 不能为空");
        }

        LeadCreateDTO leadDto = new LeadCreateDTO();
        leadDto.setName(stringOf(fields.get("name")));
        leadDto.setPhone(stringOf(fields.get("phone")));
        leadDto.setEmail(stringOf(fields.get("email")));
        leadDto.setWechat(stringOf(fields.get("wechat")));
        leadDto.setCompany(stringOf(fields.get("company")));
        leadDto.setRemark(stringOf(fields.get("remark")));
        leadDto.setSource(form.getDefaultSource());
        leadDto.setSourceDetail("form:" + form.getFormCode());
        leadDto.setOwnerId(form.getDefaultOwnerId());
        if (leadDto.getName() == null || leadDto.getName().isBlank()) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "name 字段必填");
        }
        CrmLead lead = leadService.create(leadDto);

        // 计数器 +1
        form.setSubmitCount((form.getSubmitCount() == null ? 0 : form.getSubmitCount()) + 1);
        formMapper.updateById(form);
        return lead;
    }

    private static String stringOf(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
