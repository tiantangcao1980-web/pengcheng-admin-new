package com.pengcheng.integration.spi;

import com.pengcheng.integration.spi.dto.ContactSyncResult;

/**
 * IM 通讯录同步服务 SPI。
 * <p>
 * 拉取外部 IM 平台的部门树 + 人员列表，写入本地 sys_user / integration_dept_mapping / integration_user_mapping。
 */
public interface ImContactService {

    /**
     * 全量同步通讯录（部门 + 人员）。
     * <p>
     * 采用"先同步部门，再按部门拉用户"策略；
     * 用户匹配以 integration_user_mapping.external_id 为键做 upsert，不删除本地已有用户。
     *
     * @param tenantId 租户 ID
     * @return 同步结果汇总
     */
    ContactSyncResult syncContacts(Long tenantId);
}
