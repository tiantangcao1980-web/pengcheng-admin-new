package com.pengcheng.system.smarttable.formula;

import com.pengcheng.system.smarttable.entity.SmartTableField;
import com.pengcheng.system.smarttable.entity.SmartTableRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公式字段富化器（外挂 Enricher，不修改 SmartTableServiceImpl）
 *
 * 使用方式：
 *   在 Controller 或其他 Service 层，查询记录列表后调用：
 *
 *   <pre>
 *   List&lt;SmartTableRecord&gt; records = ...;
 *   List&lt;SmartTableField&gt;  fields  = smartTableService.listFields(tableId);
 *   formulaFieldEnricher.enrich(records, fields);
 *   </pre>
 *
 * 逻辑：
 *   遍历字段列表，找出 fieldType = "formula" 的字段；
 *   对每条记录执行 FormulaService.evaluate()，将结果写入 record.data；
 *   原始行数据（非 formula 字段）不受影响。
 *
 * 注意 N+1：公式字段的计算是纯内存操作，不涉及 DB 查询，无 N+1 风险。
 *          关联字段（relation）的 N+1 缓解见 RelationFieldResolver。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FormulaFieldEnricher {

    private final FormulaService formulaService;

    /**
     * 批量富化：将所有 formula 字段的计算值注入各记录的 data Map
     *
     * @param records 记录列表（会原地修改 data）
     * @param fields  该表所有字段定义
     */
    public void enrich(List<SmartTableRecord> records, List<SmartTableField> fields) {
        if (records == null || records.isEmpty() || fields == null) return;

        // 过滤出公式字段
        List<SmartTableField> formulaFields = fields.stream()
                .filter(f -> "formula".equals(f.getFieldType()))
                .collect(java.util.stream.Collectors.toList());

        if (formulaFields.isEmpty()) return;

        for (SmartTableRecord record : records) {
            enrichSingle(record, formulaFields, fields);
        }
    }

    /**
     * 单条记录富化
     */
    public void enrichSingle(SmartTableRecord record,
                              List<SmartTableField> formulaFields,
                              List<SmartTableField> allFields) {
        if (record == null) return;
        Map<String, Object> data = record.getData();
        if (data == null) {
            data = new HashMap<>();
            record.setData(data);
        }

        for (SmartTableField ff : formulaFields) {
            String expr = getFormulaExpr(ff);
            if (expr == null || expr.isBlank()) continue;

            try {
                Object result = formulaService.evaluate(expr, data, allFields);
                data.put(ff.getFieldKey(), result);
            } catch (Exception e) {
                log.warn("公式字段富化失败 fieldKey={} expr={} err={}",
                        ff.getFieldKey(), expr, e.getMessage());
                data.put(ff.getFieldKey(), "#ERROR!");
            }
        }
    }

    /**
     * 从字段 options 中读取 formula 表达式
     * 约定：options.formula 存储公式字符串
     */
    private String getFormulaExpr(SmartTableField field) {
        if (field.getOptions() == null) return null;
        Object expr = field.getOptions().get("formula");
        return expr != null ? expr.toString() : null;
    }
}
