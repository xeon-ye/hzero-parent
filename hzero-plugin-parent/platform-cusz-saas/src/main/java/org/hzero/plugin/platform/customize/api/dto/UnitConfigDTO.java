package org.hzero.plugin.platform.customize.api.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;
import org.hzero.plugin.platform.customize.domain.entity.Config;
import org.hzero.plugin.platform.customize.domain.entity.Unit;

/**
 * 个性化配置DTO
 *
 * @author xiangyu.qi01@hand-china.com on 2019-12-16.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnitConfigDTO {

    @JsonIgnore
    private Long unitId;

    /**
     * 个性化单元编码
     */
    @JsonIgnore
    private String unitCode;

    /**
     * 个性化单元类型
     */
    private String unitType;

    /**
     * 表单列最大值
     */
    private Long maxCol;

    private Integer pageSize;

    private Boolean readOnly;

    /**
     * label列数
     */
    private Integer labelCol;

    /**
     * wrapper列数
     */
    private Integer wrapperCol;



    /**
     * 个性化单元列配置
     */
    private List<UnitConfigFieldDTO> fields;

    private List<Map<String,String>> unitAlias;

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public List<UnitConfigFieldDTO> getFields() {
        return fields;
    }

    public void setFields(List<UnitConfigFieldDTO> fields) {
        this.fields = fields;
    }

    public Long getMaxCol() {
        return maxCol;
    }

    public void setMaxCol(Long maxCol) {
        this.maxCol = maxCol;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Integer getLabelCol() {
        return labelCol;
    }

    public void setLabelCol(Integer labelCol) {
        this.labelCol = labelCol;
    }

    public Integer getWrapperCol() {
        return wrapperCol;
    }

    public void setWrapperCol(Integer wrapperCol) {
        this.wrapperCol = wrapperCol;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<Map<String, String>> getUnitAlias() {
        return unitAlias;
    }

    public void setUnitAlias(List<Map<String, String>> unitAlias) {
        this.unitAlias = unitAlias;
    }

    public void readFormConfig(Config config, Unit unit) {
        if (config != null) {
            this.maxCol = config.getMaxCol();
            this.readOnly = Objects.equal(config.getReadOnly(), 1);
            this.pageSize = config.getPageSize();
        }
        this.unitCode = unit.getUnitCode();
        this.unitType = unit.getUnitType();
        this.labelCol = unit.getLabelCol();
        this.wrapperCol = unit.getWrapperCol();
    }
}
