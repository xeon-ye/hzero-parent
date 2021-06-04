package org.hzero.plugin.platform.customize.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hzero.plugin.platform.customize.domain.entity.BaseFieldParam;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;

import java.util.List;

/**
 * @author peng.yu01@hand-china.com 2020-02-13
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UnitFieldCompositeDTO {

    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_FIELD)
    private Long modelFieldId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL)
    private Long modelId;

    private String unitFieldCode;

    private String unitFieldName;

    private String widgetType;

    private String sourceFieldValueCode;

    private List<BaseFieldParam> paramList;

    @JsonIgnore
    private Long unitFieldId;

    @JsonIgnore
    private Long configFieldId;

    public Long getModelFieldId() {
        return modelFieldId;
    }

    public void setModelFieldId(Long modelFieldId) {
        this.modelFieldId = modelFieldId;
    }

    public String getUnitFieldCode() {
        return unitFieldCode;
    }

    public void setUnitFieldCode(String unitFieldCode) {
        this.unitFieldCode = unitFieldCode;
    }

    public String getUnitFieldName() {
        return unitFieldName;
    }

    public void setUnitFieldName(String unitFieldName) {
        this.unitFieldName = unitFieldName;
    }

    public String getWidgetType() {
        return widgetType;
    }

    public void setWidgetType(String widgetType) {
        this.widgetType = widgetType;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getSourceFieldValueCode() {
        return sourceFieldValueCode;
    }

    public void setSourceFieldValueCode(String sourceFieldValueCode) {
        this.sourceFieldValueCode = sourceFieldValueCode;
    }

    public Long getUnitFieldId() {
        return unitFieldId;
    }

    public void setUnitFieldId(Long unitFieldId) {
        this.unitFieldId = unitFieldId;
    }

    public List<BaseFieldParam> getParamList() {
        return paramList;
    }

    public void setParamList(List<BaseFieldParam> paramList) {
        this.paramList = paramList;
    }

    public Long getConfigFieldId() {
        return configFieldId;
    }

    public void setConfigFieldId(Long configFieldId) {
        this.configFieldId = configFieldId;
    }
}
