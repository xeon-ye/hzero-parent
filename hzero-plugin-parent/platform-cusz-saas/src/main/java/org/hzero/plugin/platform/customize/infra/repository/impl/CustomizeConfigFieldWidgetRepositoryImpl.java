package org.hzero.plugin.platform.customize.infra.repository.impl;

import java.util.Collection;

import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldWidget;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeConfigFieldWidgetRepository;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeConfigFieldWidgetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : peng.yu01@hand-china.com 2019/12/19 19:51
 */
@Component
public class CustomizeConfigFieldWidgetRepositoryImpl extends BaseRepositoryImpl<ConfigFieldWidget> implements CustomizeConfigFieldWidgetRepository {

    @Autowired
    private CustomizeConfigFieldWidgetMapper customizeConfigFieldWidgetMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByConfigFieldIds(Collection<Long> configFieldIds) {
        customizeConfigFieldWidgetMapper.deleteByConfigFieldIds(configFieldIds);
    }
}
