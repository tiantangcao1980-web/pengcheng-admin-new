package com.pengcheng.system.smarttable.automation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConditionEvaluator — 条件 DSL 评估")
class ConditionEvaluatorTest {

    private ConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ConditionEvaluator(new ObjectMapper());
    }

    @Test
    @DisplayName("AND：全部子条件满足时返回 true")
    void and_allMatch_returnsTrue() throws Exception {
        String cond = """
                {
                  "op": "AND",
                  "children": [
                    {"op": "EQ", "field": "status", "value": "done"},
                    {"op": "GT", "field": "amount", "value": 100}
                  ]
                }
                """;
        Map<String, Object> row = Map.of("status", "done", "amount", 200);
        assertThat(evaluator.evaluate(cond, row)).isTrue();
    }

    @Test
    @DisplayName("OR：只要一个子条件满足即返回 true")
    void or_oneMatch_returnsTrue() throws Exception {
        String cond = """
                {
                  "op": "OR",
                  "children": [
                    {"op": "EQ", "field": "status", "value": "done"},
                    {"op": "EQ", "field": "status", "value": "pending"}
                  ]
                }
                """;
        Map<String, Object> row = Map.of("status", "pending");
        assertThat(evaluator.evaluate(cond, row)).isTrue();
    }

    @Test
    @DisplayName("NOT：取反子条件")
    void not_negatesChild() throws Exception {
        String cond = """
                {
                  "op": "NOT",
                  "children": [
                    {"op": "EQ", "field": "status", "value": "done"}
                  ]
                }
                """;
        Map<String, Object> row = Map.of("status", "pending");
        assertThat(evaluator.evaluate(cond, row)).isTrue();

        Map<String, Object> rowDone = Map.of("status", "done");
        assertThat(evaluator.evaluate(cond, rowDone)).isFalse();
    }

    @Test
    @DisplayName("NULL 安全：conditionJson 为 null 时视为无条件（返回 true）")
    void nullCondition_alwaysTrue() {
        assertThat(evaluator.evaluate(null, Map.of("x", "y"))).isTrue();
        assertThat(evaluator.evaluate("", Map.of())).isTrue();
    }

    @Test
    @DisplayName("字段不存在：除 EMPTY 外均返回 false")
    void fieldNotExist_returnsFalse() throws Exception {
        String cond = """
                {"op": "EQ", "field": "nonexistent", "value": "v"}
                """;
        assertThat(evaluator.evaluate(cond, Map.of("other", "x"))).isFalse();

        String emptyCond = """
                {"op": "EMPTY", "field": "nonexistent"}
                """;
        assertThat(evaluator.evaluate(emptyCond, Map.of("other", "x"))).isTrue();
    }
}
