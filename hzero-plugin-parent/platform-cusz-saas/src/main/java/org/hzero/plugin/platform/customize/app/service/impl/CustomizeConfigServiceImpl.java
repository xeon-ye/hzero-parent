package org.hzero.plugin.platform.customize.app.service.impl;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.util.StringUtil;
import org.apache.commons.collections4.MapUtils;
import org.hzero.boot.customize.dto.ModelFieldMetaData;
import org.hzero.boot.customize.dto.ModelMetaData;
import org.hzero.boot.customize.util.JdbcToJava;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.hzero.boot.platform.lov.dto.LovValueDTO;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.helper.LanguageHelper;
import org.hzero.plugin.platform.customize.api.dto.*;
import org.hzero.plugin.platform.customize.app.service.CustomizeCommonService;
import org.hzero.plugin.platform.customize.app.service.CustomizeConfigService;
import org.hzero.plugin.platform.customize.domain.entity.*;
import org.hzero.plugin.platform.customize.domain.repository.*;
import org.hzero.plugin.platform.customize.infra.common.ConfigLocalCache;
import org.hzero.plugin.platform.customize.infra.common.ModelLocalCache;
import org.hzero.plugin.platform.customize.infra.common.UnitLocalCache;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author : peng.yu01@hand-china.com 2019/12/19 19:49
 */
@Service
public class CustomizeConfigServiceImpl implements CustomizeConfigService {

    @Autowired
    private CustomizeConfigFieldRepository configFieldRepository;
    @Autowired
    private CustomizeConfigFieldWidgetRepository fieldWidgetRepository;
    @Autowired
    private CustomizeCommonService customizeCommonService;
    @Autowired
    private CustomizeUnitFieldRepository customizeUnitFieldRepository;
    @Autowired
    private CustomizeConfigFieldRepository customizeConfigFieldRepository;
    @Autowired
    private CustomizeConfigFieldMapRepository customizeConfigFieldMapRepository;
    @Autowired
    private CustomizeConfigRepository configRepository;
    @Autowired
    private CustomizeUnitRepository unitRepository;
    @Autowired
    private LovAdapter lovAdapter;
    @Autowired
    private CustomizeConfigFieldWidgetRepository customizeConfigFieldWidgetRepository;
    @Autowired
    private CustomizeModelFieldRepository modelFieldRepository;
    @Autowired
    private CustomizeModelRepository modelRepository;
    @Autowired
    private CustomizeFieldConditionHeaderRepository conditionHeaderRepository;
    @Autowired
    private CustomizeFieldConditionLineRepository conditionLineRepository;
    @Autowired
    private CustomizeConfigFieldParamRepository configFieldParamRepository;
    @Autowired
    private CustomizeUnitFieldParamRepository unitFieldParamRepository;
    @Autowired
    private CustomizeFieldConditionValidRepository conditionValidRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUnitConfig(Config config) {
        Assert.notNull(config.getUnitId(), "unitId can not be null!");
        Assert.notNull(config.getTenantId(), "tenantId can not be null !");
        if (config.getId() == null) {
            configRepository.insertSelective(config);
        } else {
            configRepository.updateOptional(config, Config.FIELD_MAX_COL, Config.FIELD_UNIT_TITLE);
        }
        Unit unit = unitRepository.selectByPrimaryKey(config.getUnitId());
        if (unit == null) {
            throw new CommonException("unitId not exists! id = " + config.getUnitId());
        }
        config.setUnitCode(unit.getUnitCode());
        configRepository.cacheConfig(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUnitConfigFieldAndWdg(ConfigField configField) {
        Assert.notNull(configField.getUnitId(), "unitId can not be null!");
        Assert.notNull(configField.getTenantId(), "tenantId can not be null !");
//        if (configField.isNotTableField()) {
//            configField.setRenderOptions(CustomizeConstants.RenderOptions.TEXT);
//        }
        Unit unit = unitRepository.selectByPrimaryKey(configField.getUnitId());
        if (unit == null) {
            throw new CommonException("unitId not exists! id = " + configField.getUnitId());
        }
        UnitLocalCache unitLocalCache = new UnitLocalCache(customizeUnitFieldRepository);
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        boolean needDel = true;
        //校验操作列fieldAlias唯一性
        ConfigField checkOption = new ConfigField();
        checkOption.setUnitId(configField.getUnitId());
        checkOption.setTenantId(configField.getTenantId());
        checkOption.setUserId(-1L);
        if (!configField.isNotTableField()) {
            //校验modelId,fieldId唯一性
            Assert.notNull(configField.getFieldId(), "fieldId can not be null!");
            Assert.notNull(configField.getModelId(), "modelId can not be null!");
            checkOption.setFieldId(configField.getFieldId());
            checkOption.setModelId(configField.getModelId());
            checkOption.setFieldAlias(null);
            ConfigField fromDb = configFieldRepository.selectOne(checkOption);
            if (fromDb != null && !fromDb.getConfigFieldId().equals(configField.getConfigFieldId())) {
                throw new CommonException("modelId and fieldId repeat !");
            }
        }
        //更新configField
        if (configField.getConfigFieldId() == null) {
            needDel = false;
            //插入时，处理多语言
            UnitField unitField = unitLocalCache.getUnitField(unit.getUnitCode(), configField.getFieldId());
            //判断字段名称是否与单元字段一致
            if (unitField != null) {
                if (ObjectUtils.nullSafeEquals(unitField.getFieldName(), configField.getFieldName())) {
                    configField.initFieldNameTlMaps(" ");
                } else {
                    configField.initFieldNameTlMaps(configField.getFieldName());
                }
            }
            //保存fieldCode
            if (configField.isNotTableField()) {
                configField.setFieldAlias(StringUtil.underlineToCamelhump(configField.getField().getFieldCode()));
                configField.setFieldCode(StringUtil.camelhumpToUnderline(configField.getFieldAlias()));
            } else {
                configField.setFieldCode(modelLocalCache.getModelField(configField.getModelId(), configField.getFieldId()).getFieldCode());
            }
            configFieldRepository.insertSelective(configField);
        } else {
//            if (configField.getCustType().equalsIgnoreCase(CustomizeConstants.CustType.STD_TYPE) && !StringUtils.isEmpty(configField.getWhereOption())) {
//                throw new CommonException("error.not_support_whereOption");
//            }
            if (configField.isNotTableField() && !StringUtils.isEmpty(configField.getFieldCode())) {
                configField.setFieldAlias(StringUtil.underlineToCamelhump(configField.getFieldCode()));
            }
            configFieldRepository.updateOptional(configField, ConfigField.FIELD_FIELD_NAME,
                    ConfigField.FIELD_FIELD_EDITABLE, ConfigField.FIELD_FIELD_REQUIRED,
                    ConfigField.FIELD_VISIBLE, ConfigField.FIELD_FIELD_RENDER_RULE,
                    ConfigField.FIELD_FIELD_ALIAS, ConfigField.FIELD_GRID_SEQ, ConfigField.FIELD_FORM_COL,
                    ConfigField.FIELD_FORM_ROW, ConfigField.FIELD_GRID_FIXED, ConfigField.FIELD_GRID_WIDTH, ConfigField.FIELD_RENDER_OPTIONS,
                    ConfigField.FIELD_FIELD_LABEL_COL, ConfigField.FIELD_FIELD_WRAPPER_COL, ConfigField.FIELD_WHERE_OPTION);
        }
        //更新configFieldWidget 租户个性化不能删除组件
        if (configField.getWidget() != null && !StringUtils.isEmpty(configField.getWidget().getFieldWidget())) {
            ConfigFieldWidget widget = configField.getWidget();
            widget.setTenantId(configField.getTenantId());
            if (widget.getId() != null) {
                fieldWidgetRepository.updateOptional(widget, ConfigFieldWidget.UPDATE_FIELD_COMMON_LIST);
            } else {
                widget.setConfigFieldId(configField.getConfigFieldId());
                fieldWidgetRepository.insertSelective(widget);
            }

        }
        List<FieldConditionHeader> conditionHeaders = configField.getConditionHeaders();
        boolean conditionNull = false;
        if (CollectionUtils.isEmpty(conditionHeaders)) {
            conditionNull = true;
        }
        if (needDel) {
            //更新configFieldMap
            ConfigFieldMap del = new ConfigFieldMap();
            del.setConfigFieldId(configField.getConfigFieldId());
            customizeConfigFieldMapRepository.delete(del);
            //删除关联的字段参数配置
            configFieldParamRepository.delete(new ConfigFieldParam().setConfigFieldId(configField.getConfigFieldId()));
        }
        if (!CollectionUtils.isEmpty(configField.getFieldLovMaps())) {
            configField.getFieldLovMaps().forEach(fieldLovMap -> {
                fieldLovMap.setTenantId(configField.getTenantId());
                fieldLovMap.setConfigFieldId(configField.getConfigFieldId());
                customizeConfigFieldMapRepository.insertSelective(fieldLovMap);
            });
        }
        //删除条件
        List<FieldConditionHeader> dbConditionHeaders = conditionHeaderRepository.select(FieldConditionHeader.CONFIG_FIELD_ID, configField.getConfigFieldId());
        if (!CollectionUtils.isEmpty(dbConditionHeaders)) {
            dbConditionHeaders.forEach(header -> {
                conditionHeaderRepository.deleteByPrimaryKey(header);
                FieldConditionLine line = new FieldConditionLine();
                line.setConHeaderId(header.getConHeaderId());
                conditionLineRepository.delete(line);
            });
        }
        //更新条件
        if (!conditionNull) {
            conditionHeaders.forEach(header -> {
                //默认没有行时为删除操作
                if (CollectionUtils.isEmpty(header.getLines())) {
                    return;
                }
                if (StringUtils.isEmpty(header.getConExpression())) {
                    throw new CommonException("error.conditon_header_expression.null");
                }
                header.setTenantId(configField.getTenantId());
                header.setConfigFieldId(configField.getConfigFieldId());
                conditionHeaderRepository.insertSelective(header);
                header.getLines().forEach(line -> {
                    line.setTenantId(header.getTenantId());
                    line.setConHeaderId(header.getConHeaderId());
                    if (line.getTargetFieldId() != null) {
                        line.setTargetUnitId(configField.getUnitId());
                    }
                    conditionLineRepository.insertSelective(line);
                });
            });
        }
        //更新高级校验配置
        this.saveConValid(configField.getConValid() == null ? null : configField.getConValid().setConfigFieldId(configField.getConfigFieldId()).setTenantId(configField.getTenantId()), configField.getUnitId());

        //更新字段参数
        UnitField unitField = unitLocalCache.getUnitField(unit.getUnitCode(), configField.getFieldId());
        if (unitField == null) {
            if (!CollectionUtils.isEmpty(configField.getParamList())) {
                configFieldParamRepository.batchInsertSelective(configField.getParamList().stream().filter(v -> v.getParamId() == null).map(v -> {
                    v.setConfigFieldId(configField.getConfigFieldId());
                    v.setTenantId(configField.getTenantId());
                    return v;
                }).collect(Collectors.toList()));
            }
        }
        //头配置
        Config config = new Config();
        config.setTenantId(configField.getTenantId());
        config.setUnitId(configField.getUnitId());
        config.setUnitCode(unit.getUnitCode());
        config.setUserId(-1L);
        if (configField.getUserId() != null && configRepository.selectOne(config) == null) {
            configRepository.insertSelective(config);
        }
        configRepository.cacheConfig(config);
        //删除当前字段关联的租户级缓存
        configFieldRepository.cacheDel(configField.getTenantId(), unit.getUnitCode());
        customizeCommonService.cacheCustomizeConfig(configField.getTenantId(), configField.getUnitId());
    }

    @Override
    public List<ConfigField> selectUnitConfig(Long tenantId, Long unitId) {
        //获取单元配置字段
        List<ConfigField> defaultUnitFields = customizeUnitFieldRepository.selectUnitFieldByUnitId(unitId);
        Map<String, ConfigField> unitFieldMap = new HashMap<>(defaultUnitFields.size());
        Map<Long, Long> unitFieldIdMap = new HashMap<>(defaultUnitFields.size());
        for (ConfigField field : defaultUnitFields) {
            unitFieldMap.put(field.bizHashKey(), field);
            unitFieldIdMap.put(field.getUnitFieldId(), field.getFieldId());
        }
        //获取个性化单元字段关联的参数配置
        List<UnitFieldParam> unitFieldParamList = MapUtils.isEmpty(unitFieldIdMap) ? Collections.emptyList() : unitFieldParamRepository.selectByUnitFieldIds(unitFieldIdMap.keySet());
        Map<Long, List<ConfigFieldParam>> unitParamMap = new HashMap<>(unitFieldParamList.size());
        unitFieldParamList.forEach(param -> unitParamMap.computeIfAbsent(unitFieldIdMap.get(param.getUnitFieldId()), v -> new ArrayList<>(12)).add(param.convertToConfigFieldParam()));
        // 获取租户个性化已配置的字段，包含参数配置
        List<ConfigField> configFields = configFieldRepository.selectConfigFieldByUnitId(tenantId, unitId);
        //key= fieldId
        Map<String, ConfigField> configFieldMap = configFields.stream().collect(Collectors.toMap(ConfigField::bizHashKey, Function.identity(), (key1, key2) -> key2, LinkedHashMap::new));

        List<ConfigField> result = new ArrayList<>(defaultUnitFields.size() + configFields.size());

        //替换已配置的字段
        unitFieldMap.forEach((key, unitField) -> {
            //拷贝单元参数
            List<ConfigFieldParam> unitParam = unitParamMap.get(unitField.getFieldId());
            //字段已经配置到租户层
            if (configFieldMap.containsKey(key)) {
                ConfigField configField = configFieldMap.get(key);
                if (org.apache.commons.lang3.StringUtils.isBlank(configField.getFieldName())) {
                    configField.setFieldName(unitField.getFieldName());
                }
                //添加单元配置的参数
                List<ConfigFieldParam> params = new ArrayList<>(6);
                params.addAll(Optional.ofNullable(unitParam).orElse(new ArrayList<>(6)));
                params.addAll(Optional.ofNullable(configField.getParamList()).orElse(new ArrayList<>(6)));
                configField.setParamList(params);
                result.add(configField);
                //删除后，剩下的是扩展字段
                configFieldMap.remove(key);
            } else {
                //扩展字段设置参数配置
                unitField.setParamList(unitParam);
                result.add(unitField);
            }
        });
        //合并已配置的模型字段
        result.addAll(configFieldMap.values());
        result.forEach(t -> {
            String mapKey = t.bizHashKey();
            if (unitFieldMap.containsKey(mapKey)) {
                t.setCustType(CustomizeConstants.CustType.STD_TYPE);
                t.setFieldAlias(unitFieldMap.get(mapKey).getFieldAlias());
            } else {
                t.setCustType(CustomizeConstants.CustType.EXT_TYPE);
                t.setFieldAlias(configFieldMap.get(mapKey).getFieldAlias());
            }
            if (t.getField() != null) {
                t.getField().setFieldType(JdbcToJava.convertJavaType(t.getField().getFieldType()));
            }
        });
        //排序
        //查询unit类型
        Unit unit = unitRepository.selectByPrimaryKey(unitId);
        //排序
        Unit.sortField(unit.getUnitType(), result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfigFieldById(ConfigField configField) {
        Assert.notNull(configField.getConfigFieldId(), "configField can not be null!");
        ConfigField dbField = customizeConfigFieldRepository.selectById(configField.getConfigFieldId());
        //级联删除相关表数据
        deleteConfigField(dbField);
        customizeConfigFieldRepository.delConfigFieldCache(dbField);
        customizeCommonService.cacheCustomizeConfig(dbField.getTenantId(), dbField.getUnitId());
    }

    @Override
    public List<ConfigFieldLovDTO> selectSimpleConfigField(Long tenantId, Long unitId, FieldCommonSearchDTO searchDTO, PageRequest pageRequest) {
        List<ConfigFieldLovDTO> defaultConfigFields = customizeUnitFieldRepository.selectSimpleUnitFieldByUnitId(unitId, searchDTO);
        List<ConfigFieldLovDTO> configFields = customizeConfigFieldRepository.selectSimpleConfigFieldByUnitId(tenantId, unitId, searchDTO);
        //合并
        List<ConfigFieldLovDTO> merge = merge(defaultConfigFields, configFields);
        Map<String, LovValueDTO> fieldTypeMap = lovAdapter.queryLovValue(CustomizeConstants.LovCode.FIELD_TYPE_LOV_CODE, BaseConstants.DEFAULT_TENANT_ID).stream().collect(Collectors.toMap(LovValueDTO::getValue, Function.identity(), (key1, key2) -> key2));
        //无权访问 直接返回
        Map<String, LovValueDTO> fieldCategoryMap = lovAdapter.queryLovValue(CustomizeConstants.LovCode.FIELD_CATEGORY_LOV_CODE, BaseConstants.DEFAULT_TENANT_ID).stream().collect(Collectors.toMap(LovValueDTO::getValue, Function.identity(), (key1, key2) -> key2));
        if (CollectionUtils.isEmpty(fieldTypeMap) || CollectionUtils.isEmpty(fieldCategoryMap)) {
            return merge;
        }
        //翻译字段类型
        for (ConfigFieldLovDTO lovDTO : merge) {
            lovDTO.setFieldType(JdbcToJava.convertJavaType(lovDTO.getFieldType()));
            if (fieldTypeMap.containsKey(lovDTO.getFieldType())) {
                lovDTO.setFieldType(fieldTypeMap.get(lovDTO.getFieldType()).getMeaning());
            }
            if (fieldCategoryMap.containsKey(lovDTO.getFieldCategory())) {
                lovDTO.setFieldCategory(fieldCategoryMap.get(lovDTO.getFieldCategory()).getMeaning());
            }
        }
        return merge;
    }

    protected List<ConfigFieldLovDTO> merge(List<ConfigFieldLovDTO> unitConfigFields, List<ConfigFieldLovDTO> configFields) {
        List<ConfigFieldLovDTO> result = new ArrayList<>(unitConfigFields.size() + configFields.size());
        //key= fieldId
        Map<Long, ConfigFieldLovDTO> configFieldMap = configFields.stream().collect(Collectors.toMap(ConfigFieldLovDTO::getFieldId, Function.identity(), (key1, key2) -> key2, LinkedHashMap::new));
        //替换已配置的模型字段
        for (ConfigFieldLovDTO configField : unitConfigFields) {
            Long fieldId = configField.getFieldId();
            //字段已经配置到租户层
            if (configFieldMap.containsKey(fieldId)) {
                ConfigFieldLovDTO temp = configFieldMap.get(fieldId);
                if (org.apache.commons.lang3.StringUtils.isBlank(temp.getFieldName())) {
                    temp.setFieldName(configField.getFieldName());
                }
                result.add(temp);
                configFieldMap.remove(fieldId);
            } else {
                result.add(configField);
            }
        }
        //合并已配置的模型字段
        result.addAll(configFieldMap.values());
        return result;
    }

    @Override
    public List<ConfigFieldMap> selectFieldMapping(Long configFieldId) {
        if (configFieldId == null) {
            return Collections.emptyList();
        }
        List<ConfigFieldMap> fieldMappingLists = customizeConfigFieldMapRepository.select(ConfigFieldMap.FIELD_CONFIG_FIELD_ID, configFieldId);
        if (CollectionUtils.isEmpty(fieldMappingLists)) {
            return Collections.emptyList();
        }
        ConfigField dbField = customizeConfigFieldRepository.selectById(configFieldId);

        Long tenantId = dbField.getTenantId();
        String unitCode = dbField.getUnitCode();

        UnitLocalCache unitLocalCache = new UnitLocalCache(customizeUnitFieldRepository);
        ConfigLocalCache configLocalCache = new ConfigLocalCache(configFieldRepository);

        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);

        //字段多语言
        String lang = LanguageHelper.language();
        for (ConfigFieldMap fieldMap : fieldMappingLists) {

            ModelMetaData modelMetaData = modelLocalCache.getModel(fieldMap.getSourceModelId());

            String targetFieldName = "";
            Long fieldId = fieldMap.getTargetFieldId();
            if (configLocalCache.containsConfigField(tenantId, unitCode, fieldId)) {
                ConfigField configField = configLocalCache.getConfigField(tenantId, unitCode, fieldId);
                configField.convertTls(lang, false);
                if (!StringUtils.isEmpty(configField.getFieldName())) {
                    targetFieldName = configField.getFieldName();
                }
            } else if (unitLocalCache.containsField(unitCode, fieldId)) {
                UnitField unitField = unitLocalCache.getUnitField(unitCode, fieldId);
                if (!StringUtils.isEmpty(unitField.getFieldName())) {
                    targetFieldName = unitField.getFieldName();
                }
            }
            fieldMap.setTargetFieldName(targetFieldName);
            fieldMap.setModelName(modelMetaData.getModelName());
            fieldMap.setSourceFieldName(modelLocalCache.getModelField(fieldMap.getSourceModelId(), fieldMap.getSourceFieldId()).getFieldName());
        }
        return fieldMappingLists;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfigField(ConfigField configField) {

        //如果字段被引用，删除值集映射
        Long deleteFieldId = configField.getFieldId();
        if (!ObjectUtils.nullSafeEquals(-1L, deleteFieldId)) {
            List<ConfigFieldMap> configFieldMaps = customizeConfigFieldMapRepository.selectByFieldIdAndUnitId(configField.getUnitId(), deleteFieldId);
            Set<Long> tempIds = new HashSet<>();
            if (!CollectionUtils.isEmpty(configFieldMaps)) {
                configFieldMaps.forEach(map -> {
                    customizeConfigFieldMapRepository.deleteByPrimaryKey(map.getId());
                    tempIds.add(map.getConfigFieldId());
                });
            }
            //被引用条件，删除条件
            List<FieldConditionLine> fieldConditionLines = conditionLineRepository.selectByUnitIdAndFieldId(configField.getUnitId(), deleteFieldId);
            Set<Long> conditionLineIds = new HashSet<>();
            Set<Long> conditionHeaderIds = new HashSet<>();
            if (!CollectionUtils.isEmpty(fieldConditionLines)) {
                conditionLineIds = fieldConditionLines.stream().map(FieldConditionLine::getConLineId).collect(Collectors.toSet());
                conditionHeaderIds = fieldConditionLines.stream().map(FieldConditionLine::getConHeaderId).collect(Collectors.toSet());
            }
            if (!CollectionUtils.isEmpty(conditionLineIds)) {
                List<FieldConditionHeader> conditionHeaders = conditionHeaderRepository.selectByIds(StringUtils.collectionToCommaDelimitedString(conditionLineIds));
                //删除行
                conditionLineRepository.deleteByIds(conditionLineIds);
                //删除头
                //conditionHeaderRepository.batchDeleteByPrimaryKey(conditionHeaders);
                tempIds.addAll(conditionHeaders.stream().map(FieldConditionHeader::getConfigFieldId).collect(Collectors.toList()));
            }


            if (!CollectionUtils.isEmpty(tempIds)) {
                List<ConfigField> configFields = customizeConfigFieldRepository.selectConfigFieldAndUnitCode(tempIds);
                configFields.forEach(t ->
                        customizeConfigFieldRepository.delConfigFieldCache(t)
                );
            }
        }

        List<ConfigField> dbConfigFields = customizeConfigFieldRepository.selectByUnitIdAndFieldId(configField);
        if (CollectionUtils.isEmpty(dbConfigFields)) {
            return;
        }
        Set<Long> configFieldIdSet = dbConfigFields.stream().map(ConfigField::getConfigFieldId).collect(Collectors.toSet());
        //删除ConfigField
        customizeConfigFieldRepository.batchDelete(dbConfigFields);
        //删除关联的字段参数配置
        configFieldParamRepository.batchDelete(configFieldIdSet.stream().map(v -> new ConfigFieldParam().setConfigFieldId(v)).collect(Collectors.toList()));
        //删除ConfigFieldMap
        customizeConfigFieldMapRepository.deleteByConfigFieldIds(configFieldIdSet);
        //删除ConfigFieldWidget
        customizeConfigFieldWidgetRepository.deleteByConfigFieldIds(configFieldIdSet);
        //删除FieldConditionHeader、FieldConditionLine
        List<FieldConditionHeader> headers = conditionHeaderRepository.selectAllConditions(configFieldIdSet);
        if (!CollectionUtils.isEmpty(headers)) {
            headers.forEach(header -> {
                FieldConditionHeader delHeader = new FieldConditionHeader();
                delHeader.setConfigFieldId(header.getConfigFieldId());
                conditionHeaderRepository.delete(delHeader);
                FieldConditionLine delLine = new FieldConditionLine();
                delLine.setConHeaderId(header.getConHeaderId());
                conditionLineRepository.delete(delLine);
            });
        }
        //configField 缓存删除 、包含组件和映射缓存
        for (ConfigField field : dbConfigFields) {
            customizeConfigFieldRepository.delConfigFieldCache(field);
        }
    }

    @Override
    public Map<String, UserConfigDTO> userConfigByUnitCodes(Long tenantId, String[] unitCodes) {
        Assert.notNull(unitCodes, "unitCodes can not be null");
        Assert.notNull(tenantId, "tenantId can not be null");
        Map<String, UserConfigDTO> result = new HashMap<>(unitCodes.length);
        for (String unitCode : unitCodes) {
            result.put(unitCode, this.userConfigByUnitCode(tenantId, unitCode));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public UserConfigDTO saveUserConfigFields(Long tenantId, UserConfigDTO userConfigDTO) {

        Long userId = DetailsHelper.getUserDetails().getUserId();
        Config configHeader = userConfigDTO.getConfig();
        Long unitId = userConfigDTO.getConfig().getUnitId();
        configHeader.setTenantId(tenantId);
        configHeader.setUserId(userId);
        Unit unit = unitRepository.selectByPrimaryKey(unitId);
        configHeader.setUnitCode(unit.getUnitCode());
        UnitLocalCache unitLocalCache = new UnitLocalCache(customizeUnitFieldRepository);
        ConfigLocalCache configLocalCache = new ConfigLocalCache(configFieldRepository);
        //保存头配置
        if (configHeader.getId() != null) {
            //更新
            configRepository.updateOptional(configHeader, Config.FIELD_PAGE_SIZE);
        } else {
            configRepository.insertSelective(configHeader);
        }
        //先删除
        List<ConfigField> configFields = userConfigDTO.getFields();
        ConfigField configField = new ConfigField();
        configField.setUnitId(unit.getId());
        configField.setUserId(userId);
        configField.setTenantId(tenantId);
        //删除
        //查询Id，删除多语言表
        List<ConfigField> dbList = customizeConfigFieldRepository.select(configField);
        if (!CollectionUtils.isEmpty(dbList)) {
            //删除多语言
            customizeConfigFieldRepository.deleteConfigFieldTl(dbList.stream().map(ConfigField::getConfigFieldId).collect(Collectors.toList()));
            customizeConfigFieldRepository.deleteUserConfigField(configField);
        }
        //重新保存
        for (ConfigField field : configFields) {
            field.setTenantId(tenantId);
            field.setUserId(userId);
            field.setConfigFieldId(null);
            //判断字段名称是否更新
            UnitField unitField = unitLocalCache.getUnitField(unit.getUnitCode(), field.bizHashKey());
            ConfigField configTem = configLocalCache.getConfigField(tenantId, unit.getUnitCode(), field.bizHashKey());
            String oldName = null;
            if (configTem != null && org.apache.commons.lang3.StringUtils.isNotBlank(configTem.getFieldName())) {
                oldName = configTem.getFieldName();
            } else if (unitField != null && org.apache.commons.lang3.StringUtils.isNotBlank(unitField.getFieldName())) {
                oldName = unitField.getFieldName();
            }
            if (ObjectUtils.nullSafeEquals(oldName, field.getFieldName())) {
                field.initFieldNameTlMaps(" ");
            } else {
                field.initFieldNameTlMaps(field.getFieldName());
            }
            customizeConfigFieldRepository.insertSelective(field);
        }
        //删除缓存
        configRepository.deleteUserConfigCache(configHeader);
        customizeConfigFieldRepository.delUserConfigFieldCache(tenantId, userId, unit.getUnitCode());
        return userConfigDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserConfigField(Long tenantId, Long unitId) {
        Assert.notNull(tenantId, "tenantId can not be null ");
        Assert.notNull(unitId, "unitId can not be null ");
        Long userId = DetailsHelper.getUserDetails().getUserId();
        //先删除
        ConfigField configField = new ConfigField();
        configField.setUnitId(unitId);
        configField.setUserId(userId);
        configField.setTenantId(tenantId);
        Unit unit = unitRepository.selectByPrimaryKey(unitId);
        //删除
        //查询Id，删除多语言表
        List<ConfigField> dbList = customizeConfigFieldRepository.select(configField);
        if (!CollectionUtils.isEmpty(dbList)) {
            //删除多语言
            customizeConfigFieldRepository.deleteConfigFieldTl(dbList.stream().map(ConfigField::getConfigFieldId).collect(Collectors.toList()));
            customizeConfigFieldRepository.deleteUserConfigField(configField);
        }

        //删除缓存
        Config config = new Config();
        config.setUnitCode(unit.getUnitCode());
        config.setUnitId(unitId);
        config.setUserId(userId);
        config.setTenantId(tenantId);
        configRepository.delete(config);
        configRepository.deleteUserConfigCache(config);
        customizeConfigFieldRepository.delUserConfigFieldCache(tenantId, userId, unit.getUnitCode());
        return 0;
    }

    public UserConfigDTO userConfigByUnitCode(Long tenantId, String unitCode) {

        UserConfigDTO userConfigDTO = new UserConfigDTO();

        Unit unit = unitRepository.getUnitCache(unitCode);
        if (unit == null || unit.isDisabled()) {
            return userConfigDTO;
        }
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        Long userId = DetailsHelper.getUserDetails().getUserId();

        Config configHeader = new Config();
        configHeader.setUserId(userId);
        configHeader.setTenantId(tenantId);
        configHeader.setUnitId(unit.getId());

        //查询个性化头配置
        Config userConfigHeaderDb = configRepository.selectOne(configHeader);
        if (userConfigHeaderDb == null) {
            //获取租户级配置
            userConfigHeaderDb = configRepository.getConfigCache(tenantId, unitCode);
        }
        if (userConfigHeaderDb != null) {
            configHeader.setPageSize(userConfigHeaderDb.getPageSize());
            configHeader.setId(userConfigHeaderDb.getId());
        }

        userConfigDTO.setConfig(configHeader);

        userConfigDTO.setUnitName(unit.getUnitName());
        //查询用户个性化配置
        ConfigField userSearch = new ConfigField();
        userSearch.setTenantId(tenantId);
        userSearch.setUnitId(unit.getId());
        userSearch.setUserId(DetailsHelper.getUserDetails().getUserId());
        List<ConfigField> userConfigDb = customizeConfigFieldRepository.selectUserConfigField(userSearch);
        Map<String, ConfigField> userConfigMap = CollectionUtils.isEmpty(userConfigDb) ? Collections.emptyMap() :
                userConfigDb.stream().collect(Collectors.toMap(BaseField::bizHashKey, v -> v));
        //查询租户配置
        userSearch.setUserId(-1L);
        List<ConfigField> tenantConfigs = configFieldRepository.select(userSearch);
        Map<String, ConfigField> tenantConfigMap = CollectionUtils.isEmpty(tenantConfigs) ? Collections.emptyMap() :
                tenantConfigs.stream().collect(Collectors.toMap(BaseField::bizHashKey, ConfigField::cacheConvert));
        //查询单元配置
        UnitField unitSearch = new UnitField();
        unitSearch.setUnitId(unit.getId());
        List<UnitField> unitFields = customizeUnitFieldRepository.select(unitSearch);
        Map<String, UnitField> unitFieldMap = CollectionUtils.isEmpty(unitFields) ? Collections.emptyMap() :
                unitFields.stream().collect(Collectors.toMap(BaseField::bizHashKey, UnitField::cacheConvert));

        //过滤，用户-》租户-》单元
        //如果用户已经配置，过滤掉租户级删掉的字段或者单元删掉的字段
        List<ConfigField> configFields = new ArrayList<>(32);
        userConfigMap.forEach((mapKey, configField) -> {
            if (tenantConfigMap.containsKey(mapKey)) {
                ConfigField temp = tenantConfigMap.get(mapKey);
                //租户隐藏的字段，不支持个性化配置
                if (ObjectUtils.nullSafeEquals(0, temp.getVisible())) {
                    return;
                }
            } else if (unitFieldMap.containsKey(mapKey)) {
                UnitField unitTemp = unitFieldMap.get(mapKey);
                if (ObjectUtils.nullSafeEquals(0, unitTemp.getFieldVisible())) {
                    //单元隐藏字段
                    return;
                }
            } else {
                //租户和单元都不存在，可能字段已经被删除
                return;
            }
            configFields.add(configField);
        });
        //租户级配置,如果是租户级新增字段
        tenantConfigMap.forEach((mapKey, configField) -> {
            if (!userConfigMap.containsKey(mapKey) && !ObjectUtils.nullSafeEquals(0, configField.getVisible())) {
                configFields.add(configField);
            }
        });
        //如果是单元新增字段
        unitFieldMap.forEach((mapKey, unitField) -> {
            if (userConfigMap.containsKey(mapKey)) {
                return;
            }
            if (tenantConfigMap.containsKey(mapKey)) {
                return;
            }
            configFields.add(ConfigField.readFormUnitField(unitField));
        });

        configFields.forEach(configField -> {
            if (!StringUtils.isEmpty(configField.getFieldAlias())) {
                configField.setFieldCode(configField.getFieldAlias());
            } else {
                configField.setFieldCode(StringUtil.underlineToCamelhump(modelLocalCache.getModelField(configField.getModelId(), configField.getFieldId()).getFieldCode()));
            }
        });

        userConfigDTO.setFields(configFields);

        return userConfigDTO;
    }

    @Override
    public List<FieldConditionHeader> selectCondHeaderByFieldId(FieldConditionHeader search, Long unitId, boolean needConValid) {
        Assert.notNull(search.getTenantId(), "error.tenantId.null.");
        if (search.getConfigFieldId() == null) {
            return null;
        }

        List<FieldConditionHeader> headers = conditionHeaderRepository.selectWithLineByFieldId(Arrays.asList(search.getConfigFieldId()), StringUtils.isEmpty(search.getConType()) ? null : Arrays.asList(search.getConType()), needConValid);
        if (CollectionUtils.isEmpty(headers)) {
            return null;
        }

        UnitLocalCache unitLocalCache = new UnitLocalCache(customizeUnitFieldRepository);
        ConfigLocalCache configFieldCache = new ConfigLocalCache(configFieldRepository);
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        //配置覆盖，租户个性化配置优于个性化单元配置
        for (FieldConditionHeader header : headers) {
            for (FieldConditionLine line : header.getLines()) {
                ConfigField sourceConfig = configFieldCache.getConfigField(search.getTenantId(), line.getSourceUnitCode(), line.getSourceFieldId());
                ModelFieldMetaData sourceField = modelLocalCache.getModelField(line.getSourceModelId(), line.getSourceFieldId());
                UnitField unitField = unitLocalCache.getUnitField(line.getSourceUnitCode(), line.getSourceFieldId());
                line.setSourceFieldCode(sourceField.getFieldCode());
                if (sourceField.getWdgMetaData() != null) {
                    line.setSourceFieldWidget(sourceField.getWdgMetaData().getFieldWidget());
                    line.setSourceFieldValueCode(sourceField.getWdgMetaData().getSourceCode());
                }
                if (unitField != null) {
                    if (!StringUtils.isEmpty(unitField.getFieldAlias())) {
                        line.setSourceFieldCode(unitField.getFieldAlias());
                    }
                }
                if (sourceConfig != null) {
                    ConfigFieldWidget widget = sourceConfig.getWidget();
                    if (widget != null) {
                        line.setSourceFieldWidget(widget.getFieldWidget());
                        if (!StringUtils.isEmpty(widget.getSourceCode())) {
                            line.setSourceFieldValueCode(widget.getSourceCode());
                        }
                    }
                    if (!StringUtils.isEmpty(sourceConfig.getFieldAlias())) {
                        line.setSourceFieldCode(sourceConfig.getFieldAlias());
                    }
                }
                if (unitField != null && !CollectionUtils.isEmpty(unitField.getParamList())) {
                    line.setParamList(unitField.getParamList());
                }
                if (line.getTargetFieldId() != null) {
                    sourceConfig = configFieldCache.getConfigField(search.getTenantId(), line.getTargetUnitCode(), line.getTargetFieldId());
                    sourceField = modelLocalCache.getModelField(line.getTargetModelId(), line.getTargetFieldId());
                    unitField = unitLocalCache.getUnitField(line.getTargetUnitCode(), line.getTargetFieldId());
                    line.setTargetFieldCode(sourceField.getFieldCode());
                    if (unitField != null && !StringUtils.isEmpty(unitField.getFieldAlias())) {
                        line.setTargetFieldCode(unitField.getFieldAlias());
                    }
                    if (sourceConfig != null && !StringUtils.isEmpty(sourceConfig.getFieldAlias())) {
                        line.setTargetFieldCode(sourceConfig.getFieldAlias());
                    }
                }
            }
        }
        return headers;
    }

    @Override
    public List<UnitRelatedDTO> selectWithWdgByUnitId(Long unitId, Long tenantId) {
        Assert.notNull(unitId, "error.unitId.null");
        Unit unit = unitRepository.selectByPrimaryKey(unitId);
        if (unit == null) {
            return null;
        }
        Set<String> allUnitCodes = new LinkedHashSet<>(8);
        allUnitCodes.add(unit.getUnitCode());
        if (!StringUtils.isEmpty(unit.getConRelatedUnit())) {
            allUnitCodes.addAll(Arrays.asList(unit.getConRelatedUnit().split(",")));
        }
        List<UnitRelatedDTO> unitRelates = unitRepository.selectRelatedUnit(allUnitCodes);

        //查询租户个性化添加的扩展字段,已排除操作列字段、用户级配置
        List<UnitRelatedDTO> configRelates = customizeConfigFieldRepository.selectWithWdgByUnitId(allUnitCodes, tenantId);

        //获取字段参数配置
        Set<Long> fieldIdSet = new HashSet<>(64);
        unitRelates.forEach(item -> {
            if (CollectionUtils.isEmpty(item.getUnitFields())) {
                return;
            }
            fieldIdSet.addAll(item.getUnitFields().stream().map(UnitFieldCompositeDTO::getUnitFieldId).collect(Collectors.toSet()));
        });

        //查询字段参数配置
        List<UnitFieldParam> unitFieldParamList = CollectionUtils.isEmpty(fieldIdSet) ? Collections.emptyList() : unitFieldParamRepository.selectByUnitFieldIds(fieldIdSet);
        Map<Long, List<BaseFieldParam>> unitFieldParamMap = new HashMap<>(unitFieldParamList.size());
        if (!CollectionUtils.isEmpty(unitFieldParamList)) {
            unitFieldParamList.forEach(unitParam -> {
                List<BaseFieldParam> fieldParams = unitFieldParamMap.computeIfAbsent(unitParam.getUnitFieldId(), v -> new ArrayList<>(12));
                fieldParams.add(unitParam);
            });
        }
        //设置字段参数配置
        Map<Long, UnitRelatedDTO> unitRelatedMap = new HashMap<>(12);
        unitRelates.forEach(unitRelated -> {
            if (!CollectionUtils.isEmpty(unitRelated.getUnitFields())) {
                unitRelated.getUnitFields().forEach(unitField -> unitField.setParamList(unitFieldParamMap.getOrDefault(unitField.getUnitFieldId(), new ArrayList<>())));
            }
            unitRelatedMap.put(unitRelated.getUnitId(), unitRelated);
        });

        //查询租户字段参数配置
        Map<Long, List<BaseFieldParam>> configFieldParamMap = new HashMap<>(32);
        Map<Long, UnitRelatedDTO> configRelatedMap = new HashMap<>(12);
        if (!CollectionUtils.isEmpty(configRelates)) {
            Set<Long> configFieldIdSet = new HashSet<>(64);
            configRelates.forEach(item -> {
                if (CollectionUtils.isEmpty(item.getUnitFields())) {
                    return;
                }
                configFieldIdSet.addAll(item.getUnitFields().stream().map(UnitFieldCompositeDTO::getConfigFieldId).collect(Collectors.toSet()));
            });
            List<ConfigFieldParam> configFieldParamList = CollectionUtils.isEmpty(fieldIdSet) ? Collections.emptyList() : configFieldParamRepository.selectByConfigFieldIds(configFieldIdSet);
            if (!CollectionUtils.isEmpty(configFieldParamList)) {
                UnitLocalCache unitLocalCache = new UnitLocalCache(customizeUnitFieldRepository);
                ConfigLocalCache configLocalCache = new ConfigLocalCache(configFieldRepository);
                ConfigFieldParam.translateParmaUnit(configFieldParamList, tenantId, unitLocalCache, configLocalCache);
                configFieldParamList.forEach(unitParam -> {
                    List<BaseFieldParam> fieldParams = configFieldParamMap.computeIfAbsent(unitParam.getConfigFieldId(), v -> new ArrayList<>(12));
                    fieldParams.add(unitParam);
                });
            }
        }
        if (MapUtils.isNotEmpty(configFieldParamMap)) {
            configRelates.forEach(configRelated -> {
                if (!CollectionUtils.isEmpty(configRelated.getUnitFields())) {
                    configRelated.getUnitFields().forEach(unitField -> unitField.setParamList(configFieldParamMap.getOrDefault(unitField.getConfigFieldId(), new ArrayList<>())));
                }
                configRelatedMap.put(configRelated.getUnitId(), configRelated);
            });
        }

        //添加租户个性化添加的扩展字段
        for (UnitRelatedDTO configRelated : configRelates) {
            if (!unitRelatedMap.containsKey(configRelated.getUnitId()) && !configRelatedMap.containsKey(configRelated.getUnitId())) {
                continue;
            }
            //个性化单元标准字段
            Map<Long, UnitFieldCompositeDTO> unitModelFieldMap = unitRelatedMap.get(configRelated.getUnitId()).getUnitFields().stream().collect(Collectors.toMap(UnitFieldCompositeDTO::getModelFieldId, v -> v));

            for (UnitFieldCompositeDTO configCom : configRelated.getUnitFields()) {
                //单元存在则覆盖，不存在则添加
                unitModelFieldMap.put(configCom.getModelFieldId(), configCom);
            }
            unitRelatedMap.get(configRelated.getUnitId()).setUnitFields(new ArrayList<>(unitModelFieldMap.values()));
        }
        //保证单元稳定排序
//        List<String> sortUnitOrder = new ArrayList<>(allUnitCodes);
//        List<UnitRelatedDTO> result = new ArrayList<>(unitRelatedMap.values());
//        result.sort(Comparator.comparingInt(o -> sortUnitOrder.indexOf(o.getUnitCode())));
        return new ArrayList<>(unitRelatedMap.values());
    }

    @Override
    public UnitCompositeDTO selectConfigDetails(Long tenantId, Long unitId) {
        UnitCompositeDTO result = new UnitCompositeDTO();
        result.setUnit(unitRepository.selectUnitWithConfigById(tenantId, unitId));
        result.setConfigFields(this.selectUnitConfig(tenantId, unitId));
        result.setUnitAlias(result.getUnit().autoGenerateUnitAlias(unitRepository));
        return result;
    }

    @Override
    public FieldConditionHeader selectConValidByFieldId(FieldConditionHeader search, Long unitId) {
        List<FieldConditionHeader> headers = this.selectCondHeaderByFieldId(search, unitId, true);
        return CollectionUtils.isEmpty(headers) ? null : headers.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConValid(FieldConditionHeader conditionHeader, Long unitId) {
        if (conditionHeader == null) {
            return;
        }
        if (conditionHeader.getConHeaderId() != null) {
            conditionHeaderRepository.deleteByPrimaryKey(conditionHeader);
            conditionLineRepository.delete(new FieldConditionLine().setConHeaderId(conditionHeader.getConHeaderId()));
            conditionValidRepository.batchDeleteByPrimaryKey(conditionValidRepository.select(FieldConditionValid.CON_HEADER_ID, conditionHeader.getConHeaderId()));
        }

        if (!CollectionUtils.isEmpty(conditionHeader.getLines()) || !CollectionUtils.isEmpty(conditionHeader.getValids())) {
            conditionHeaderRepository.insertSelective(conditionHeader);
        }
        if (!CollectionUtils.isEmpty(conditionHeader.getLines())) {
            conditionLineRepository.batchInsertSelective(conditionHeader.getLines().stream().map(valid -> {
                valid.setTenantId(conditionHeader.getTenantId());
                valid.setConHeaderId(conditionHeader.getConHeaderId());
                if (valid.getTargetFieldId() != null) {
                    valid.setTargetUnitId(unitId);
                }
                return valid;
            }).collect(Collectors.toList()));
        }
        if (!CollectionUtils.isEmpty(conditionHeader.getValids())) {
            conditionValidRepository.batchInsertSelective(conditionHeader.getValids().stream().map(line -> {
                line.setTenantId(conditionHeader.getTenantId());
                line.setConHeaderId(conditionHeader.getConHeaderId());
                return line;
            }).collect(Collectors.toList()));
        }
    }
}
