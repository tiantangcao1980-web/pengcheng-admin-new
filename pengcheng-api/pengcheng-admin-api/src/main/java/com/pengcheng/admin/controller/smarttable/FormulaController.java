package com.pengcheng.admin.controller.smarttable;

import com.pengcheng.common.result.Result;
import com.pengcheng.system.smarttable.entity.SmartTableField;
import com.pengcheng.system.smarttable.formula.FormulaParser;
import com.pengcheng.system.smarttable.formula.FormulaService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 公式预览 & 校验端点
 *
 * POST /admin/smarttable/formula/preview  — 实时预览（含行数据求值）
 * POST /admin/smarttable/formula/validate — 语法校验（仅解析，不求值）
 */
@Slf4j
@RestController
@RequestMapping("/admin/smarttable/formula")
@RequiredArgsConstructor
public class FormulaController {

    private final FormulaService formulaService;

    // ========================= 预览端点 =========================

    /**
     * 公式实时预览（供前端公式编辑器调用）
     *
     * 请求体示例：
     * {
     *   "expr": "{amount} * 1.1",
     *   "sampleRow": {"amount": 100, "qty": 2},
     *   "fieldDefs": [{"fieldKey": "amount", "fieldType": "number"}, ...]
     * }
     *
     * 响应示例（成功）：{"code": 200, "data": {"result": 110.0}}
     * 响应示例（错误）：{"code": 200, "data": {"result": "#ERROR!", "error": "除以零"}}
     */
    @PostMapping("/preview")
    public Result<PreviewResponse> preview(@RequestBody PreviewRequest req) {
        if (req == null || req.getExpr() == null || req.getExpr().isBlank()) {
            return Result.ok(PreviewResponse.error("公式表达式不能为空"));
        }

        try {
            Map<String, Object> row    = req.getSampleRow()   != null ? req.getSampleRow()   : Collections.emptyMap();
            List<SmartTableField> defs = req.getFieldDefs()   != null ? req.getFieldDefs()   : Collections.emptyList();
            Object result = formulaService.evaluate(req.getExpr(), row, defs);
            if ("#ERROR!".equals(result)) {
                return Result.ok(PreviewResponse.error("#ERROR!"));
            }
            return Result.ok(PreviewResponse.success(result));
        } catch (Exception e) {
            log.warn("公式预览异常 expr=[{}] err={}", req.getExpr(), e.getMessage());
            return Result.ok(PreviewResponse.error(e.getMessage()));
        }
    }

    // ========================= 校验端点 =========================

    /**
     * 公式语法校验（仅解析，不求值）
     *
     * 请求体示例：{"expr": "{amount} * 1.1 + IF({qty} > 0, 5, 0)"}
     *
     * 响应示例（合法）：{"code": 200, "data": {"valid": true}}
     * 响应示例（非法）：{"code": 200, "data": {"valid": false, "error": "期望 RPAREN 但遇到 EOF"}}
     */
    @PostMapping("/validate")
    public Result<ValidateResponse> validate(@RequestBody ValidateRequest req) {
        if (req == null || req.getExpr() == null || req.getExpr().isBlank()) {
            return Result.ok(new ValidateResponse(false, "公式表达式不能为空"));
        }

        try {
            formulaService.compile(req.getExpr());
            return Result.ok(new ValidateResponse(true, null));
        } catch (FormulaParser.FormulaParseException e) {
            return Result.ok(new ValidateResponse(false, e.getMessage()));
        } catch (Exception e) {
            return Result.ok(new ValidateResponse(false, "解析失败: " + e.getMessage()));
        }
    }

    // ========================= 请求 / 响应 DTO =========================

    @Data
    public static class PreviewRequest {
        /** 公式表达式 */
        private String expr;
        /** 示例行数据：fieldKey → value */
        private Map<String, Object> sampleRow;
        /** 字段元信息（可选，用于类型提示） */
        private List<SmartTableField> fieldDefs;
    }

    @Data
    public static class PreviewResponse {
        private Object result;
        private String error;

        static PreviewResponse success(Object result) {
            PreviewResponse r = new PreviewResponse();
            r.result = result;
            return r;
        }

        static PreviewResponse error(String msg) {
            PreviewResponse r = new PreviewResponse();
            r.result = "#ERROR!";
            r.error  = msg;
            return r;
        }
    }

    @Data
    public static class ValidateRequest {
        private String expr;
    }

    @Data
    public static class ValidateResponse {
        private boolean valid;
        private String error;

        ValidateResponse(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }
    }
}
