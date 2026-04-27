package com.pengcheng.bi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.bi.model.entity.BiSavedQuery;
import com.pengcheng.bi.model.mapper.BiSavedQueryMapper;
import com.pengcheng.bi.service.BiSavedQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户保存查询服务实现。
 */
@Service
@RequiredArgsConstructor
public class BiSavedQueryServiceImpl implements BiSavedQueryService {

    private final BiSavedQueryMapper savedQueryMapper;

    @Override
    public Long saveQuery(BiSavedQuery query) {
        query.setCreateTime(LocalDateTime.now());
        savedQueryMapper.insert(query);
        return query.getId();
    }

    @Override
    public List<BiSavedQuery> listByUser(Long userId) {
        return savedQueryMapper.selectList(
                new LambdaQueryWrapper<BiSavedQuery>()
                        .eq(BiSavedQuery::getUserId, userId)
                        .orderByDesc(BiSavedQuery::getCreateTime)
        );
    }

    @Override
    public void deleteById(Long id, Long userId) {
        int rows = savedQueryMapper.delete(
                new LambdaQueryWrapper<BiSavedQuery>()
                        .eq(BiSavedQuery::getId, id)
                        .eq(BiSavedQuery::getUserId, userId)
        );
        if (rows == 0) {
            throw new IllegalArgumentException("保存查询不存在或无权限删除，id=" + id);
        }
    }
}
