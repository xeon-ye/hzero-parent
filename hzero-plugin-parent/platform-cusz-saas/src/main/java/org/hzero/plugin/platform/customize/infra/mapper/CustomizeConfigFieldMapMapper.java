package org.hzero.plugin.platform.customize.infra.mapper;

import java.util.Collection;
import java.util.List;

import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldMap;

/**
 * @author : peng.yu01@hand-china.com 2019/12/25 15:31
 */
public interface CustomizeConfigFieldMapMapper extends BaseMapper<ConfigFieldMap> {


    /**
     * 根据configFieldId删除字段映射
     *
     * @param configFieldIds configFeldIds
     */
    void deleteByConfigFieldIds(@Param("list") Collection<Long> configFieldIds);

    List<ConfigFieldMap> selectByFieldIdAndUnitId(Long unitId, Long fieldId);

}
