package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import org.hzero.plugin.platform.customize.api.dto.UnitConfigFieldConditionLineDTO;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.BeanUtils;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2020/2/7 9:49
 */

@Table(name = "hpfm_cusz_field_con_line")
/*@VersionAudit*/
@ModifyAudit
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FieldConditionLine extends AuditDomain {

    public static final String CON_LINE_ID = "conLineId";
    public static final String CON_HEADER_ID = "conHeaderId";
    public static final String TENANT_ID = "tenantId";
    public static final String SOURCE_UNIT_ID = "sourceUnitId";
    public static final String SOURCE_FIELD_ID = "sourceFieldId";
    public static final String SOURCE_FIELD_NAME = "sourceFieldName";
    public static final String SOURCE_UNIT_NAME = "sourceUnitName";
    public static final String TARGET_FIELD_ID = "targetFieldId";
    public static final String TARGET_FIELD_NAME = "targetFieldName";

    @Id
    @GeneratedValue
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_FIELD_COND_LINE)
    private Long conLineId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_FIELD_COND_HEADER)
    private Long conHeaderId;

    private Long tenantId;

    private Integer conCode;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT)
    private Long sourceUnitId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL)
    private Long sourceModelId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_FIELD)
    private Long sourceFieldId;

    private String conExpression;

    private String targetType;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT)
    private Long targetUnitId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL)
    private Long targetModelId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_FIELD)
    private Long targetFieldId;

    private String targetValue;

    private String targetValueMeaning;

    @Transient
    private String sourceFieldWidget;

    @Transient
    private String sourceFieldValueCode;

    @Transient
    private String sourceUnitCode;

    @Transient
    private String sourceUnitName;

    @Transient
    private String sourceFieldCode;
    @Transient
    private String sourceFieldName;

    @Transient
    private String targetFieldCode;
    @Transient
    private String targetFieldName;
    @Transient
    private String targetUnitCode;

    @Transient
    private List<UnitFieldParam> paramList;

    public Long getConLineId() {
        return conLineId;
    }

    public void setConLineId(Long conLineId) {
        this.conLineId = conLineId;
    }

    public Long getConHeaderId() {
        return conHeaderId;
    }

    public FieldConditionLine setConHeaderId(Long conHeaderId) {
        this.conHeaderId = conHeaderId;
        return this;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getConCode() {
        return conCode;
    }

    public void setConCode(Integer conCode) {
        this.conCode = conCode;
    }

    public Long getSourceUnitId() {
        return sourceUnitId;
    }

    public void setSourceUnitId(Long sourceUnitId) {
        this.sourceUnitId = sourceUnitId;
    }

    public Long getSourceFieldId() {
        return sourceFieldId;
    }

    public void setSourceFieldId(Long sourceFieldId) {
        this.sourceFieldId = sourceFieldId;
    }

    public String getConExpression() {
        return conExpression;
    }

    public void setConExpression(String conExpression) {
        this.conExpression = conExpression;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetFieldId() {
        return targetFieldId;
    }

    public void setTargetFieldId(Long targetFieldId) {
        this.targetFieldId = targetFieldId;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    public String getSourceFieldCode() {
        return sourceFieldCode;
    }

    public void setSourceFieldCode(String sourceFieldCode) {
        this.sourceFieldCode = sourceFieldCode;
    }

    public String getTargetFieldCode() {
        return targetFieldCode;
    }

    public void setTargetFieldCode(String targetFieldCode) {
        this.targetFieldCode = targetFieldCode;
    }

    public String getSourceUnitCode() {
        return sourceUnitCode;
    }

    public void setSourceUnitCode(String sourceUnitCode) {
        this.sourceUnitCode = sourceUnitCode;
    }

    public String getSourceUnitName() {
        return sourceUnitName;
    }

    public void setSourceUnitName(String sourceUnitName) {
        this.sourceUnitName = sourceUnitName;
    }

    public String getSourceFieldName() {
        return sourceFieldName;
    }

    public void setSourceFieldName(String sourceFieldName) {
        this.sourceFieldName = sourceFieldName;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldName(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public Long getTargetUnitId() {
        return targetUnitId;
    }

    public void setTargetUnitId(Long targetUnitId) {
        this.targetUnitId = targetUnitId;
    }

    public Long getSourceModelId() {
        return sourceModelId;
    }

    public void setSourceModelId(Long sourceModelId) {
        this.sourceModelId = sourceModelId;
    }

    public Long getTargetModelId() {
        return targetModelId;
    }

    public void setTargetModelId(Long targetModelId) {
        this.targetModelId = targetModelId;
    }

    public String getTargetValueMeaning() {
        return targetValueMeaning;
    }

    public void setTargetValueMeaning(String targetValueMeaning) {
        this.targetValueMeaning = targetValueMeaning;
    }

    public String getSourceFieldWidget() {
        return sourceFieldWidget;
    }

    public void setSourceFieldWidget(String sourceFieldWidget) {
        this.sourceFieldWidget = sourceFieldWidget;
    }

    public String getSourceFieldValueCode() {
        return sourceFieldValueCode;
    }

    public void setSourceFieldValueCode(String sourceFieldValueCode) {
        this.sourceFieldValueCode = sourceFieldValueCode;
    }

    public String getTargetUnitCode() {
        return targetUnitCode;
    }

    public void setTargetUnitCode(String targetUnitCode) {
        this.targetUnitCode = targetUnitCode;
    }

    public List<UnitFieldParam> getParamList() {
        return paramList;
    }

    public void setParamList(List<UnitFieldParam> paramList) {
        this.paramList = paramList;
    }

    public FieldConditionLine cacheConvert() {
        FieldConditionLine line = new FieldConditionLine();
        BeanUtils.copyProperties(this, line, CON_LINE_ID, FIELD_TABLE_ID, FIELD_CREATION_DATE, FIELD_CREATED_BY, FIELD_LAST_UPDATE_DATE,
                FIELD_LAST_UPDATED_BY, FIELD_OBJECT_VERSION_NUMBER, "_token");
        return line;
    }

    public UnitConfigFieldConditionLineDTO cacheConvertToDTO() {
        UnitConfigFieldConditionLineDTO lineDTO = new UnitConfigFieldConditionLineDTO();
        BeanUtils.copyProperties(this, lineDTO);
        return lineDTO;
    }

}
