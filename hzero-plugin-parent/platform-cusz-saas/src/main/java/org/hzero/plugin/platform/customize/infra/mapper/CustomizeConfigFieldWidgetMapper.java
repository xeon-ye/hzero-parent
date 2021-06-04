package org.hzero.plugin.platform.customize.infra.mapper;

import java.util.Collection;

import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldWidget;

/**
 * @author : peng.yu01@hand-china.com 2019/12/19 19:51
 */
public interface CustomizeConfigFieldWidgetMapper extends BaseMapper<ConfigFieldWidget> {

    /**
     * 删除指定ConfigField关联的ConfigFieldWidget
     *
     * @param configFieldIds
     */
    void deleteByConfigFieldIds(@Param("list") Collection<Long> configFieldIds);
}
