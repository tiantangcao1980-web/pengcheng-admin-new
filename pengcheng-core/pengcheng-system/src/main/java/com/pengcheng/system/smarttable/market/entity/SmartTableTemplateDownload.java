package com.pengcheng.system.smarttable.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("smart_table_template_download")
public class SmartTableTemplateDownload implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long templateId;
    private Long userId;
    private Long targetTableId;
    private LocalDateTime createTime;
}
