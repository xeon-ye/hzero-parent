package org.hzero.plugin.platform.customize.infra.repository.impl;

import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.domain.entity.UnitFieldParam;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeUnitFieldParamRepository;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeUnitFieldParamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;

/**
 * @author peng.yu01@hand-china.com on 2020-02-25
 */
@Component
public class CustomizeUnitFieldParamRepositoryImpl extends BaseRepositoryImpl<UnitFieldParam> implements CustomizeUnitFieldParamRepository {
    @Autowired
    private CustomizeUnitFieldParamMapper unitFieldParamMapper;

    @Override
    public List<UnitFieldParam> selectByUnitFieldIds(Collection<Long> unitFieldIds) {
        Assert.notNull(unitFieldIds, "error.unitFieldId.null.");
        return unitFieldParamMapper.selectByUnitFieldIds(unitFieldIds);
    }
}
