package com.pengcheng.system.i18n.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.i18n.entity.I18nMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface I18nMessageMapper extends BaseMapper<I18nMessage> {

    @Select("SELECT * FROM i18n_message WHERE locale = #{locale}")
    List<I18nMessage> findByLocale(@Param("locale") String locale);
}
