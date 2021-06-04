package org.hzero.plugin.platform.customize.domain.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

import io.choerodon.mybatis.domain.AuditDomain;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author qiang.zeng@hand-china.com 2020-01-10 22:22
 */
public class BaseModelObject extends AuditDomain {
    public static final String FIELD_ID = "id";
    public static final String FIELD_REF_TABLE_CODE = "refTableCode";
    public static final String FIELD_REF_TABLE_NAME = "refTableName";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_CODE = "code";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_DATA_SOURCE_TYPE = "dataSourceType";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_PUBLISH_VERSION = "publishVersion";
    public static final String FIELD_APP_ID = "appId";
    public static final String FIELD_TENANT_FLAG = "tenantFlag";
    public static final String FIELD_MULTI_LANGUAGE_FLAG = "multiLanguageFlag";

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
    @ApiModelProperty(value = "元数据表代码，hmde_table.code")
    private String refTableCode;
    @ApiModelProperty(value = "元数据表名称，hmde_table.code", required = true)
    @NotBlank
    private String refTableName;
    @ApiModelProperty(value = "服务名", required = true)
    @NotBlank
    private String refServiceName;
    @ApiModelProperty(value = "数据库名", required = true)
    @NotBlank
    private String refDatabaseName;
    @ApiModelProperty(value = "数据库类型", required = true)
    @NotBlank
    private String refDatabaseType;
    @ApiModelProperty(value = "租户ID", required = true)
    private Long tenantId;
    @ApiModelProperty(value = "代码", required = true)
    private String code;
    @ApiModelProperty(value = "名称", required = true)
    @NotBlank
    private String name;
    @ApiModelProperty(value = "描述")
    private String description;
    @ApiModelProperty(value = "数据来源类型。")
    private String dataSourceType;
    @ApiModelProperty(value = "模型类型")
    private String type;
    @ApiModelProperty(value = "模型发布版本号", required = true)
    private Long publishVersion;
    @ApiModelProperty(value = "应用ID")
    private Long appId;
    @ApiModelProperty(value = "是否是租户级。1是，0不是", required = true)
    private Boolean tenantFlag;
    @ApiModelProperty(value = "主键编码")
    private String primaryKey;
    @ApiModelProperty(value = "是否支持多语言")
    private Integer multiLanguageFlag;

    //
    // 非数据库字段
    // ------------------------------------------------------------------------------


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
     * @return 元数据表代码，hmde_table.code
     */
    public String getRefTableCode() {
        return refTableCode;
    }

    public void setRefTableCode(String refTableCode) {
        this.refTableCode = refTableCode;
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
     * @return 代码
     */
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return 名称
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
     * @return 模型发布版本号
     */
    public Long getPublishVersion() {
        return publishVersion;
    }

    public void setPublishVersion(Long publishVersion) {
        this.publishVersion = publishVersion;
    }

    /**
     * @return 元数据表名称，hmde_table.name
     */
    public String getRefTableName() {
        return refTableName;
    }

    public void setRefTableName(String refTableName) {
        this.refTableName = refTableName;
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(String dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public Boolean getTenantFlag() {
        return tenantFlag;
    }

    public void setTenantFlag(Boolean tenantFlag) {
        this.tenantFlag = tenantFlag;
    }

    public String getRefServiceName() {
        return refServiceName;
    }

    public void setRefServiceName(String refServiceName) {
        this.refServiceName = refServiceName;
    }

    public String getRefDatabaseName() {
        return refDatabaseName;
    }

    public void setRefDatabaseName(String refDatabaseName) {
        this.refDatabaseName = refDatabaseName;
    }

    public String getRefDatabaseType() {
        return refDatabaseType;
    }

    public void setRefDatabaseType(String refDatabaseType) {
        this.refDatabaseType = refDatabaseType;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public Integer getMultiLanguageFlag() {
        return multiLanguageFlag;
    }

    public void setMultiLanguageFlag(Integer multiLanguageFlag) {
        this.multiLanguageFlag = multiLanguageFlag;
    }
}
