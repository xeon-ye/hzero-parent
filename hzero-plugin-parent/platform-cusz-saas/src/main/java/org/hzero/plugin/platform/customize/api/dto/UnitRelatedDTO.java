package org.hzero.plugin.platform.customize.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;

import java.util.List;

/**
 * @author peng.yu01@hand-china.com 2020-02-13
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UnitRelatedDTO {

    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT)
    private Long unitId;

    private String unitName;

    private String unitCode;

    private String unitType;

    private List<UnitFieldCompositeDTO> unitFields;

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public List<UnitFieldCompositeDTO> getUnitFields() {
        return unitFields;
    }

    public void setUnitFields(List<UnitFieldCompositeDTO> unitFields) {
        this.unitFields = unitFields;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

}
