package org.hzero.plugin.platform.customize.api.dto;

import java.util.List;

import org.hzero.plugin.platform.customize.domain.entity.Config;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.entity.Unit;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;

/**
 * 用户个性化配置对象
 * @author xiangyu.qi01@hand-china.com on 2019-12-24.
 */
public class UserConfigDTO {

    /**
     * 单元名称
     */
    private String unitName;

    /**
     * 单元配置
     */
    private Config config;

    /**
     * 字段配置
     */
    private List<ConfigField> fields;

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public List<ConfigField> getFields() {
        return fields;
    }

    public void setFields(List<ConfigField> fields) {
        this.fields = fields;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }
}
