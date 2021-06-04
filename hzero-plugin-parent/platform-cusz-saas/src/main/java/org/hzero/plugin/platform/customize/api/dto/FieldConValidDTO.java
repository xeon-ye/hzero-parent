package org.hzero.plugin.platform.customize.api.dto;

import org.hzero.plugin.platform.customize.domain.entity.FieldConditionValid;

import java.util.List;

/**
 * @author peng.yu01@hand-china.com on 2020-04-10
 */
public class FieldConValidDTO {

    private List<UnitConfigFieldConditionLineDTO> conLineList;

    private List<FieldConditionValid> conValidList;

    public List<UnitConfigFieldConditionLineDTO> getConLineList() {
        return conLineList;
    }

    public void setConLineList(List<UnitConfigFieldConditionLineDTO> conLineList) {
        this.conLineList = conLineList;
    }

    public List<FieldConditionValid> getConValidList() {
        return conValidList;
    }

    public void setConValidList(List<FieldConditionValid> conValidList) {
        this.conValidList = conValidList;
    }
}
