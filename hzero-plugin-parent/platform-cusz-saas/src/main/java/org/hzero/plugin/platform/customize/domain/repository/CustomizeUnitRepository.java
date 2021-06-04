package org.hzero.plugin.platform.customize.domain.repository;

import java.util.Collection;
import java.util.List;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.api.dto.MenuDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitConfigDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitRelatedDTO;
import org.hzero.plugin.platform.customize.domain.entity.Unit;

/**
 * 单元Repository
 *
 * @author : xiangyu.qi01@hand-china.com 2019/12/13
 */
public interface CustomizeUnitRepository extends BaseRepository<Unit> {

    /**
     * 查询用户拥有的角色菜单
     *
     * @return
     */
    List<MenuDTO> selectMenuByRole();

    /**
     * 查询所有菜单
     *
     * @return
     */
    List<MenuDTO> selectAllMenu();

    /**
     * 根据主键查询单元配置
     *
     * @param tenantId
     * @param unitId
     * @return
     */
    Unit selectUnitWithConfigById(Long tenantId, Long unitId);

    /**
     * 条件查询单元列表
     *
     * @param unit 单元查询条件
     * @return
     */
    List<Unit> selectByOption(Unit unit);

    /**
     * 返回单元信息，以及对应的模型名称，功能名称
     *
     * @param unitId 单元Id
     * @return
     */
    Unit selectById(Long unitId);

    /**
     * 根据单元编码查询单元配置头
     *
     * @param unitCode 单元编码
     * @return
     */
    UnitConfigDTO selectUnitConfigByCode(String unitCode, Long tenantId);

    /**
     * 缓存单元
     *
     * @param unit
     */
    void cacheUnit(Unit unit);

    /**
     * 删除单元
     * @param unitCode
     */
    void deleteUnitCache(String unitCode);

    /**
     * 根据单元单元编码获取单元
     *
     * @param unitCode
     */
    Unit getUnitCache(String unitCode);

    /**
     * 初始化单元缓存
     */
    void initUnitCache();

    /**
     * 查询指定单元关联的单元列表，包含单元字段配置
     *
     * @param unitIds 当前单元ID
     * @return 已过滤操作列字段
     */
    List<UnitRelatedDTO> selectRelatedUnit(Collection<String> unitIds);

    /**
     * 查询单元以主模型表名
     * @param unitId
     * @return
     */
    Unit selectUnitAndModelTable(Long unitId);
}
