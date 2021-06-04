package org.hzero.plugin.platform.customize.domain.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Objects;

import io.choerodon.mybatis.domain.AuditDomain;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author qiang.zeng@hand-china.com 2020-01-10 22:22
 */
public class BaseModelField extends AuditDomain {

    public static final String FIELD_ID = "id";
    public static final String FIELD_MODEL_OBJECT_ID = "modelObjectId";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_CODE = "code";
    public static final String FIELD_FIELD_NAME = "fieldName";
    public static final String FIELD_DISPLAY_NAME = "displayName";
    public static final String FIELD_DATA_TYPE = "dataType";
    public static final String FIELD_DATA_SIZE = "dataSize";
    public static final String FIELD_DEFAULT_VALUE = "defaultValue";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_REQUIRED_FLAG = "requiredFlag";
    public static final String FIELD_CHANGE_REQUIRED_FLAG = "changeRequiredFlag";
    public static final String FIELD_PRIMARY_FLAG = "primaryFlag";
    public static final String FIELD_FIELD_TYPE = "fieldType";
    public static final String FIELD_FORMULA_TYPE = "formulaType";
    public static final String FIELD_FORMULA_CONTENT = "formulaContent";
    public static final String FIELD_VALUE_LIST_TYPE = "valueListType";
    public static final String FIELD_VALUE_LIST_CODE = "valueListCode";

    //
    // 业务方法(按public protected private顺序排列)
    // ------------------------------------------------------------------------------

    //
    // 数据库字段
    // ------------------------------------------------------------------------------


    @ApiModelProperty("表ID，主键，供其他表做外键")
    @Id
    @GeneratedValue
    private Long id;
    @ApiModelProperty(value = "模型对象ID，hmde_model_object.id", required = true)
    @NotNull
    private Long modelObjectId;
    //TODO 租户ID默认值
    @ApiModelProperty(value = "租户ID", required = true)
    @NotNull
    private Long tenantId = 0L;
    @ApiModelProperty(value = "字段代码")
    private String code;
    @ApiModelProperty(value = "字段名称", required = true)
    @NotBlank
    private String fieldName;
    @ApiModelProperty(value = "显示名称", required = true)
    @NotBlank
    private String displayName;
    @ApiModelProperty(value = "描述")
    private String description;
    @ApiModelProperty(value = "是否必输。1是，0不是", required = true)
    @NotNull
    private Integer requiredFlag;
    @ApiModelProperty(value = "是否可修改必输。1是，0不是", required = true)
    @NotNull
    private Integer changeRequiredFlag;
    @ApiModelProperty(value = "是否主键。1是，0不是", required = true)
    @NotNull
    private Integer primaryFlag;
    @ApiModelProperty(value = "是否多语言字段。1是，0不是", required = true)
    @NotNull
    private Integer multiLanguageFlag;
    @ApiModelProperty(value = "字段数据类型", required = true)
    @NotBlank
    private String dataType;
    @ApiModelProperty(value = "数据长度", required = true)
    @NotNull
    private Long dataSize;
    @ApiModelProperty(value = "默认值")
    private String defaultValue;
    @ApiModelProperty(value = "字段类型")
    private String fieldType;
    @ApiModelProperty(value = "公式类型")
    private String formulaType;
    @ApiModelProperty(value = "公式内容")
    private String formulaContent;
    @ApiModelProperty(value = "值集类型")
    private String valueListType;
    @ApiModelProperty(value = "值集编码或值集视图编码")
    private String valueListCode;

    //
    // 非数据库字段
    // ------------------------------------------------------------------------------
    @ApiModelProperty(value = "模型对象代码，hmde_model_object.code")
    @Transient
    private String modelObjectCode;
    @ApiModelProperty(value = "值集名称或值集视图名称")
    @Transient
    private String valueListName;
    @Transient
    @ApiModelProperty(value = "字段别名")
    private String aliasName;
    //
    // getter/setter
    // ------------------------------------------------------------------------------

    /**
     * @return 表ID，主键，供其他表做外键
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return 模型对象ID，hmde_model_object.id
     */
    public Long getModelObjectId() {
        return modelObjectId;
    }

    public void setModelObjectId(Long modelObjectId) {
        this.modelObjectId = modelObjectId;
    }

    /**
     * @return 租户ID
     */
    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * @return 字段代码
     */
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return 描述
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return 是否必输。1是，0不是
     */
    public Integer getRequiredFlag() {
        return requiredFlag;
    }

    public void setRequiredFlag(Integer requiredFlag) {
        this.requiredFlag = requiredFlag;
    }

    /**
     * @return 是否主键。1是，0不是
     */
    public Integer getPrimaryFlag() {
        return primaryFlag;
    }

    public void setPrimaryFlag(Integer primaryFlag) {
        this.primaryFlag = primaryFlag;
    }


    /**
     * @return 数据长度
     */
    public Long getDataSize() {
        return dataSize;
    }

    public void setDataSize(Long dataSize) {
        this.dataSize = dataSize;
    }

    /**
     * @return 默认值
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getChangeRequiredFlag() {
        return changeRequiredFlag;
    }

    public void setChangeRequiredFlag(Integer changeRequiredFlag) {
        this.changeRequiredFlag = changeRequiredFlag;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFormulaType() {
        return formulaType;
    }

    public void setFormulaType(String formulaType) {
        this.formulaType = formulaType;
    }

    public String getFormulaContent() {
        return formulaContent;
    }

    public void setFormulaContent(String formulaContent) {
        this.formulaContent = formulaContent;
    }

    public String getValueListType() {
        return valueListType;
    }

    public void setValueListType(String valueListType) {
        this.valueListType = valueListType;
    }

    public String getValueListCode() {
        return valueListCode;
    }

    public void setValueListCode(String valueListCode) {
        this.valueListCode = valueListCode;
    }

    public String getModelObjectCode() {
        return modelObjectCode;
    }

    public void setModelObjectCode(String modelObjectCode) {
        this.modelObjectCode = modelObjectCode;
    }

    public String getValueListName() {
        return valueListName;
    }

    public void setValueListName(String valueListName) {
        this.valueListName = valueListName;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseModelField modelField = (BaseModelField) o;
        if (!Objects.equals(this.id, modelField.id)) {
            return false;
        }
        if (!Objects.equals(this.modelObjectId, modelField.modelObjectId)) {
            return false;
        }
        if (!Objects.equals(this.code, modelField.code)) {
            return false;
        }
        return Objects.equals(this.fieldName, modelField.fieldName);
    }

    @Override
    public int hashCode() {
        int result = this.id != null ? this.id.hashCode() : 0;
        result = 31 * result + (this.modelObjectId != null ? this.modelObjectId.hashCode() : 0);
        result = 31 * result + (this.code != null ? this.code.hashCode() : 0);
        result = 31 * result + (this.fieldName != null ? this.fieldName.hashCode() : 0);
        return result;
    }

    public Integer getMultiLanguageFlag() {
        return multiLanguageFlag;
    }

    public void setMultiLanguageFlag(Integer multiLanguageFlag) {
        this.multiLanguageFlag = multiLanguageFlag;
    }
}
