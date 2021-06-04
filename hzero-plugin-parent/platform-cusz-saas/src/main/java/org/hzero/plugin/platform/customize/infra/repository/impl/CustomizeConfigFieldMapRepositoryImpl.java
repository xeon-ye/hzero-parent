package org.hzero.plugin.platform.customize.infra.repository.impl;

import java.util.Collection;
import java.util.List;

import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldMap;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeConfigFieldMapRepository;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeConfigFieldMapMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : peng.yu01@hand-china.com 2019/12/25 15:29
 */
@Component
public class CustomizeConfigFieldMapRepositoryImpl extends BaseRepositoryImpl<ConfigFieldMap> implements CustomizeConfigFieldMapRepository {

    @Autowired
    private CustomizeConfigFieldMapMapper mapMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByConfigFieldIds(Collection<Long> configFieldIds) {
        mapMapper.deleteByConfigFieldIds(configFieldIds);
    }

    @Override
    public List<ConfigFieldMap> selectByFieldIdAndUnitId(Long unitId, Long fieldId) {

        return mapMapper.selectByFieldIdAndUnitId(unitId,fieldId);
    }

}
