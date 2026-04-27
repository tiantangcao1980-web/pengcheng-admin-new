package com.pengcheng.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.pengcheng.common.result.Result;
import com.pengcheng.oss.FileStorage;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import com.pengcheng.system.ocr.integration.CardImportToCustomerService;
import com.pengcheng.system.ocr.integration.CardPreviewException;
import com.pengcheng.system.ocr.integration.CustomerImportPreview;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;

/**
 * 名片 OCR 识别接口
 *
 * <p>提供两种接入方式：
 * <ol>
 *   <li>{@code POST /admin/ocr/business-card/preview} — 前端已有 Base64 图片时直接识别</li>
 *   <li>{@code POST /admin/ocr/business-card/upload} — 上传图片文件到 MinIO 后识别（大文件友好）</li>
 * </ol>
 *
 * <p>两个接口均需要 {@code ocr:card:use} 权限。
 */
@Tag(name = "名片 OCR", description = "名片扫描识别，返回客户字段预填数据")
@Slf4j
@RestController
@RequestMapping("/admin/ocr/business-card")
@RequiredArgsConstructor
public class OcrCardController {

    private final CardImportToCustomerService cardImportService;
    private final FileStorage fileStorage;

    // ------------------------------------------------------------------ //
    //  接口 1：Base64 直传识别                                              //
    // ------------------------------------------------------------------ //

    /**
     * 名片预览（Base64 方式）
     *
     * <p>前端将图片编码为 Base64 后直接传入，适合小图片（建议 ≤ 2 MB）。</p>
     *
     * @param body {@code { "imageBase64": "..." }}
     * @return 识别结果预填 DTO
     */
    @Operation(summary = "名片识别（Base64）", description = "传入 Base64 图片，返回客户字段预填数据")
    @PostMapping("/preview")
    @SaCheckPermission("ocr:card:use")
    @Log(title = "名片 OCR 预览", businessType = BusinessType.OTHER)
    public Result<CustomerImportPreview> preview(@RequestBody PreviewRequest body) {
        if (body == null || body.getImageBase64() == null || body.getImageBase64().isBlank()) {
            return Result.fail("imageBase64 不能为空");
        }
        try {
            CustomerImportPreview preview = cardImportService.previewFromImage(body.getImageBase64());
            return Result.ok(preview);
        } catch (IllegalArgumentException e) {
            log.warn("名片预览参数异常: {}", e.getMessage());
            return Result.fail("图片格式不合法: " + e.getMessage());
        } catch (CardPreviewException e) {
            log.warn("名片 OCR 失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  接口 2：文件上传 + 识别（大文件兼容）                                 //
    // ------------------------------------------------------------------ //

    /**
     * 上传名片图片并识别
     *
     * <p>图片先上传到 MinIO（路径 {@code ocr/card/{yyyy-MM-dd}/{uuid}.{ext}}），
     * 再将字节数据交给 OCR 识别，最终返回 OSS key 和预填数据。</p>
     *
     * @param file 名片图片（multipart/form-data）
     * @return {@code { "ossKey": "...", "preview": { ... } }}
     */
    @Operation(summary = "上传名片并识别", description = "multipart 上传图片到 MinIO 后识别，返回 ossKey 和预填数据")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SaCheckPermission("ocr:card:use")
    @Log(title = "名片 OCR 上传识别", businessType = BusinessType.OTHER)
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择名片图片");
        }

        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传图片失败", e);
            return Result.fail("读取图片失败，请重试");
        }

        // 上传到 MinIO
        String ext = resolveExt(file.getOriginalFilename());
        String ossKey = "ocr/card/" + LocalDate.now() + "/" +
                java.util.UUID.randomUUID() + ext;
        String ossUrl;
        try {
            ossUrl = fileStorage.upload(
                    new java.io.ByteArrayInputStream(imageBytes),
                    "ocr/card/" + LocalDate.now(),
                    java.util.UUID.randomUUID() + ext);
        } catch (Exception e) {
            log.error("名片图片上传 MinIO 失败: {}", e.getMessage(), e);
            return Result.fail("图片上传失败，请重试");
        }

        // OCR 识别：将字节转 Base64 后调用 Service
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        CustomerImportPreview preview;
        try {
            preview = cardImportService.previewFromImage(imageBase64);
        } catch (IllegalArgumentException e) {
            log.warn("名片 OCR 参数异常: {}", e.getMessage());
            return Result.fail("图片格式不合法: " + e.getMessage());
        } catch (CardPreviewException e) {
            log.warn("名片 OCR 失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        }

        return Result.ok(Map.of(
                "ossKey", ossKey,
                "ossUrl", ossUrl != null ? ossUrl : "",
                "preview", preview));
    }

    // ------------------------------------------------------------------ //
    //  内部工具                                                             //
    // ------------------------------------------------------------------ //

    private String resolveExt(String originalFilename) {
        if (originalFilename == null) {
            return ".jpg";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : ".jpg";
    }

    // ------------------------------------------------------------------ //
    //  请求体 DTO                                                           //
    // ------------------------------------------------------------------ //

    @Data
    public static class PreviewRequest {
        /** Base64 编码的图片（可含 data URI 前缀） */
        private String imageBase64;
    }
}
