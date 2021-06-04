package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import org.apache.commons.collections4.MapUtils;
import org.hzero.boot.customize.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : peng.yu01@hand-china.com 2019/12/21 10:57
 */
@Table(name = "hpfm_cusz_unit_field")
@ModifyAudit
/*@VersionAudit*/
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UnitField extends BaseField {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_UNIT_ID = "unitId";
    public static final String FIELD_MODEL_ID = "modelId";
    public static final String FIELD_FIELD_ID = "fieldId";
    public static final String FIELD_FIELD_EDITABLE = "fieldEditable";
    public static final String FIELD_FIELD_REQUIRED = "fieldRequired";
    public static final String FIELD_FIELD_VISIBLE = "fieldVisible";
    public static final String FIELD_FORM_COL = "formCol";
    public static final String FIELD_FORM_ROW = "formRow";
    public static final String FIELD_GRID_SEQ = "gridSeq";
    public static final String FIELD_GRID_WIDTH = "gridWidth";
    public static final String FIELD_GRID_FIXED = "gridFixed";
    public static final String FIELD_RENDER_OPTIONS = "renderOptions";
    public static final String FIELD_FIELD_ALIAS = "fieldAlias";
    public static final String FIELD_FIELD_LABEL_COL = "labelCol";
    public static final String FIELD_FIELD_WRAPPER_COL = "wrapperCol";


    public static final String[] UPDATE_FIELD_COMMON_LIST = {FIELD_FORM_COL, FIELD_FORM_ROW, FIELD_GRID_SEQ, FIELD_GRID_WIDTH, FIELD_GRID_FIXED,
            FIELD_RENDER_OPTIONS, FIELD_FIELD_NAME, FIELD_FIELD_ALIAS, FIELD_FIELD_LABEL_COL, FIELD_FIELD_WRAPPER_COL};


    @Id
    @GeneratedValue
    @Encrypt(org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT_FIELD)
    private Long id;

    private Long tenantId;
    @Encrypt(org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT)
    private Long unitId;


    private String fieldName;


    private Integer fieldEditable;

    private Integer fieldRequired;

    private Integer fieldVisible;

    private Integer gridWidth;

    private String gridFixed;

    private String renderOptions;

    private Integer labelCol;

    private Integer wrapperCol;

    @Transient
    private ModelField field;

    @Transient
    private String unitCode;

    @Transient
    private List<UnitFieldParam> paramList;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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


    public Integer getGridWidth() {
        return gridWidth;
    }

    public void setGridWidth(Integer gridWidth) {
        this.gridWidth = gridWidth;
    }

    public String getGridFixed() {
        return gridFixed;
    }

    public void setGridFixed(String gridFixed) {
        this.gridFixed = gridFixed;
    }

    public ModelField getField() {
        return field;
    }

    public void setField(ModelField field) {
        this.field = field;
    }

    public String getRenderOptions() {
        return renderOptions;
    }

    public void setRenderOptions(String renderOptions) {
        this.renderOptions = renderOptions;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public Integer getFieldVisible() {
        return fieldVisible;
    }

    public void setFieldVisible(Integer fieldVisible) {
        this.fieldVisible = fieldVisible;
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

    public List<UnitFieldParam> getParamList() {
        return paramList;
    }

    public void setParamList(List<UnitFieldParam> paramList) {
        this.paramList = paramList;
    }

    public static String cacheKey(String unitCode) {
        return CustomizeConstants.CUSTOMIZE_CACHE_KEY + "unit:field:" + unitCode;
    }

    public UnitField cacheConvert() {
        UnitField unitField = new UnitField();

        BeanUtils.copyProperties(this, unitField, FIELD_TENANT_ID, FIELD_CREATION_DATE, FIELD_CREATED_BY, FIELD_LAST_UPDATE_DATE,
                FIELD_LAST_UPDATED_BY, FIELD_OBJECT_VERSION_NUMBER, "_token", "unitCode", "field");

        if (!CollectionUtils.isEmpty((this.paramList))) {
            unitField.setParamList(this.paramList.stream().map(UnitFieldParam::cacheConvert).collect(Collectors.toList()));
        }

        return unitField;
    }


    /**
     * redis缓存hash key
     *
     * @return
     */
    public String cacheHashKey() {
        return String.valueOf(this.id);
    }

    public static Map<String, UnitField> translateMap(Map<String, UnitField> configFieldMap) {
        if (MapUtils.isEmpty(configFieldMap)) {
            return new HashMap<>(32);
        }
        return configFieldMap.entrySet().stream().collect(Collectors.toMap(entry -> entry.getValue().bizHashKey(), Map.Entry::getValue, (key1, key2) -> key2));
    }

    public void copyUnitParamList() {
        if (!CollectionUtils.isEmpty(this.getParamList())) {
            this.setParamList(getParamList().stream().map(t -> {
                UnitFieldParam param = t.cacheConvert();
                param.setParamId(null);
                return param;
            }).collect(Collectors.toList()));
        }
    }

}
