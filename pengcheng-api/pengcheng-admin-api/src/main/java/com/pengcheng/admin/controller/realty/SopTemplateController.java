package com.pengcheng.admin.controller.realty;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.result.Result;
import com.pengcheng.realty.sop.entity.RealtySopTemplate;
import com.pengcheng.realty.sop.mapper.RealtySopTemplateMapper;
import com.pengcheng.system.annotation.Log;
import com.pengcheng.system.annotation.Log.BusinessType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 带看 SOP 模板管理控制器（管理员）
 * <p>
 * 管理 visit_confirm / commission_tripartite 两种 HTML 模板的增删改查。
 */
@RestController
@RequestMapping("/admin/realty/sop-templates")
@RequiredArgsConstructor
public class SopTemplateController {

    private final RealtySopTemplateMapper sopTemplateMapper;

    /**
     * 查询所有模板列表
     */
    @GetMapping
    @SaCheckPermission("realty:sop:template:list")
    public Result<List<RealtySopTemplate>> list() {
        return Result.ok(sopTemplateMapper.selectList(
                new LambdaQueryWrapper<RealtySopTemplate>()
                        .orderByAsc(RealtySopTemplate::getId)));
    }

    /**
     * 查询单个模板详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("realty:sop:template:list")
    public Result<RealtySopTemplate> detail(@PathVariable Long id) {
        return Result.ok(sopTemplateMapper.selectById(id));
    }

    /**
     * 按 code 查询模板
     */
    @GetMapping("/by-code")
    @SaCheckPermission("realty:sop:template:list")
    public Result<RealtySopTemplate> byCode(@RequestParam String code) {
        return Result.ok(sopTemplateMapper.selectByCode(code));
    }

    /**
     * 创建新模板
     */
    @PostMapping
    @SaCheckPermission("realty:sop:template:edit")
    @Log(title = "SOP模板管理", businessType = BusinessType.INSERT)
    public Result<Long> create(@RequestBody RealtySopTemplate template) {
        template.setId(null);
        sopTemplateMapper.insert(template);
        return Result.ok(template.getId());
    }

    /**
     * 更新模板内容（通常用于调整 HTML 样式或占位符）
     */
    @PutMapping("/{id}")
    @SaCheckPermission("realty:sop:template:edit")
    @Log(title = "SOP模板管理", businessType = BusinessType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @RequestBody RealtySopTemplate template) {
        template.setId(id);
        sopTemplateMapper.updateById(template);
        return Result.ok();
    }

    /**
     * 启用模板
     */
    @PostMapping("/{id}/enable")
    @SaCheckPermission("realty:sop:template:edit")
    @Log(title = "SOP模板管理-启用", businessType = BusinessType.UPDATE)
    public Result<Void> enable(@PathVariable Long id) {
        RealtySopTemplate template = new RealtySopTemplate();
        template.setId(id);
        template.setEnabled(1);
        sopTemplateMapper.updateById(template);
        return Result.ok();
    }

    /**
     * 停用模板
     */
    @PostMapping("/{id}/disable")
    @SaCheckPermission("realty:sop:template:edit")
    @Log(title = "SOP模板管理-停用", businessType = BusinessType.UPDATE)
    public Result<Void> disable(@PathVariable Long id) {
        RealtySopTemplate template = new RealtySopTemplate();
        template.setId(id);
        template.setEnabled(0);
        sopTemplateMapper.updateById(template);
        return Result.ok();
    }

    /**
     * 删除模板（谨慎操作，通常建议停用而非删除）
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("realty:sop:template:edit")
    @Log(title = "SOP模板管理-删除", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        sopTemplateMapper.deleteById(id);
        return Result.ok();
    }
}
