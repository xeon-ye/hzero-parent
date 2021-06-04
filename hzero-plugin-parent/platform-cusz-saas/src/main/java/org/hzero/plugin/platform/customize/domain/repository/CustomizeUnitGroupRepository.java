package org.hzero.plugin.platform.customize.domain.repository;

import java.util.List;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.domain.entity.UnitGroup;

/**
 * @author xiangyu.qi01@hand-china.com on 2020-01-07.
 */
public interface CustomizeUnitGroupRepository extends BaseRepository<UnitGroup> {

    /**
     * 查询组以及对应单元
     *
     * @param menuCode
     * @return
     */
    List<UnitGroup> selectGroupAndUnits(String menuCode);

}
