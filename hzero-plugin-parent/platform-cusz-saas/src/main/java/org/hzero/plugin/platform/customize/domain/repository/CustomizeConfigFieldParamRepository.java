package org.hzero.plugin.platform.customize.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldParam;

import java.util.Collection;
import java.util.List;

/**
 * @author peng.yu01@hand-china.com on 2020-02-25
 */
public interface CustomizeConfigFieldParamRepository extends BaseRepository<ConfigFieldParam> {

    /**
     * 查询字段参数
     *
     * @param configFieldIds
     * @return
     */
    List<ConfigFieldParam> selectByConfigFieldIds(Collection<Long> configFieldIds);

}
