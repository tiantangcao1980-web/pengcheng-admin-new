package com.pengcheng.system.tenant.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.tenant.dto.InviteCreateRequest;
import com.pengcheng.system.tenant.dto.InviteImportResult;
import com.pengcheng.system.tenant.dto.InviteImportRow;
import com.pengcheng.system.tenant.entity.TenantMemberInvite;
import com.pengcheng.system.tenant.mapper.TenantMemberInviteMapper;
import com.pengcheng.system.tenant.service.TenantMemberInviteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 租户成员邀请服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantMemberInviteServiceImpl extends ServiceImpl<TenantMemberInviteMapper, TenantMemberInvite>
        implements TenantMemberInviteService {

    private static final int DEFAULT_EXPIRE_HOURS = 72;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantMemberInvite createInvite(InviteCreateRequest request, Long inviterId) {
        if (request.getTenantId() == null) {
            throw new BusinessException("租户ID不能为空");
        }
        String channel = StringUtils.hasText(request.getChannel())
                ? request.getChannel()
                : TenantMemberInvite.CHANNEL_LINK;
        if (TenantMemberInvite.CHANNEL_SMS.equals(channel)) {
            if (!StringUtils.hasText(request.getPhone()) || !PHONE_PATTERN.matcher(request.getPhone()).matches()) {
                throw new BusinessException("短信邀请必须提供合法手机号");
            }
        }

        TenantMemberInvite invite = new TenantMemberInvite();
        invite.setTenantId(request.getTenantId());
        invite.setInviteCode(generateInviteCode());
        invite.setChannel(channel);
        invite.setPhone(request.getPhone());
        invite.setEmail(request.getEmail());
        invite.setDeptId(request.getDeptId());
        invite.setRoleIds(request.getRoleIds() == null ? null
                : request.getRoleIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
        invite.setInviterId(inviterId);
        int hours = request.getExpireHours() != null && request.getExpireHours() > 0
                ? request.getExpireHours() : DEFAULT_EXPIRE_HOURS;
        invite.setExpiresAt(LocalDateTime.now().plusHours(hours));
        invite.setStatus(TenantMemberInvite.STATUS_PENDING);
        save(invite);
        return invite;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InviteImportResult importInvites(Long tenantId, InputStream csv, Long inviterId) {
        if (tenantId == null) {
            throw new BusinessException("租户ID不能为空");
        }
        InviteImportResult result = new InviteImportResult();
        if (csv == null) {
            return result;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                // 跳过 BOM
                if (lineNo == 1 && trimmed.startsWith("﻿")) {
                    trimmed = trimmed.substring(1);
                }
                // 表头行：phone,...
                if (lineNo == 1 && trimmed.toLowerCase().startsWith("phone")) {
                    continue;
                }
                String[] cols = trimmed.split(",", -1);
                String phone = cols.length > 0 ? cols[0].trim() : null;
                String deptName = cols.length > 1 ? cols[1].trim() : null;
                String roleCode = cols.length > 2 ? cols[2].trim() : null;

                if (!StringUtils.hasText(phone) || !PHONE_PATTERN.matcher(phone).matches()) {
                    result.add(InviteImportRow.fail(lineNo, phone, "手机号格式不合法"));
                    continue;
                }

                // 重复检查（同租户 + 同手机 + 待接受）
                Long exists = baseMapper.selectCount(new LambdaQueryWrapper<TenantMemberInvite>()
                        .eq(TenantMemberInvite::getTenantId, tenantId)
                        .eq(TenantMemberInvite::getPhone, phone)
                        .eq(TenantMemberInvite::getStatus, TenantMemberInvite.STATUS_PENDING));
                if (exists != null && exists > 0) {
                    result.add(InviteImportRow.fail(lineNo, phone, "存在同手机号的待接受邀请"));
                    continue;
                }

                try {
                    TenantMemberInvite invite = new TenantMemberInvite();
                    invite.setTenantId(tenantId);
                    invite.setInviteCode(generateInviteCode());
                    invite.setChannel(TenantMemberInvite.CHANNEL_EXCEL);
                    invite.setPhone(phone);
                    invite.setInviterId(inviterId);
                    invite.setExpiresAt(LocalDateTime.now().plusHours(DEFAULT_EXPIRE_HOURS));
                    invite.setStatus(TenantMemberInvite.STATUS_PENDING);
                    // deptName / roleCode 仅作为备注存入 fail_reason 字段无意义，
                    // 业务层后续可在 acceptInvite 时再二次解析（避免在导入期跨模块查询）。
                    if (StringUtils.hasText(deptName) || StringUtils.hasText(roleCode)) {
                        invite.setFailReason("dept=" + deptName + "; role=" + roleCode);
                    }
                    save(invite);
                    result.add(InviteImportRow.ok(lineNo, phone, invite.getId()));
                } catch (Exception e) {
                    log.warn("导入邀请失败: line={}, phone={}", lineNo, phone, e);
                    result.add(InviteImportRow.fail(lineNo, phone, "落库失败: " + e.getMessage()));
                }
            }
        } catch (Exception e) {
            throw new BusinessException("解析 CSV 失败：" + e.getMessage());
        }
        return result;
    }

    @Override
    public List<TenantMemberInvite> listInvites(Long tenantId, Integer status) {
        if (tenantId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TenantMemberInvite> wrapper = new LambdaQueryWrapper<TenantMemberInvite>()
                .eq(TenantMemberInvite::getTenantId, tenantId)
                .orderByDesc(TenantMemberInvite::getCreateTime);
        if (status != null) {
            wrapper.eq(TenantMemberInvite::getStatus, status);
        }
        return list(wrapper);
    }

    @Override
    public void revokeInvite(Long id, Long operatorId) {
        TenantMemberInvite invite = getById(id);
        if (invite == null) {
            throw new BusinessException("邀请不存在");
        }
        if (invite.getStatus() == TenantMemberInvite.STATUS_ACCEPTED) {
            throw new BusinessException("已接受的邀请不可撤销");
        }
        invite.setStatus(TenantMemberInvite.STATUS_REVOKED);
        invite.setUpdateBy(operatorId);
        updateById(invite);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantMemberInvite acceptInvite(String code, Long acceptUserId) {
        TenantMemberInvite invite = getByCode(code);
        if (invite == null) {
            throw new BusinessException("邀请不存在");
        }
        if (invite.getStatus() == TenantMemberInvite.STATUS_REVOKED) {
            throw new BusinessException("邀请已被撤销");
        }
        if (invite.getStatus() == TenantMemberInvite.STATUS_ACCEPTED) {
            throw new BusinessException("邀请已被接受");
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(TenantMemberInvite.STATUS_EXPIRED);
            updateById(invite);
            throw new BusinessException("邀请已过期");
        }
        invite.setStatus(TenantMemberInvite.STATUS_ACCEPTED);
        invite.setAcceptedUserId(acceptUserId);
        invite.setAcceptedAt(LocalDateTime.now());
        updateById(invite);
        return invite;
    }

    @Override
    public TenantMemberInvite getByCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<TenantMemberInvite>()
                .eq(TenantMemberInvite::getInviteCode, code));
    }

    private String generateInviteCode() {
        String code;
        int retry = 0;
        do {
            code = IdUtil.simpleUUID().substring(0, 24);
            retry++;
            if (retry > 5) {
                throw new BusinessException("生成邀请码失败，请重试");
            }
        } while (getByCode(code) != null);
        return code;
    }
}
