package com.pengcheng.system.smarttable.market;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.smarttable.entity.SmartTableTemplate;
import com.pengcheng.system.smarttable.mapper.SmartTableTemplateMapper;
import com.pengcheng.system.smarttable.market.entity.SmartTableTemplateDownload;
import com.pengcheng.system.smarttable.market.entity.SmartTableTemplateRating;
import com.pengcheng.system.smarttable.market.mapper.SmartTableTemplateDownloadMapper;
import com.pengcheng.system.smarttable.market.mapper.SmartTableTemplateRatingMapper;
import com.pengcheng.system.smarttable.market.service.SmartTableMarketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SmartTableMarketService")
class SmartTableMarketServiceTest {

    @Mock private SmartTableTemplateMapper templateMapper;
    @Mock private SmartTableTemplateRatingMapper ratingMapper;
    @Mock private SmartTableTemplateDownloadMapper downloadMapper;

    private SmartTableMarketService service;

    @BeforeEach
    void setUp() {
        service = new SmartTableMarketService(templateMapper, ratingMapper, downloadMapper);
    }

    private SmartTableTemplate template(Long id, Boolean builtIn) {
        SmartTableTemplate t = new SmartTableTemplate();
        t.setId(id);
        t.setName("测试模板");
        t.setBuiltIn(builtIn);
        return t;
    }

    @Test
    @DisplayName("shareTemplate — 模板不存在时抛 IllegalArgumentException")
    void shareTemplate_templateNotFound_throwsIllegalArgument() {
        when(templateMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.shareTemplate(99L, 100L, "张三", "sales"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("模板不存在");
    }

    @Test
    @DisplayName("shareTemplate — 内置模板（builtIn=Boolean.TRUE）应被拒绝分享（已通过 audit 修 Boolean.TRUE.equals 修复）")
    void shareTemplate_builtInRejectedAfterFix() {
        // audit 修复（commit a09c1d2）：Integer.valueOf(1).equals(Boolean) → Boolean.TRUE.equals
        // 现在内置模板会正确抛 IllegalStateException 拒绝分享
        SmartTableTemplate t = template(1L, Boolean.TRUE);
        when(templateMapper.selectById(1L)).thenReturn(t);

        assertThatThrownBy(() -> service.shareTemplate(1L, 100L, "张三", "sales"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("内置模板");
    }

    @Test
    @DisplayName("approveSharing — 审核通过 → templateMapper.update 被调用（update PUBLIC）")
    void approveSharing_approve_callsUpdate() {
        service.approveSharing(1L, true);
        // update 被调用：第一个参数 null，第二个 wrapper 非 null
        verify(templateMapper).update(isNull(), notNull());
    }

    @Test
    @DisplayName("approveSharing — 审核拒绝 → templateMapper.update 被调用（update REJECTED）")
    void approveSharing_reject_callsUpdate() {
        service.approveSharing(1L, false);
        verify(templateMapper).update(isNull(), notNull());
    }

    @Test
    @DisplayName("recordDownload — 写下载记录 + 原子 +1 incrementDownloadCount")
    void recordDownload_insertsRecordAndIncrementsCount() {
        service.recordDownload(1L, 100L, 200L);

        ArgumentCaptor<SmartTableTemplateDownload> captor =
                ArgumentCaptor.forClass(SmartTableTemplateDownload.class);
        verify(downloadMapper).insert(captor.capture());
        SmartTableTemplateDownload d = captor.getValue();
        assertThat(d.getTemplateId()).isEqualTo(1L);
        assertThat(d.getUserId()).isEqualTo(100L);
        assertThat(d.getTargetTableId()).isEqualTo(200L);
        verify(downloadMapper).incrementDownloadCount(1L);
    }

    @Test
    @DisplayName("rate — 首次评分 → INSERT rating 记录 + recomputeAggregate")
    void rate_firstTime_insertsRating() {
        when(ratingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.rate(1L, 100L, 5, "非常好");

        ArgumentCaptor<SmartTableTemplateRating> captor =
                ArgumentCaptor.forClass(SmartTableTemplateRating.class);
        verify(ratingMapper).insert(captor.capture());
        assertThat(captor.getValue().getRating()).isEqualTo(5);
        verify(ratingMapper).recomputeAggregate(1L);
    }

    @Test
    @DisplayName("rate — 范围校验：评分 0 或 6 抛 IllegalArgumentException")
    void rate_outOfRange_throwsException() {
        assertThatThrownBy(() -> service.rate(1L, 100L, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1-5");

        assertThatThrownBy(() -> service.rate(1L, 100L, 6, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1-5");
    }
}
