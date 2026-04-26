package com.pengcheng.ai.copilot.action.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Copilot 对话动作操作日志。
 *
 * <p>状态机：PENDING -&gt; CONFIRMED -&gt; EXECUTED；其中 CANCELLED/FAILED 为终态。
 */
@Data
@TableName("ai_copilot_action_log")
public class AiCopilotActionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String conversationId;

    private Long userId;

    private String pagePath;

    private String actionCode;

    private String payload;

    private String confirmToken;

    private String status;

    private String resultSummary;

    private String errorMessage;

    private LocalDateTime executedAt;

    private LocalDateTime createTime;

    /** MVP 三个动作编码 */
    public static final class Code {
        public static final String FOLLOW_UP_CREATE = "FOLLOW_UP_CREATE";
        public static final String TODO_CREATE = "TODO_CREATE";
        public static final String APPROVAL_SUBMIT = "APPROVAL_SUBMIT";

        private Code() {
        }
    }

    /** 状态常量 */
    public static final class Status {
        public static final String PENDING = "PENDING";
        public static final String CONFIRMED = "CONFIRMED";
        public static final String EXECUTED = "EXECUTED";
        public static final String CANCELLED = "CANCELLED";
        public static final String FAILED = "FAILED";

        private Status() {
        }
    }
}
