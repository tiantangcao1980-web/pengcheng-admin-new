package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.realty.report.file.ReportFileRequest;
import com.pengcheng.realty.report.file.ReportFileResult;
import com.pengcheng.realty.report.file.ReportFileService;
import com.pengcheng.realty.report.file.ReportFileType;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 报表文件下载控制器
 *
 * 用法：GET /admin/report/file/download?type=sales-performance&startDate=2026-04-01&endDate=2026-04-30
 */
@RestController
@RequestMapping("/admin/report/file")
@RequiredArgsConstructor
public class ReportFileController {

    private final ReportFileService reportFileService;

    @GetMapping("/download")
    @SaCheckPermission("realty:report:download")
    @Log(title = "下载报表", businessType = BusinessType.EXPORT)
    public ResponseEntity<ByteArrayResource> download(
            @RequestParam("type") String typeCode,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "customerId", required = false) Long customerId
    ) {
        ReportFileType type = ReportFileType.fromCode(typeCode);
        ReportFileRequest req = ReportFileRequest.builder()
                .type(type).startDate(startDate).endDate(endDate).customerId(customerId).build();
        ReportFileResult result = reportFileService.generate(req);

        String encodedName = URLEncoder.encode(result.getFileName(), StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedName);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(result.getSize())
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .body(new ByteArrayResource(result.getContent()));
    }
}
