package com.pengcheng.realty.commission.listener;

import com.pengcheng.message.entity.Notification;
import com.pengcheng.message.service.NotificationService;
import com.pengcheng.realty.commission.entity.Commission;
import com.pengcheng.realty.commission.event.CommissionApprovalEvent;
import com.pengcheng.realty.commission.mapper.CommissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 佣金审批通知订阅器（Sprint B 收尾）
 *
 * 监听 CommissionApprovalEvent，给"下一审批人/提单人"推站内通知。
 *
 * 推送规则：
 *   SUBMIT  → 通知主管（V1.0 暂未配置主管角色映射，留 TODO，先打 log）
 *   APPROVE 到 MANAGER_APPROVED → 通知财务
 *   APPROVE 到 FINANCE_APPROVED → 通知出纳
 *   APPROVE 到 PAID            → 通知提交人
 *   REJECT                     → 通知提交人
 *
 * 当前 V1.0 仅通知"提交人"（确定可达），其他角色待 RBAC 角色-用户映射配齐后补。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommissionApprovalNotificationListener {

    private final NotificationService notificationService;
    private final CommissionMapper commissionMapper;

    @Async
    @EventListener
    public void onApproval(CommissionApprovalEvent event) {
        try {
            Commission commission = commissionMapper.selectById(event.getCommissionId());
            if (commission == null) return;

            Long submitterId = commission.getSubmittedBy();
            if (submitterId == null) {
                log.debug("[CommissionApprovalNotification] commissionId={} 无 submitter，跳过通知",
                        event.getCommissionId());
                return;
            }

            String title = buildTitle(event);
            String content = buildContent(event, commission);

            Notification n = new Notification();
            n.setUserId(submitterId);
            n.setType(2);  // 2 = 审批类
            n.setBizType("commission_approval");
            n.setBizId(event.getCommissionId());
            n.setTitle(title);
            n.setContent(content);
            n.setReadStatus(0);
            n.setCreateTime(LocalDateTime.now());
            notificationService.createNotification(n);
        } catch (Exception e) {
            log.warn("[CommissionApprovalNotification] 推送失败 commissionId={}: {}",
                    event.getCommissionId(), e.getMessage());
        }
    }

    private String buildTitle(CommissionApprovalEvent event) {
        return switch (event.getAction()) {
            case CommissionApprovalEvent.ACTION_APPROVE -> "佣金审批通过";
            case CommissionApprovalEvent.ACTION_REJECT -> "佣金审批驳回";
            case CommissionApprovalEvent.ACTION_PAY -> "佣金已放款";
            case CommissionApprovalEvent.ACTION_SUBMIT -> "佣金已提交审批";
            default -> "佣金审批状态变更";
        };
    }

    private String buildContent(CommissionApprovalEvent event, Commission commission) {
        return String.format("佣金 #%d 由 %s 流转到 %s%s",
                commission.getId(),
                event.getFromNode(),
                event.getToNode(),
                event.getRemark() == null || event.getRemark().isBlank()
                        ? "" : "，备注：" + event.getRemark());
    }
}
