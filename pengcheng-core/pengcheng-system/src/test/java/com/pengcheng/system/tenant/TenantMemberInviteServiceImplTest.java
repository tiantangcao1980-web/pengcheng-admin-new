package com.pengcheng.system.tenant;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.system.tenant.dto.InviteCreateRequest;
import com.pengcheng.system.tenant.dto.InviteImportResult;
import com.pengcheng.system.tenant.entity.TenantMemberInvite;
import com.pengcheng.system.tenant.mapper.TenantMemberInviteMapper;
import com.pengcheng.system.tenant.service.impl.TenantMemberInviteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantMemberInviteServiceImpl")
class TenantMemberInviteServiceImplTest {

    @Mock
    private TenantMemberInviteMapper inviteMapper;

    private TenantMemberInviteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new TenantMemberInviteServiceImpl());
        ReflectionTestUtils.setField(service, "baseMapper", inviteMapper);
        // 默认：getByCode 返回 null（生成的邀请码不冲突）
        lenient().when(inviteMapper.selectOne(any(Wrapper.class))).thenReturn(null);
    }

    @Test
    @DisplayName("createInvite SMS：手机号合法时创建并设置过期时间")
    void createInvite_sms_success() {
        InviteCreateRequest req = new InviteCreateRequest();
        req.setTenantId(1L);
        req.setChannel("SMS");
        req.setPhone("13800138000");
        req.setExpireHours(24);
        req.setRoleIds(List.of(11L, 22L));
        doAnswer(inv -> {
            TenantMemberInvite saved = inv.getArgument(0);
            saved.setId(99L);
            return 1;
        }).when(inviteMapper).insert(any(TenantMemberInvite.class));

        TenantMemberInvite invite = service.createInvite(req, 5L);

        assertThat(invite.getId()).isEqualTo(99L);
        assertThat(invite.getInviterId()).isEqualTo(5L);
        assertThat(invite.getStatus()).isEqualTo(TenantMemberInvite.STATUS_PENDING);
        assertThat(invite.getRoleIds()).isEqualTo("11,22");
        assertThat(invite.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
        assertThat(invite.getInviteCode()).hasSize(24);
    }

    @Test
    @DisplayName("createInvite SMS：手机号非法抛业务异常")
    void createInvite_sms_invalidPhone() {
        InviteCreateRequest req = new InviteCreateRequest();
        req.setTenantId(1L);
        req.setChannel("SMS");
        req.setPhone("not-a-phone");
        assertThatThrownBy(() -> service.createInvite(req, 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号");
        verify(inviteMapper, never()).insert(any(TenantMemberInvite.class));
    }

    @Test
    @DisplayName("createInvite LINK：channel 默认 LINK 也允许无 phone")
    void createInvite_link_default() {
        InviteCreateRequest req = new InviteCreateRequest();
        req.setTenantId(1L);
        // channel 留空 -> 默认 LINK
        doAnswer(inv -> {
            TenantMemberInvite saved = inv.getArgument(0);
            saved.setId(11L);
            return 1;
        }).when(inviteMapper).insert(any(TenantMemberInvite.class));

        TenantMemberInvite invite = service.createInvite(req, 5L);

        assertThat(invite.getChannel()).isEqualTo(TenantMemberInvite.CHANNEL_LINK);
        assertThat(invite.getStatus()).isEqualTo(TenantMemberInvite.STATUS_PENDING);
    }

    @Test
    @DisplayName("acceptInvite：邀请已过期则置为 EXPIRED 并抛异常")
    void acceptInvite_expired() {
        TenantMemberInvite invite = new TenantMemberInvite();
        invite.setId(1L);
        invite.setStatus(TenantMemberInvite.STATUS_PENDING);
        invite.setExpiresAt(LocalDateTime.now().minusHours(1));
        invite.setInviteCode("EXPIRED-CODE");
        when(inviteMapper.selectOne(any(Wrapper.class))).thenReturn(invite);

        assertThatThrownBy(() -> service.acceptInvite("EXPIRED-CODE", 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("过期");
        // EXPIRED 持久化更新
        verify(inviteMapper).updateById(any(TenantMemberInvite.class));
        assertThat(invite.getStatus()).isEqualTo(TenantMemberInvite.STATUS_EXPIRED);
    }

    @Test
    @DisplayName("acceptInvite：邀请已被接受不可重复接受")
    void acceptInvite_alreadyAccepted() {
        TenantMemberInvite invite = new TenantMemberInvite();
        invite.setId(1L);
        invite.setStatus(TenantMemberInvite.STATUS_ACCEPTED);
        invite.setInviteCode("ACC-CODE");
        when(inviteMapper.selectOne(any(Wrapper.class))).thenReturn(invite);

        assertThatThrownBy(() -> service.acceptInvite("ACC-CODE", 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被接受");
    }

    @Test
    @DisplayName("acceptInvite 成功路径：写入 acceptedUserId 与状态")
    void acceptInvite_success() {
        TenantMemberInvite invite = new TenantMemberInvite();
        invite.setId(1L);
        invite.setStatus(TenantMemberInvite.STATUS_PENDING);
        invite.setExpiresAt(LocalDateTime.now().plusHours(1));
        invite.setInviteCode("OK-CODE");
        when(inviteMapper.selectOne(any(Wrapper.class))).thenReturn(invite);

        TenantMemberInvite accepted = service.acceptInvite("OK-CODE", 100L);

        assertThat(accepted.getStatus()).isEqualTo(TenantMemberInvite.STATUS_ACCEPTED);
        assertThat(accepted.getAcceptedUserId()).isEqualTo(100L);
        assertThat(accepted.getAcceptedAt()).isNotNull();
    }

    @Test
    @DisplayName("revokeInvite：已接受邀请不可撤销")
    void revokeInvite_acceptedRejects() {
        TenantMemberInvite invite = new TenantMemberInvite();
        invite.setId(1L);
        invite.setStatus(TenantMemberInvite.STATUS_ACCEPTED);
        when(inviteMapper.selectById(1L)).thenReturn(invite);
        assertThatThrownBy(() -> service.revokeInvite(1L, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可撤销");
    }

    @Test
    @DisplayName("importInvites：含表头 + 1 行成功 + 1 行非法手机 + 1 行重复")
    void importInvites_mixed() {
        // 第二行：合法手机号，但同手机已存在 PENDING -> 失败（重复）
        // 第三行：非法手机号 -> 失败
        // 第四行：合法手机号且无重复 -> 成功
        // selectCount stub：13800138000 -> 返回 1（重复），13900139000 -> 返回 0（OK）
        when(inviteMapper.selectCount(any(Wrapper.class)))
                .thenReturn(1L)  // line 2 重复
                .thenReturn(0L); // line 4 OK
        doAnswer(inv -> {
            TenantMemberInvite saved = inv.getArgument(0);
            saved.setId(System.nanoTime());
            return 1;
        }).when(inviteMapper).insert(any(TenantMemberInvite.class));

        String csv = "phone,deptName,roleCode\n" +
                "13800138000,销售部,tenant_sales\n" +
                "abc,无效,tenant_sales\n" +
                "13900139000,销售部,tenant_sales\n";

        InviteImportResult result = service.importInvites(1L, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), 5L);

        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(2);
        assertThat(result.getRows()).hasSize(3);

        // 失败行的失败原因：第 1 行重复，第 2 行非法手机号
        assertThat(result.getRows().get(0).getFailReason()).contains("待接受邀请");
        assertThat(result.getRows().get(1).getFailReason()).contains("格式不合法");
        assertThat(result.getRows().get(2).getSuccess()).isTrue();

        // 仅 1 次 insert 落库（成功的那行）
        verify(inviteMapper, times(1)).insert(any(TenantMemberInvite.class));
    }

    @Test
    @DisplayName("importInvites：tenantId 为空抛异常")
    void importInvites_nullTenant() {
        assertThatThrownBy(() -> service.importInvites(null, new ByteArrayInputStream(new byte[0]), 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("租户ID");
    }
}
