package org.hzero.plugin.platform.customize.infra.repository.impl;

import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.api.dto.FieldConValidTlDTO;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionValid;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeFieldConditionValidRepository;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeFieldConditionValidMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2020/2/11 15:19
 */
@Component
public class CustomizeFieldConditionValidRepositoryImpl extends BaseRepositoryImpl<FieldConditionValid> implements CustomizeFieldConditionValidRepository {

    @Autowired
    private CustomizeFieldConditionValidMapper validMapper;

    @Override
    public List<FieldConValidTlDTO> selectValidTl(Collection<Long> validId) {
        return validMapper.selectValidTl(validId);
    }
}
