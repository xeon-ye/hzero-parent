package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.domain.AuditDomain;
import io.swagger.annotations.ApiModelProperty;
import org.hzero.boot.customize.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 10:45
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ModelRelation extends AuditDomain {

    public static final String CACHE_KEY_PREFIX = CustomizeConstants.CUSTOMIZE_CACHE_KEY + "model:relation:";

    @Encrypt(org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_REL_HEADER)
    private Long id;
    @ApiModelProperty("主模型ID")
    @Encrypt(org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL)
    private Long masterModelId;
    @ApiModelProperty("从模型ID")
    @Encrypt(org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL)
    private Long slaveModelId;
    @ApiModelProperty("主模型字段ID")
    @Encrypt(org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_FIELD)
    private Long masterFieldId;
    @ApiModelProperty("从模型字段ID")
    @Encrypt(org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_FIELD)
    private Long slaveFieldId;
    @ApiModelProperty("模型关系")
    private String relation;

    @ApiModelProperty("主模型名称")
    private String masterModelName;
    @ApiModelProperty("从模型编码")
    private String masterModelCode;
    @ApiModelProperty("从模型名称")
    private String slaveModelName;
    @ApiModelProperty("从模型编码")
    private String slaveModelCode;
    @ApiModelProperty("主模型字段名称")
    private String masterFieldName;
    @ApiModelProperty("主模型字段编码")
    private String masterFieldCode;
    @ApiModelProperty("从模型字段名称")
    private String slaveFieldName;
    @ApiModelProperty("从模型字段编码")
    private String slaveFieldCode;
    @ApiModelProperty("主模型表名")
    private String masterTableName;
    @ApiModelProperty("从模型表名")
    private String slaveTableName;

    public ModelRelation() {
    }

    public void resetRelation() {
        Long copyMasterModelId = this.masterModelId;
        String copyMasterModelCode = this.masterModelCode;
        String copyMasterModelName = this.masterModelName;
        String copyMasterTableName = this.masterTableName;

        Long copyMasterFieldId = this.masterFieldId;
        String copyMasterFieldCode = this.masterFieldCode;
        String copyMasterFieldName = this.masterFieldName;

        Long copySlaveModelId = this.slaveModelId;
        String copySlaveModelCode = this.slaveModelCode;
        String copySlaveModelName = this.slaveModelName;
        String copySlaveTableName = this.slaveTableName;

        Long copySlaveFieldId = this.slaveFieldId;
        String copySlaveFieldCode = this.slaveFieldCode;
        String copySlaveFieldName = this.slaveFieldName;

        this.masterModelId = copySlaveModelId;
        this.masterModelCode = copySlaveModelCode;
        this.masterModelName = copySlaveModelName;
        this.masterTableName = copySlaveTableName;

        this.masterFieldId = copySlaveFieldId;
        this.masterFieldCode = copySlaveFieldCode;
        this.masterFieldName = copySlaveFieldName;

        this.slaveModelId = copyMasterModelId;
        this.slaveModelCode = copyMasterModelCode;
        this.slaveModelName = copyMasterModelName;
        this.slaveTableName = copyMasterTableName;

        this.slaveFieldId = copyMasterFieldId;
        this.slaveFieldCode = copyMasterFieldCode;
        this.slaveFieldName = copyMasterFieldName;

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMasterModelId() {
        return masterModelId;
    }

    public void setMasterModelId(Long masterModelId) {
        this.masterModelId = masterModelId;
    }

    public Long getSlaveModelId() {
        return slaveModelId;
    }

    public void setSlaveModelId(Long slaveModelId) {
        this.slaveModelId = slaveModelId;
    }

    public Long getMasterFieldId() {
        return masterFieldId;
    }

    public void setMasterFieldId(Long masterFieldId) {
        this.masterFieldId = masterFieldId;
    }

    public Long getSlaveFieldId() {
        return slaveFieldId;
    }

    public void setSlaveFieldId(Long slaveFieldId) {
        this.slaveFieldId = slaveFieldId;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getMasterModelName() {
        return masterModelName;
    }

    public void setMasterModelName(String masterModelName) {
        this.masterModelName = masterModelName;
    }

    public String getMasterModelCode() {
        return masterModelCode;
    }

    public void setMasterModelCode(String masterModelCode) {
        this.masterModelCode = masterModelCode;
    }

    public String getSlaveModelName() {
        return slaveModelName;
    }

    public void setSlaveModelName(String slaveModelName) {
        this.slaveModelName = slaveModelName;
    }

    public String getSlaveModelCode() {
        return slaveModelCode;
    }

    public void setSlaveModelCode(String slaveModelCode) {
        this.slaveModelCode = slaveModelCode;
    }

    public String getMasterFieldName() {
        return masterFieldName;
    }

    public void setMasterFieldName(String masterFieldName) {
        this.masterFieldName = masterFieldName;
    }

    public String getMasterFieldCode() {
        return masterFieldCode;
    }

    public void setMasterFieldCode(String masterFieldCode) {
        this.masterFieldCode = masterFieldCode;
    }

    public String getSlaveFieldName() {
        return slaveFieldName;
    }

    public void setSlaveFieldName(String slaveFieldName) {
        this.slaveFieldName = slaveFieldName;
    }

    public String getSlaveFieldCode() {
        return slaveFieldCode;
    }

    public void setSlaveFieldCode(String slaveFieldCode) {
        this.slaveFieldCode = slaveFieldCode;
    }

    public String getMasterTableName() {
        return masterTableName;
    }

    public void setMasterTableName(String masterTableName) {
        this.masterTableName = masterTableName;
    }

    public String getSlaveTableName() {
        return slaveTableName;
    }

    public void setSlaveTableName(String slaveTableName) {
        this.slaveTableName = slaveTableName;
    }


}
