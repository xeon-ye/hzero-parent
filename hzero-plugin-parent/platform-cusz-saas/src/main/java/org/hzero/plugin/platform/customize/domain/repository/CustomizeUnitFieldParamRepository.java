package org.hzero.plugin.platform.customize.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.domain.entity.UnitFieldParam;

import java.util.Collection;
import java.util.List;

/**
 * @author peng.yu01@hand-china.com on 2020-02-25
 */
public interface CustomizeUnitFieldParamRepository extends BaseRepository<UnitFieldParam> {

    /**
     * 根据unitFieldId查询关联参数配置
     *
     * @param unitFieldIds
     * @return
     */
    List<UnitFieldParam> selectByUnitFieldIds(Collection<Long> unitFieldIds);

}
