package com.pengcheng.system.smarttable.automation.action;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pengcheng.system.smarttable.automation.AutomationEvent;
import com.pengcheng.system.smarttable.entity.SmartTableRecord;
import com.pengcheng.system.smarttable.mapper.SmartTableRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 动作：更新记录字段（UPDATE_RECORD）
 *
 * <p>在同一事务中反向更新 smart_table_record 的 data 字段。
 *
 * <p>params 字段说明：
 * <pre>
 * {
 *   "recordId":  123,           // 目标记录 ID，省略时使用触发记录 ID
 *   "fields": {                 // 要更新的字段 key→value
 *     "status": "done",
 *     "remark": "自动更新"
 *   }
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateRecordAction implements AutomationAction {

    private final SmartTableRecordMapper recordMapper;

    @Override
    public String type() {
        return "UPDATE_RECORD";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(Map<String, Object> params, AutomationEvent event) throws Exception {
        Long recordId = toLong(params.get("recordId"));
        if (recordId == null) {
            recordId = event.getRecordId();
        }
        if (recordId == null) {
            throw new IllegalArgumentException("UPDATE_RECORD 动作缺少 recordId 且事件无 recordId");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) params.get("fields");
        if (fields == null || fields.isEmpty()) {
            log.warn("[Automation] UPDATE_RECORD fields 为空，跳过");
            return;
        }

        SmartTableRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalStateException("UPDATE_RECORD 目标记录不存在: recordId=" + recordId);
        }

        Map<String, Object> newData = new HashMap<>(record.getData() != null ? record.getData() : Map.of());
        newData.putAll(fields);
        record.setData(newData);
        recordMapper.updateById(record);

        log.info("[Automation] UPDATE_RECORD 成功: recordId={}, fields={}", recordId, fields.keySet());
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return null; }
    }
}
