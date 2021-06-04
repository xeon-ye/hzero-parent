package org.hzero.plugin.platform.customize.infra.mapper;

import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionHeader;

import java.util.Collection;
import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2020/2/7 10:05
 */
public interface CustomizeFieldConditionHeaderMapper extends BaseMapper<FieldConditionHeader> {

    /**
     * 查询指定字段的条件列表
     *
     * @param configFieldIds 字段ID
     * @return 返回结果包含行数据
     */
    List<FieldConditionHeader> selectByConfigFieldId(@Param("configFieldIds") Collection<Long> configFieldIds, @Param("conTypes") Collection<String> conTypes, @Param("needConValid") boolean needValid);

    /**
     * 查询所有条件
     *
     * @return 仅包含头id和行id
     */
    List<FieldConditionHeader> selectAllConditions(@Param("list") Collection<Long> configFieldIds);


}
