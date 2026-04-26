package com.pengcheng.crm.customfield.service;

import com.pengcheng.crm.customfield.entity.CustomFieldDef;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 自定义字段 6 类型校验器（纯函数，便于单测）。
 * <ul>
 *   <li>text         —— required + maxLength + pattern</li>
 *   <li>number       —— required + min/max</li>
 *   <li>date         —— required + ISO_LOCAL_DATE/ISO_LOCAL_DATE_TIME</li>
 *   <li>select       —— required + 必须命中 options 的 value</li>
 *   <li>multi_select —— required + value 必须是 List 且每项命中 options</li>
 *   <li>file         —— required + value 应为 url 列表</li>
 * </ul>
 */
public final class CustomFieldValidator {

    public static final List<String> SUPPORTED_TYPES =
            Arrays.asList("text", "number", "date", "select", "multi_select", "file");

    private CustomFieldValidator() {}

    /**
     * @return 错误消息；返回 null 表示通过
     */
    public static String validate(CustomFieldDef def, Object value) {
        if (def == null) return "field def is null";
        boolean required = def.getRequired() != null && def.getRequired() == 1;

        if (isBlank(value)) {
            if (required) return def.getLabel() + " 必填";
            return null;
        }

        Map<String, Object> rules = parseRules(def.getValidationJson());
        Set<String> options = parseOptions(def.getOptionsJson());

        switch (String.valueOf(def.getFieldType())) {
            case "text":
                String s = String.valueOf(value);
                Object maxLen = rules.get("maxLength");
                if (maxLen instanceof Number n && s.length() > n.intValue()) {
                    return def.getLabel() + " 长度超过 " + n.intValue();
                }
                Object pat = rules.get("pattern");
                if (pat instanceof String ps && !ps.isBlank()) {
                    if (!Pattern.matches(ps, s)) {
                        return def.getLabel() + " 格式不匹配";
                    }
                }
                return null;

            case "number":
                BigDecimal num;
                try {
                    num = new BigDecimal(String.valueOf(value));
                } catch (NumberFormatException e) {
                    return def.getLabel() + " 必须是数字";
                }
                Object min = rules.get("min");
                Object max = rules.get("max");
                if (min instanceof Number nn && num.compareTo(BigDecimal.valueOf(nn.doubleValue())) < 0) {
                    return def.getLabel() + " 小于最小值 " + nn;
                }
                if (max instanceof Number nx && num.compareTo(BigDecimal.valueOf(nx.doubleValue())) > 0) {
                    return def.getLabel() + " 大于最大值 " + nx;
                }
                return null;

            case "date":
                String dStr = String.valueOf(value);
                try {
                    if (dStr.length() <= 10) {
                        LocalDate.parse(dStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    } else {
                        LocalDateTime.parse(dStr.replace(' ', 'T'));
                    }
                } catch (Exception ex) {
                    return def.getLabel() + " 不是合法日期";
                }
                return null;

            case "select":
                if (options.isEmpty()) return null;
                if (!options.contains(String.valueOf(value))) {
                    return def.getLabel() + " 不在可选项中";
                }
                return null;

            case "multi_select":
                if (!(value instanceof Collection<?> coll)) {
                    return def.getLabel() + " 必须是数组";
                }
                if (options.isEmpty()) return null;
                for (Object item : coll) {
                    if (!options.contains(String.valueOf(item))) {
                        return def.getLabel() + " 含不在可选项中的值: " + item;
                    }
                }
                return null;

            case "file":
                // 接受 url 字符串 / url 数组
                if (value instanceof Collection<?> files) {
                    for (Object f : files) {
                        if (!isUrlLike(String.valueOf(f))) {
                            return def.getLabel() + " 含非法文件链接";
                        }
                    }
                    return null;
                }
                if (value instanceof String fs) {
                    return isUrlLike(fs) ? null : def.getLabel() + " 文件链接非法";
                }
                return def.getLabel() + " 文件值类型非法";

            default:
                return def.getLabel() + " 不支持的字段类型: " + def.getFieldType();
        }
    }

    private static boolean isBlank(Object value) {
        if (value == null) return true;
        if (value instanceof String s) return s.isBlank();
        if (value instanceof Collection<?> c) return c.isEmpty();
        return false;
    }

    private static boolean isUrlLike(String s) {
        if (s == null) return false;
        return s.startsWith("http://") || s.startsWith("https://") || s.startsWith("/") || s.startsWith("oss://") || s.startsWith("minio://");
    }

    /**
     * 极简 JSON 解析：只识别字面量 {"key":val,...}。
     * 实际生产可换 Jackson；测试不依赖完整 JSON 解析。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseRules(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static Set<String> parseOptions(String json) {
        Set<String> result = new HashSet<>();
        if (json == null || json.isBlank()) return result;
        try {
            List<Map<String, Object>> arr = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, List.class);
            for (Map<String, Object> item : arr) {
                Object v = item.get("value");
                if (v != null) result.add(String.valueOf(v));
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
