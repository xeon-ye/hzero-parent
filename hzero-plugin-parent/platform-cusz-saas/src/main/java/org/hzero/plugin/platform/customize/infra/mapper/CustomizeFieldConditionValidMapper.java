package org.hzero.plugin.platform.customize.infra.mapper;

import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.hzero.plugin.platform.customize.api.dto.FieldConValidTlDTO;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionValid;

import java.util.Collection;
import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2020/2/7 10:05
 */
public interface CustomizeFieldConditionValidMapper extends BaseMapper<FieldConditionValid> {

    List<FieldConValidTlDTO> selectValidTl(@Param("validIds") Collection<Long> validId);
}
