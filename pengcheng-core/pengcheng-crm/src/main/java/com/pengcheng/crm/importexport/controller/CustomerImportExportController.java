package com.pengcheng.crm.importexport.controller;

import com.pengcheng.common.result.Result;
import com.pengcheng.crm.importexport.dto.ImportResultVO;
import com.pengcheng.crm.importexport.service.CustomerImportExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/crm/import-export")
public class CustomerImportExportController {

    @Autowired
    private CustomerImportExportService service;

    @PostMapping("/leads/import")
    public Result<ImportResultVO> importLeads(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(service.importLeads(file.getInputStream()));
    }

    @GetMapping("/leads/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String filename = URLEncoder.encode("线索导入模板", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + filename + ".xlsx");
        service.exportTemplate(response.getOutputStream());
    }
}
