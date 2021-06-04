package org.hzero.plugin.platform.customize.domain.entity;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.domain.AuditDomain;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 10:41
 */
/*@VersionAudit*/
@ModifyAudit
@Table(name = "hpfm_cusz_model_field_rul")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ModelFieldRule extends AuditDomain {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_FIELD_ID = "fieldId";
    public static final String FIELD_MODEL_ID = "modelId";
    public static final String FIELD_PERMISSION_TYPE = "permissionType";

    @Id
    @GeneratedValue
    private Long id;
    private Long tenantId;
    private Long fieldId;
    private Long modelId;
    /**
     * 字段权限限制类型， SELECT，INSERT，UPDATE
     */
    @Column(name = "field_permission_type")
    private String permissionType;

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

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }
}
