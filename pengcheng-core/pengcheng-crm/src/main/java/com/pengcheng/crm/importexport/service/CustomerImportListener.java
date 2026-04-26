package com.pengcheng.crm.importexport.service;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.pengcheng.crm.importexport.dto.CustomerImportRowDTO;
import com.pengcheng.crm.importexport.dto.ImportResultVO;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * EasyExcel 行监听器：负责逐行校验并回写失败行。
 */
public class CustomerImportListener implements ReadListener<CustomerImportRowDTO> {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    private final ImportResultVO result;

    /** 行处理函数：业务侧负责落库；返回 null 表示成功，非 null 表示错误信息 */
    private final java.util.function.Function<CustomerImportRowDTO, String> handler;

    /** 行号（含表头偏移） */
    private int rowNum = 1; // 表头是第 1 行

    /** 手机号本批内去重 */
    private final Set<String> phoneSeen = new HashSet<>();

    public CustomerImportListener(ImportResultVO result,
                                  java.util.function.Function<CustomerImportRowDTO, String> handler) {
        this.result = result;
        this.handler = handler;
    }

    /** 适配测试场景的便利构造（BiConsumer 风格） */
    public CustomerImportListener(ImportResultVO result, BiConsumer<CustomerImportRowDTO, ImportResultVO> handler) {
        this.result = result;
        this.handler = row -> {
            try {
                handler.accept(row, result);
                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        };
    }

    @Override
    public void invoke(CustomerImportRowDTO row, AnalysisContext context) {
        rowNum++;
        result.setTotal(result.getTotal() + 1);

        String err = validate(row);
        if (err != null) {
            result.addFail(rowNum, err, row);
            return;
        }

        String bizErr = handler == null ? null : handler.apply(row);
        if (bizErr != null) {
            result.addFail(rowNum, bizErr, row);
        } else {
            result.incrSuccess();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // no-op
    }

    /** 公开给测试用 */
    public String validate(CustomerImportRowDTO row) {
        if (row == null) return "空行";
        if (row.getName() == null || row.getName().isBlank()) return "姓名必填";
        if (row.getPhone() == null || row.getPhone().isBlank()) return "手机号必填";
        if (!PHONE.matcher(row.getPhone()).matches()) return "手机号格式非法";
        if (!phoneSeen.add(row.getPhone())) return "本批次内手机号重复";
        if (row.getIntentionLevel() != null && !row.getIntentionLevel().isBlank()) {
            String il = row.getIntentionLevel().trim();
            if (!"高".equals(il) && !"中".equals(il) && !"低".equals(il)) {
                return "意向必须是 高/中/低";
            }
        }
        return null;
    }
}
