package com.pengcheng.system.smarttable.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.smarttable.market.entity.SmartTableTemplateDownload;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SmartTableTemplateDownloadMapper extends BaseMapper<SmartTableTemplateDownload> {

    /** 原子 +1 download_count（避免 SELECT-then-UPDATE 并发）。 */
    @Update("UPDATE smart_table_template SET download_count = download_count + 1 WHERE id = #{tplId}")
    int incrementDownloadCount(@Param("tplId") Long templateId);
}
