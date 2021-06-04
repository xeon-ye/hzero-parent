package org.hzero.plugin.platform.customize.api.dto;

import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;

/**
 * @author : peng.yu01@hand-china.com 2019/12/27 13:41
 */
public class ConfigFieldLovDTO {

    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_CONFIG_FIELD)
    private Long configFieldId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT)
    private Long unitId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL)
    private Long modelId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_FIELD)
    private Long fieldId;

    private String fieldCode;

    private String fieldName;

    private String modelCode;

    private String modelName;

    private String fieldType;

    private String fieldCategory;

    public Long getConfigFieldId() {
        return configFieldId;
    }

    public void setConfigFieldId(Long configFieldId) {
        this.configFieldId = configFieldId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldCategory() {
        return fieldCategory;
    }

    public void setFieldCategory(String fieldCategory) {
        this.fieldCategory = fieldCategory;
    }
}
