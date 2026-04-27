package com.pengcheng.system.i18n.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_locale_preference")
public class UserLocalePreference implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String locale;
    private String timezone;
    private String currency;
    private String dateFormat;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
