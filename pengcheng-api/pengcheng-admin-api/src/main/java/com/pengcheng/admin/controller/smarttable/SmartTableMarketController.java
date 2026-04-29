package com.pengcheng.admin.controller.smarttable;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pengcheng.common.result.Result;
import com.pengcheng.system.smarttable.entity.SmartTableTemplate;
import com.pengcheng.system.smarttable.market.service.SmartTableMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 智能表格模板市场（M4 — V1.2 长期收口）。
 */
@Tag(name = "智能表格 - 模板市场", description = "公开模板浏览、用户分享、评分、下载")
@RestController
@RequestMapping("/admin/smarttable/template-market")
@RequiredArgsConstructor
public class SmartTableMarketController {

    private final SmartTableMarketService marketService;

    @Operation(summary = "市场列表（仅显示 PUBLIC + 内置模板）")
    @GetMapping
    @SaCheckPermission("smarttable:template:list")
    public Result<IPage<SmartTableTemplate>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "downloads") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(marketService.listMarket(category, keyword, sort, page, size));
    }

    @Operation(summary = "分享私有模板到市场（进入审核）")
    @PostMapping("/{id}/share")
    @SaCheckPermission("smarttable:template:share")
    public Result<Void> share(@PathVariable Long id, @RequestBody ShareRequest req) {
        Long uid = StpUtil.getLoginIdAsLong();
        marketService.shareTemplate(id, uid, req.getAuthorName(), req.getTags());
        return Result.ok();
    }

    @Operation(summary = "管理员审核分享（通过 / 拒绝）")
    @PostMapping("/{id}/review")
    @SaCheckPermission("smarttable:template:moderate")
    public Result<Void> review(@PathVariable Long id, @RequestParam boolean approve) {
        marketService.approveSharing(id, approve);
        return Result.ok();
    }

    @Operation(summary = "下载模板（用户从模板创建表后回调）")
    @PostMapping("/{id}/download")
    @SaCheckPermission("smarttable:template:download")
    public Result<Void> download(@PathVariable Long id, @RequestParam(required = false) Long targetTableId) {
        Long uid = StpUtil.getLoginIdAsLong();
        marketService.recordDownload(id, uid, targetTableId);
        return Result.ok();
    }

    @Operation(summary = "评分（1-5 + 可选短评）")
    @PostMapping("/{id}/rate")
    @SaCheckPermission("smarttable:template:download")
    public Result<Void> rate(@PathVariable Long id, @RequestBody RateRequest req) {
        Long uid = StpUtil.getLoginIdAsLong();
        marketService.rate(id, uid, req.getRating(), req.getReview());
        return Result.ok();
    }

    @Data
    public static class ShareRequest {
        private String authorName;
        private String tags;
    }

    @Data
    public static class RateRequest {
        private Integer rating;
        private String review;
    }
}
