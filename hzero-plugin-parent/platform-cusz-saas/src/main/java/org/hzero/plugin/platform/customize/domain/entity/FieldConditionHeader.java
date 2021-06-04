package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import org.apache.commons.collections4.CollectionUtils;
import org.hzero.plugin.platform.customize.api.dto.UnitConfigFieldConditionHeaderDTO;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : peng.yu01@hand-china.com 2020/2/7 9:45
 */

@Table(name = "hpfm_cusz_field_con_header")
/*@VersionAudit*/
@ModifyAudit
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FieldConditionHeader extends AuditDomain {

    public static final String CON_HEADER_ID = "conHeaderId";
    public static final String CONFIG_FIELD_ID = "configFieldId";
    public static final String TENANT_ID = "tenantId";

    @Id
    @GeneratedValue
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_FIELD_COND_HEADER)
    private Long conHeaderId;

    private Long tenantId;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_CONFIG_FIELD)
    private Long configFieldId;

    private String conType;

    private String conExpression;

    @Transient
    private List<FieldConditionLine> lines;

    @Transient
    private List<FieldConditionValid> valids;

    public Long getConHeaderId() {
        return conHeaderId;
    }

    public void setConHeaderId(Long conHeaderId) {
        this.conHeaderId = conHeaderId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public FieldConditionHeader setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public Long getConfigFieldId() {
        return configFieldId;
    }

    public FieldConditionHeader setConfigFieldId(Long configFieldId) {
        this.configFieldId = configFieldId;
        return this;
    }

    public String getConType() {
        return conType;
    }

    public void setConType(String conType) {
        this.conType = conType;
    }

    public String getConExpression() {
        return conExpression;
    }

    public void setConExpression(String conExpression) {
        this.conExpression = conExpression;
    }

    public List<FieldConditionLine> getLines() {
        return lines;
    }

    public void setLines(List<FieldConditionLine> lines) {
        this.lines = lines;
    }

    public List<FieldConditionValid> getValids() {
        return valids;
    }

    public void setValids(List<FieldConditionValid> valids) {
        this.valids = valids;
    }

    public FieldConditionHeader cacheConvert() {
        FieldConditionHeader header = new FieldConditionHeader();

        header.setConType(this.conType);
        header.setConExpression(this.conExpression);
        if (CollectionUtils.isNotEmpty(this.lines)) {
            header.setLines(this.lines.stream().map(FieldConditionLine::cacheConvert).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(this.valids)) {
            header.setValids(this.valids.stream().map(FieldConditionValid::cacheConvert).collect(Collectors.toList()));
        }

        return header;
    }

    public UnitConfigFieldConditionHeaderDTO cacheConvertToDTO() {
        UnitConfigFieldConditionHeaderDTO headerDTO = new UnitConfigFieldConditionHeaderDTO();

        headerDTO.setConExpression(this.getConExpression());
        headerDTO.setConType(this.getConType());
        headerDTO.setLines(new ArrayList<>(6));

        return headerDTO;
    }

}
