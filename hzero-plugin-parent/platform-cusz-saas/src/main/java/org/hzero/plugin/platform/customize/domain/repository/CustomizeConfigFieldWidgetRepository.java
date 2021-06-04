package org.hzero.plugin.platform.customize.domain.repository;

import java.util.Collection;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldWidget;

/**
 * @author : peng.yu01@hand-china.com 2019/12/19 19:50
 */
public interface CustomizeConfigFieldWidgetRepository extends BaseRepository<ConfigFieldWidget> {
    void deleteByConfigFieldIds(Collection<Long> configFieldIds);

}
