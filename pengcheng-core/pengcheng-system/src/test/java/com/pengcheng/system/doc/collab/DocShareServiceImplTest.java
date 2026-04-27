package com.pengcheng.system.doc.collab;

import com.pengcheng.system.doc.collab.dto.ShareCreateDTO;
import com.pengcheng.system.doc.collab.entity.DocShare;
import com.pengcheng.system.doc.collab.mapper.DocShareMapper;
import com.pengcheng.system.doc.collab.service.impl.DocShareServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocShareServiceImpl")
class DocShareServiceImplTest {

    @Mock
    private DocShareMapper shareMapper;

    private DocShareServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocShareServiceImpl(shareMapper);
    }

    @Test
    @DisplayName("1. share LINK 类型：自动生成 32 位 shareCode，targetId 为 null")
    void should_generate_share_code_for_link_type() {
        ShareCreateDTO dto = new ShareCreateDTO();
        dto.setTargetType("LINK");
        dto.setPermission("READ");
        dto.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(shareMapper.insert(any())).thenReturn(1);

        DocShare result = service.share(10L, 1L, dto);

        assertThat(result.getShareCode()).isNotBlank();
        assertThat(result.getShareCode()).hasSize(32);
        assertThat(result.getTargetId()).isNull();
        assertThat(result.getTargetType()).isEqualTo("LINK");
    }

    @Test
    @DisplayName("2. accessByCode：访问码有效时返回分享记录")
    void should_return_share_for_valid_code() {
        DocShare share = new DocShare();
        share.setDocId(10L);
        share.setPermission("READ");
        share.setExpiresAt(LocalDateTime.now().plusHours(1));
        share.setShareCode("abc123validcode0000000000000001");

        when(shareMapper.selectByShareCode("abc123validcode0000000000000001")).thenReturn(share);

        DocShare result = service.accessByCode("abc123validcode0000000000000001");

        assertThat(result.getDocId()).isEqualTo(10L);
        assertThat(result.getPermission()).isEqualTo("READ");
    }

    @Test
    @DisplayName("3. accessByCode：链接过期时抛出 IllegalArgumentException")
    void should_reject_expired_share_code() {
        DocShare share = new DocShare();
        share.setShareCode("expiredcode00000000000000000001");
        share.setExpiresAt(LocalDateTime.now().minusHours(1)); // 已过期

        when(shareMapper.selectByShareCode("expiredcode00000000000000000001")).thenReturn(share);

        assertThatThrownBy(() -> service.accessByCode("expiredcode00000000000000000001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已过期");
    }

    @Test
    @DisplayName("4. checkPermission 多源：USER 分享赋予 EDIT 权限，校验 EDIT 返回 true；无分享时返回 false")
    void should_check_permission_from_multiple_sources() {
        DocShare userShare = new DocShare();
        userShare.setDocId(10L);
        userShare.setTargetType("USER");
        userShare.setTargetId(5L);
        userShare.setPermission("EDIT");

        when(shareMapper.selectUserShare(10L, 5L)).thenReturn(userShare);
        when(shareMapper.selectUserShare(10L, 99L)).thenReturn(null);
        when(shareMapper.selectDeptShare(anyLong(), anyLong())).thenReturn(null);

        // userId=5 有 EDIT 权限，校验 EDIT 通过
        assertThat(service.checkPermission(10L, 5L, null, "EDIT")).isTrue();
        // userId=5 有 EDIT 权限，校验 READ 也通过（EDIT >= READ）
        assertThat(service.checkPermission(10L, 5L, null, "READ")).isTrue();
        // userId=99 无任何分享，拒绝
        assertThat(service.checkPermission(10L, 99L, 100L, "READ")).isFalse();
    }

    @Test
    @DisplayName("5. cancelShare：调用 deleteById 完成取消")
    void should_call_delete_on_cancel() {
        when(shareMapper.deleteById(77L)).thenReturn(1);

        service.cancelShare(77L);

        verify(shareMapper).deleteById(77L);
    }
}
