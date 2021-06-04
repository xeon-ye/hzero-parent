package org.hzero.plugin.platform.customize.infra.repository.impl;

import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionHeader;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeFieldConditionHeaderRepository;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeFieldConditionHeaderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2020/2/11 15:19
 */
@Component
public class CustomizeFieldConditionHeaderRepositoryImpl extends BaseRepositoryImpl<FieldConditionHeader> implements CustomizeFieldConditionHeaderRepository {

    @Autowired
    private CustomizeFieldConditionHeaderMapper headerMapper;

    @Override
    public List<FieldConditionHeader> selectWithLineByFieldId(Collection<Long> configFieldIds, Collection<String> conTypes, boolean needValid) {
        return headerMapper.selectByConfigFieldId(configFieldIds, conTypes, needValid);
    }

    @Override
    public List<FieldConditionHeader> selectAllConditions(Collection<Long> configFieldIds) {
        Assert.notNull(configFieldIds, "error.configFieldIds.null");
        return headerMapper.selectAllConditions(configFieldIds);
    }

}
