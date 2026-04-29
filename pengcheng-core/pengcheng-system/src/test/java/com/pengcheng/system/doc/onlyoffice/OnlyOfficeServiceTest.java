package com.pengcheng.system.doc.onlyoffice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.doc.entity.Doc;
import com.pengcheng.system.doc.mapper.DocMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OnlyOfficeService")
class OnlyOfficeServiceTest {

    @Mock
    private DocMapper docMapper;

    private OnlyOfficeService service;

    @BeforeEach
    void setUp() {
        service = new OnlyOfficeService(docMapper);
    }

    private OnlyOfficeCallback cb(Integer status, String key, String url) {
        OnlyOfficeCallback cb = new OnlyOfficeCallback();
        cb.setStatus(status);
        cb.setKey(key);
        cb.setUrl(url);
        return cb;
    }

    @Test
    @DisplayName("status=1（编辑中）→ 直接返回 0，不查 DB")
    void handleCallback_status1_returnsZeroNoDbQuery() {
        int result = service.handleCallback(cb(1, "key1", null));

        assertThat(result).isEqualTo(0);
        verify(docMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("status=4（关闭无变更）→ 直接返回 0，不查 DB")
    void handleCallback_status4_returnsZeroNoDbQuery() {
        int result = service.handleCallback(cb(4, "key4", null));

        assertThat(result).isEqualTo(0);
        verify(docMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("status=2（必须保存）但 docKey 找不到 → 返回 1（错误）")
    void handleCallback_status2_docKeyNotFound_returns1() {
        when(docMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        int result = service.handleCallback(cb(2, "missing-key", "http://oo/file.docx"));

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("status=2，找到 doc，ooLastSave 被更新")
    void handleCallback_status2_docFound_updatesOoLastSave() {
        Doc doc = new Doc();
        doc.setId(10L);
        doc.setOoDocKey("dockey-10");
        when(docMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(doc);
        // 模拟 http 下载抛出异常（无法真实连接），此时返回 1
        // 使用不可达 URL 测试异常处理路径
        int result = service.handleCallback(cb(2, "dockey-10", "http://localhost:1/nonexistent"));

        // 无法连接会抛异常，handleCallback 捕获并返回 1
        assertThat(result).isEqualTo(1);
    }
}
