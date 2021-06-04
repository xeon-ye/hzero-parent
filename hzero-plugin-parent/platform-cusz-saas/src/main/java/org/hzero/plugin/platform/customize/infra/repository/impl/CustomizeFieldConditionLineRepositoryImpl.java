package org.hzero.plugin.platform.customize.infra.repository.impl;

import java.util.Collection;
import java.util.List;

import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionLine;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeFieldConditionLineRepository;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeFieldConditionLineMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author : peng.yu01@hand-china.com 2020/2/11 15:22
 */
@Component
public class CustomizeFieldConditionLineRepositoryImpl extends BaseRepositoryImpl<FieldConditionLine> implements CustomizeFieldConditionLineRepository {

    @Autowired
    private CustomizeFieldConditionLineMapper lineMapper;

    @Override
    public int deleteByIds(Collection<Long> ids) {
        return lineMapper.deleteByIds(ids);
    }

    @Override
    public List<FieldConditionLine> selectByUnitIdAndFieldId(Long unitId, Long fieldId) {
        return lineMapper.selectByUnitIdAndFieldId(unitId, fieldId);
    }
}
