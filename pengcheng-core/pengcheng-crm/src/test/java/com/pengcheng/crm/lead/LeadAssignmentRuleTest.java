package com.pengcheng.crm.lead;

import com.pengcheng.crm.lead.service.LeadAssignmentRule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeadAssignmentRuleTest {

    @Test
    void manual_returns_targetUserId() {
        Long picked = LeadAssignmentRule.pick("manual", 7L, null, null, null, null);
        assertEquals(7L, picked);
    }

    @Test
    void manual_without_target_returns_null() {
        assertNull(LeadAssignmentRule.pick("manual", null, null, null, null, null));
    }

    @Test
    void roundRobin_cycles_through_candidates() {
        Map<Long, Integer> state = new HashMap<>();
        Long a = LeadAssignmentRule.pick("round_robin", null, Arrays.asList(1L, 2L, 3L), state, null, null);
        Long b = LeadAssignmentRule.pick("round_robin", null, Arrays.asList(1L, 2L, 3L), state, null, null);
        Long c = LeadAssignmentRule.pick("round_robin", null, Arrays.asList(1L, 2L, 3L), state, null, null);
        Long d = LeadAssignmentRule.pick("round_robin", null, Arrays.asList(1L, 2L, 3L), state, null, null);
        assertEquals(1L, a);
        assertEquals(2L, b);
        assertEquals(3L, c);
        assertEquals(1L, d, "应回到第一个");
    }

    @Test
    void roundRobin_empty_candidates_returns_null() {
        assertNull(LeadAssignmentRule.pick("round_robin", null, java.util.Collections.emptyList(),
                new HashMap<>(), null, null));
    }

    @Test
    void loadBalance_picks_least_loaded() {
        Map<Long, Integer> load = new HashMap<>();
        load.put(1L, 5);
        load.put(2L, 1);
        load.put(3L, 9);
        Long picked = LeadAssignmentRule.pick("load_balance", null, Arrays.asList(1L, 2L, 3L), load, null, null);
        assertEquals(2L, picked);
    }

    @Test
    void rule_uses_sourceMapping_first() {
        Map<String, Long> mapping = new HashMap<>();
        mapping.put("qrcode", 99L);
        Long picked = LeadAssignmentRule.pick("rule", null, Arrays.asList(1L, 2L),
                new HashMap<>(), "qrcode", mapping);
        assertEquals(99L, picked);
    }

    @Test
    void rule_falls_back_to_round_robin_when_no_mapping() {
        Long picked = LeadAssignmentRule.pick("rule", null, Arrays.asList(1L, 2L, 3L),
                new HashMap<>(), "form", null);
        assertNotNull(picked);
    }

    @Test
    void unknown_rule_returns_null() {
        assertNull(LeadAssignmentRule.pick("nonsense", 1L, Arrays.asList(1L), new HashMap<>(), null, null));
    }
}
