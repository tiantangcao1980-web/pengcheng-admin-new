package com.pengcheng.finance.contract.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.finance.contract.entity.ContractTemplate;

import java.util.List;

/**
 * 合同模板服务接口。
 * <p>
 * 提供模板的 CRUD 及版本管理能力，具体实现由 Phase 2 工单完成。
 */
public interface ContractTemplateService {

    /**
     * 创建合同模板。
     *
     * @param template 模板数据（name、bizType、content、variablesJson 必填）
     * @return 新创建的模板 ID
     */
    Long createTemplate(ContractTemplate template);

    /**
     * 更新合同模板内容（版本号自动递增）。
     *
     * @param template 包含 id 及变更字段
     */
    void updateTemplate(ContractTemplate template);

    /**
     * 启用或停用模板。
     *
     * @param id     模板 ID
     * @param active 1=启用 0=停用
     */
    void toggleActive(Long id, int active);

    /**
     * 逻辑删除模板。
     *
     * @param id 模板 ID
     */
    void deleteTemplate(Long id);

    /**
     * 按 ID 查询模板详情。
     *
     * @param id 模板 ID
     * @return 模板实体；不存在时返回 null
     */
    ContractTemplate getById(Long id);

    /**
     * 按业务类型查询可用模板列表。
     *
     * @param bizType 业务类型，null 则返回全部启用模板
     * @return 启用中的模板列表
     */
    List<ContractTemplate> listByBizType(String bizType);

    /**
     * 分页查询模板列表（含已停用）。
     *
     * @param bizType  业务类型过滤（可为 null）
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 分页结果
     */
    IPage<ContractTemplate> pageTemplates(String bizType, int pageNum, int pageSize);

    /**
     * 用变量值渲染模板，生成合同正文草稿。
     * <p>
     * 占位符格式：{@code {{变量名}}}，例如 {@code {{partyA}}}。
     *
     * @param templateId 模板 ID
     * @param variables  变量 key→value 映射（JSON 字符串）
     * @return 替换变量后的合同正文
     * @throws UnsupportedOperationException 模板引擎 Phase 2 待实现
     */
    String renderTemplate(Long templateId, String variables);

    /**
     * 导出模板为 PDF 字节数组。
     *
     * @param templateId 模板 ID
     * @param variables  变量 key→value 映射（JSON 字符串）
     * @return PDF 字节数组
     * @throws UnsupportedOperationException PDF 导出 Phase 2 待实现
     */
    byte[] exportToPdf(Long templateId, String variables);

    /**
     * 克隆已有模板，生成新的草稿模板（version 重置为 1）。
     *
     * @param sourceId 源模板 ID
     * @param newName  克隆后的名称
     * @return 新模板 ID
     */
    Long cloneTemplate(Long sourceId, String newName);
}
