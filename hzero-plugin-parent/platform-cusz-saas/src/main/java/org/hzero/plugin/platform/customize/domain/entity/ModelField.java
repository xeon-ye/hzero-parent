package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.choerodon.mybatis.domain.AuditDomain;
import io.choerodon.mybatis.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.customize.dto.ColumnMetaData;
import org.hzero.boot.customize.dto.ModelFieldMetaData;
import org.hzero.boot.customize.dto.ModelFieldWdgMetaData;
import org.hzero.boot.customize.util.JdbcToJava;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import java.util.UUID;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 10:30
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ModelField extends AuditDomain {

    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_FIELD)
    private Long fieldId;
    private Long tenantId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL)
    private Long modelId;
    /**
     * 字段编码
     */
    private String fieldCode;
    /**
     * 字段名称
     */
//    @MultiLanguageField
    private String fieldName;
    /**
     * 字段类型，值集
     */
    private String fieldType;
    /**
     * 字段默认值
     */
    private String defaultValue;
    /**
     * 是否为空
     */
    private Integer notNull;
    /**
     * 字段类别 实体字段、虚拟字段
     */
    private String fieldCategory;

    /**
     * 是否是多语言字段
     */
    private Integer fieldMultiLang;

    /**
     * 是否是主键字段
     */
    private boolean fieldPrimaryKey;

    @JsonProperty("modelFieldWidget")
    private ModelFieldWidget wdg;

    private String modelCode;

    private String modelName;

    private Model model;

    private Integer dataSize;

    public ModelField(ColumnMetaData metadata) {
        this.fieldType = metadata.getColumnType();
        this.fieldCode = metadata.getColumnName();
        this.fieldName = StringUtils.isEmpty(metadata.getRemarks()) ? metadata.getColumnName() : metadata.getRemarks();
        this.notNull = metadata.getNotNullFlag() ? 1 : 0;
        this.defaultValue = metadata.getDefaultValue();
        //导入时，字段均为实体字段
        this.fieldCategory = CustomizeConstants.FieldCategory.ENTITY_FIELD;
        this.fieldPrimaryKey = metadata.getPrimaryKeyFlag();
        this.fieldMultiLang = metadata.getSupportMultiLang() ? 1 : 0;
        this.dataSize = metadata.getColumnDataSize();
    }

    public ModelField() {
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
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

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Integer getNotNull() {
        return notNull;
    }

    public void setNotNull(Integer notNull) {
        this.notNull = notNull;
    }

    public String getFieldCategory() {
        return fieldCategory;
    }

    public void setFieldCategory(String fieldCategory) {
        this.fieldCategory = fieldCategory;
    }

    public ModelFieldWidget getWdg() {
        return wdg;
    }

    public void setWdg(ModelFieldWidget wdg) {
        this.wdg = wdg;
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

    public Integer getFieldMultiLang() {
        return fieldMultiLang;
    }

    public void setFieldMultiLang(Integer fieldMultiLang) {
        this.fieldMultiLang = fieldMultiLang;
    }

    public boolean isFieldPrimaryKey() {
        return fieldPrimaryKey;
    }

    public void setFieldPrimaryKey(boolean fieldPrimaryKey) {
        this.fieldPrimaryKey = fieldPrimaryKey;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Integer getDataSize() {
        return dataSize;
    }

    public void setDataSize(Integer dataSize) {
        this.dataSize = dataSize;
    }

    public static String generateKey(Long modelId) {
        Assert.notNull(modelId, "error.modelId.null");
        return ModelFieldMetaData.MODEL_FIELD_CACHE_KEY + modelId;
    }

    public ModelFieldMetaData conversionField() {
        ModelFieldMetaData metaData = new ModelFieldMetaData();
        metaData.setFieldCode(this.getFieldCode());
        metaData.setModelId(this.getModelId());
        metaData.setFieldType(this.getFieldType());
        metaData.setFieldCategory(this.getFieldCategory());
        metaData.setDefaultValue(this.getDefaultValue());
        metaData.setFieldId(this.getFieldId());
        metaData.setFieldName(this.getFieldName());
        metaData.setFieldMultiLang(this.getFieldMultiLang());
        if (this.getNotNull() == null || this.getNotNull() == 0) {
            metaData.setNotNull(false);
        } else {
            metaData.setNotNull(true);
        }
        //组件
        ModelFieldWdgMetaData wdgMetaDat = new ModelFieldWdgMetaData();
        if (this.getWdg() != null) {
            BeanUtils.copyProperties(this.getWdg(), wdgMetaDat);
        }
        metaData.setWdgMetaData(wdgMetaDat);
        return metaData;
    }

    public ModelField cacheConvert() {
        ModelField field = new ModelField();
        field.setFieldType(this.fieldType);
        field.setDefaultValue(this.defaultValue);
        field.setFieldCategory(this.fieldCategory);
        field.setFieldCode(this.fieldCode);
        field.setFieldMultiLang(this.fieldMultiLang);
        field.setFieldId(this.fieldId);
        return field;
    }

    public static ModelField convertFromModelFieldPub(ModelFieldPub pub) {
        if (pub == null) {
            return null;
        }
        ModelField field = new ModelField();
        BeanUtils.copyProperties(pub, field);
        field.setFieldId(pub.getId());
        field.setFieldCode(pub.getFieldName());
        field.setFieldName(pub.getDisplayName());
        field.setModelId(pub.getModelObjectId());
        field.setNotNull(pub.getRequiredFlag());
        field.setFieldMultiLang(pub.getMultiLanguageFlag());
        field.setFieldType(JdbcToJava.convertJavaType(pub.getDataType()));
        field.setFieldCategory(pub.getFieldType());
        field.setFieldPrimaryKey(pub.getPrimaryFlag() == 1);
        return field;
    }

    public static ModelFieldPub convertFromModelField(ModelField field) {
        ModelFieldPub pub = new ModelFieldPub();
        BeanUtils.copyProperties(field, pub);
        pub.setId(field.getFieldId());
        pub.setModelObjectId(field.getModelId());
        pub.setCode(UUID.randomUUID().toString().replace("-", ""));
        pub.setFieldName(field.getFieldCode());
        pub.setDisplayName(field.getFieldName());
        pub.setMultiLanguageFlag(field.getFieldMultiLang());
        pub.setRequiredFlag(field.getNotNull());
        pub.setChangeRequiredFlag(1);
        pub.setDefaultValue(field.getDefaultValue());
        pub.setDataType(field.getFieldType());
        pub.setFieldType(field.getFieldCategory());
        pub.setTenantId(field.getTenantId());
        pub.setDataSize(field.getDataSize() == null ? 30 : Long.valueOf(field.getDataSize()));
        return pub;
    }

    public static ModelField convertFormFieldMetadata(ModelFieldMetaData modelFieldMetaData) {
        ModelField modelField = new ModelField();
        BeanUtils.copyProperties(modelFieldMetaData, modelField, "wdgMetaData", "notNull");
        ModelFieldWidget modelFieldWidget = new ModelFieldWidget();
        if (modelFieldMetaData.getWdgMetaData() != null) {
            BeanUtils.copyProperties(modelFieldMetaData.getWdgMetaData(), modelFieldWidget);
            modelField.setWdg(modelFieldWidget);
        }
        return modelField;
    }

    public String getFieldCodeCamel() {
        if (StringUtil.isNotEmpty(this.getFieldCode())) {
            return StringUtil.underlineToCamelhump(this.getFieldCode());
        }
        return this.getFieldCode();
    }

}
