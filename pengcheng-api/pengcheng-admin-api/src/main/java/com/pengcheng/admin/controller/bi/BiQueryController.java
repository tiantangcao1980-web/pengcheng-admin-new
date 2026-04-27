package com.pengcheng.admin.controller.bi;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.excel.EasyExcel;
import com.pengcheng.bi.engine.BiQueryEngine;
import com.pengcheng.bi.engine.BiQueryRequest;
import com.pengcheng.bi.engine.BiQueryResponse;
import com.pengcheng.bi.model.entity.BiSavedQuery;
import com.pengcheng.bi.model.entity.BiViewModel;
import com.pengcheng.bi.service.BiSavedQueryService;
import com.pengcheng.bi.service.BiViewModelService;
import com.pengcheng.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BI 自助分析接口。
 *
 * <p>URL 前缀 {@code /admin/bi}，权限码 {@code bi:query:use}。
 * <p>所有查询请求经 {@link BiQueryEngine} 进行白名单校验和参数化 SQL，防止 SQL 注入。
 */
@RestController
@RequestMapping("/admin/bi")
@RequiredArgsConstructor
public class BiQueryController {

    private final BiViewModelService viewModelService;
    private final BiSavedQueryService savedQueryService;
    private final BiQueryEngine biQueryEngine;

    // ====================================================================
    // 视图模型接口
    // ====================================================================

    /**
     * 列出所有可用（已启用）的视图模型。
     */
    @GetMapping("/views")
    @SaCheckPermission("bi:query:use")
    public Result<List<BiViewModel>> listViews() {
        return Result.ok(viewModelService.listEnabled());
    }

    /**
     * 查询视图模型详情（含维度/指标定义）。
     *
     * @param code 视图编码
     */
    @GetMapping("/views/{code}")
    @SaCheckPermission("bi:query:use")
    public Result<BiViewModel> getView(@PathVariable String code) {
        BiViewModel vm = viewModelService.findByCode(code);
        if (vm == null) {
            return Result.fail("视图不存在：" + code);
        }
        return Result.ok(vm);
    }

    // ====================================================================
    // 查询接口
    // ====================================================================

    /**
     * 执行多维查询。
     *
     * <p>引擎对 dimensions / metrics / filter.column 进行白名单校验，
     * filter.values 全部参数化，防止 SQL 注入。
     *
     * @param req 查询请求
     * @return 查询结果（列元数据 + 数据行 + 总行数）
     */
    @PostMapping("/query")
    @SaCheckPermission("bi:query:use")
    public Result<BiQueryResponse> query(@RequestBody BiQueryRequest req) {
        return Result.ok(biQueryEngine.execute(req));
    }

    // ====================================================================
    // 保存查询接口
    // ====================================================================

    /**
     * 保存当前用户的查询配置。
     *
     * @param query 保存的查询实体（viewCode / name / queryJson 必填，userId 由服务端填充）
     * @return 新记录 ID
     */
    @PostMapping("/saved-queries")
    @SaCheckPermission("bi:query:use")
    public Result<Long> saveQuery(@RequestBody BiSavedQuery query) {
        // TODO Phase 3：从 Sa-Token 获取当前用户 ID
        // query.setUserId(StpUtil.getLoginIdAsLong());
        return Result.ok(savedQueryService.saveQuery(query));
    }

    /**
     * 列出当前用户的所有保存查询（按创建时间倒序）。
     *
     * @return 保存查询列表
     */
    @GetMapping("/saved-queries")
    @SaCheckPermission("bi:query:use")
    public Result<List<BiSavedQuery>> listSavedQueries() {
        // TODO Phase 3：从 Sa-Token 获取当前用户 ID
        // Long userId = StpUtil.getLoginIdAsLong();
        Long userId = 0L; // placeholder
        return Result.ok(savedQueryService.listByUser(userId));
    }

    // ====================================================================
    // 导出接口
    // ====================================================================

    /**
     * 将查询结果导出为 Excel 文件（EasyExcel 实现）。
     *
     * <p>同 {@code POST /admin/bi/query}，但以流式方式输出 xlsx。
     *
     * @param req      查询请求（同 /query 接口）
     * @param response HttpServletResponse（用于写出文件流）
     */
    @PostMapping("/export/excel")
    @SaCheckPermission("bi:query:use")
    public void exportExcel(@RequestBody BiQueryRequest req,
                             HttpServletResponse response) throws IOException {
        BiQueryResponse result = biQueryEngine.execute(req);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("BI导出_" + req.getViewCode(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");

        // 构建表头（使用列标签）
        List<List<String>> head = new ArrayList<>();
        for (com.pengcheng.bi.dto.Column col : result.getColumns()) {
            head.add(List.of(col.getLabel()));
        }

        // 构建数据行
        List<List<Object>> dataList = new ArrayList<>();
        for (Map<String, Object> row : result.getRows()) {
            List<Object> rowData = new ArrayList<>();
            for (com.pengcheng.bi.dto.Column col : result.getColumns()) {
                rowData.add(row.get(col.getKey()));
            }
            dataList.add(rowData);
        }

        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet("数据")
                .doWrite(dataList);
    }
}
