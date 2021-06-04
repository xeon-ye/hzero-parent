package org.hzero.plugin.platform.customize.app.service;

import java.util.List;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.api.dto.MenuDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitDTO;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;
import org.hzero.plugin.platform.customize.domain.entity.Unit;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;
import org.hzero.plugin.platform.customize.domain.entity.UnitGroup;

/**
 * 个性化单元服务
 *
 * @author : xiangyu.qi01@hand-china.com 2019/12/13
 */
public interface CustomizeUnitService {

    /**
     * 个性化单元获取菜单树，返回配置了个性化单元的菜单
     *
     * @return
     */
    List<MenuDTO> selectMenuTree();

    /**
     * 返回所有菜单
     *
     * @return
     */
    List<MenuDTO> selectAllMenuForTree();

    /**
     * 分页查询所有的单元
     *
     * @param pageRequest 分页参数
     * @return 单元列表
     */
    Page<Unit> selectUnitByOption(Unit unit, PageRequest pageRequest);

    /**
     * 创建单元
     *
     * @param unit
     * @return
     */
    Unit createUnit(Unit unit);

    /**
     * 根据ID获取单元详情，包括字段信息
     *
     * @param unitId 单元ID
     * @return
     */
    UnitDTO selectUnitById(Long unitId);

    /**
     * 保存单元
     *
     * @param unit 单元
     * @return
     */
    Unit saveUnit(Unit unit);

    /**
     * 保存单元字段
     *
     * @param unitField 单元字段
     * @return
     */
    UnitField saveUnitField(UnitField unitField);

    void deleteUnitField(Long unitFieldId);

    /**
     * 分页-查询还未配置到指定单元的模型字段，只能查出 标准字段 和 虚拟字段
     *
     * @param unitId
     * @param searchDTO
     * @param pageRequest
     * @return
     */
    Page<ModelField> selectNotConfigField(Long unitId, FieldCommonSearchDTO searchDTO, PageRequest pageRequest);

    /**
     * 查询指定单元的配置 只包含unit和config
     *
     * @param tenantId
     * @param unitId
     * @return
     */
    Unit selectUnitWithConfig(Long tenantId, Long unitId);

    /**
     * 根据组查询单元集合
     *
     * @param groupId
     * @return
     */
    List<Unit> selectByGroupId(Long groupId);

    /**
     * 创建组
     *
     * @param group
     * @return
     */
    UnitGroup createGroup(UnitGroup group);

    /**
     * 更新组
     *
     * @param group
     * @return
     */
    UnitGroup updateGroup(UnitGroup group);

    /**
     * 删除组
     *
     * @param groupId
     * @return
     */
    int deleteGroup(Long groupId);

    /**
     * 查询组
     *
     * @param group
     * @return
     */
    List<UnitGroup> selectByOptions(UnitGroup group);

    /**
     * 查询组以及对应单元
     *
     * @param menuCode
     * @return
     */
    List<UnitGroup> selectGroupAndUnits(String menuCode);


    /**
     * 复制单元
     * @param unit 单元信息
     * @param copyUnitCode 复制单元编码
     */
    void copyUnit(Unit unit, String copyUnitCode);

    /**
     * 删除单元
     * @param unitCode
     * @return
     */
    int deleteUnit(String unitCode);
}
