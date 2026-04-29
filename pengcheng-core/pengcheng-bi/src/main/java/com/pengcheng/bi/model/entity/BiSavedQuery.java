package com.pengcheng.bi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户保存的 BI 查询（bi_saved_query）。
 */
@Data
@TableName("bi_saved_query")
public class BiSavedQuery {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 保存人 user_id */
    private Long userId;

    /** 视图编码 */
    private String viewCode;

    /** 查询名称 */
    private String name;

    /**
     * 查询参数 JSON（{dimensions, metrics, filters, sort, limit}）。
     * 对应 {@link com.pengcheng.bi.engine.BiQueryRequest} 序列化。
     */
    private String queryJson;

    private LocalDateTime createTime;
}
