package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.annotation.MultiLanguage;
import io.choerodon.mybatis.annotation.MultiLanguageField;
import io.choerodon.mybatis.domain.AuditDomain;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : peng.yu01@hand-china.com 2020/2/7 9:49
 */

@Table(name = "hpfm_cusz_field_con_valid")
/*@VersionAudit*/
@ModifyAudit
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@MultiLanguage
public class FieldConditionValid extends AuditDomain {

    public static final String CON_VALID_ID = "conValidId";
    public static final String TENANT_ID = "tenantId";
    public static final String CON_HEADER_ID = "conHeaderId";
    public static final String ERROR_MESSAGE = "errorMessage";

    @Id
    @GeneratedValue
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_FIELD_COND_VALID)
    private Long conValidId;

    private Long tenantId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_FIELD_COND_HEADER)
    private Long conHeaderId;

    private String conExpression;

    @MultiLanguageField
    private String errorMessage;

    private Integer conCode;

    @Transient
    private List<Map<String, String>> tlMaps;

    public Long getConValidId() {
        return conValidId;
    }

    public void setConValidId(Long conValidId) {
        this.conValidId = conValidId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getConHeaderId() {
        return conHeaderId;
    }

    public FieldConditionValid setConHeaderId(Long conHeaderId) {
        this.conHeaderId = conHeaderId;
        return this;
    }

    public String getConExpression() {
        return conExpression;
    }

    public void setConExpression(String conExpression) {
        this.conExpression = conExpression;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getConCode() {
        return conCode;
    }

    public void setConCode(Integer conCode) {
        this.conCode = conCode;
    }

    public List<Map<String, String>> getTlMaps() {
        return tlMaps;
    }

    public void setTlMaps(List<Map<String, String>> tlMaps) {
        this.tlMaps = tlMaps;
    }

    public FieldConditionValid cacheConvert() {
        FieldConditionValid valid = new FieldConditionValid();
        BeanUtils.copyProperties(this, valid, CON_VALID_ID, TENANT_ID, CON_HEADER_ID, FIELD_CREATION_DATE, FIELD_CREATED_BY, FIELD_LAST_UPDATE_DATE,
                FIELD_LAST_UPDATED_BY, FIELD_OBJECT_VERSION_NUMBER, "_token", "tlMaps");
        if (CollectionUtils.isNotEmpty(this.tlMaps)) {
            Map<String, Map<String, String>> _tls = new HashMap(1);
            Map<String, String> tlsInnerMap = new HashMap(2);
            for (Map<String, String> item : tlMaps) {
                tlsInnerMap.put(item.get("lang"), item.get(ERROR_MESSAGE));
            }
            _tls.put(ERROR_MESSAGE, tlsInnerMap);
            valid.set_tls(_tls);
        }
        return valid;
    }

    public FieldConditionValid convertTls(String lang, boolean isStd) {
        if (isStd) {
            this.setErrorMessage(null);
        }
        if (MapUtils.isNotEmpty(this.get_tls()) && this.get_tls().containsKey(ERROR_MESSAGE)) {
            Map<String, String> fieldNames = this.get_tls().get(ERROR_MESSAGE);
            if (fieldNames.containsKey(lang) && StringUtils.isNotBlank(fieldNames.get(lang))) {
                this.setErrorMessage(fieldNames.get(lang));
            }
        }
        return this;
    }
}
