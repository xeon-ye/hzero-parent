package org.hzero.plugin.platform.customize.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldMap;

/**
 * @author xiangyu.qi01@hand-china.com on 2019-12-27.
 */
public class UnitConfigFieldMappingDTO {

    /**
     * 值集字段编码
     */
    private String sourceCode;

    /**
     * 映射目标字段编码
     */
    private String targetCode;

    /**
     * 用于从缓存中获取字段编码
     */
    @JsonIgnore
    private Long targetFieldId;

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
    }

    public Long getTargetFieldId() {
        return targetFieldId;
    }

    public void setTargetFieldId(Long targetFieldId) {
        this.targetFieldId = targetFieldId;
    }

    public static UnitConfigFieldMappingDTO readFormConfig(ConfigFieldMap configFieldMap) {
        UnitConfigFieldMappingDTO dto = new UnitConfigFieldMappingDTO();
        dto.setSourceCode(configFieldMap.getSourceFieldAlias());
        dto.setTargetFieldId(configFieldMap.getTargetFieldId());
        return dto;
    }


}
