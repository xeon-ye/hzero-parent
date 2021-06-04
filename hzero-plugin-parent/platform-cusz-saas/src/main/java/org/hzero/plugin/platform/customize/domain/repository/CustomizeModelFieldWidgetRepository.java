package org.hzero.plugin.platform.customize.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.domain.entity.ModelFieldWidget;

import java.util.Collection;
import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2019/12/21 15:32
 */
public interface CustomizeModelFieldWidgetRepository extends BaseRepository<ModelFieldWidget> {

    /**
     * 查询字段的组件配置
     *
     * @param fieldIds
     * @return
     */
    List<ModelFieldWidget> selectByFieldIds(Collection<Long> fieldIds);

}
