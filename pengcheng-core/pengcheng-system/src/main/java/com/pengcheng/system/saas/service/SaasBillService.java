package com.pengcheng.system.saas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.saas.entity.SaasBill;
import com.pengcheng.system.saas.mapper.SaasBillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaasBillService {

    private final SaasBillMapper mapper;

    /**
     * 月初批量为活跃订阅生成账单（占位实现，留 TODO）。
     * <p>真实实现：扫所有 ACTIVE subscription → 按 plan.price + overage（API/Storage 超量计费）拼 SaasBill INSERT。
     */
    public int generateBills(String yyyymm) {
        // TODO Phase 6 收尾：扫订阅 + 计算 overage + 生成 bill_no + INSERT
        log.warn("[SaasBill] generateBills({}) 占位实现，后续工单填充", yyyymm);
        return 0;
    }

    public void payBill(Long id) {
        SaasBill bill = mapper.selectById(id);
        if (bill == null) throw new IllegalArgumentException("bill not found");
        bill.setStatus(SaasBill.STATUS_PAID);
        bill.setPaidAt(LocalDateTime.now());
        mapper.updateById(bill);
    }

    public List<SaasBill> findUnpaid(Long tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<SaasBill>()
                .eq(SaasBill::getTenantId, tenantId)
                .in(SaasBill::getStatus, SaasBill.STATUS_UNPAID, SaasBill.STATUS_OVERDUE)
                .orderByDesc(SaasBill::getPeriodEnd));
    }
}
