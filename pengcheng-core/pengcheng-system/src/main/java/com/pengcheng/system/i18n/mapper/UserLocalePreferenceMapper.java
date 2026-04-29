package com.pengcheng.system.i18n.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.i18n.entity.UserLocalePreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserLocalePreferenceMapper extends BaseMapper<UserLocalePreference> {

    @Select("SELECT * FROM user_locale_preference WHERE user_id = #{userId} LIMIT 1")
    UserLocalePreference findByUserId(@Param("userId") Long userId);
}
