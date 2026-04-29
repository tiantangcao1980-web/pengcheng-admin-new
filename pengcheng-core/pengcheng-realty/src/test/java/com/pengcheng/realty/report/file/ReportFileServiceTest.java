package com.pengcheng.realty.report.file;

import com.pengcheng.realty.report.file.generator.ReportFileGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ReportFileService 路由层单测
 */
@DisplayName("ReportFileService — 报表路由")
class ReportFileServiceTest {

    static class StubGen implements ReportFileGenerator {
        private final ReportFileType type;
        StubGen(ReportFileType type) { this.type = type; }
        @Override public ReportFileType supportedType() { return type; }
        @Override
        public ReportFileResult generate(ReportFileRequest req) {
            byte[] content = "stub".getBytes();
            return ReportFileResult.builder()
                    .fileName(type.getCode() + "." + type.getExtension())
                    .contentType("application/octet-stream")
                    .content(content).size(content.length).build();
        }
    }

    @Test
    @DisplayName("路由：按类型分发到对应 Generator")
    void routesByType() {
        ReportFileService service = new ReportFileService(List.of(
                new StubGen(ReportFileType.SALES_PERFORMANCE),
                new StubGen(ReportFileType.CUSTOMER_ANALYSIS)));

        ReportFileResult r = service.generate(ReportFileRequest.builder()
                .type(ReportFileType.CUSTOMER_ANALYSIS).build());

        assertThat(r.getFileName()).startsWith(ReportFileType.CUSTOMER_ANALYSIS.getCode());
        assertThat(r.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("未注册类型 → IllegalArgumentException")
    void unregisteredType_throws() {
        ReportFileService service = new ReportFileService(List.of(
                new StubGen(ReportFileType.SALES_PERFORMANCE)));

        assertThatThrownBy(() -> service.generate(ReportFileRequest.builder()
                .type(ReportFileType.COMMISSION_LIST).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未注册");
    }

    @Test
    @DisplayName("type 为 null → IllegalArgumentException")
    void nullType_throws() {
        ReportFileService service = new ReportFileService(List.of());
        assertThatThrownBy(() -> service.generate(ReportFileRequest.builder().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("类型");
    }

    @Test
    @DisplayName("registeredTypes 反映已注册集合")
    void registeredTypes() {
        ReportFileService service = new ReportFileService(List.of(
                new StubGen(ReportFileType.SALES_PERFORMANCE),
                new StubGen(ReportFileType.COMMISSION_LIST)));
        assertThat(service.registeredTypes())
                .containsExactlyInAnyOrder(
                        ReportFileType.SALES_PERFORMANCE,
                        ReportFileType.COMMISSION_LIST);
    }

    @Test
    @DisplayName("ReportFileType.fromCode 正确解析")
    void typeFromCode() {
        assertThat(ReportFileType.fromCode("sales-performance"))
                .isEqualTo(ReportFileType.SALES_PERFORMANCE);
        assertThatThrownBy(() -> ReportFileType.fromCode("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
