package org.hzero.plugin.platform.customize.infra.mapper;

import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.hzero.plugin.platform.customize.domain.entity.ModelFieldWidget;

import java.util.Collection;
import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2019/12/15 13:46
 */
public interface CustomizeModelFieldWidgetMapper extends BaseMapper<ModelFieldWidget> {

    /**
     * 查询字段的组件配置
     *
     * @param fieldIds
     * @return
     */
    List<ModelFieldWidget> selectByFieldIds(@Param("fieldIds") Collection<Long> fieldIds);
}
