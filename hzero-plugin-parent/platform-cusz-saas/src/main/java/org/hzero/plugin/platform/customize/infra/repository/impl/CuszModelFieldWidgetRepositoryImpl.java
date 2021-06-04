package org.hzero.plugin.platform.customize.infra.repository.impl;

import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.domain.entity.ModelFieldWidget;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelFieldWidgetRepository;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeModelFieldWidgetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2019/12/15 13:45
 */
@Component
public class CuszModelFieldWidgetRepositoryImpl extends BaseRepositoryImpl<ModelFieldWidget> implements CustomizeModelFieldWidgetRepository {

    @Autowired
    private CustomizeModelFieldWidgetMapper fieldWidgetMapper;

    @Override
    public List<ModelFieldWidget> selectByFieldIds(Collection<Long> fieldIds) {
        return fieldWidgetMapper.selectByFieldIds(fieldIds);
    }
}
