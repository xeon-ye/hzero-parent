package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import org.apache.commons.collections4.CollectionUtils;
import org.hzero.core.helper.LanguageHelper;
import org.hzero.plugin.platform.customize.infra.common.ConfigLocalCache;
import org.hzero.plugin.platform.customize.infra.common.UnitLocalCache;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.BeanUtils;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

/**
 * @author peng.yu01@hand-china.com on 2020-02-24
 */
@Table(name = "hpfm_cusz_config_field_par")
/*@VersionAudit*/
@ModifyAudit
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ConfigFieldParam extends BaseFieldParam {

    public static final String FIELD_TENANT_ID = "tenantId";

    @Id
    @GeneratedValue
    @Column(name = "config_field_par_id")
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_CONFIG_FIELD_PAR)
    private Long paramId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_CONFIG_FIELD)
    private Long configFieldId;


    @Override
    public Long getParamId() {
        return paramId;
    }

    @Override
    public void setParamId(Long paramId) {
        this.paramId = paramId;
    }

    public Long getConfigFieldId() {
        return configFieldId;
    }

    public ConfigFieldParam setConfigFieldId(Long configFieldId) {
        this.configFieldId = configFieldId;
        return this;
    }

    public String mapKey() {
        return this.getParamUnitId() + ":" + getParamFieldId();
    }

    public ConfigFieldParam cacheConvert() {
        ConfigFieldParam param = new ConfigFieldParam();
        BeanUtils.copyProperties(this, param, "paramUnitName", "paramFieldName", FIELD_TENANT_ID, FIELD_CREATION_DATE, FIELD_CREATED_BY, FIELD_LAST_UPDATE_DATE,
                FIELD_LAST_UPDATED_BY, FIELD_OBJECT_VERSION_NUMBER, "_token");
        return param;
    }

    public static void translateParmaUnit(List<? extends BaseFieldParam> paramList, Long tenantId, UnitLocalCache unitLocalCache, ConfigLocalCache configLocalCache) {
        if (CollectionUtils.isEmpty(paramList)) {
            return;
        }
        paramList.forEach(param -> {
            if ("unit".equalsIgnoreCase(param.getParamType())) {
                if (configLocalCache.containsConfigField(tenantId, param.getParamUnitCode(), param.getParamFieldId())) {
                    ConfigField configField = configLocalCache.getConfigField(tenantId, param.getParamUnitCode(), param.getParamFieldId());
                    configField.convertTls(LanguageHelper.language(), false);
                    param.setParamFieldCode(configField.getFieldCodeAlias());
                    param.setParamFieldName(configField.getFieldName());
                } else if (unitLocalCache.containsField(param.getParamUnitCode(), param.getParamFieldId())) {
                    UnitField unitField = unitLocalCache.getUnitField(param.getParamUnitCode(), param.getParamFieldId());
                    param.setParamFieldName(unitField.getFieldName());
                    param.setParamFieldCode(unitField.getFieldCodeAlias());
                }
            }
        });
    }
}
