package org.hzero.plugin.platform.customize.api.dto;

import java.util.List;

import org.hzero.plugin.platform.customize.domain.entity.Unit;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;

/**
 * @author xiangyu.qi01@hand-china.com on 2019-12-24.
 */
public class UnitDTO {

    /**
     * 单元配置
     */
    private Unit unit;

    /**
     * 字段配置
     */
    private List<UnitField> fields;

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public List<UnitField> getFields() {
        return fields;
    }

    public void setFields(List<UnitField> fields) {
        this.fields = fields;
    }
}
