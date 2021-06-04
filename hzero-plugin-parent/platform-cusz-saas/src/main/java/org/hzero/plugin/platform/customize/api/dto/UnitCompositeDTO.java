package org.hzero.plugin.platform.customize.api.dto;

import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.entity.Unit;

import java.util.List;
import java.util.Map;

/**
 * @author peng.yu01@hand-china.com on 2020-02-21
 */
public class UnitCompositeDTO {

    private Unit unit;

    private List<ConfigField> configFields;

    private List<Map<String,String>> unitAlias;

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public List<ConfigField> getConfigFields() {
        return configFields;
    }

    public void setConfigFields(List<ConfigField> configFields) {
        this.configFields = configFields;
    }

    public List<Map<String, String>> getUnitAlias() {
        return unitAlias;
    }

    public void setUnitAlias(List<Map<String, String>> unitAlias) {
        this.unitAlias = unitAlias;
    }
}
