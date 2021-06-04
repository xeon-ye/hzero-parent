package org.hzero.plugin.platform.customize.infra.mapper;

import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.hzero.plugin.platform.customize.domain.entity.UnitFieldParam;

import java.util.Collection;
import java.util.List;

/**
 * @author peng.yu01@hand-china.com on 2020-02-25
 */
public interface CustomizeUnitFieldParamMapper extends BaseMapper<UnitFieldParam> {

    /**
     * 根据unitFieldId查询参数配置
     *
     * @param unitFieldIds
     * @return
     */
    List<UnitFieldParam> selectByUnitFieldIds(@Param("unitFieldIds") Collection<Long> unitFieldIds);
}
