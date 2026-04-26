package com.pengcheng.crm.ext.service;

import com.pengcheng.crm.ext.entity.CustomerRealtyExt;
import com.pengcheng.crm.ext.mapper.CustomerRealtyExtMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 房产行业扩展字段读写适配器。
 * <p>用法：上层在写 customer 主表的同时，调用 {@link #upsert(CustomerRealtyExt)} 双写到扩展表。
 * 待主表行业字段下线后，由本 Service 单写。
 */
@Service
public class CustomerRealtyExtService {

    @Autowired
    private CustomerRealtyExtMapper extMapper;

    public CustomerRealtyExt get(Long customerId) {
        if (customerId == null) return null;
        return extMapper.selectById(customerId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void upsert(CustomerRealtyExt ext) {
        if (ext == null || ext.getCustomerId() == null) return;
        LocalDateTime now = LocalDateTime.now();
        ext.setUpdateTime(now);
        CustomerRealtyExt existing = extMapper.selectById(ext.getCustomerId());
        if (existing == null) {
            ext.setCreateTime(now);
            extMapper.insert(ext);
        } else {
            extMapper.updateById(ext);
        }
    }
}
