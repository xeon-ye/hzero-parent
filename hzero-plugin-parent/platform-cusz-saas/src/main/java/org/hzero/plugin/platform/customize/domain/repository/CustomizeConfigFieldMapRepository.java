package org.hzero.plugin.platform.customize.domain.repository;

import java.util.Collection;
import java.util.List;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldMap;

/**
 * @author : peng.yu01@hand-china.com 2019/12/25 15:28
 */
public interface CustomizeConfigFieldMapRepository extends BaseRepository<ConfigFieldMap> {


    void deleteByConfigFieldIds(Collection<Long> configFieldIds);

    /**
     * 根据单元编码和字段编码查询被引用的字段
     * @param unitId
     * @return
     */
    List<ConfigFieldMap> selectByFieldIdAndUnitId(Long unitId, Long fieldId);
}
