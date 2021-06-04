package org.hzero.plugin.platform.customize.app.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.util.StringUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.hzero.boot.platform.lov.dto.LovValueDTO;
import org.hzero.core.base.BaseConstants;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.api.dto.MenuDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitDTO;
import org.hzero.plugin.platform.customize.app.service.CustomizeConfigService;
import org.hzero.plugin.platform.customize.app.service.CustomizeUnitService;
import org.hzero.plugin.platform.customize.domain.entity.*;
import org.hzero.plugin.platform.customize.domain.repository.*;
import org.hzero.plugin.platform.customize.infra.common.ModelLocalCache;
import org.hzero.plugin.platform.customize.infra.common.utils.MenuUtils;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author : peng.yu01@hand-china.com 2019/12/17 19:33
 */
@Service
public class CustomizeUnitServiceImpl implements CustomizeUnitService {

    @Autowired
    private CustomizeUnitRepository customizeUnitRepository;
    @Autowired
    private CustomizeUnitFieldRepository customizeUnitFieldRepository;
    @Autowired
    private CustomizeConfigService customizeConfigService;
    @Autowired
    private CustomizeModelRepository modelRepository;
    @Autowired
    private CustomizeModelFieldRepository modelFieldRepository;
    @Autowired
    private CustomizeUnitGroupRepository unitGroupRepository;
    @Autowired
    private CustomizeUnitFieldParamRepository unitFieldParamRepository;
    @Autowired
    private CustomizeConfigFieldRepository configFieldRepository;
    @Autowired
    private CustomizeConfigRepository configRepository;
    @Autowired
    private LovAdapter lovAdapter;

    @Override
    public List<MenuDTO> selectMenuTree() {
        List<MenuDTO> menuDTOS = customizeUnitRepository.selectMenuByRole();
        if (CollectionUtils.isEmpty(menuDTOS)) {
            return null;
        }
        List<MenuDTO> filterMenuDTOList = new ArrayList<>(6);
        List<String> unitMenuCode = unitGroupRepository.selectAll().stream().map(UnitGroup::getMenuCode).collect(Collectors.toList());
        //移除还未配置个性化单元的菜单
        for (MenuDTO menuDTO : menuDTOS) {
            if ("menu".equalsIgnoreCase(menuDTO.getMenuType()) && !unitMenuCode.contains(menuDTO.getMenuCode())) {
                filterMenuDTOList.add(menuDTO);
            }
        }
        menuDTOS.removeAll(filterMenuDTOList);
        //排序，返回树结构
        return MenuUtils.formatMenusToTree(menuDTOS);
    }

    @Override
    public List<MenuDTO> selectAllMenuForTree() {
        List<MenuDTO> menuDTOS = customizeUnitRepository.selectAllMenu();
        if (CollectionUtils.isEmpty(menuDTOS)) {
            return null;
        }
        //排序，返回树结构
        return MenuUtils.formatMenusToTree(menuDTOS);
    }

    @Override
    public Page<Unit> selectUnitByOption(Unit unit, PageRequest pageRequest) {
        return PageHelper.doPageAndSort(pageRequest, () -> customizeUnitRepository.selectByOption(unit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Unit createUnit(Unit unit) {
        checkUnit(unit);
        unit.setUnitCode(unit.getUnitCode().toUpperCase());
        unit.setEnableFlag(1);
        customizeUnitRepository.insertSelective(unit);
        customizeUnitRepository.cacheUnit(unit);
        return unit;
    }

    @Override
    public UnitDTO selectUnitById(Long unitId) {
        Assert.notNull(unitId, "unitId can not be null !");
        Unit unit = customizeUnitRepository.selectById(unitId);
        if (unit == null) {
            return null;
        }
        UnitDTO customizeUnitDTO = new UnitDTO();
        customizeUnitDTO.setUnit(unit);
        List<UnitField> unitFields = customizeUnitFieldRepository.selectUnitFieldsByUnitId(unit.getId());
        //翻译
        if (CollectionUtils.isNotEmpty(unitFields)) {
            Map<String, LovValueDTO> fieldCategoryMap = lovAdapter.queryLovValue(CustomizeConstants.LovCode.FIELD_CATEGORY_LOV_CODE, BaseConstants.DEFAULT_TENANT_ID).stream().collect(Collectors.toMap(LovValueDTO::getValue, Function.identity(), (key1, key2) -> key2));
            Map<String, LovValueDTO> fieldTypeMap = lovAdapter.queryLovValue(CustomizeConstants.LovCode.FIELD_TYPE_LOV_CODE, BaseConstants.DEFAULT_TENANT_ID).stream().collect(Collectors.toMap(LovValueDTO::getValue, Function.identity(), (key1, key2) -> key2));
            boolean translateCategory = MapUtils.isNotEmpty(fieldCategoryMap);
            boolean translateType = MapUtils.isNotEmpty(fieldTypeMap);
            unitFields.forEach(t -> {
                if (t.getField() == null) {
                    return;
                }
                if (t.isNotTableField()) {
                    ModelField modelField = new ModelField();
                    modelField.setFieldCode(t.getFieldAlias());
                    //设置为虚拟字段
                    modelField.setFieldCategory(CustomizeConstants.FieldCategory.VIRTUAL_FIELD);
                    t.setField(modelField);
                    //渲染方式置空
                    //t.setRenderOptions(null);
                }
                ModelField field = t.getField();
                if (translateCategory && fieldCategoryMap.containsKey(field.getFieldCategory())) {
                    LovValueDTO valueDTO = fieldCategoryMap.get(field.getFieldCategory());
                    field.set_innerMap("fieldCategoryMeaning", valueDTO.getMeaning());
                }
                if (translateType && fieldTypeMap.containsKey(field.getFieldCategory())) {
                    LovValueDTO valueDTO = fieldCategoryMap.get(field.getFieldCategory());
                    field.set_innerMap("fieldTypeMeaning", valueDTO.getMeaning());
                }
            });
        }
        Unit.sortField(unit.getUnitType(), unitFields);
        customizeUnitDTO.setFields(unitFields);
        return customizeUnitDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Unit saveUnit(Unit unit) {
        checkUnit(unit);
        Assert.notNull(unit.getId(), "unitId can not be null!");
        if (CollectionUtils.isNotEmpty(unit.getConRelatedUnits())) {
            StringBuffer sb = new StringBuffer();
            unit.getConRelatedUnits().forEach(code -> sb.append(code).append(","));
            unit.setConRelatedUnit(sb.substring(0, sb.length() - 1));
        }
        customizeUnitRepository.updateOptional(unit, Unit.UPDATE_FIELD_COMMON_LIST);
        Unit dbUnit = customizeUnitRepository.selectByPrimaryKey(unit.getId());
        customizeUnitRepository.cacheUnit(dbUnit);
        return dbUnit;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UnitField saveUnitField(UnitField unitField) {
        Unit unit = customizeUnitRepository.selectByPrimaryKey(unitField.getUnitId());
        if (unit == null) {
            throw new CommonException("unitId not exists! id = " + unitField.getUnitId());
        }
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        //校验fieldAlias唯一性
        UnitField checkOption = new UnitField();
        checkOption.setUnitId(unitField.getUnitId());
        UnitField fromDb = null;
        if (!unitField.isNotTableField()) {
            //校验modelId,fieldId唯一性
            Assert.notNull(unitField.getFieldId(), "fieldId can not be null!");
            Assert.notNull(unitField.getModelId(), "modelId can not be null!");
            checkOption.setFieldId(unitField.getFieldId());
            checkOption.setModelId(unitField.getModelId());
            checkOption.setFieldAlias(null);
            fromDb = customizeUnitFieldRepository.selectOne(checkOption);
            if (fromDb != null && !fromDb.getId().equals(unitField.getId())) {
                throw new CommonException("modelId and fieldId repeat !");
            }
        }
        if (unitField.getId() != null) {
            //更新
            if (unitField.isNotTableField() && !StringUtils.isEmpty(unitField.getFieldCode())) {
                unitField.setFieldAlias(StringUtil.underlineToCamelhump(unitField.getFieldCode()));
            }
            customizeUnitFieldRepository.updateOptional(unitField, UnitField.UPDATE_FIELD_COMMON_LIST);
            //更新租户配置，处理别名更新
            if (fromDb != null && !ObjectUtils.nullSafeEquals(fromDb.getFieldAlias(), unitField.getFieldAlias())) {
                unitField.setUnitCode(unit.getUnitCode());
                configFieldRepository.updateFieldAliasByUnitField(unitField);
                configRepository.delConfigFieldCacheByUnitField(unitField);
            }

            //删除字段参数
            unitFieldParamRepository.delete(new UnitFieldParam().setUnitFieldId(unitField.getId()));
        } else {
            unitField.setTenantId(0L);
            //保存fieldCode
            if (unitField.isNotTableField()) {
                unitField.setFieldAlias(StringUtil.underlineToCamelhump(unitField.getFieldCode()));
                unitField.setFieldCode(StringUtil.camelhumpToUnderline(unitField.getFieldAlias()));
            } else {
                unitField.setFieldCode(modelLocalCache.getModelField(unitField.getModelId(), unitField.getFieldId()).getFieldCode());
            }
            customizeUnitFieldRepository.insertSelective(unitField);
        }
        //插入字段参数配置
        if (CollectionUtils.isNotEmpty(unitField.getParamList())) {
            unitFieldParamRepository.batchInsertSelective(unitField.getParamList().stream().map(v -> v.setUnitFieldId(unitField.getId())).collect(Collectors.toList()));
        }

        UnitField unitFieldDb = customizeUnitFieldRepository.selectByPrimaryKey(unitField.getId());
        unitField.setUnitCode(unit.getUnitCode());
        unitField.setField(ModelField.convertFromModelFieldPub(modelFieldRepository.selectByPrimaryKey(unitField.getFieldId())));
        unitField.setModelId(unitFieldDb.getModelId());
        customizeUnitFieldRepository.cacheUnitField(unitField);
        return unitField;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUnitField(Long unitFieldId) {
        UnitField dbUnitField = customizeUnitFieldRepository.selectWithUnitCodeById(unitFieldId);
        if (dbUnitField == null) {
            return;
        }
        //unitField Db redis delete
        customizeUnitFieldRepository.deleteByPrimaryKey(unitFieldId);
        //删除关联的字段参数
        unitFieldParamRepository.delete(new UnitFieldParam().setUnitFieldId(unitFieldId));
        //configField Db redis delete
        ConfigField configField = new ConfigField();
        configField.setFieldId(dbUnitField.getFieldId());
        configField.setUnitId(dbUnitField.getUnitId());
        configField.setFieldCode(dbUnitField.getFieldCode());
        customizeConfigService.deleteConfigField(configField);
        customizeUnitFieldRepository.delUnitFieldCache(dbUnitField.getUnitCode(), dbUnitField.cacheHashKey());
    }

    @Override
    public Page<ModelField> selectNotConfigField(Long unitId, FieldCommonSearchDTO searchDTO, PageRequest pageRequest) {
        Assert.notNull(searchDTO.getModelId(), "error.modelId.null.");
        Page<ModelField> modelFields = PageHelper.doPage(pageRequest, () -> customizeUnitFieldRepository.selectNotConfigField(unitId, searchDTO));
        if (CollectionUtils.isNotEmpty(modelFields)) {
            Map<String, LovValueDTO> fieldCategoryMap = lovAdapter.queryLovValue(CustomizeConstants.LovCode.FIELD_CATEGORY_LOV_CODE, BaseConstants.DEFAULT_TENANT_ID).stream().collect(Collectors.toMap(LovValueDTO::getValue, Function.identity(), (key1, key2) -> key2));
            modelFields.forEach(field -> {
                if (fieldCategoryMap.containsKey(field.getFieldCategory())) {
                    LovValueDTO valueDTO = fieldCategoryMap.get(field.getFieldCategory());
                    field.set_innerMap("fieldCategoryMeaning", valueDTO.getMeaning());
                }
            });
        }
        return modelFields;
    }

    @Override
    public Unit selectUnitWithConfig(Long tenantId, Long unitId) {
        return customizeUnitRepository.selectUnitWithConfigById(tenantId, unitId);
    }

    @Override
    public List<Unit> selectByGroupId(Long groupId) {
        Unit unit = new Unit();
        unit.setUnitGroupId(groupId);
        return customizeUnitRepository.selectByOption(unit);
    }

    private void checkUnit(Unit unit) {
        if (CustomizeConstants.FormType.FILTER.equals(unit.getUnitType())
                || CustomizeConstants.FormType.FORM.equals(unit.getUnitType())
                || CustomizeConstants.FormType.GRID.equals(unit.getUnitType())
                || CustomizeConstants.FormType.QUERY_FORM.equals(unit.getUnitType())) {
            Assert.notNull(unit.getModelId(), "modelId can not be null!");
        } else {
            unit.setModelId(-1L);
        }
        Assert.notNull(unit.getUnitGroupId(), "unitGroupId can not be null!");
        Assert.notNull(unit.getUnitCode(), "unitCode can not be null!");
        Assert.notNull(unit.getUnitType(), "unitType can not be null!");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UnitGroup createGroup(UnitGroup group) {
        Assert.notNull(group.getGroupCode(), "groupCode can not be null!");
        Assert.notNull(group.getMenuCode(), "menuCode can not be null!");
        group.setGroupCode(group.getGroupCode().toUpperCase());
        unitGroupRepository.insertSelective(group);
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UnitGroup updateGroup(UnitGroup group) {
        Assert.notNull(group.getUnitGroupId(), "unitGroupId can not be null!");
        Assert.notNull(group.getGroupCode(), "getGroupCode can not be null!");
        unitGroupRepository.updateOptional(group, UnitGroup.FIELD_GROUP_NAME);
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteGroup(Long groupId) {
        return unitGroupRepository.deleteByPrimaryKey(groupId);
    }

    @Override
    public List<UnitGroup> selectByOptions(UnitGroup group) {
        return unitGroupRepository.select(group);
    }

    @Override
    public List<UnitGroup> selectGroupAndUnits(String menuCode) {
        return unitGroupRepository.selectGroupAndUnits(menuCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyUnit(Unit unit, String unitCode) {
        Assert.notNull(unitCode, "copy unitCode can not be null!");
        //拷贝单元配置
        Unit unitCondition = new Unit();
        unitCondition.setUnitCode(unitCode);
        unitCondition = customizeUnitRepository.selectOne(unitCondition);
        BeanUtils.copyProperties(unitCondition, unit, "_token", Unit.FIELD_UNIT_GROUP_ID, Unit.FIELD_UNIT_CODE,
                Unit.FIELD_UNIT_NAME, Unit.FIELD_ID, Unit.FIELD_CON_RELATED_UNIT, Unit.FIELD_UNIT_GROUP_ID, Unit.FIELD_UNIT_CODE, Unit.FIELD_UNIT_NAME,
                Unit.FIELD_TABLE_ID, Unit.FIELD_CREATION_DATE, Unit.FIELD_CREATED_BY, Unit.FIELD_LAST_UPDATE_DATE,
                Unit.FIELD_LAST_UPDATED_BY, Unit.FIELD_OBJECT_VERSION_NUMBER);
        unit.setId(null);
        checkUnit(unit);
        unit.setUnitCode(unit.getUnitCode().toUpperCase());
        customizeUnitRepository.insertSelective(unit);
        //查询被复制的单元字段
        List<UnitField> unitFields = customizeUnitFieldRepository.selectUnitFieldsByUnitId(unitCondition.getId());
        if (CollectionUtils.isNotEmpty(unitFields)) {
            unitFields.forEach(field -> {
                field.setUnitId(unit.getId());
                field.setId(null);
                field.copyUnitParamList();
                saveUnitField(field);
            });
        }
        customizeUnitRepository.cacheUnit(unit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUnit(String unitCode) {
        Assert.notNull(unitCode, "unitCode can not be null");
        Unit unit = new Unit();
        unit.setUnitCode(unitCode);
        unit = customizeUnitRepository.selectOne(unit);
        if (unit == null) {
            return 0;
        }
        UnitField unitField = new UnitField();
        unitField.setUnitId(unit.getId());
        List<UnitField> unitFields = customizeUnitFieldRepository.select(unitField);
        if (CollectionUtils.isNotEmpty(unitFields)) {
            unitFields.forEach(t -> deleteUnitField(t.getId()));
        }
        Config config = new Config();
        config.setUnitId(unit.getId());
        configRepository.delete(config);
        customizeUnitRepository.deleteByPrimaryKey(unit);
        //TODO 删除用户数据
        customizeUnitRepository.deleteUnitCache(unit.getUnitCode());
        return 0;
    }

}
