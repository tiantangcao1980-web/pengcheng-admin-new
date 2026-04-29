package com.pengcheng.integration.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.integration.config.IntegrationDeptMapping;
import com.pengcheng.integration.config.IntegrationDeptMappingMapper;
import com.pengcheng.integration.config.IntegrationProviderConfig;
import com.pengcheng.integration.config.IntegrationProviderConfigMapper;
import com.pengcheng.integration.config.IntegrationSyncLog;
import com.pengcheng.integration.config.IntegrationSyncLogMapper;
import com.pengcheng.integration.config.IntegrationUserMapping;
import com.pengcheng.integration.config.IntegrationUserMappingMapper;
import com.pengcheng.integration.spi.ImContactService;
import com.pengcheng.integration.spi.dto.ContactSyncResult;
import com.pengcheng.system.entity.SysUser;
import com.pengcheng.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业微信通讯录同步实现。
 * <p>
 * 同步策略（全量 upsert，不删除本地用户）：
 * 1. 拉取全量部门树 → upsert integration_dept_mapping
 * 2. 按部门 cursor 翻页拉 userId → 批量调 user/get 拿详情
 * 3. 以 externalId 为键：找不到 mapping → 创建 sys_user + mapping；找到 → 更新 sys_user 基础信息
 * 4. 写 integration_sync_log
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WecomContactServiceImpl implements ImContactService {

    private static final String DEPT_LIST_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/department/list?access_token=%s";

    private static final String USER_LIST_ID_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/user/list_id?access_token=%s";

    private static final String USER_GET_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/user/get?access_token=%s&userid=%s";

    private final IntegrationProviderConfigMapper configMapper;
    private final IntegrationDeptMappingMapper    deptMappingMapper;
    private final IntegrationUserMappingMapper    userMappingMapper;
    private final IntegrationSyncLogMapper        syncLogMapper;
    private final SysUserMapper                   sysUserMapper;
    private final WecomTokenCache                 tokenCache;
    private final WecomHttpClient                 httpClient;
    private final ObjectMapper                    objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContactSyncResult syncContacts(Long tenantId) {
        long startMs = System.currentTimeMillis();
        ContactSyncResult result = new ContactSyncResult();
        try {
            IntegrationProviderConfig cfg    = loadConfig(tenantId);
            String                    token  = tokenCache.getToken(cfg.getCorpId(), cfg.getSecretRef());

            // Step 1: 同步部门
            int deptSynced = syncDepts(tenantId, cfg.getProvider(), token);
            result.setDeptSynced(deptSynced);

            // Step 2: 同步用户
            List<String> allUserIds = fetchAllUserIds(token);
            int created = 0, updated = 0;
            for (String externalUserId : allUserIds) {
                boolean isNew = upsertUser(tenantId, cfg.getProvider(), token, externalUserId);
                if (isNew) created++; else updated++;
            }
            result.setUserSynced(allUserIds.size())
                  .setUserCreated(created)
                  .setUserUpdated(updated)
                  .setSuccess(true);

        } catch (Exception e) {
            log.error("[WecomContact] sync failed for tenantId={}", tenantId, e);
            result.setSuccess(false).setErrorMsg(e.getMessage());
        }

        long duration = System.currentTimeMillis() - startMs;
        result.setDurationMs(duration);
        writeSyncLog(tenantId, "CONTACT", result, (int) duration);
        return result;
    }

    // ---- private helpers ----

    @SuppressWarnings("unchecked")
    private int syncDepts(Long tenantId, String provider, String token) {
        String url = String.format(DEPT_LIST_URL, token);
        Map<String, Object> resp = httpClient.get(url);
        List<Map<String, Object>> deptList = (List<Map<String, Object>>) resp.get("department");
        if (deptList == null) return 0;

        for (Map<String, Object> dept : deptList) {
            String externalId = String.valueOf(dept.get("id"));
            Object parentObj  = dept.get("parentid");
            String parentId   = parentObj != null ? String.valueOf(parentObj) : null;

            IntegrationDeptMapping existing = deptMappingMapper.selectOne(
                    new LambdaQueryWrapper<IntegrationDeptMapping>()
                            .eq(IntegrationDeptMapping::getTenantId, tenantId)
                            .eq(IntegrationDeptMapping::getProvider, provider)
                            .eq(IntegrationDeptMapping::getExternalId, externalId));

            if (existing == null) {
                IntegrationDeptMapping mapping = new IntegrationDeptMapping();
                mapping.setTenantId(tenantId);
                mapping.setProvider(provider);
                mapping.setExternalId(externalId);
                mapping.setExternalParentId(parentId);
                mapping.setDeptId(0L); // 未关联本地部门时占位 0
                mapping.setCreateTime(LocalDateTime.now());
                deptMappingMapper.insert(mapping);
            } else {
                existing.setExternalParentId(parentId);
                deptMappingMapper.updateById(existing);
            }
        }
        return deptList.size();
    }

    /**
     * 使用 cursor 翻页拉取全量 userId 列表。
     */
    @SuppressWarnings("unchecked")
    private List<String> fetchAllUserIds(String token) {
        List<String> result = new ArrayList<>();
        String cursor = "";
        do {
            Map<String, Object> body = new HashMap<>();
            body.put("cursor", cursor);
            body.put("limit", 1000);

            String url = String.format(USER_LIST_ID_URL, token);
            Map<String, Object> resp = httpClient.post(url, body);

            List<Map<String, Object>> userList =
                    (List<Map<String, Object>>) resp.get("dept_user");
            if (userList != null) {
                for (Map<String, Object> u : userList) {
                    result.add(String.valueOf(u.get("userid")));
                }
            }
            cursor = (String) resp.getOrDefault("next_cursor", "");
        } while (!cursor.isBlank());
        return result;
    }

    /**
     * upsert 单个用户。返回 true=新增，false=更新。
     */
    @SuppressWarnings("unchecked")
    private boolean upsertUser(Long tenantId, String provider, String token, String externalId) {
        String url = String.format(USER_GET_URL, token, externalId);
        Map<String, Object> detail = httpClient.get(url);

        String name   = String.valueOf(detail.getOrDefault("name", externalId));
        String mobile = String.valueOf(detail.getOrDefault("mobile", ""));
        String email  = String.valueOf(detail.getOrDefault("email", ""));
        String avatar = String.valueOf(detail.getOrDefault("avatar", ""));

        // 序列化部门 ID 列表
        Object deptObj = detail.get("department");
        String deptIdsJson = "[]";
        if (deptObj instanceof List<?> deptList) {
            try {
                deptIdsJson = objectMapper.writeValueAsString(deptList);
            } catch (Exception ignored) {}
        }

        // 查 mapping
        IntegrationUserMapping mapping = userMappingMapper.selectOne(
                new LambdaQueryWrapper<IntegrationUserMapping>()
                        .eq(IntegrationUserMapping::getTenantId, tenantId)
                        .eq(IntegrationUserMapping::getProvider, provider)
                        .eq(IntegrationUserMapping::getExternalId, externalId));

        if (mapping == null) {
            // 新建 sys_user（简化：仅填充基础字段，密码随机）
            SysUser user = new SysUser();
            user.setNickname(name);
            user.setUsername("wecom_" + externalId);
            user.setPassword("{noop}WECOM_SSO");
            user.setPhone(mobile.equals("null") ? null : mobile);
            user.setEmail(email.equals("null") ? null : email);
            user.setAvatar(avatar.equals("null") ? null : avatar);
            user.setStatus(1);
            user.setUserType("pc");
            sysUserMapper.insert(user);

            IntegrationUserMapping newMapping = new IntegrationUserMapping();
            newMapping.setTenantId(tenantId);
            newMapping.setUserId(user.getId());
            newMapping.setProvider(provider);
            newMapping.setExternalId(externalId);
            newMapping.setExternalDeptIds(deptIdsJson);
            newMapping.setAvatar(avatar.equals("null") ? null : avatar);
            newMapping.setBindAt(LocalDateTime.now());
            userMappingMapper.insert(newMapping);
            return true;
        } else {
            // 更新基础信息
            SysUser user = sysUserMapper.selectById(mapping.getUserId());
            if (user != null) {
                user.setNickname(name);
                if (!mobile.equals("null") && !mobile.isBlank()) user.setPhone(mobile);
                if (!email.equals("null") && !email.isBlank()) user.setEmail(email);
                if (!avatar.equals("null") && !avatar.isBlank()) user.setAvatar(avatar);
                sysUserMapper.updateById(user);
            }
            mapping.setExternalDeptIds(deptIdsJson);
            mapping.setAvatar(avatar.equals("null") ? null : avatar);
            userMappingMapper.updateById(mapping);
            return false;
        }
    }

    private void writeSyncLog(Long tenantId, String syncType, ContactSyncResult result, int durationMs) {
        IntegrationSyncLog syncLog = new IntegrationSyncLog();
        syncLog.setTenantId(tenantId);
        syncLog.setProvider("wecom");
        syncLog.setSyncType(syncType);
        syncLog.setSuccess(result.isSuccess() ? 1 : 0);
        syncLog.setAffected(result.getUserSynced());
        syncLog.setErrorMsg(result.getErrorMsg());
        syncLog.setDurationMs(durationMs);
        syncLog.setCreateTime(LocalDateTime.now());
        syncLogMapper.insert(syncLog);
    }

    private IntegrationProviderConfig loadConfig(Long tenantId) {
        IntegrationProviderConfig cfg = configMapper.selectOne(
                new LambdaQueryWrapper<IntegrationProviderConfig>()
                        .eq(IntegrationProviderConfig::getTenantId, tenantId)
                        .eq(IntegrationProviderConfig::getProvider, "wecom"));
        if (cfg == null) {
            throw new IllegalStateException("No wecom config for tenantId=" + tenantId);
        }
        return cfg;
    }
}
