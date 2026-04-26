package com.pengcheng.crm.tag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.common.exception.BizErrorCode;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.crm.tag.entity.CustomerTag;
import com.pengcheng.crm.tag.entity.CustomerTagRel;
import com.pengcheng.crm.tag.mapper.CustomerTagMapper;
import com.pengcheng.crm.tag.mapper.CustomerTagRelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CustomerTagService {

    @Autowired
    private CustomerTagMapper tagMapper;

    @Autowired
    private CustomerTagRelMapper relMapper;

    public CustomerTag createTag(CustomerTag tag) {
        if (tag.getTagName() == null || tag.getTagName().isBlank()) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "tagName 必填");
        }
        if (tag.getEnabled() == null) tag.setEnabled(1);
        if (tag.getSortOrder() == null) tag.setSortOrder(100);
        tagMapper.insert(tag);
        return tag;
    }

    public List<CustomerTag> listTags() {
        return tagMapper.selectList(new LambdaQueryWrapper<CustomerTag>()
                .eq(CustomerTag::getEnabled, 1)
                .orderByAsc(CustomerTag::getSortOrder));
    }

    @Transactional(rollbackFor = Exception.class)
    public void setCustomerTags(Long customerId, List<Long> tagIds) {
        if (customerId == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "customerId 必填");
        }
        relMapper.delete(new LambdaQueryWrapper<CustomerTagRel>()
                .eq(CustomerTagRel::getCustomerId, customerId));
        if (tagIds == null || tagIds.isEmpty()) return;
        Set<Long> dedup = new HashSet<>(tagIds);
        for (Long tagId : dedup) {
            relMapper.insert(CustomerTagRel.builder()
                    .customerId(customerId)
                    .tagId(tagId)
                    .createTime(LocalDateTime.now())
                    .build());
        }
    }

    public List<Long> listCustomerTagIds(Long customerId) {
        return relMapper.selectList(new LambdaQueryWrapper<CustomerTagRel>()
                .eq(CustomerTagRel::getCustomerId, customerId))
                .stream().map(CustomerTagRel::getTagId).toList();
    }
}
