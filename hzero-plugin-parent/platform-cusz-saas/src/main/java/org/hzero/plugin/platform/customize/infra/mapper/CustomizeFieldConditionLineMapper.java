package org.hzero.plugin.platform.customize.infra.mapper;

import java.util.Collection;
import java.util.List;

import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionLine;

/**
 * @author : peng.yu01@hand-china.com 2020/2/7 11:44
 */
public interface CustomizeFieldConditionLineMapper extends BaseMapper<FieldConditionLine> {

    int deleteByIds(@Param("ids") Collection<Long> ids);

    List<FieldConditionLine> selectByUnitIdAndFieldId(Long unitId, Long fieldId);
}
