package com.pengcheng.finance.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pengcheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 合同模板实体（contract_template）。
 * <p>
 * 存储合同起草所用的文本模板及变量声明，支持多业务类型复用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract_template")
public class ContractTemplate extends BaseEntity {

    /** 模板名称 */
    private String name;

    /** 业务类型，如 realty / crm / general */
    private String bizType;

    /** 模板正文，支持 {{变量}} 占位符 */
    private String content;

    /**
     * 变量定义 JSON 数组，示例：
     * <pre>[{"key":"partyA","label":"甲方名称"}]</pre>
     */
    private String variablesJson;

    /** 模板版本号，从 1 递增 */
    private Integer version;

    /** 是否启用：1=启用 0=停用 */
    private Integer active;
}
