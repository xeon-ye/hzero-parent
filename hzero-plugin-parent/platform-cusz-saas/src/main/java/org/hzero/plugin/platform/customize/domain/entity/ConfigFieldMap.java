package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import io.swagger.annotations.ApiModelProperty;
import org.hzero.starter.keyencrypt.core.Encrypt;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import static org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.*;

/**
 * @author : peng.yu01@hand-china.com 2019/12/25 15:17
 */
@Table(name = "hpfm_cusz_config_field_map")
@ModifyAudit
//@VersionAudit
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ConfigFieldMap extends AuditDomain {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_CONFIG_FIELD_ID = "configFieldId";
    public static final String FIELD_TARGET_CONFIG_FIELD_ID = "targetConfigFieldId";
    public static final String FIELD_TARGET_FIELD_ID = "targetFieldId";
    public static final String FIELD_MODEL_ID = "modelId";
    public static final String FIELD_SOURCE_FIELD_ID = "sourceFieldId";
    public static final String FIELD_SOURCE_FIELD_ALIAS = "sourceFieldAlias";

    public static final String[] UPDATE_FIELD_COMMON_LIST = {FIELD_TARGET_CONFIG_FIELD_ID, FIELD_TARGET_FIELD_ID, FIELD_MODEL_ID, FIELD_SOURCE_FIELD_ID, FIELD_SOURCE_FIELD_ALIAS};

    @Id
    @GeneratedValue
    @Encrypt(ENCRYPT_KEY_CONFIG_FIELD_PAR)
    private Long id;

    private Long tenantId;

    @ApiModelProperty("个性化字段ID")
    @Encrypt(ENCRYPT_KEY_CONFIG_FIELD)
    private Long configFieldId;

    @ApiModelProperty("目标字段ID")
    @Encrypt(ENCRYPT_KEY_MODEL_FIELD)
    private Long targetFieldId;

    @ApiModelProperty("映射源字段所属模型ID")
    @Encrypt(ENCRYPT_KEY_MODEL)
    private Long sourceModelId;

    @ApiModelProperty("映射源字段ID")
    @Encrypt(ENCRYPT_KEY_MODEL_FIELD)
    private Long sourceFieldId;

    @ApiModelProperty("映射源字段别名编码")
    private String sourceFieldAlias;

    @Transient
    @ApiModelProperty(hidden = true)
    private String modelName;
    @Transient
    @ApiModelProperty(hidden = true)
    private String targetFieldName;
    @Transient
    @ApiModelProperty(hidden = true)
    private String sourceFieldName;
    @Transient
    @ApiModelProperty(hidden = true)
    private String unitCode;

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

    public Long getConfigFieldId() {
        return configFieldId;
    }

    public void setConfigFieldId(Long configFieldId) {
        this.configFieldId = configFieldId;
    }

    public String getSourceFieldAlias() {
        return sourceFieldAlias;
    }

    public void setSourceFieldAlias(String sourceFieldAlias) {
        this.sourceFieldAlias = sourceFieldAlias;
    }

    public Long getTargetFieldId() {
        return targetFieldId;
    }

    public void setTargetFieldId(Long targetFieldId) {
        this.targetFieldId = targetFieldId;
    }

    public Long getSourceFieldId() {
        return sourceFieldId;
    }

    public void setSourceFieldId(Long sourceFieldId) {
        this.sourceFieldId = sourceFieldId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Long getSourceModelId() {
        return sourceModelId;
    }

    public void setSourceModelId(Long sourceModelId) {
        this.sourceModelId = sourceModelId;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldName(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public String getSourceFieldName() {
        return sourceFieldName;
    }

    public void setSourceFieldName(String sourceFieldName) {
        this.sourceFieldName = sourceFieldName;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public ConfigFieldMap cacheConvert() {
        ConfigFieldMap fieldMap = new ConfigFieldMap();
        fieldMap.setSourceFieldAlias(this.sourceFieldAlias);
        fieldMap.setTargetFieldId(this.targetFieldId);
        return fieldMap;
    }
}
