package com.pengcheng.integration.spi.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 企业 IM 卡片消息（textcard 类型）。
 */
@Data
@Accessors(chain = true)
public class ImCardMessage {

    /** 标题 */
    private String title;

    /** 描述/正文 */
    private String description;

    /** 跳转 URL */
    private String url;

    /** 按钮文字（企业微信为 btntxt，其他平台按钮内容） */
    private String btnTxt;
}
