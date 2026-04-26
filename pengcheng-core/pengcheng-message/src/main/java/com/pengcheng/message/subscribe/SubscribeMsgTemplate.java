package com.pengcheng.message.subscribe;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小程序订阅消息模板（V23 / subscribe_msg_template）
 *
 * <p>用于把业务事件（审批状态变更/客户提醒等）映射到具体的微信小程序订阅消息模板，
 * 在不同微信主体下复用同一段业务代码。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("subscribe_msg_template")
public class SubscribeMsgTemplate implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 业务类型（与 ChannelPushRequest.bizType 对齐） */
    private String bizType;

    /** 业务子事件，例如 approval/created、approval/passed */
    private String eventCode;

    /** 微信小程序模板 ID（用户在公众平台申请） */
    private String templateId;

    /** 模板字段映射 JSON：业务字段 -> 模板字段名（thing1.value 等），用于动态渲染 */
    private String fieldMappingJson;

    /** 默认跳转页面 */
    private String defaultPage;

    /** 是否启用：0 否 1 是 */
    private Integer enabled;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
