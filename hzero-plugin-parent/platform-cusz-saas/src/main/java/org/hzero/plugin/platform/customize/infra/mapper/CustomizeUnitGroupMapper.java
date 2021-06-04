package org.hzero.plugin.platform.customize.infra.mapper;

import java.util.List;

import io.choerodon.mybatis.common.BaseMapper;
import org.hzero.plugin.platform.customize.domain.entity.UnitGroup;

/**
 * 单元mapper
 *
 * @author : xiangyu.qi01@hand-china.com 2019/12/13
 */
public interface CustomizeUnitGroupMapper extends BaseMapper<UnitGroup> {

    /**
     * 查询组以及对应单元
     *
     * @param menuCode
     * @return
     */
    List<UnitGroup> selectGroupAndUnits(String menuCode);
}
