package com.pengcheng.system.doc.collab.ws;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单个协同编辑会话（一个 WebSocket 连接对应一个 Session）
 */
@Data
public class DocCollabSession {

    private Long docId;

    private Long userId;

    /** Spring WebSocket session ID */
    private String sessionId;

    private LocalDateTime joinedAt;

    /** 用户昵称（握手时从 JWT claims 或请求参数读取） */
    private String userName;

    public DocCollabSession(Long docId, Long userId, String sessionId, String userName) {
        this.docId = docId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.userName = userName;
        this.joinedAt = LocalDateTime.now();
    }
}
