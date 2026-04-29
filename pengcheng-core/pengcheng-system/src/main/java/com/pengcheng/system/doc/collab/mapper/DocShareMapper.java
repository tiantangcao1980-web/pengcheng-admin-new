package com.pengcheng.system.doc.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.doc.collab.entity.DocShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档分享权限 Mapper
 */
@Mapper
public interface DocShareMapper extends BaseMapper<DocShare> {

    /**
     * 按链接访问码查询分享记录
     */
    @Select("SELECT * FROM sys_doc_share WHERE share_code = #{code} LIMIT 1")
    DocShare selectByShareCode(@Param("code") String code);

    /**
     * 按 docId 和目标类型查询分享列表
     */
    @Select("SELECT * FROM sys_doc_share WHERE doc_id = #{docId} AND target_type = #{targetType}")
    List<DocShare> selectByDocAndType(@Param("docId") Long docId, @Param("targetType") String targetType);

    /**
     * 查询指定文档下某用户的最高权限（USER 类型）
     */
    @Select("SELECT * FROM sys_doc_share WHERE doc_id = #{docId} AND target_type = 'USER' AND target_id = #{userId} LIMIT 1")
    DocShare selectUserShare(@Param("docId") Long docId, @Param("userId") Long userId);

    /**
     * 查询文档下某部门的分享记录
     */
    @Select("SELECT * FROM sys_doc_share WHERE doc_id = #{docId} AND target_type = 'DEPT' AND target_id = #{deptId} LIMIT 1")
    DocShare selectDeptShare(@Param("docId") Long docId, @Param("deptId") Long deptId);
}
