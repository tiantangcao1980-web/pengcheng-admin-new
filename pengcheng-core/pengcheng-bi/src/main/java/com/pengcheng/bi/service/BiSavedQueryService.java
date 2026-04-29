package com.pengcheng.bi.service;

import com.pengcheng.bi.model.entity.BiSavedQuery;

import java.util.List;

/**
 * 用户保存查询服务接口。
 */
public interface BiSavedQueryService {

    /**
     * 保存当前用户的查询配置。
     *
     * @param query  保存的查询实体（userId / viewCode / name / queryJson 必填）
     * @return 新记录 ID
     */
    Long saveQuery(BiSavedQuery query);

    /**
     * 查询指定用户的所有保存记录（按 create_time 倒序）。
     *
     * @param userId 用户 ID
     * @return 保存查询列表
     */
    List<BiSavedQuery> listByUser(Long userId);

    /**
     * 删除指定保存查询（仅允许本人删除）。
     *
     * @param id     记录 ID
     * @param userId 当前登录用户 ID（鉴权）
     */
    void deleteById(Long id, Long userId);
}
