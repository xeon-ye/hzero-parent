package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import io.swagger.annotations.ApiModelProperty;
import org.hzero.boot.customize.constant.CustomizeConstants;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeUnitRepository;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 10:54
 */
@Table(name = "hpfm_cusz_unit")
/*@VersionAudit*/
@ModifyAudit
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Unit extends AuditDomain {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_MODEL_ID = "modelId";
    public static final String FIELD_UNIT_CODE = "unitCode";
    public static final String FIELD_UNIT_NAME = "unitName";
    public static final String FIELD_UNIT_TYPE = "unitType";
    public static final String FIELD_SQL_IDS = "sqlIds";
    public static final String FIELD_READ_ONLY = "readOnly";
    public static final String FIELD_FORM_MAX_COL = "formMaxCol";
    public static final String FIELD_FIELD_LABEL_COL = "labelCol";
    public static final String FIELD_FIELD_WRAPPER_COL = "wrapperCol";
    public static final String FIELD_FIELD_ENABLE_FLAG = "enableFlag";
    public static final String FIELD_CON_RELATED_UNIT = "conRelatedUnit";
    public static final String FIELD_UNIT_GROUP_ID = "unitGroupId";

    public static final String[] UPDATE_FIELD_COMMON_LIST = {FIELD_CON_RELATED_UNIT, FIELD_UNIT_NAME, FIELD_SQL_IDS,
            FIELD_FORM_MAX_COL, FIELD_FIELD_LABEL_COL, FIELD_FIELD_WRAPPER_COL, FIELD_FIELD_ENABLE_FLAG};

    @Id
    @GeneratedValue
    @Encrypt(org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT)
    private Long id;
    private Long tenantId;
    @ApiModelProperty("模型ID")
    @Encrypt(org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL)
    private Long modelId;
    /**
     * 个性化单元编码
     */
    @ApiModelProperty("单元编码")
    private String unitCode;
    /**
     * 个性化单元名称
     */
    @ApiModelProperty("单元名称")
    private String unitName;
    /**
     * 单元类型,表单,表格,tab页
     */
    @ApiModelProperty("单元类型")
    private String unitType;
    /**
     * 单元所属菜单编码
     */
    @Transient
    private String menuCode;

    @ApiModelProperty("是否只读")
    private Integer readOnly;

    /**
     * sqlID
     */
    private String sqlIds;

    /**
     * 表单最大列数
     */
    private Integer formMaxCol;

    /**
     * 所属组ID
     */
    @Encrypt(org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT_GROUP)
    private Long unitGroupId;

    /**
     * label列数
     */
    private Integer labelCol;

    /**
     * wrapper列数
     */
    private Integer wrapperCol;

    /**
     * 启用标识
     */
    private Integer enableFlag;

    /**
     * 关联单元，多个使用逗号隔开，主要用于条件配置
     */
    @ApiModelProperty(value = "关联单元", hidden = true)
    private String conRelatedUnit;

    @Transient
    private List<String> conRelatedUnits;

    /**
     * 功能名称
     */
    @Transient
    @ApiModelProperty(value = "菜单名称", hidden = true)
    private String menuName;

    /**
     * 模型名称
     */
    @Transient
    @ApiModelProperty(hidden = true)
    private String modelName;

    /**
     * 单元组编码
     */
    @Transient
    private String unitGroupCode;

    /**
     * 组名称
     */
    @Transient
    private String unitGroupName;

    @Transient
    @ApiModelProperty(hidden = true)
    private Config config;

    /**
     * 模型表名
     */
    @Transient
    private String modelTable;

    @Transient
    private String copyUnitCode;


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

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getSqlIds() {
        return sqlIds;
    }

    public void setSqlIds(String sqlIds) {
        this.sqlIds = sqlIds;
    }

    public Integer getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Integer readOnly) {
        this.readOnly = readOnly;
    }

    public Integer getFormMaxCol() {
        return formMaxCol;
    }

    public void setFormMaxCol(Integer formMaxCol) {
        this.formMaxCol = formMaxCol;
    }

    public Long getUnitGroupId() {
        return unitGroupId;
    }

    public void setUnitGroupId(Long unitGroupId) {
        this.unitGroupId = unitGroupId;
    }

    public String getUnitGroupCode() {
        return unitGroupCode;
    }

    public void setUnitGroupCode(String unitGroupCode) {
        this.unitGroupCode = unitGroupCode;
    }

    public String getUnitGroupName() {
        return unitGroupName;
    }

    public void setUnitGroupName(String unitGroupName) {
        this.unitGroupName = unitGroupName;
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

    public Integer getEnableFlag() {
        return enableFlag;
    }

    public void setEnableFlag(Integer enableFlag) {
        this.enableFlag = enableFlag;
    }

    public String getConRelatedUnit() {
        return conRelatedUnit;
    }

    public void setConRelatedUnit(String conRelatedUnit) {
        this.conRelatedUnit = conRelatedUnit;
    }

    public List<String> getConRelatedUnits() {
        return conRelatedUnits;
    }

    public void setConRelatedUnits(List<String> conRelatedUnits) {
        this.conRelatedUnits = conRelatedUnits;
    }

    public static String cacheKey() {
        return CustomizeConstants.UNIT_CACHE_KEY;
    }

    public String getModelTable() {
        return modelTable;
    }

    public void setModelTable(String modelTable) {
        this.modelTable = modelTable;
    }

    public String getCopyUnitCode() {
        return copyUnitCode;
    }

    public void setCopyUnitCode(String copyUnitCode) {
        this.copyUnitCode = copyUnitCode;
    }

    public Unit cacheConvert() {
        Unit unit = new Unit();
        BeanUtils.copyProperties(this, unit, FIELD_UNIT_CODE, FIELD_TENANT_ID, FIELD_CREATION_DATE, FIELD_CREATED_BY, FIELD_LAST_UPDATE_DATE,
                FIELD_LAST_UPDATED_BY, FIELD_OBJECT_VERSION_NUMBER, "_token");
        return unit;
    }

    @JsonIgnore
    public boolean isDisabled() {
        return ObjectUtils.nullSafeEquals(0, this.enableFlag);
    }

    public static List<? extends BaseField> sortField(String unitType, List<? extends BaseField> fields) {
        if (org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.FormType.FORM.equalsIgnoreCase(unitType)
                || org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.FormType.QUERY_FORM.equalsIgnoreCase(unitType)) {
            fields.sort((o1, o2) -> {
                int rowSort = comparatorIntNull(o1.getFormRow(), o2.getFormRow());
                if (rowSort != 0) {
                    return rowSort;
                }
                //比较行
                return comparatorIntNull(o1.getFormCol(), o2.getFormCol());
            });
        } else {
            fields.sort((o1, o2) -> comparatorIntNull(o1.getGridSeq(), o2.getGridSeq()));
        }
        return fields;
    }

    protected static int comparatorIntNull(Integer a, Integer b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        } else if (b == null) {
            return -1;
        }
        //比较行
        return a.compareTo(b);

    }

    public List<Map<String, String>> autoGenerateUnitAlias(CustomizeUnitRepository unitRepository) {
        List<String> allUnitCode = new ArrayList<>();
        allUnitCode.add(this.getUnitCode());
        if (!StringUtils.isEmpty(this.getConRelatedUnit())) {
            allUnitCode.addAll(StringUtils.commaDelimitedListToSet(this.getConRelatedUnit()));
        }
        List<Map<String, String>> result = new ArrayList<>(allUnitCode.size());
        for (int i = 0; i < allUnitCode.size(); i++) {
            Map<String, String> index = new HashMap();
            String unitCode = allUnitCode.get(i);
            index.put(FIELD_UNIT_CODE, unitCode);
            index.put("alias", "u" + (i + 1));
            if(unitRepository != null){
                index.put("unitName",unitRepository.getUnitCache(unitCode).getUnitName());
            }
            result.add(index);
        }
        return result;
    }
}
