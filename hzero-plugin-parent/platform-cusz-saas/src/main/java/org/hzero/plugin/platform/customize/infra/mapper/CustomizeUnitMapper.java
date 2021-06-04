package org.hzero.plugin.platform.customize.infra.mapper;

import java.util.Collection;
import java.util.List;

import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.hzero.plugin.platform.customize.api.dto.MenuDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitConfigDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitRelatedDTO;
import org.hzero.plugin.platform.customize.domain.entity.Unit;

/**
 * 单元mapper
 *
 * @author : xiangyu.qi01@hand-china.com 2019/12/13
 */
public interface CustomizeUnitMapper extends BaseMapper<Unit> {

    /**
     * 查询菜单列表
     *
     * @param roleIds 角色ID列表
     * @return
     */
    List<MenuDTO> selectMenuByRoleId(@Param("roleIds") List<Long> roleIds);

    /**
     * 查询所有的菜单列表
     *
     * @return
     */
    List<MenuDTO> selectAllMenus();

    /**
     * 查询指定单元信息，包含个性化配置信息
     *
     * @param tenantId
     * @param unitId
     * @return
     */
    Unit selectUnitWithConfigById(Long tenantId, Long unitId);

    /**
     * 根据条件查询单元，包含分组信息、关联的模型信息、所属菜单
     *
     * @param unit
     * @return
     */
    List<Unit> selectByOptions(Unit unit);

    /**
     * 查询指定单元，包含分组信息、关联的模型信息、所属菜单
     *
     * @param unitId
     * @return
     */
    Unit selectById(Long unitId);

    /**
     * 根据单元编码查询单元，包含个性化配置信息
     *
     * @param unitCode
     * @param tenantId
     * @return
     */
    UnitConfigDTO selectUnitConfigByCode(String unitCode, Long tenantId);


    /**
     * 查询指定单元关联的单元列表，包含单元字段配置
     *
     * @param unitCodes 当前单元ID
     * @return
     */
    List<UnitRelatedDTO> selectRelatedUnit(@Param("list") Collection<String> unitCodes);


    /**
     * 查询指定单元，包含分组信息、关联的模型信息、所属菜单
     *
     * @param unitId
     * @return
     */
    Unit selectUnitAndModelTable(Long unitId);
}
