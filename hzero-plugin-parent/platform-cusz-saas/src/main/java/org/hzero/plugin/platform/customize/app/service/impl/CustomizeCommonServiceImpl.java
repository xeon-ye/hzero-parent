package org.hzero.plugin.platform.customize.app.service.impl;

import org.hzero.boot.customize.dto.CustomizeConfig;
import org.hzero.plugin.platform.customize.api.dto.UnitConfigDTO;
import org.hzero.plugin.platform.customize.app.service.CustomizeCommonService;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeCommonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xiangyu.qi01@hand-china.com on 2019-12-16.
 */
@Service
public class CustomizeCommonServiceImpl implements CustomizeCommonService {

    @Autowired
    private CustomizeCommonRepository commonRepository;

    @Override
    public Map<String, UnitConfigDTO> getUiCustomize(Long tenantId, String[] unitCodes) {
        Assert.notNull(tenantId, "tenantId can not be null !");
        Assert.notNull(unitCodes, "unitCodes can not be null !");
        return commonRepository.unitConfigDetailCache(tenantId,unitCodes);
    }

    @Override
    public CustomizeConfig cacheCustomizeConfig(Long tenantId, Long unitId) {
        Assert.notNull(unitId, "unitCode can not be null!!");
        CustomizeConfig config = commonRepository.cacheCustomizeConfig(tenantId, unitId);
        return config;
    }


}
