package com.pengcheng.oa.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.oa.flow.entity.ApprovalFlowDef;
import com.pengcheng.oa.flow.entity.ApprovalFlowNode;
import com.pengcheng.oa.flow.mapper.ApprovalFlowDefMapper;
import com.pengcheng.oa.flow.mapper.ApprovalFlowNodeMapper;
import com.pengcheng.oa.flow.service.ApprovalFlowDefService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalFlowDefServiceImpl implements ApprovalFlowDefService {

    private final ApprovalFlowDefMapper defMapper;
    private final ApprovalFlowNodeMapper nodeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDef(ApprovalFlowDef def, List<ApprovalFlowNode> nodes) {
        validateDef(def);
        if (def.getEnabled() == null) def.setEnabled(1);
        if (def.getIsDefault() == null) def.setIsDefault(0);
        if (Integer.valueOf(1).equals(def.getIsDefault())) {
            unsetOtherDefaults(def.getBizType(), null);
        }
        defMapper.insert(def);
        saveNodes(def.getId(), nodes);
        return def.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDef(ApprovalFlowDef def, List<ApprovalFlowNode> nodes) {
        if (def.getId() == null) {
            throw new IllegalArgumentException("流程定义ID不能为空");
        }
        validateDef(def);
        if (Integer.valueOf(1).equals(def.getIsDefault())) {
            unsetOtherDefaults(def.getBizType(), def.getId());
        }
        defMapper.updateById(def);
        // 删旧节点，插入新节点
        nodeMapper.delete(new LambdaQueryWrapper<ApprovalFlowNode>().eq(ApprovalFlowNode::getFlowDefId, def.getId()));
        saveNodes(def.getId(), nodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDef(Long id) {
        nodeMapper.delete(new LambdaQueryWrapper<ApprovalFlowNode>().eq(ApprovalFlowNode::getFlowDefId, id));
        defMapper.deleteById(id);
    }

    @Override
    public ApprovalFlowDef getDef(Long id) {
        return defMapper.selectById(id);
    }

    @Override
    public List<ApprovalFlowNode> listNodes(Long defId) {
        if (defId == null) return Collections.emptyList();
        LambdaQueryWrapper<ApprovalFlowNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalFlowNode::getFlowDefId, defId)
                .orderByAsc(ApprovalFlowNode::getNodeOrder);
        return nodeMapper.selectList(wrapper);
    }

    @Override
    public List<ApprovalFlowDef> listByBizType(String bizType) {
        LambdaQueryWrapper<ApprovalFlowDef> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(ApprovalFlowDef::getBizType, bizType);
        }
        wrapper.orderByDesc(ApprovalFlowDef::getIsDefault).orderByDesc(ApprovalFlowDef::getId);
        return defMapper.selectList(wrapper);
    }

    @Override
    public List<ApprovalFlowDef> listAll() {
        return defMapper.selectList(new LambdaQueryWrapper<>());
    }

    private void validateDef(ApprovalFlowDef def) {
        if (def.getBizType() == null || def.getBizType().isBlank()) {
            throw new IllegalArgumentException("业务类型不能为空");
        }
        if (def.getName() == null || def.getName().isBlank()) {
            throw new IllegalArgumentException("流程名称不能为空");
        }
    }

    private void unsetOtherDefaults(String bizType, Long excludeId) {
        LambdaQueryWrapper<ApprovalFlowDef> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalFlowDef::getBizType, bizType)
                .eq(ApprovalFlowDef::getIsDefault, 1);
        if (excludeId != null) wrapper.ne(ApprovalFlowDef::getId, excludeId);
        List<ApprovalFlowDef> existing = defMapper.selectList(wrapper);
        for (ApprovalFlowDef d : existing) {
            d.setIsDefault(0);
            defMapper.updateById(d);
        }
    }

    private void saveNodes(Long defId, List<ApprovalFlowNode> nodes) {
        if (nodes == null) return;
        List<ApprovalFlowNode> normalized = new ArrayList<>(nodes);
        // 按 nodeOrder 升序，nodeOrder 缺失则按插入顺序补 1..N
        boolean missingOrder = normalized.stream().anyMatch(n -> n.getNodeOrder() == null);
        if (missingOrder) {
            for (int i = 0; i < normalized.size(); i++) {
                normalized.get(i).setNodeOrder(i + 1);
            }
        } else {
            normalized.sort((a, b) -> Integer.compare(a.getNodeOrder(), b.getNodeOrder()));
        }
        for (ApprovalFlowNode n : normalized) {
            n.setId(null);
            n.setFlowDefId(defId);
            nodeMapper.insert(n);
        }
    }
}
