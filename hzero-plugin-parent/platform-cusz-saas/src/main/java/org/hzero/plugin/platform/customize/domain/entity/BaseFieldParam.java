package org.hzero.plugin.platform.customize.domain.entity;

import io.choerodon.mybatis.domain.AuditDomain;
import org.hzero.starter.keyencrypt.core.Encrypt;

import javax.persistence.Transient;

/**
 * @author xiangyu.qi01@hand-china.com on 2020-03-03
 */
public class BaseFieldParam extends AuditDomain {

    public static final String FIELD_TENANT_ID = "tenantId";
    @Encrypt
    private Long paramId;

    private Long tenantId;

    private String paramKey;

    private String paramValue;

    private String paramType;

    @Encrypt
    private Long paramUnitId;
    @Encrypt
    private Long paramFieldId;

    @Transient
    private String paramUnitCode;
    @Transient
    private String paramUnitName;
    @Transient
    private String paramFieldCode;
    @Transient
    private String paramFieldName;

    public Long getParamId() {
        return paramId;
    }

    public void setParamId(Long paramId) {
        this.paramId = paramId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }


    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

    public String getParamType() {
        return paramType;
    }

    public void setParamType(String paramType) {
        this.paramType = paramType;
    }

    public Long getParamUnitId() {
        return paramUnitId;
    }

    public void setParamUnitId(Long paramUnitId) {
        this.paramUnitId = paramUnitId;
    }

    public Long getParamFieldId() {
        return paramFieldId;
    }

    public void setParamFieldId(Long paramFieldId) {
        this.paramFieldId = paramFieldId;
    }

    public String getParamUnitCode() {
        return paramUnitCode;
    }

    public void setParamUnitCode(String paramUnitCode) {
        this.paramUnitCode = paramUnitCode;
    }

    public String getParamUnitName() {
        return paramUnitName;
    }

    public void setParamUnitName(String paramUnitName) {
        this.paramUnitName = paramUnitName;
    }

    public String getParamFieldCode() {
        return paramFieldCode;
    }

    public void setParamFieldCode(String paramFieldCode) {
        this.paramFieldCode = paramFieldCode;
    }

    public String getParamFieldName() {
        return paramFieldName;
    }

    public void setParamFieldName(String paramFieldName) {
        this.paramFieldName = paramFieldName;
    }


}
