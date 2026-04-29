package com.pengcheng.system.doc.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.doc.collab.entity.DocCollabState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Y.js 文档协作状态 Mapper
 */
@Mapper
public interface DocCollabStateMapper extends BaseMapper<DocCollabState> {

    /**
     * 按 docId 查询最新快照
     */
    @Select("SELECT * FROM sys_doc_collab_state WHERE doc_id = #{docId}")
    DocCollabState selectByDocId(@Param("docId") Long docId);

    /**
     * 原子性递增版本号并更新 blob
     * 若记录不存在则由 Service 层先 insert
     */
    @Update("UPDATE sys_doc_collab_state " +
            "SET update_blob = #{updateBlob}, state_vector = #{stateVector}, " +
            "    version = version + 1, last_updater = #{lastUpdater} " +
            "WHERE doc_id = #{docId}")
    int updateBlob(@Param("docId") Long docId,
                   @Param("stateVector") byte[] stateVector,
                   @Param("updateBlob") byte[] updateBlob,
                   @Param("lastUpdater") Long lastUpdater);
}
