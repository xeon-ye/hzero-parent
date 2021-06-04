package org.hzero.plugin.platform.customize.api.dto;

import io.choerodon.mybatis.util.StringUtil;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionLine;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;
import org.springframework.beans.BeanUtils;

/**
 * @author : peng.yu01@hand-china.com 2020/2/10 9:29
 */
public class UnitConfigFieldConditionLineDTO {

    private Integer conCode;

    private String sourceUnitCode;

    private String sourceFieldCode;

    private String conExpression;

    private String targetFieldCode;

    private String targetType;

    private String targetValue;

    public Integer getConCode() {
        return conCode;
    }

    public void setConCode(Integer conCode) {
        this.conCode = conCode;
    }

    public String getSourceUnitCode() {
        return sourceUnitCode;
    }

    public void setSourceUnitCode(String sourceUnitCode) {
        this.sourceUnitCode = sourceUnitCode;
    }

    public String getSourceFieldCode() {
        return sourceFieldCode;
    }

    public void setSourceFieldCode(String sourceFieldCode) {
        this.sourceFieldCode = sourceFieldCode;
    }

    public String getConExpression() {
        return conExpression;
    }

    public void setConExpression(String conExpression) {
        this.conExpression = conExpression;
    }

    public String getTargetFieldCode() {
        return targetFieldCode;
    }

    public void setTargetFieldCode(String targetFieldCode) {
        this.targetFieldCode = targetFieldCode;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    public static UnitConfigFieldConditionLineDTO convertFromCache(FieldConditionLine line) {
        UnitConfigFieldConditionLineDTO lineDTO = new UnitConfigFieldConditionLineDTO();
        BeanUtils.copyProperties(line, lineDTO);
        return lineDTO;
    }

    public void coverSourceFieldCode(ConfigField configField, UnitField unitField) {
        this.sourceFieldCode = innerCoverFieldCode(configField, unitField);
    }

    public void coverTargetFieldCode(ConfigField configField, UnitField unitField) {
        this.targetFieldCode = innerCoverFieldCode(configField, unitField);
    }

    private String innerCoverFieldCode(ConfigField configField, UnitField unitField) {
        String code = null;
        if (unitField != null) {
            code = unitField.fieldCode();
        }
        if (configField != null) {
            code = configField.fieldCode();
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(code)) {
            code = StringUtil.underlineToCamelhump(code);
        }
        return code;
    }

}
