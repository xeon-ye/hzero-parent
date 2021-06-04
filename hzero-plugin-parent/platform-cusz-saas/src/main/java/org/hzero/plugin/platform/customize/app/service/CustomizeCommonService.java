package org.hzero.plugin.platform.customize.app.service;

import java.util.Map;

import org.hzero.boot.customize.dto.CustomizeConfig;
import org.hzero.plugin.platform.customize.api.dto.UnitConfigDTO;

/**
 * 个性化单元配置服务
 *
 * @author : xiangyu.qi01@hand-china.com 2019/12/13
 */
public interface CustomizeCommonService {

    /**
     * 根据单元编码获取个性化配置
     *
     * @param tenantId  租户Id
     * @param unitCodes 单元编码数组
     * @return
     */
    Map<String, UnitConfigDTO> getUiCustomize(Long tenantId, String[] unitCodes);

    /**
     * 缓存个性化配置
     *
     * @param tenantId 租户Id
     * @param unitId   单元编码
     * @return
     */
    CustomizeConfig cacheCustomizeConfig(Long tenantId, Long unitId);

}
