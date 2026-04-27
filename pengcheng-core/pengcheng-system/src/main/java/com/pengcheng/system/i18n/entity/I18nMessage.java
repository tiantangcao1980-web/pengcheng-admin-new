package com.pengcheng.system.i18n.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("i18n_message")
public class I18nMessage implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String namespace;
    private String keyName;
    private String locale;
    private String valueText;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
