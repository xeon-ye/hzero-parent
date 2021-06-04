package org.hzero.plugin.platform.customize.infra.repository.impl;

import java.util.Collection;
import java.util.List;

import io.choerodon.core.oauth.DetailsHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.hzero.core.helper.LanguageHelper;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldParam;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeConfigFieldParamRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeConfigFieldRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeUnitFieldRepository;
import org.hzero.plugin.platform.customize.infra.common.ConfigLocalCache;
import org.hzero.plugin.platform.customize.infra.common.UnitLocalCache;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeConfigFieldParamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author peng.yu01@hand-china.com on 2020-02-25
 */
@Component
public class CustomizeConfigFieldParamRepositoryImpl extends BaseRepositoryImpl<ConfigFieldParam> implements CustomizeConfigFieldParamRepository {
    @Autowired
    private CustomizeConfigFieldParamMapper configFieldParamMapper;

    @Override
    public List<ConfigFieldParam> selectByConfigFieldIds(Collection<Long> configFieldIds) {
        Assert.notNull(configFieldIds, "error.configFieldParam.configFieldId.null.");
        List<ConfigFieldParam> configFieldParams = configFieldParamMapper.selectByConfigFieldIds(configFieldIds);
        return configFieldParams;
    }
}
