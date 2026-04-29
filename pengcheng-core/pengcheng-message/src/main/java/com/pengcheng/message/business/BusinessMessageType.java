package com.pengcheng.message.business;

import lombok.Getter;

/**
 * 业务消息类型（V1.0 Sprint B 第 8 任务）
 *
 * 与基础 msgType（1文本/2图片/3文件/4语音/5视频）正交：
 *   - 基础 msgType 决定渲染容器
 *   - businessType 决定卡片业务语义（房产销售场景化）
 *
 * 6 种类型来自 ByteDesk 设计思想（仅借鉴枚举概念，自主实现，避免 AGPL 污染）：
 *   CARD     ：客户名片（销售相互转客户）
 *   LOCATION ：位置（外勤上报 / 楼盘踏勘点）
 *   GOODS    ：楼盘卡片（销售推荐房源给同事）
 *   FORM     ：表单（群内调研 / 看房问卷）
 *   CHOICE   ：快速选项（"今晚加班吗？是/否"）
 *   EVENT    ：系统事件嵌入（"张三加入群聊"）
 */
@Getter
public enum BusinessMessageType {

    CARD("客户名片"),
    LOCATION("位置"),
    GOODS("楼盘卡片"),
    FORM("表单"),
    CHOICE("快速选项"),
    EVENT("系统事件");

    private final String displayName;

    BusinessMessageType(String displayName) {
        this.displayName = displayName;
    }

    public static BusinessMessageType fromCode(String code) {
        if (code == null) return null;
        for (BusinessMessageType t : values()) {
            if (t.name().equalsIgnoreCase(code)) return t;
        }
        return null;
    }
}
