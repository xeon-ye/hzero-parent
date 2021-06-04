package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.annotation.MultiLanguage;
import io.choerodon.mybatis.annotation.MultiLanguageField;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.customize.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.*;

/**
 * @author : peng.yu01@hand-china.com 2019/12/15 13:15
 */
@ModifyAudit
/*@VersionAudit*/
@Table(name = "hpfm_cusz_config_field")
@MultiLanguage
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ConfigField extends BaseField {
    public static final String FIELD_CONFIG_FIELD_ID = "configFieldId";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_UNIT_ID = "unitId";
    public static final String FIELD_MODEL_ID = "modelId";
    public static final String FIELD_FIELD_ID = "fieldId";
    public static final String FIELD_FIELD_EDITABLE = "fieldEditable";
    public static final String FIELD_FIELD_REQUIRED = "fieldRequired";
    public static final String FIELD_VISIBLE = "visible";
    public static final String FIELD_FIELD_ALIAS = "fieldAlias";
    public static final String FIELD_GRID_SEQ = "gridSeq";
    public static final String FIELD_FORM_COL = "formCol";
    public static final String FIELD_FORM_ROW = "formRow";
    public static final String FIELD_GRID_FIXED = "gridFixed";
    public static final String FIELD_GRID_WIDTH = "gridWidth";
    public static final String FIELD_RENDER_OPTIONS = "renderOptions";
    public static final String FIELD_FIELD_LABEL_COL = "labelCol";
    public static final String FIELD_FIELD_WRAPPER_COL = "wrapperCol";
    public static final String FIELD_FIELD_USER_ID = "userId";
    public static final String FIELD_FIELD_RENDER_RULE = "renderRule";
    public static final String FIELD_WHERE_OPTION = "whereOption";
    @Id
    @GeneratedValue
    @Encrypt(ENCRYPT_KEY_CONFIG_FIELD)
    private Long configFieldId;

    private Long tenantId;
    @Encrypt(ENCRYPT_KEY_UNIT)
    private Long unitId;

    /**
     * 字段名称
     */
    @MultiLanguageField
    private String fieldName;

    /**
     * 是否编辑
     */
    private Integer fieldEditable;

    /**
     * 是否必输
     */
    private Integer fieldRequired;

    @Column(name = "field_visible")
    private Integer visible;

    /**
     * 表格列冻结配置
     */
    private String gridFixed;

    /**
     * 表格列宽度
     */
    private Integer gridWidth;

    /**
     * 渲染方式
     */
    private String renderOptions;

    /**
     * label列数
     */
    private Integer labelCol;

    /**
     * wrapper列数
     */
    private Integer wrapperCol;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 虚拟字段渲染规则
     */
    private String renderRule;

    private String whereOption;

    @Transient
    @ApiModelProperty(hidden = true)
    private ModelField field;

    @Transient
    @ApiModelProperty(hidden = true)
    private ConfigFieldWidget widget;

    @Transient
    private List<ConfigFieldMap> fieldLovMaps;

    @Transient
    private String unitCode;

    @Transient
    private List<Map<String, String>> tlMaps;

    @Transient
    private String custType;

    @Transient
    private List<FieldConditionHeader> conditionHeaders;

    @Transient
    private FieldConditionHeader conValid;

    @Transient
    private List<ConfigFieldParam> paramList;

    @Transient
    @JsonIgnore
    private Long unitFieldId;

    public ConfigField() {
    }

    public Long getConfigFieldId() {
        return configFieldId;
    }

    public void setConfigFieldId(Long configFieldId) {
        this.configFieldId = configFieldId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Integer getFieldEditable() {
        return fieldEditable;
    }

    public void setFieldEditable(Integer fieldEditable) {
        this.fieldEditable = fieldEditable;
    }

    public Integer getFieldRequired() {
        return fieldRequired;
    }

    public void setFieldRequired(Integer fieldRequired) {
        this.fieldRequired = fieldRequired;
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


    public ModelField getField() {
        return field;
    }

    public void setField(ModelField field) {
        this.field = field;
    }

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public ConfigFieldWidget getWidget() {
        return widget;
    }

    public void setWidget(ConfigFieldWidget widget) {
        this.widget = widget;
    }

    public List<ConfigFieldMap> getFieldLovMaps() {
        return fieldLovMaps;
    }

    public void setFieldLovMaps(List<ConfigFieldMap> fieldLovMaps) {
        this.fieldLovMaps = fieldLovMaps;
    }

    public String getRenderOptions() {
        return renderOptions;
    }

    public void setRenderOptions(String renderOptions) {
        this.renderOptions = renderOptions;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public List<Map<String, String>> getTlMaps() {
        return tlMaps;
    }

    public void setTlMaps(List<Map<String, String>> tlMaps) {
        this.tlMaps = tlMaps;
    }

    public String getCustType() {
        return custType;
    }

    public void setCustType(String custType) {
        this.custType = custType;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRenderRule() {
        return renderRule;
    }

    public void setRenderRule(String renderRule) {
        this.renderRule = renderRule;
    }

    public List<FieldConditionHeader> getConditionHeaders() {
        if (CollectionUtils.isNotEmpty(this.conditionHeaders)) {
            return this.conditionHeaders;
        }
        return new ArrayList<>(3);
    }

    public void setConditionHeaders(List<FieldConditionHeader> conditionHeaders) {
        this.conditionHeaders = conditionHeaders;
    }


    public List<ConfigFieldParam> getParamList() {
        return paramList;
    }

    public void setParamList(List<ConfigFieldParam> paramList) {
        this.paramList = paramList;
    }

    public Long getUnitFieldId() {
        return unitFieldId;
    }

    public void setUnitFieldId(Long unitFieldId) {
        this.unitFieldId = unitFieldId;
    }

    public FieldConditionHeader getConValid() {
        return conValid;
    }

    public void setConValid(FieldConditionHeader conValid) {
        this.conValid = conValid;
    }

    public String getWhereOption() {
        return whereOption;
    }

    public void setWhereOption(String whereOption) {
        this.whereOption = whereOption;
    }

    public static String cacheKey(Long tenantId) {
        return CustomizeConstants.CUSTOMIZE_CACHE_KEY + "config:field:" + tenantId;
    }

    public static String cacheKeyPattern() {
        return CustomizeConstants.CUSTOMIZE_CACHE_KEY + "config:field:*";
    }

    public static String cacheKey(Long tenantId, String unitCode) {
        return cacheKey(tenantId) + ":" + unitCode;
    }

    public static String cacheKey(Long tenantId, Long userId) {
        return CustomizeConstants.CUSTOMIZE_CACHE_KEY + "user:config:field:" + tenantId + ":" + userId;
    }

    public static String cacheKey(Long tenantId, Long userId, String unitCode) {
        return cacheKey(tenantId, userId) + ":" + unitCode;
    }

    public ConfigField cacheConvert() {
        if (CollectionUtils.isNotEmpty(this.tlMaps)) {
            Map<String, Map<String, String>> _tls = new HashMap(1);
            Map<String, String> tlsInnerMap = new HashMap(2);
            for (Map<String, String> item : tlMaps) {
                tlsInnerMap.put(item.get("lang"), item.get("fieldName"));
            }
            _tls.put("fieldName", tlsInnerMap);
            this.set_tls(_tls);
        }

        ConfigField configField = new ConfigField();

        BeanUtils.copyProperties(this, configField, FIELD_TENANT_ID, FIELD_CREATION_DATE, FIELD_CREATED_BY, FIELD_LAST_UPDATE_DATE,
                FIELD_LAST_UPDATED_BY, FIELD_OBJECT_VERSION_NUMBER, "_token", "widget", "fieldLovMaps", "unitCode", "tlMaps", "field");

        if (this.widget != null) {
            configField.setWidget(widget.cacheConvert());
        }

        if (CollectionUtils.isNotEmpty(this.fieldLovMaps)) {
            configField.setFieldLovMaps(this.fieldLovMaps.stream().map(ConfigFieldMap::cacheConvert).collect(Collectors.toList()));
        }

        if (CollectionUtils.isNotEmpty(this.conditionHeaders)) {
            configField.setConditionHeaders(this.conditionHeaders.stream().map(FieldConditionHeader::cacheConvert).collect(Collectors.toList()));
        }

        if (this.conValid != null) {
            configField.setConValid(this.conValid.cacheConvert());
        }

        if (CollectionUtils.isNotEmpty(this.paramList)) {
            configField.setParamList(this.paramList.stream().map(ConfigFieldParam::cacheConvert).collect(Collectors.toList()));
        }

        if (this.field != null) {
            configField.setField(this.field.cacheConvert());
        }

        return configField;
    }

    public static ConfigField readFormUnitField(UnitField unitField) {
        ConfigField configField = new ConfigField();
        configField.setField(unitField.getField());
        BeanUtils.copyProperties(unitField, configField, "field");
        if (unitField.isNotTableField()) {
            ModelField modelField = unitField.getField();
            if (modelField == null) {
                modelField = new ModelField();
            }
            modelField.setFieldCode(unitField.getFieldAlias());
            configField.setField(modelField);
        }
        configField.setVisible(unitField.getFieldVisible());
        return configField;
    }

    public void convertTls(String lang, boolean isStd) {
        if (isStd) {
            this.setFieldName(null);
        }
        if (MapUtils.isNotEmpty(this.get_tls()) && this.get_tls().containsKey("fieldName")) {
            Map<String, String> fieldNames = this.get_tls().get("fieldName");
            if (fieldNames.containsKey(lang) && StringUtils.isNotBlank(fieldNames.get(lang))) {
                this.setFieldName(fieldNames.get(lang));
            }
        }
    }


    public String mapKey() {
        if (isNotTableField()) {
            return getFieldAlias();
        }
        return this.getUnitId() + ":" + this.getField();
    }

    public String cacheHashKey() {
        return String.valueOf(this.configFieldId);
    }

    public static Map<String, ConfigField> translateMap(Map<String, ConfigField> configFieldMap) {
        if (MapUtils.isEmpty(configFieldMap)) {
            return new HashMap<>(32);
        }
        return configFieldMap.entrySet().stream().collect(Collectors.toMap(entry -> entry.getValue().bizHashKey(), Map.Entry::getValue));
    }

}
