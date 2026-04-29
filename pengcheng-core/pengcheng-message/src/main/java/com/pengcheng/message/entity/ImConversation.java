package com.pengcheng.message.entity;

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
 * IM 会话索引表（一行一"用户视角的会话"，A 与 B 单聊会有两行）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("im_conversation")
public class ImConversation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID（min(uid,uid)_max(uid,uid) 或 group_xxx） */
    private String conversationId;

    private Long ownerId;

    private Long peerId;

    /** 1单聊 2群聊 */
    private Integer peerType;

    private Long lastMsgId;

    private String lastMsgContent;

    private LocalDateTime lastMsgTime;

    /** 未读数（Redis 权威，DB 兜底） */
    private Integer unreadCount;

    private Integer pinned;

    private Integer muted;

    private String extra;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static final int PEER_SINGLE = 1;
    public static final int PEER_GROUP = 2;
}
