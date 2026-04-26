package com.pengcheng.crm.lead;

import com.pengcheng.crm.lead.service.LeadAssignmentRule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeadAssignmentRuleEdgeTest {

    @Test
    void null_ruleType_treated_as_manual() {
        assertEquals(7L, LeadAssignmentRule.pick(null, 7L, null, null, null, null));
    }

    @Test
    void loadBalance_empty_returns_null() {
        assertNull(LeadAssignmentRule.pick("load_balance", null, java.util.Collections.emptyList(),
                new HashMap<>(), null, null));
    }

    @Test
    void loadBalance_default_zero_load() {
        // currentLoad 为空 -> 大家都是 0，挑第一个
        Long picked = LeadAssignmentRule.pick("load_balance", null, Arrays.asList(11L, 22L, 33L),
                new HashMap<>(), null, null);
        assertEquals(11L, picked);
    }

    @Test
    void rule_with_null_source_falls_back_to_round_robin() {
        Map<String, Long> mapping = new HashMap<>();
        mapping.put("qrcode", 99L);
        Long picked = LeadAssignmentRule.pick("rule", null, Arrays.asList(5L, 6L),
                new HashMap<>(), null, mapping);
        assertNotNull(picked);
    }
}
