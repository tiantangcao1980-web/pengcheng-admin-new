package com.pengcheng.finance.contract.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合同版本历史（contract_version）。
 * <p>
 * 每次合同内容修改均生成一条版本记录，支持回溯与比对。
 * 本实体不继承 BaseEntity，因为无 update_by/update_time 字段（不可修改）。
 */
@Data
@TableName("contract_version")
public class ContractVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 合同 ID（contract.id） */
    private Long contractId;

    /** 版本号，从 1 递增 */
    private Integer version;

    /** 该版本合同全文快照 */
    private String content;

    /** 与上一版本的差异描述（unified diff 或摘要） */
    private String diff;

    /** 创建人 user_id */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
