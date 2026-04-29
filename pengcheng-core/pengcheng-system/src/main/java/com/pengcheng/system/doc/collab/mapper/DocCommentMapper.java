package com.pengcheng.system.doc.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.doc.collab.entity.DocComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档评论 Mapper
 */
@Mapper
public interface DocCommentMapper extends BaseMapper<DocComment> {

    /**
     * 按 docId 查询所有评论，按创建时间升序
     */
    @Select("SELECT * FROM sys_doc_comment WHERE doc_id = #{docId} ORDER BY create_time ASC")
    List<DocComment> selectByDocId(@Param("docId") Long docId);
}
