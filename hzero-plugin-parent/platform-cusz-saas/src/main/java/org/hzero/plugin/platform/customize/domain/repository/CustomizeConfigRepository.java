package org.hzero.plugin.platform.customize.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.domain.entity.Config;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;

/**
 * 个性化配置Repository
 *
 * @author : xiangyu.qi01@hand-china.com 2019/12/13
 */
public interface CustomizeConfigRepository extends BaseRepository<Config> {

    /**
     * 查询单元配置头
     *
     * @param tenantId 租户ID
     * @param unitId   个性化单元ID
     * @return
     */
    Config selectConfigByUnitId(Long tenantId, Long unitId);

    /**
     * 缓存单元配置头
     *
     * @param config
     */
    void cacheConfig(Config config);


    /**
     * 根据租户ID，单元ID查询缓存
     *
     * @param tenantId
     * @param unitCode
     * @return
     */
    Config getConfigCache(Long tenantId, String unitCode);

    /**
     * 初始化缓存
     */
    void initConfigCache();

    /**
     * 删除用户配置
     *
     * @param config
     */
    void deleteUserConfigCache(Config config);

    /**
     * 获取用户配置
     *
     * @param tenantId
     * @param userId
     * @param unitCode
     * @return
     */
    Config getUserConfigCache(Long tenantId, Long userId, String unitCode);

    /**
     * 根据单元字段删除租户缓存
     * @param unitField
     */
    void delConfigFieldCacheByUnitField(UnitField unitField);
}
