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

/**
 * @author qiang.zeng@hand-china.com 2020-01-10 22:22
 */
public class BaseModelRelationField extends AuditDomain {
    public static final String FIELD_ID = "id";
    public static final String FIELD_RELATION_ID = "relationId";
    public static final String FIELD_MASTER_MODEL_FIELD_CODE = "masterModelFieldCode";
    public static final String FIELD_RELATION_MODEL_FIELD_CODE = "relationModelFieldCode";

    //
    // 业务方法(按public protected private顺序排列)
    // ------------------------------------------------------------------------------

    //
    // 数据库字段
    // ------------------------------------------------------------------------------

    @ApiModelProperty("表ID，主键，供其他表做外键")
    @Id
    @GeneratedValue
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_REL_LINE)
    private Long id;
    @ApiModelProperty(value = "关系ID", required = true)
    @NotNull
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_REL_HEADER)
    private Long relationId;
    @ApiModelProperty(value = "主字段代码，hmde_model_field.code", required = true)
    @NotBlank
    private String masterModelFieldCode;
    @ApiModelProperty(value = "关联字段代码，hmde_model_field.code", required = true)
    @NotBlank
    private String relationModelFieldCode;
    @Transient
    @ApiModelProperty(value = "主字段名称")
    private String masterModelFieldName;
    @Transient
    @ApiModelProperty(value = "关联字段名称")
    private String relationModelFieldName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRelationId() {
        return relationId;
    }

    public void setRelationId(Long relationId) {
        this.relationId = relationId;
    }

    public String getMasterModelFieldCode() {
        return masterModelFieldCode;
    }

    public void setMasterModelFieldCode(String masterModelFieldCode) {
        this.masterModelFieldCode = masterModelFieldCode;
    }

    public String getRelationModelFieldCode() {
        return relationModelFieldCode;
    }

    public void setRelationModelFieldCode(String relationModelFieldCode) {
        this.relationModelFieldCode = relationModelFieldCode;
    }

    public String getMasterModelFieldName() {
        return masterModelFieldName;
    }

    public void setMasterModelFieldName(String masterModelFieldName) {
        this.masterModelFieldName = masterModelFieldName;
    }

    public String getRelationModelFieldName() {
        return relationModelFieldName;
    }

    public void setRelationModelFieldName(String relationModelFieldName) {
        this.relationModelFieldName = relationModelFieldName;
    }
}
