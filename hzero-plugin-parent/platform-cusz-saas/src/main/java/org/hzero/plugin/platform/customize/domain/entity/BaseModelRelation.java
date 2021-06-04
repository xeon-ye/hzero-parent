package org.hzero.plugin.platform.customize.domain.entity;

import io.choerodon.mybatis.domain.AuditDomain;
import io.swagger.annotations.ApiModelProperty;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author qiang.zeng@hand-china.com 2020-01-10 22:22
 */
public class BaseModelRelation extends AuditDomain {
    public static final String FIELD_ID = "id";
    public static final String FIELD_CODE = "code";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_RELATION_TYPE = "relationType";
    public static final String FIELD_MASTER_MODEL_OBJECT_CODE = "masterModelObjectCode";
    public static final String FIELD_RELATION_MODEL_OBJECT_CODE = "relationModelObjectCode";
    public static final String FIELD_DESCRIPTION = "description";

    //
    // 业务方法(按public protected private顺序排列)
    // ------------------------------------------------------------------------------

    //
    // 数据库字段
    // ------------------------------------------------------------------------------


    @ApiModelProperty("表ID，主键，供其他表做外键")
    @Id
    @GeneratedValue
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_REL_HEADER)
    private Long id;
    @ApiModelProperty(value = "关系代码")
    private String code;
    @ApiModelProperty(value = "关系名称")
    private String name;
    @ApiModelProperty(value = "租户ID", required = true)
    @NotNull
    private Long tenantId;
    @ApiModelProperty(value = "关系类型。包括ONE_TO_ONE，ONE_TO_MANY", required = true)
    @NotBlank
    private String relationType;
    @ApiModelProperty(value = "主模型代码，hmde_model_object.code", required = true)
    @NotBlank
    private String masterModelObjectCode;
    @ApiModelProperty(value = "关联模型代码，hmde_model_object.code", required = true)
    @NotBlank
    private String relationModelObjectCode;
    @ApiModelProperty(value = "关系描述")
    private String description;
    //
    // 非数据库字段
    // ------------------------------------------------------------------------------


    @Transient
    @ApiModelProperty(value = "模型关系字段列表")
    private List<BaseModelRelationField> modelRelationFields;
    @Transient
    @ApiModelProperty(value = "主模型ID，hmde_model_object.id")
    private String masterModelObjectId;
    @Transient
    @ApiModelProperty(value = "关联模型ID，hmde_model_object.id")
    private String relationModelObjectId;
    @Transient
    @ApiModelProperty(value = "主模型名称，hmde_model_object.name")
    private String masterModelObjectName;
    @Transient
    @ApiModelProperty(value = "关联模型名称，hmde_model_object.name")
    private String relationModelObjectName;

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
     * @return 关系代码
     */
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
     * @return 关系类型。包括ONE_TO_ONE，ONE_TO_MANY
     */
    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    /**
     * @return 主模型代码，hmde_model_object.code
     */
    public String getMasterModelObjectCode() {
        return masterModelObjectCode;
    }

    public void setMasterModelObjectCode(String masterModelObjectCode) {
        this.masterModelObjectCode = masterModelObjectCode;
    }

    /**
     * @return 关联模型代码，hmde_model_object.code
     */
    public String getRelationModelObjectCode() {
        return relationModelObjectCode;
    }

    public void setRelationModelObjectCode(String relationModelObjectCode) {
        this.relationModelObjectCode = relationModelObjectCode;
    }

    /**
     * @return 关系描述
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * @return 主模型ID，hmde_model_object.id
     */
    public String getMasterModelObjectId() {
        return masterModelObjectId;
    }

    public void setMasterModelObjectId(String masterModelObjectId) {
        this.masterModelObjectId = masterModelObjectId;
    }

    /**
     * @return 关联模型ID，hmde_model_object.id
     */
    public String getRelationModelObjectId() {
        return relationModelObjectId;
    }

    public void setRelationModelObjectId(String relationModelObjectId) {
        this.relationModelObjectId = relationModelObjectId;
    }

    /**
     * @return 主模型名称
     */
    public String getMasterModelObjectName() {
        return masterModelObjectName;
    }

    public void setMasterModelObjectName(String masterModelObjectName) {
        this.masterModelObjectName = masterModelObjectName;
    }

    /**
     * @return 关联模型名称
     */
    public String getRelationModelObjectName() {
        return relationModelObjectName;
    }

    public void setRelationModelObjectName(String relationModelObjectName) {
        this.relationModelObjectName = relationModelObjectName;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<BaseModelRelationField> getModelRelationFields() {
        return modelRelationFields;
    }

    public void setModelRelationFields(List<BaseModelRelationField> modelRelationFields) {
        this.modelRelationFields = modelRelationFields;
    }
}
