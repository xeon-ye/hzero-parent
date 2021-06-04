package org.hzero.plugin.platform.customize.infra.common;

import java.util.HashMap;
import java.util.Map;

import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeConfigFieldRepository;
import org.springframework.util.Assert;

/**
 * @author peng.yu01@hand-china.com on 2020-02-19
 */
public class ConfigLocalCache {

    private CustomizeConfigFieldRepository configFieldRepository;

    private Map<String, Map<String, ConfigField>> configFieldCache;

    public ConfigLocalCache(CustomizeConfigFieldRepository configFieldRepository) {
        this.configFieldRepository = configFieldRepository;
        this.configFieldCache = new HashMap<>(64);
    }

    public Map<String, ConfigField> getAllConfigFields(Long tenantId, String unitCode) {
        Assert.notNull(tenantId, "tenantId can not be null !");
        Assert.notNull(unitCode, "unitCode can not be null !");
        return configFieldCache.computeIfAbsent(unitCode, key -> configFieldRepository.getConfigFieldsCacheMap(tenantId, unitCode));
    }

    public ConfigField getConfigField(Long tenantId, String unitCode, Long fieldId) {
        return getAllConfigFields(tenantId, unitCode).get(String.valueOf(fieldId));
    }

    public ConfigField getConfigField(Long tenantId, String unitCode, String bizKey) {
        return getAllConfigFields(tenantId, unitCode).get(bizKey);
    }

    public boolean containsConfigField(Long tenantId, String unitCode, Long fieldId) {
        return getAllConfigFields(tenantId, unitCode).containsKey(String.valueOf(fieldId));
    }
}
