package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import io.swagger.annotations.ApiModelProperty;
import org.hzero.boot.customize.dto.ModelMetaData;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.BeanUtils;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 10:26
 */
@ModifyAudit
/*@VersionAudit*/
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Model extends AuditDomain {

    public static final String FIELD_MODEL_ID = "modelId";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_SERVICE_NAME = "serviceName";
    public static final String FIELD_MODEL_CODE = "modelCode";
    public static final String FIELD_MODEL_NAME = "modelName";
    public static final String FIELD_MODEL_TABLE = "modelTable";
    public static final String FIELD_SUPPORT_MULTI_LANG = "supportMultiLang";

    @ApiModelProperty("模型主键")
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL)
    private Long modelId;
    @ApiModelProperty("租户ID")
    private Long tenantId;
    @ApiModelProperty("模型所属服务")
    private String serviceName;
    @ApiModelProperty("模型编码")
    private String modelCode;
    @ApiModelProperty("模型名称")
    private String modelName;
    @ApiModelProperty("模型关联的表名")
    private String modelTable;
    @ApiModelProperty("模型关联的表是否为多语言")
    private Integer supportMultiLang;
    @ApiModelProperty("主键字段编码")
    private String primaryKey;

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelTable() {
        return modelTable;
    }

    public void setModelTable(String modelTable) {
        this.modelTable = modelTable;
    }

    public Integer getSupportMultiLang() {
        return supportMultiLang;
    }

    public void setSupportMultiLang(Integer supportMultiLang) {
        this.supportMultiLang = supportMultiLang;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public ModelMetaData conversionModel() {
        ModelMetaData metaData = new ModelMetaData();
        metaData.setModelCode(this.getModelCode());
        metaData.setModelTable(this.getModelTable());
        metaData.setPrimaryKey(this.getPrimaryKey());
        metaData.setServiceName(this.getServiceName());
        metaData.setModelName(this.getModelName());
        if (this.getSupportMultiLang() == null || this.getSupportMultiLang() == 0) {
            metaData.setSupportMultiLang(false);
        } else {
            metaData.setSupportMultiLang(true);
        }
        return metaData;
    }


    public static Model convertFromModelObject(ModelObjectPub modelObjectPub) {
        Model model = new Model();
        BeanUtils.copyProperties(modelObjectPub, model, FIELD_CREATED_BY, FIELD_CREATION_DATE, FIELD_CREATED_BY, FIELD_LAST_UPDATE_DATE,
                FIELD_LAST_UPDATED_BY, FIELD_OBJECT_VERSION_NUMBER);
        model.setModelCode(modelObjectPub.getCode());
        model.setModelId(modelObjectPub.getId());
        model.setModelName(modelObjectPub.getName());
        model.setModelTable(modelObjectPub.getRefTableName());
        model.setServiceName(modelObjectPub.getRefServiceName());
        model.setSupportMultiLang(modelObjectPub.getMultiLanguageFlag());
        return model;
    }

    public static ModelObjectPub convertFromModel(Model model) {
        ModelObjectPub pub = new ModelObjectPub();
        BeanUtils.copyProperties(model, pub, FIELD_CREATED_BY, FIELD_CREATION_DATE, FIELD_CREATED_BY, FIELD_LAST_UPDATE_DATE,
                FIELD_LAST_UPDATED_BY, FIELD_OBJECT_VERSION_NUMBER);
        pub.setId(model.getModelId());
        pub.setCode(model.getModelCode());
        pub.setName(model.getModelName());
        pub.setRefServiceName(model.getServiceName());
        pub.setRefTableName(model.getModelTable());
        pub.setPublishVersion(1L);
        pub.setAppId(-1L);
        pub.setTenantFlag(false);
        pub.setDataSourceType("TABLE");
        pub.setPrimaryKey(model.getPrimaryKey());
        pub.setMultiLanguageFlag(model.getSupportMultiLang());
        return pub;
    }
}
