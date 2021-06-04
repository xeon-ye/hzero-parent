package org.hzero.plugin.platform.customize.infra.common;

import org.hzero.plugin.platform.customize.domain.entity.UnitField;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeUnitFieldRepository;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * @author peng.yu01@hand-china.com on 2020-02-19
 */
public class UnitLocalCache {

    private Map<String, Map<String, UnitField>> unitFieldCache;

    private CustomizeUnitFieldRepository unitFieldRepository;

    public UnitLocalCache(CustomizeUnitFieldRepository unitFieldRepository) {
        Assert.notNull(unitFieldRepository, "CustomizeUnitFieldRepository can't null !");
        this.unitFieldRepository = unitFieldRepository;
        this.unitFieldCache = new HashMap<>(64);
    }


    public Map<String, UnitField> getAllUnitField(String unitCode) {
        return unitFieldCache.computeIfAbsent(unitCode, (k ->
                unitFieldRepository.getUnitFieldsCacheMap(unitCode)));
    }

    public UnitField getUnitField(String unitCode,Long fieldId){
        Map<String, UnitField> fieldMap = getAllUnitField(unitCode);
        String key = String.valueOf(fieldId);
        return fieldMap.get(key);
    }

    public UnitField getUnitField(String unitCode,String bizKey){
        Map<String, UnitField> fieldMap = getAllUnitField(unitCode);
        return fieldMap.get(bizKey);
    }

    public boolean containsField(String unitCode,Long fieldId){
        Map<String, UnitField> fieldMap = getAllUnitField(unitCode);
        String key = String.valueOf(fieldId);
        return fieldMap.containsKey(key);
    }



}
