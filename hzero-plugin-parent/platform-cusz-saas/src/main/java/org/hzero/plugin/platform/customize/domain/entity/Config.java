package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import org.hzero.boot.customize.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;

import javax.persistence.*;
import static org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.*;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 10:47
 */
@Table(name = "hpfm_cusz_config")
/*@VersionAudit*/
@ModifyAudit
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Config extends AuditDomain {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_UNIT_ID = "unitId";
    public static final String FIELD_UNIT_TITLE = "unitTitle";
    public static final String FIELD_MAX_COL = "maxCol";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_PAGE_SIZE = "pageSize";
    @Id
    @GeneratedValue
    @Encrypt(ENCRYPT_KEY_CONFIG)
    private Long id;
    private Long tenantId;
    @Encrypt(ENCRYPT_KEY_UNIT)
    private Long unitId;
    /**
     * 单元标题
     */
    private Long unitTitle;

    @Column(name = "form_max_col")
    private Long maxCol;

    @Column(name = "form_page_size")
    private Integer pageSize;

    @Transient
    private Integer readOnly;

    @Transient
    private String unitCode;

    /**
     * 用户id
     */
    private Long userId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Config setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public Long getUnitId() {
        return unitId;
    }

    public Config setUnitId(Long unitId) {
        this.unitId = unitId;
        return this;
    }

    public Long getUnitTitle() {
        return unitTitle;
    }

    public void setUnitTitle(Long unitTitle) {
        this.unitTitle = unitTitle;
    }

    public Long getMaxCol() {
        return maxCol;
    }

    public void setMaxCol(Long maxCol) {
        this.maxCol = maxCol;
    }

    public Integer getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Integer readOnly) {
        this.readOnly = readOnly;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Config setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public static String cacheKey(Long tenantId) {
        return CustomizeConstants.CUSTOMIZE_CACHE_KEY + "config:header:" + tenantId;
    }

    public static String cacheKey(Long tenantId, Long userId) {
        return CustomizeConstants.CUSTOMIZE_CACHE_KEY + "user:config:" + tenantId + ":" + userId;
    }

    public Config cacheConvert() {
        Config config = new Config();

        config.setId(this.id);
        config.setMaxCol(this.maxCol);
        config.setReadOnly(this.readOnly);
        config.setUnitTitle(this.unitTitle);
        config.setPageSize(this.getPageSize());

        return config;
    }
}
