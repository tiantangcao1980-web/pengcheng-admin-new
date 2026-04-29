package com.pengcheng.system.i18n.service;

import com.pengcheng.system.i18n.entity.UserLocalePreference;
import com.pengcheng.system.i18n.mapper.UserLocalePreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserLocalePreferenceService {

    private final UserLocalePreferenceMapper mapper;

    public UserLocalePreference get(Long userId) {
        return mapper.findByUserId(userId);
    }

    public void upsert(UserLocalePreference pref) {
        UserLocalePreference exist = mapper.findByUserId(pref.getUserId());
        if (exist == null) {
            mapper.insert(pref);
        } else {
            pref.setId(exist.getId());
            mapper.updateById(pref);
        }
    }
}
