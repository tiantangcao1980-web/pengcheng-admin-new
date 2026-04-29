package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import com.pengcheng.system.ticket.dto.TicketActionDTO;
import com.pengcheng.system.ticket.dto.TicketCreateDTO;
import com.pengcheng.system.ticket.entity.SysTicket;
import com.pengcheng.system.ticket.entity.SysTicketLog;
import com.pengcheng.system.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工单管理 Controller（V1.0 周五上线 BLOCKER B1 修复）
 *
 * 9 个 endpoint 一一对应 TicketService 公共方法。
 * 状态机：CREATED → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED
 *         任意中间态可 → CANCELLED；CLOSED/CANCELLED 可 → reopen → IN_PROGRESS
 */
@RestController
@RequestMapping("/admin/ticket")
@RequiredArgsConstructor
@SaCheckLogin
public class TicketController {

    private final TicketService ticketService;

    /** 提单 */
    @PostMapping
    @SaCheckPermission("sys:ticket:create")
    @Log(title = "提交工单", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody TicketCreateDTO dto) {
        if (dto.getSubmitterId() == null) {
            dto.setSubmitterId(StpUtil.getLoginIdAsLong());
        }
        return Result.ok(ticketService.create(dto));
    }

    /** 分配 */
    @PostMapping("/{id}/assign")
    @SaCheckPermission("sys:ticket:assign")
    @Log(title = "工单分配", businessType = BusinessType.UPDATE)
    public Result<Void> assign(@PathVariable("id") Long id,
                               @RequestBody TicketActionDTO dto) {
        dto.setTicketId(id);
        if (dto.getOperatorId() == null) dto.setOperatorId(StpUtil.getLoginIdAsLong());
        ticketService.assign(dto);
        return Result.ok();
    }

    /** 受理人开始处理 */
    @PostMapping("/{id}/start")
    @SaCheckPermission("sys:ticket:handle")
    @Log(title = "工单开始处理", businessType = BusinessType.UPDATE)
    public Result<Void> start(@PathVariable("id") Long id) {
        TicketActionDTO dto = TicketActionDTO.builder()
                .ticketId(id)
                .operatorId(StpUtil.getLoginIdAsLong()).build();
        ticketService.start(dto);
        return Result.ok();
    }

    /** 处理过程回复（不变更状态） */
    @PostMapping("/{id}/reply")
    @SaCheckPermission("sys:ticket:handle")
    @Log(title = "工单回复", businessType = BusinessType.UPDATE)
    public Result<Void> reply(@PathVariable("id") Long id,
                              @RequestBody TicketActionDTO dto) {
        dto.setTicketId(id);
        if (dto.getOperatorId() == null) dto.setOperatorId(StpUtil.getLoginIdAsLong());
        ticketService.reply(dto);
        return Result.ok();
    }

    /** 标记已解决（待用户确认） */
    @PostMapping("/{id}/resolve")
    @SaCheckPermission("sys:ticket:handle")
    @Log(title = "工单标记解决", businessType = BusinessType.UPDATE)
    public Result<Void> resolve(@PathVariable("id") Long id,
                                @RequestBody TicketActionDTO dto) {
        dto.setTicketId(id);
        if (dto.getOperatorId() == null) dto.setOperatorId(StpUtil.getLoginIdAsLong());
        ticketService.resolve(dto);
        return Result.ok();
    }

    /** 关闭工单 */
    @PostMapping("/{id}/close")
    @SaCheckPermission("sys:ticket:close")
    @Log(title = "工单关闭", businessType = BusinessType.UPDATE)
    public Result<Void> close(@PathVariable("id") Long id) {
        TicketActionDTO dto = TicketActionDTO.builder()
                .ticketId(id)
                .operatorId(StpUtil.getLoginIdAsLong()).build();
        ticketService.close(dto);
        return Result.ok();
    }

    /** 取消工单 */
    @PostMapping("/{id}/cancel")
    @SaCheckPermission("sys:ticket:close")
    @Log(title = "工单取消", businessType = BusinessType.UPDATE)
    public Result<Void> cancel(@PathVariable("id") Long id,
                               @RequestBody TicketActionDTO dto) {
        dto.setTicketId(id);
        if (dto.getOperatorId() == null) dto.setOperatorId(StpUtil.getLoginIdAsLong());
        ticketService.cancel(dto);
        return Result.ok();
    }

    /** 重开工单（关闭/取消后重新激活） */
    @PostMapping("/{id}/reopen")
    @SaCheckPermission("sys:ticket:handle")
    @Log(title = "工单重开", businessType = BusinessType.UPDATE)
    public Result<Void> reopen(@PathVariable("id") Long id,
                               @RequestBody TicketActionDTO dto) {
        dto.setTicketId(id);
        if (dto.getOperatorId() == null) dto.setOperatorId(StpUtil.getLoginIdAsLong());
        ticketService.reopen(dto);
        return Result.ok();
    }

    /** 我的待办工单（提单人或受理人，状态非终态） */
    @GetMapping("/my-open")
    @SaCheckPermission("sys:ticket:list")
    public Result<List<SysTicket>> myOpen(@RequestParam(value = "userId", required = false) Long userId) {
        Long uid = userId == null ? StpUtil.getLoginIdAsLong() : userId;
        return Result.ok(ticketService.listMyOpen(uid));
    }

    /** 工单流转日志 */
    @GetMapping("/{id}/logs")
    @SaCheckPermission("sys:ticket:list")
    public Result<List<SysTicketLog>> logs(@PathVariable("id") Long id) {
        return Result.ok(ticketService.getLogs(id));
    }
}
