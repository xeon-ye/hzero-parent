package org.hzero.plugin.platform.customize.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.hzero.plugin.platform.customize.domain.entity.BaseFieldParam;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldWidget;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 个性化配置 字段DTO
 *
 * @author xiangyu.qi01@hand-china.com on 2019-12-16.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnitConfigFieldDTO {

    /**
     * 字段标题
     */
    private String fieldName;

    /**
     * 字段名
     */
    private String fieldCode;

    /**
     * 字段别名
     */
    @JsonIgnore
    private String fieldAlias;

    /**
     * 字段组件类型
     */
    private String fieldType;

    /**
     * 字段类别，标准字段，非标准字段，弹性域字段
     */
    @JsonIgnore
    private String fieldCategory;

    /**
     * 值集编码
     */
    private String lovCode;

    /**
     * 文本最大长度
     */
    private Integer textMaxLength;

    /**
     * 文本最小长度
     */
    private Integer textMinLength;

    private Integer textAreaMaxLine;

    /**
     * 字段默认值
     */
    private String defaultValue;

    private Object defaultValueMeaning;

    /**
     * 是否必输
     */
    private Integer required;

    /**
     * 是否编辑
     */
    private Integer editable;

    /**
     * 字段顺序
     */
    private Integer seq;

    /**
     * 表单行序号
     */
    private Integer formRow;

    /**
     * 表单列序号
     */
    private Integer formCol;

    /**
     * 表格字段列冻结方向
     */
    @JsonProperty("fixed")
    private String gridFixed;

    /**
     * 表格列宽度
     */
    @JsonProperty("width")
    private Integer gridWidth;

    /**
     * 日期控件格式
     */
    private String dateFormat;

    /**
     * 数值精度
     */
    private Integer numberPrecision;

    /**
     * 数值最大值
     */
    private Long numberMax;

    /**
     * 数值最小值
     */
    private Long numberMin;


    private String bucketName;
    /**
     * 桶目录
     */
    private String bucketDirectory;

    /**
     * 是否隐藏字段
     */
    @JsonProperty("visible")
    private Integer fieldVisible;

    private String renderOptions;

    /**
     * 链接标题
     */
    private String linkTitle;
    /**
     * 链接地址
     */
    private String linkHref;
    /**
     * 是否打开新窗口
     */
    private Integer linkNewWindow;

    @JsonIgnore
    private Long fieldId;

    /**
     * label列数
     */
    private Integer labelCol;

    /**
     * wrapper列数
     */
    private Integer wrapperCol;

    /**
     * 渲染规则
     */
    private String renderRule;

    private Integer multipleFlag;

    /**
     * 值集映射字段
     */
    private List<UnitConfigFieldMappingDTO> lovMappings;

    private List<UnitConfigFieldConditionHeaderDTO> conditionHeaderDTOs;

    private FieldConValidDTO conValidDTO;

    private List<BaseFieldParam> paramList;

    private Boolean standardField = false;

    public Boolean getIsStandardField() {
        return standardField;
    }

    public void setStandardField(Boolean standardField) {
        this.standardField = standardField;
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

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getLovCode() {
        return lovCode;
    }

    public void setLovCode(String lovCode) {
        this.lovCode = lovCode;
    }

    public Integer getTextMaxLength() {
        return textMaxLength;
    }

    public void setTextMaxLength(Integer textMaxLength) {
        this.textMaxLength = textMaxLength;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Integer getRequired() {
        return required;
    }

    public void setRequired(Integer required) {
        this.required = required;
    }

    public Integer getEditable() {
        return editable;
    }

    public void setEditable(Integer editable) {
        this.editable = editable;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }


    public Integer getFormRow() {
        return formRow;
    }

    public void setFormRow(Integer formRow) {
        this.formRow = formRow;
    }

    public Integer getFormCol() {
        return formCol;
    }

    public void setFormCol(Integer formCol) {
        this.formCol = formCol;
    }

    public String getFieldCategory() {
        return fieldCategory;
    }

    public void setFieldCategory(String fieldCategory) {
        this.fieldCategory = fieldCategory;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public Integer getNumberPrecision() {
        return numberPrecision;
    }

    public void setNumberPrecision(Integer numberPrecision) {
        this.numberPrecision = numberPrecision;
    }


    public Long getNumberMax() {
        return numberMax;
    }

    public void setNumberMax(Long numberMax) {
        this.numberMax = numberMax;
    }

    public Long getNumberMin() {
        return numberMin;
    }

    public void setNumberMin(Long numberMin) {
        this.numberMin = numberMin;
    }

    public String getGridFixed() {
        return gridFixed;
    }

    public void setGridFixed(String gridFixed) {
        this.gridFixed = gridFixed;
    }

    public Integer getGridWidth() {
        return gridWidth;
    }

    public void setGridWidth(Integer gridWidth) {
        this.gridWidth = gridWidth;
    }

    public Integer getFieldVisible() {
        return fieldVisible;
    }

    public void setFieldVisible(Integer fieldVisible) {
        this.fieldVisible = fieldVisible;
    }

    public String getFieldAlias() {
        return fieldAlias;
    }

    public void setFieldAlias(String fieldAlias) {
        this.fieldAlias = fieldAlias;
    }

    public Integer getTextAreaMaxLine() {
        return textAreaMaxLine;
    }

    public void setTextAreaMaxLine(Integer textAreaMaxLine) {
        this.textAreaMaxLine = textAreaMaxLine;
    }


    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBucketDirectory() {
        return bucketDirectory;
    }

    public void setBucketDirectory(String bucketDirectory) {
        this.bucketDirectory = bucketDirectory;
    }

    public List<UnitConfigFieldMappingDTO> getLovMappings() {
        return lovMappings;
    }

    public void setLovMappings(List<UnitConfigFieldMappingDTO> lovMappings) {
        this.lovMappings = lovMappings;
    }

    public String getRenderOptions() {
        return renderOptions;
    }

    public void setRenderOptions(String renderOptions) {
        this.renderOptions = renderOptions;
    }

    public Integer getTextMinLength() {
        return textMinLength;
    }

    public void setTextMinLength(Integer textMinLength) {
        this.textMinLength = textMinLength;
    }

    public String getLinkTitle() {
        return linkTitle;
    }

    public void setLinkTitle(String linkTitle) {
        this.linkTitle = linkTitle;
    }

    public String getLinkHref() {
        return linkHref;
    }

    public void setLinkHref(String linkHref) {
        this.linkHref = linkHref;
    }

    public Integer getLinkNewWindow() {
        return linkNewWindow;
    }

    public void setLinkNewWindow(Integer linkNewWindow) {
        this.linkNewWindow = linkNewWindow;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public Integer getLabelCol() {
        return labelCol;
    }

    public void setLabelCol(Integer labelCol) {
        this.labelCol = labelCol;
    }

    public Integer getWrapperCol() {
        return wrapperCol;
    }

    public void setWrapperCol(Integer wrapperCol) {
        this.wrapperCol = wrapperCol;
    }

    public List<BaseFieldParam> getParamList() {
        return paramList;
    }

    public void setParamList(List<BaseFieldParam> paramList) {
        this.paramList = paramList;
    }

    public Object getDefaultValueMeaning() {
        return defaultValueMeaning;
    }

    public void setDefaultValueMeaning(Object defaultValueMeaning) {
        this.defaultValueMeaning = defaultValueMeaning;
    }

    public String getRenderRule() {
        return renderRule;
    }

    public void setRenderRule(String renderRule) {
        this.renderRule = renderRule;
    }

    public FieldConValidDTO getConValidDTO() {
        return conValidDTO;
    }

    public void setConValidDTO(FieldConValidDTO conValidDTO) {
        this.conValidDTO = conValidDTO;
    }

    public Integer getMultipleFlag() {
        return multipleFlag;
    }

    public void setMultipleFlag(Integer multipleFlag) {
        this.multipleFlag = multipleFlag;
    }

    public List<UnitConfigFieldConditionHeaderDTO> getConditionHeaderDTOs() {
        if (CollectionUtils.isEmpty(this.conditionHeaderDTOs)) {
            return new ArrayList<>();
        }
        return conditionHeaderDTOs;
    }

    public void setConditionHeaderDTOs(List<UnitConfigFieldConditionHeaderDTO> conditionHeaderDTOs) {
        this.conditionHeaderDTOs = conditionHeaderDTOs;
    }

    public Boolean getStandardField() {
        return standardField;
    }

    public String fieldCode() {
        return StringUtils.isNotEmpty(this.fieldAlias) ? this.fieldAlias : this.fieldCode;
    }

    public void readFormConfig(ConfigField configField) {
        ModelField field = configField.getField();
        if (field == null) {
            field = new ModelField();
        }
        BeanUtils.copyProperties(configField, this);
        ConfigFieldWidget widget = configField.getWidget();
        if (widget != null) {
            BeanUtils.copyProperties(widget, this);
            this.numberPrecision = widget.getNumberDecimal();
            this.fieldType = widget.getFieldWidget();
            this.lovCode = widget.getSourceCode();
        }
        this.fieldCode = field.getFieldCode();
        this.fieldCategory = field.getFieldCategory();
        if (this.fieldCode == null) {
            fieldCode = configField.getFieldAlias();
        }

        this.fieldVisible = configField.getVisible();
        this.required = configField.getFieldRequired();
        this.editable = configField.getFieldEditable();
        this.seq = configField.getGridSeq();

        if (!CollectionUtils.isEmpty(configField.getFieldLovMaps())) {
            this.lovMappings = configField.getFieldLovMaps().stream().map(UnitConfigFieldMappingDTO::readFormConfig).collect(Collectors.toList());
        }
    }

    public void overrideUserConfig(ConfigField userConfig) {
        //用户个性化优先级更高
        if (StringUtils.isNotBlank(userConfig.getGridFixed())) {
            this.setGridFixed(userConfig.getGridFixed());
        }
        if (!ObjectUtils.isEmpty(userConfig.getGridWidth())) {
            this.setGridWidth(userConfig.getGridWidth());
        }
        if (!ObjectUtils.isEmpty(userConfig.getGridSeq())) {
            this.setSeq(userConfig.getGridSeq());
        }
        if (!ObjectUtils.nullSafeEquals(this.getFieldVisible(), 0L)) {
            this.setFieldVisible(userConfig.getVisible());
        }
        if (StringUtils.isNotEmpty(userConfig.getFieldName())) {
            this.setFieldName(userConfig.getFieldName());
        }
    }
}
