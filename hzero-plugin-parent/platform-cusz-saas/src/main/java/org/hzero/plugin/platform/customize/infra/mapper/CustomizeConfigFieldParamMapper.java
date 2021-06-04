package org.hzero.plugin.platform.customize.infra.mapper;

import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldParam;

import java.util.Collection;
import java.util.List;

/**
 * @author peng.yu01@hand-china.com on 2020-02-25
 */
public interface CustomizeConfigFieldParamMapper extends BaseMapper<ConfigFieldParam> {

    /**
     * 查询字段参数
     *
     * @param configFieldIds
     * @return
     */
    List<ConfigFieldParam> selectByConfigFieldIds(@Param("configFieldIds") Collection<Long> configFieldIds);

}
