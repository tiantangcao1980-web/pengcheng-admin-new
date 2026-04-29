package com.pengcheng.realty.sop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 带看 SOP 文档模板实体
 * <p>
 * code UNIQUE：visit_confirm / commission_tripartite
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("realty_sop_template")
public class RealtySopTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板代码（唯一标识），如 visit_confirm / commission_tripartite */
    private String code;

    /** 模板名称 */
    private String name;

    /** HTML 内容，含 {{var}} 双花括号占位符 */
    private String contentHtml;

    /** 变量说明 JSON 数组：[{key, label, sample}] */
    private String variables;

    /** 是否启用：1-启用 0-停用 */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
