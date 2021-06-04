package org.hzero.plugin.platform.customize.infra.repository.impl;

import java.util.List;

import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.domain.entity.UnitGroup;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeUnitGroupRepository;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeUnitGroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author xiangyu.qi01@hand-china.com on 2020-01-07.
 */
@Component
public class CustomizeUnitGroupRepositoryImpl extends BaseRepositoryImpl<UnitGroup> implements CustomizeUnitGroupRepository {

    @Autowired
    private CustomizeUnitGroupMapper unitGroupMapper;

    @Override
    public List<UnitGroup> selectGroupAndUnits(String menuCode) {
        Assert.notNull(menuCode, "menuCode can not be null !");
        return unitGroupMapper.selectGroupAndUnits(menuCode);
    }
}
