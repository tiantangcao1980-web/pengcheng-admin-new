package com.pengcheng.integration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.integration.config.IntegrationDeptMapping;
import com.pengcheng.integration.config.IntegrationDeptMappingMapper;
import com.pengcheng.integration.config.IntegrationUserMapping;
import com.pengcheng.integration.config.IntegrationUserMappingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 用户/部门 Mapping 操作 Service。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationMappingService {

    private final IntegrationUserMappingMapper userMappingMapper;
    private final IntegrationDeptMappingMapper deptMappingMapper;

    // ---- User Mapping ----

    /**
     * 根据内部 userId + provider 查询 mapping。
     */
    public Optional<IntegrationUserMapping> findUserMapping(Long userId, String provider) {
        return Optional.ofNullable(userMappingMapper.selectOne(
                new LambdaQueryWrapper<IntegrationUserMapping>()
                        .eq(IntegrationUserMapping::getUserId, userId)
                        .eq(IntegrationUserMapping::getProvider, provider)));
    }

    /**
     * 根据外部 externalId + provider 查询 mapping。
     */
    public Optional<IntegrationUserMapping> findUserMappingByExternalId(Long tenantId, String provider, String externalId) {
        return Optional.ofNullable(userMappingMapper.selectOne(
                new LambdaQueryWrapper<IntegrationUserMapping>()
                        .eq(IntegrationUserMapping::getTenantId, tenantId)
                        .eq(IntegrationUserMapping::getProvider, provider)
                        .eq(IntegrationUserMapping::getExternalId, externalId)));
    }

    /**
     * 查询租户下所有用户 mapping。
     */
    public List<IntegrationUserMapping> listUserMappings(Long tenantId, String provider) {
        return userMappingMapper.selectList(
                new LambdaQueryWrapper<IntegrationUserMapping>()
                        .eq(IntegrationUserMapping::getTenantId, tenantId)
                        .eq(IntegrationUserMapping::getProvider, provider));
    }

    /**
     * 删除用户 mapping。
     */
    public void deleteUserMapping(Long userId, String provider) {
        userMappingMapper.delete(
                new LambdaQueryWrapper<IntegrationUserMapping>()
                        .eq(IntegrationUserMapping::getUserId, userId)
                        .eq(IntegrationUserMapping::getProvider, provider));
    }

    // ---- Dept Mapping ----

    /**
     * 根据外部部门 ID 查询 mapping。
     */
    public Optional<IntegrationDeptMapping> findDeptMappingByExternalId(Long tenantId, String provider, String externalId) {
        return Optional.ofNullable(deptMappingMapper.selectOne(
                new LambdaQueryWrapper<IntegrationDeptMapping>()
                        .eq(IntegrationDeptMapping::getTenantId, tenantId)
                        .eq(IntegrationDeptMapping::getProvider, provider)
                        .eq(IntegrationDeptMapping::getExternalId, externalId)));
    }

    /**
     * 查询租户下所有部门 mapping。
     */
    public List<IntegrationDeptMapping> listDeptMappings(Long tenantId, String provider) {
        return deptMappingMapper.selectList(
                new LambdaQueryWrapper<IntegrationDeptMapping>()
                        .eq(IntegrationDeptMapping::getTenantId, tenantId)
                        .eq(IntegrationDeptMapping::getProvider, provider));
    }
}
