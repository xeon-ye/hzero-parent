package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.BeanUtils;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author peng.yu01@hand-china.com on 2020-02-24
 */
@Table(name = "hpfm_cusz_unit_field_par")
/*@VersionAudit*/
@ModifyAudit
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UnitFieldParam extends BaseFieldParam {

    @Id
    @GeneratedValue
    @Column(name = "unit_field_par_id")
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT_FIELD_PAR)
    private Long paramId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT_FIELD)
    private Long unitFieldId;

    @Override
    public Long getParamId() {
        return paramId;
    }

    @Override
    public void setParamId(Long paramId) {
        this.paramId = paramId;
    }

    public Long getUnitFieldId() {
        return unitFieldId;
    }

    public UnitFieldParam setUnitFieldId(Long unitFieldId) {
        this.unitFieldId = unitFieldId;
        return this;
    }

    public ConfigFieldParam convertToConfigFieldParam() {
        ConfigFieldParam param = new ConfigFieldParam();
        BeanUtils.copyProperties(this, param, FIELD_TENANT_ID, FIELD_CREATION_DATE, FIELD_CREATED_BY, FIELD_LAST_UPDATE_DATE,
                FIELD_LAST_UPDATED_BY, FIELD_OBJECT_VERSION_NUMBER, "_token");
        return param;
    }

    public UnitFieldParam cacheConvert() {
        UnitFieldParam param = new UnitFieldParam();
        BeanUtils.copyProperties(this, param, "paramUnitName", "paramFieldName", FIELD_TENANT_ID, FIELD_CREATION_DATE, FIELD_CREATED_BY, FIELD_LAST_UPDATE_DATE,
                FIELD_LAST_UPDATED_BY, FIELD_OBJECT_VERSION_NUMBER, "_token");
        return param;
    }
}
