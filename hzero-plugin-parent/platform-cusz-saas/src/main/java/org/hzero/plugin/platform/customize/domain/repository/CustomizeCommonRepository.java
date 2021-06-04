package org.hzero.plugin.platform.customize.domain.repository;

import java.util.Map;

import org.hzero.boot.customize.dto.CustomizeConfig;
import org.hzero.plugin.platform.customize.api.dto.UnitConfigDTO;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;

/**
 * 个性化配置通用查询Repository
 *
 * @author : xiangyu.qi01@hand-china.com 2019/12/13
 */
public interface CustomizeCommonRepository {


    /**
     * 根据单元编码获取个性化配置
     *
     * @param tenantId 租户Id
     * @param unitCode 单元编码数组
     * @return
     */
    Map<String, UnitConfigDTO> unitConfigDetailCache(Long tenantId, String[] unitCode);

    /**
     * 根据单元编码获取个性化配置
     *
     * @param tenantId 租户Id
     * @return
     */
    CustomizeConfig getCustomizeConfig(Long tenantId, Long unitId);


    /**
     * 缓存个性化配置
     *
     * @param tenantId
     * @param unitId
     * @return
     */
    CustomizeConfig cacheCustomizeConfig(Long tenantId, Long unitId);

}
