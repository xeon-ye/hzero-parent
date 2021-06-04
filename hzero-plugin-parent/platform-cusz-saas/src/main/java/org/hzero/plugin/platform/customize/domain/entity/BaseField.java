package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.choerodon.mybatis.domain.AuditDomain;
import io.choerodon.mybatis.helper.LanguageHelper;
import io.choerodon.mybatis.util.StringUtil;
import org.apache.commons.collections4.MapUtils;
import org.hzero.mybatis.domian.Language;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO 整理所有重复字段
 *
 * @author xiangyu.qi01@hand-china.com on 2020-02-27.
 */
public class BaseField extends AuditDomain {

    public static final String FIELD_FIELD_NAME = "fieldName";
    public static final String FIELD_FIELD_CODE = "fieldCode";
    /**
     * 字段序号
     */
    private Integer gridSeq;

    /**
     * 表格列序号
     */
    private Integer formCol;

    /**
     * 表格行序号
     */
    private Integer formRow;

    private String fieldAlias;
    @Encrypt(value = CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL, ignoreValue = "-1")
    private Long modelId;
    @Encrypt(value = CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_FIELD, ignoreValue = "-1")
    private Long fieldId;

    private String fieldCode;


    public Integer getGridSeq() {
        return gridSeq;
    }

    public void setGridSeq(Integer gridSeq) {
        this.gridSeq = gridSeq;
    }

    public Integer getFormCol() {
        return formCol;
    }

    public void setFormCol(Integer formCol) {
        this.formCol = formCol;
    }

    public Integer getFormRow() {
        return formRow;
    }

    public void setFormRow(Integer formRow) {
        this.formRow = formRow;
    }

    public String getFieldAlias() {
        return StringUtils.isEmpty(fieldAlias) ? null : fieldAlias;
    }

    public void setFieldAlias(String fieldAlias) {
        this.fieldAlias = fieldAlias;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    @JsonIgnore
    public boolean isNotTableField() {
        return ObjectUtils.nullSafeEquals(-1L, modelId) || ObjectUtils.nullSafeEquals(-1L, fieldId);
    }

    public String fieldCode() {
        return org.apache.commons.lang3.StringUtils.isNotEmpty(getFieldAlias()) ? getFieldAlias() : getFieldCode();
    }

    /**
     * 业务处理的mapKey,对于同一字段，单元和租户一致
     *
     * @param
     * @return
     */
    public String bizHashKey() {
        if (isNotTableField()) {
            return getFieldCode();
        }
        return String.valueOf(getFieldId());
    }

    public void initFieldNameTlMaps(String fieldName) {
        List<Language> languageList = LanguageHelper.languages();
        Map<String, Map<String, String>> tlMaps = this.get_tls();
        if (MapUtils.isEmpty(tlMaps)) {
            tlMaps = new HashMap<>(1);
            Map<String, String> langMaps = new HashMap(languageList.size());
            tlMaps.put(BaseField.FIELD_FIELD_NAME, langMaps);
        }
        Map<String, String> langMaps = tlMaps.computeIfAbsent(BaseField.FIELD_FIELD_NAME, key -> new HashMap(languageList.size()));
        String currentLang = LanguageHelper.language();
        for (Language language : languageList) {
            boolean contains = langMaps.containsKey(language.getCode());
            if (language.getCode().equals(currentLang)) {
                langMaps.put(language.getCode(), fieldName);
            } else {
                if (!contains) {
                    langMaps.put(language.getCode(), " ");
                }
            }
        }
        set_tls(tlMaps);
    }

    public String getFieldCodeAlias() {
        if (StringUtils.isEmpty(this.getFieldAlias())) {
            return StringUtil.underlineToCamelhump(this.getFieldCode());
        }
        return StringUtil.underlineToCamelhump(this.getFieldAlias());
    }

}
