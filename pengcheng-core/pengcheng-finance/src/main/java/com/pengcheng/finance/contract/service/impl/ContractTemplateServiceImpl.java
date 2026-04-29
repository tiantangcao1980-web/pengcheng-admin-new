package com.pengcheng.finance.contract.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.finance.contract.entity.ContractTemplate;
import com.pengcheng.finance.contract.mapper.ContractTemplateMapper;
import com.pengcheng.finance.contract.service.ContractTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 合同模板服务实现（Phase 2 骨架占位）。
 * <p>
 * 所有方法在 Phase 2 工单中落地实现，当前抛出 {@link UnsupportedOperationException}。
 */
@Service
@RequiredArgsConstructor
public class ContractTemplateServiceImpl implements ContractTemplateService {

    private final ContractTemplateMapper contractTemplateMapper;

    @Override
    public Long createTemplate(ContractTemplate template) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同模板创建");
    }

    @Override
    public void updateTemplate(ContractTemplate template) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同模板更新");
    }

    @Override
    public void toggleActive(Long id, int active) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同模板启停");
    }

    @Override
    public void deleteTemplate(Long id) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同模板删除");
    }

    @Override
    public ContractTemplate getById(Long id) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同模板查询");
    }

    @Override
    public List<ContractTemplate> listByBizType(String bizType) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同模板列表查询");
    }

    @Override
    public IPage<ContractTemplate> pageTemplates(String bizType, int pageNum, int pageSize) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同模板分页查询");
    }

    @Override
    public String renderTemplate(Long templateId, String variables) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同模板渲染（变量替换）");
    }

    @Override
    public byte[] exportToPdf(Long templateId, String variables) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同模板 PDF 导出");
    }

    @Override
    public Long cloneTemplate(Long sourceId, String newName) {
        throw new UnsupportedOperationException("Phase 2 待实现：合同模板克隆");
    }
}
