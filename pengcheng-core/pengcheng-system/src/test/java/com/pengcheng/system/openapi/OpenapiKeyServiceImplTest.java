package com.pengcheng.system.openapi;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.SecureUtil;
import com.pengcheng.system.openapi.dto.CreateKeyRequest;
import com.pengcheng.system.openapi.dto.CreateKeyResult;
import com.pengcheng.system.openapi.entity.OpenapiKey;
import com.pengcheng.system.openapi.mapper.OpenapiKeyMapper;
import com.pengcheng.system.openapi.service.impl.OpenapiKeyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenapiKeyServiceImpl")
class OpenapiKeyServiceImplTest {

    @Mock
    private OpenapiKeyMapper mapper;

    private OpenapiKeyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OpenapiKeyServiceImpl(mapper);
    }

    @Test
    @DisplayName("create — 返回明文 SK，DB 存 SHA256 hash（不等于明文）")
    void create_returnsCleartextSk_storesHash() {
        CreateKeyRequest req = new CreateKeyRequest();
        req.setName("测试密钥");
        req.setScopes(List.of("customer:read"));
        req.setRateLimit(100);

        doAnswer(inv -> {
            OpenapiKey k = inv.getArgument(0);
            k.setId(1L);
            return 1;
        }).when(mapper).insert(any(OpenapiKey.class));

        CreateKeyResult result = service.create(1L, 100L, req);

        assertThat(result.getSecretKey()).startsWith("sk_");
        assertThat(result.getAccessKey()).startsWith("ak_");
        assertThat(result.getSecretKey().length()).isGreaterThan(10);

        ArgumentCaptor<OpenapiKey> captor = ArgumentCaptor.forClass(OpenapiKey.class);
        verify(mapper).insert(captor.capture());
        OpenapiKey stored = captor.getValue();
        // DB 存的是 hash，不是明文 SK
        assertThat(stored.getSecretKeyHash()).isNotEqualTo(result.getSecretKey());
        // 验证 hash 确实是 SHA256
        assertThat(stored.getSecretKeyHash()).isEqualTo(SecureUtil.sha256(result.getSecretKey()));
    }

    @Test
    @DisplayName("rotate — 新 SK hash 不同于旧 hash，缓存被移除")
    void rotate_newHashDiffersFromOld_cacheEvicted() {
        OpenapiKey oldKey = new OpenapiKey();
        oldKey.setId(1L);
        oldKey.setAccessKey("ak_old");
        oldKey.setSecretKeyHash("oldhash123");
        when(mapper.selectById(1L)).thenReturn(oldKey);

        CreateKeyResult result = service.rotate(1L);

        assertThat(result.getSecretKey()).startsWith("sk_");
        verify(mapper).updateById(argThat(k ->
                !k.getSecretKeyHash().equals("oldhash123")));
    }

    @Test
    @DisplayName("revoke — enabled=0 并从缓存移除")
    void revoke_setsEnabledZeroAndEvictsCache() {
        OpenapiKey k = new OpenapiKey();
        k.setId(1L);
        k.setAccessKey("ak_revoke");
        k.setEnabled(1);
        when(mapper.selectById(1L)).thenReturn(k);

        service.revoke(1L);

        ArgumentCaptor<OpenapiKey> captor = ArgumentCaptor.forClass(OpenapiKey.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getEnabled()).isEqualTo(0);
    }

    @Test
    @DisplayName("findByAccessKey — 命中内存缓存后不再查 DB")
    void findByAccessKey_cacheHit_noSecondDbCall() {
        OpenapiKey k = new OpenapiKey();
        k.setAccessKey("ak_cached");
        k.setEnabled(1);
        when(mapper.findByAccessKey("ak_cached")).thenReturn(k);

        // 第一次：DB 查
        Optional<OpenapiKey> first = service.findByAccessKey("ak_cached");
        // 第二次：命中缓存
        Optional<OpenapiKey> second = service.findByAccessKey("ak_cached");

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        // mapper 只被调用一次（第二次命中缓存）
        verify(mapper, times(1)).findByAccessKey("ak_cached");
    }

    @Test
    @DisplayName("verifySignature — HMAC-SHA256 正确比对通过")
    void verifySignature_correctHmac_returnsTrue() {
        String sk = "sk_testSecret";
        String hash = SecureUtil.sha256(sk);
        OpenapiKey k = new OpenapiKey();
        k.setAccessKey("ak_sign");
        k.setSecretKeyHash(hash);
        k.setEnabled(1);
        when(mapper.findByAccessKey("ak_sign")).thenReturn(k);

        String stringToSign = "GET\n/openapi/v1/customers\n1700000000\nnonce123";
        HMac mac = new HMac(HmacAlgorithm.HmacSHA256, hash.getBytes(StandardCharsets.UTF_8));
        String sig = Base64.encode(mac.digest(stringToSign));

        assertThat(service.verifySignature("ak_sign", sig, stringToSign)).isTrue();
        assertThat(service.verifySignature("ak_sign", "wrongsig", stringToSign)).isFalse();
    }

    @Test
    @DisplayName("secretPreview — 格式为 前6位...后4位")
    void create_secretPreview_correctFormat() {
        CreateKeyRequest req = new CreateKeyRequest();
        req.setName("preview测试");

        doAnswer(inv -> {
            OpenapiKey k = inv.getArgument(0);
            k.setId(2L);
            return 1;
        }).when(mapper).insert(any(OpenapiKey.class));

        CreateKeyResult result = service.create(1L, 1L, req);

        String sk = result.getSecretKey();
        String expectedPreview = sk.substring(0, 6) + "..." + sk.substring(sk.length() - 4);
        assertThat(result.getSecretPreview()).isEqualTo(expectedPreview);
    }
}
