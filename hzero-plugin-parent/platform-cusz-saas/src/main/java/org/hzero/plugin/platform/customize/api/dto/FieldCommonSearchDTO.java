package org.hzero.plugin.platform.customize.api.dto;

import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;

/**
 * @author : peng.yu01@hand-china.com 2020/1/2 10:09
 */
public class FieldCommonSearchDTO {

    private String modelName;

    private String fieldName;

    private String fieldCode;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL)
    private Long modelId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_FIELD)
    private Long fieldId;

    public FieldCommonSearchDTO(Long modelId) {
        this.modelId = modelId;
    }

    public FieldCommonSearchDTO() {
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }
}
