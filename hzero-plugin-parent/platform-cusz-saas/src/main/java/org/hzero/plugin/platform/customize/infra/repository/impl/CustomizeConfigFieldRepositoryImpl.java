package org.hzero.plugin.platform.customize.infra.repository.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.customize.dto.CustomizeConfigField;
import org.hzero.boot.customize.dto.ModelFieldMetaData;
import org.hzero.boot.customize.dto.ModelMetaData;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.hzero.core.redis.RedisHelper;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.api.dto.ConfigFieldLovDTO;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.api.dto.FieldConValidTlDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitRelatedDTO;
import org.hzero.plugin.platform.customize.domain.entity.*;
import org.hzero.plugin.platform.customize.domain.repository.*;
import org.hzero.plugin.platform.customize.infra.common.ConfigLocalCache;
import org.hzero.plugin.platform.customize.infra.common.ModelLocalCache;
import org.hzero.plugin.platform.customize.infra.common.UnitLocalCache;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeConfigFieldMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : peng.yu01@hand-china.com 2019/12/19 10:56
 */
@Component
public class CustomizeConfigFieldRepositoryImpl extends BaseRepositoryImpl<ConfigField> implements CustomizeConfigFieldRepository {

    private final Logger logger = LoggerFactory.getLogger(CustomizeConfigFieldRepositoryImpl.class);

    @Autowired
    private CustomizeConfigFieldMapper customizeConfigFieldMapper;
    @Autowired
    private CustomizeUnitRepository unitRepository;
    @Autowired
    private CustomizeModelRepository modelRepository;
    @Autowired
    private CustomizeModelFieldRepository modelFieldRepository;
    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private CustomizeFieldConditionHeaderRepository conditionHeaderRepository;
    @Autowired
    private CustomizeFieldConditionValidRepository conditionValidRepository;
    @Autowired
    private CustomizeConfigFieldParamRepository configFieldParamRepository;
    @Autowired
    private CustomizeUnitFieldRepository unitFieldRepository;
    @Autowired
    private CustomizeConfigFieldRepository configFieldRepository;
    @Autowired
    private LovAdapter lovAdapter;

    @Override
    public List<ConfigField> selectConfigFieldByUnitId(Long tenantId, Long unitId) {
        List<ConfigField> configFields = customizeConfigFieldMapper.selectConfigFieldByUnitId(unitId, tenantId);
        if (CollectionUtils.isEmpty(configFields)) {
            return Collections.emptyList();
        }
        //获取关联的参数配置
        List<Long> configFieldIds = configFields.stream().map(ConfigField::getConfigFieldId).collect(Collectors.toList());
        List<ConfigFieldParam> paramList = configFieldParamRepository.selectByConfigFieldIds(configFieldIds);
        Map<Long, List<ConfigFieldParam>> paramMap = new HashMap<>(configFields.size());
        if (CollectionUtils.isNotEmpty(paramList)) {
            UnitLocalCache unitLocalCache = new UnitLocalCache(unitFieldRepository);
            ConfigLocalCache configLocalCache = new ConfigLocalCache(configFieldRepository);
            ConfigFieldParam.translateParmaUnit(paramList, tenantId, unitLocalCache, configLocalCache);
            paramList.forEach(param -> paramMap.computeIfAbsent(param.getConfigFieldId(), v -> new ArrayList<>(12)).add(param));
        }
        configFields.forEach(field -> {
            if (paramMap.containsKey(field.getConfigFieldId())) {
                field.setParamList(paramMap.get(field.getConfigFieldId()));
            }
        });

        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        configFields.forEach(t -> {
            ConfigFieldWidget widget = t.getWidget();
            if (widget != null) {
                widget.translateLov(lovAdapter, tenantId);
            }
            if (t.isNotTableField()) {
                return;
            }
            ModelMetaData modelMetaData = modelLocalCache.getModel(t.getModelId());
            ModelFieldMetaData modelFieldMetaData = modelLocalCache.getModelField(t.getModelId(), t.getFieldId());

            ModelField modelField = ModelField.convertFormFieldMetadata(modelFieldMetaData);
            modelField.setModelCode(modelMetaData.getModelCode());
            modelField.setModelName(modelMetaData.getModelName());
            t.setField(modelField);
        });
        return configFields;
    }

    @Override
    public ConfigField selectById(Long id) {
        ConfigField configField = customizeConfigFieldMapper.selectById(id);
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        ModelFieldMetaData modelFieldMetaData = modelLocalCache.getModelField(configField.getModelId(), configField.getFieldId());
        ModelField modelField = new ModelField();
        modelField.setFieldCategory(modelFieldMetaData.getFieldCategory());
        modelField.setFieldCode(modelFieldMetaData.getFieldCode());
        configField.setField(modelField);
        return configField;
    }

    @Override
    public List<ConfigFieldLovDTO> selectSimpleConfigFieldByUnitId(Long tenantId, Long unitId, FieldCommonSearchDTO searchDTO) {
        List<ConfigFieldLovDTO> configFieldLovDTOS = customizeConfigFieldMapper.selectSimpleByUnitId(tenantId, unitId, searchDTO);
        if (CollectionUtils.isEmpty(configFieldLovDTOS)) {
            return Collections.emptyList();
        }
        //处理模型数据
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        configFieldLovDTOS.forEach(t -> {
            ModelField modelField = ModelField.convertFormFieldMetadata(modelLocalCache.getModelField(t.getModelId(), t.getFieldId()));
            t.setFieldCategory(modelField.getFieldCategory());
            t.setFieldType(modelField.getFieldType());
        });
        return configFieldLovDTOS;
    }


    @Override
    public void cacheDel(Long tenantId, String unitCode) {
        Assert.notNull(tenantId, "tenantId can not be null!");
        Assert.notNull(unitCode, "unitCode can not be null!");
        redisHelper.delKey(ConfigField.cacheKey(tenantId, unitCode));
    }

    @Override
    public ConfigField getConfigFieldCache(Long tenantId, String unitCode, Long fieldId) {
        Assert.notNull(tenantId, "tenantId can not be null!");
        Assert.notNull(unitCode, "unitCode can not be null!");
        Assert.notNull(fieldId, "fieldId can not be null!");

        String cache = redisHelper.hshGet(ConfigField.cacheKey(tenantId, unitCode), String.valueOf(fieldId));
        if (StringUtils.isNotEmpty(cache)) {
            return redisHelper.fromJson(cache, ConfigField.class);
        }
        return null;
    }

    @Override
    public void delConfigFieldCache(ConfigField field) {
        Assert.notNull(field.getTenantId(), "tenantId can not be null!");
        Assert.notNull(field.getUnitCode(), "unitCode can not be null!");
        redisHelper.delKey(ConfigField.cacheKey(field.getTenantId(), field.getUnitCode()));
    }

    @Override
    public Map<String, ConfigField> getConfigFieldsCacheMap(Long tenantId, String unitCode) {
        Assert.notNull(tenantId, "tenantId can not be null!");
        Assert.notNull(unitCode, "unitId can not be null!");
        String cacheKey = ConfigField.cacheKey(tenantId, unitCode);
        Map<String, String> cache = redisHelper.hshGetAll(cacheKey);
        if (MapUtils.isNotEmpty(cache)) {
            if (cache.size() == 1 && cache.containsKey(CustomizeConstants.NILL)) {
                return new HashMap<>(32);
            } else {
                return ConfigField.translateMap(cache.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey
                        , entry -> redisHelper.fromJson(entry.getValue(), ConfigField.class))));
            }
        }
        //缓存中不存在时查询数据库,查询结果包含字段映射配置、字段参数配置、字段条件配置、高级校验配置
        Map<String, ConfigField> configFieldMap = prepareCacheData(tenantId, unitCode);
        if (MapUtils.isNotEmpty(configFieldMap)) {
            redisHelper.hshPutAll(cacheKey, configFieldMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey
                    , v -> redisHelper.toJson(v.getValue().cacheConvert()))));
        } else {
            //数据库不存在时，防止之后重复查询数据库
            Map<String, String> map = new HashMap<>(1);
            map.put(CustomizeConstants.NILL, CustomizeConstants.NILL);
            redisHelper.hshPutAll(cacheKey, map);
        }
        return ConfigField.translateMap(configFieldMap);
    }

    private Map<String, ConfigField> prepareCacheData(Long tenantId, String unitCode) {
        List<ConfigField> configFields = this.selectByUnitCode(tenantId, unitCode);

        if (CollectionUtils.isEmpty(configFields)) {
            return new HashMap<>(32);
        }
        List<Long> configFieldIds = configFields.stream().map(ConfigField::getConfigFieldId).collect(Collectors.toList());
        //查询条件配置,包含高级校验配置
        List<FieldConditionHeader> headers = conditionHeaderRepository.selectWithLineByFieldId(configFieldIds, null, true);
        Map<Long, List<FieldConditionHeader>> headerMap = new HashMap<>(configFieldIds.size());
        Map<Long, List<FieldConditionValid>> conValidMap = new HashMap<>(configFieldIds.size());
        if (CollectionUtils.isNotEmpty(headers)) {
            headers.forEach(header -> {
                if (StringUtils.isNotEmpty(header.getConType()) && header.getConType().equalsIgnoreCase(CustomizeConstants.ConditionType.VALID)) {
                    List<FieldConditionValid> valids = conValidMap.computeIfAbsent(header.getConfigFieldId(), key -> new ArrayList<>());
                    if(CollectionUtils.isNotEmpty(header.getValids())) {
                        valids.addAll(header.getValids());
                    }
                }
                headerMap.computeIfAbsent(header.getConfigFieldId(), key -> new ArrayList<>(64)).add(header);
            });
        }
        //设置多语言
        if (MapUtils.isNotEmpty(conValidMap)) {
            List<Long> validIds = new ArrayList<>(128);
            conValidMap.values().forEach(conValids -> validIds.addAll(conValids.stream().map(FieldConditionValid::getConValidId).collect(Collectors.toList())));
            if (CollectionUtils.isNotEmpty(validIds)) {
                List<FieldConValidTlDTO> validTls = conditionValidRepository.selectValidTl(validIds);
                if (CollectionUtils.isNotEmpty(validTls)) {
                    validTls.forEach(tl -> {
                        List<FieldConditionValid> valids = conValidMap.get(tl.getConfigFieldId());
                        if (CollectionUtils.isEmpty(valids)) {
                            return;
                        }
                        for (FieldConditionValid valid : valids) {
                            if (valid.getConValidId().equals(tl.getConValidId())) {
                                valid.setTlMaps(tl.getTlMaps());
                                break;
                            }
                        }
                    });
                }
            }
        }
        //查询租户个性化关联的参数配置
        List<ConfigFieldParam> configParamList = configFieldParamRepository.selectByConfigFieldIds(configFieldIds);
        Map<Long, List<ConfigFieldParam>> configFieldParamMap = new HashMap<>(32);
        if (CollectionUtils.isNotEmpty(configParamList)) {
            configParamList.forEach(param -> {
                List<ConfigFieldParam> fieldParams = configFieldParamMap.computeIfAbsent(param.getConfigFieldId(), key ->
                        new ArrayList<>(12));
                fieldParams.add(param);
            });
        }
        Map<String, ConfigField> configFieldMap = new HashMap<>(configFields.size());
        configFields.forEach(t -> {
            //添加条件及高级校验配置
            if (MapUtils.isNotEmpty(headerMap) && headerMap.containsKey(t.getConfigFieldId())) {
                List<FieldConditionHeader> headerList = headerMap.get(t.getConfigFieldId());
                List<FieldConditionHeader> newHeaderList = new ArrayList<>(headerList.size());
                boolean isValid = true;
                for (FieldConditionHeader header : headerList) {
                    //高级校验
                    if (isValid && conValidMap.containsKey(header.getConfigFieldId()) && header.getConType().equalsIgnoreCase(CustomizeConstants.ConditionType.VALID)) {
                        header.setValids(conValidMap.get(header.getConfigFieldId()));
                        t.setConValid(header);
                        isValid = false;
                        continue;
                    } else {
                        newHeaderList.add(header);
                    }
                }
                t.setConditionHeaders(newHeaderList);
            }
            //添加参数
            if (configFieldParamMap.containsKey(t.getConfigFieldId())) {
                t.setParamList(configFieldParamMap.get(t.getConfigFieldId()));
            }
            configFieldMap.put(t.cacheHashKey(), t.cacheConvert());
        });
        return configFieldMap;
    }

    @Override
    public List<Map<String, String>> selectFieldTL(Long configFieldId) {
        return customizeConfigFieldMapper.selectFieldTL(configFieldId);
    }

    @Override
    public List<ConfigField> selectByUnitIdAndFieldId(ConfigField field) {
        Assert.notNull(field.getFieldId(), "fieldId can not be null!");
        return customizeConfigFieldMapper.selectByUnitIdAndFieldId(field);
    }

    @Override
    public List<ConfigField> selectByUnitIdAndTenantId(Long tenantId, Collection<Long> unitIds) {
        return customizeConfigFieldMapper.selectByUnitIdAndTenantId(tenantId, unitIds);
    }

    @Override
    public List<UnitRelatedDTO> selectWithWdgByUnitId(Collection<String> unitCodes, Long tenantId) {
        return customizeConfigFieldMapper.selectRelatedUnitAndField(unitCodes, tenantId);
    }

    @Override
    public List<ConfigField> selectAllUnitIdAndTenant() {
        return customizeConfigFieldMapper.selectAllUnitIdAndTenant();
    }


    @Override
    public List<ConfigField> selectByUnitCode(Long tenantId, String unitCode) {
        return customizeConfigFieldMapper.selectWithModelFieldByUnitCode(tenantId, unitCode);
    }

    @Override
    public int updateFieldAliasByUnitField(UnitField unitField) {
        return customizeConfigFieldMapper.updateFieldAliasByUnitField(unitField);
    }

    @Override
    public List<ConfigField> selectConfigFieldAndUnitCode(Collection<Long> configFieldIds) {
        return customizeConfigFieldMapper.selectConfigFieldAndUnitCode(configFieldIds);
    }

    @Override
    public List<CustomizeConfigField> selectConfigModelFieldsByUnitId(Long tenantId, Long unitId) {
        return customizeConfigFieldMapper.selectConfigModelFieldsByUnitId(tenantId, unitId);
    }


    @Override
    public List<ConfigField> selectUserConfigField(ConfigField field) {
        Assert.notNull(field.getUnitId(), "unitId can not be null! ");
        Assert.notNull(field.getUserId(), "userId can not be null! ");
        Assert.notNull(field.getTenantId(), "tenantId can not be null !");

        return customizeConfigFieldMapper.selectUserConfigField(field);
    }

    public void cacheUserConfigField(Long tenantId, Long userId, String unitCode, Map<String, ConfigField> configFields) {
        Assert.notNull(userId, "userId can not be null !");
        Assert.notNull(tenantId, "tenantId can not be null!");
        Assert.notNull(unitCode, "unitCode can not be null!");
        String cacheKey = ConfigField.cacheKey(tenantId, userId, unitCode);
        redisHelper.delKey(cacheKey);
        redisHelper.hshPutAll(cacheKey, configFields.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> redisHelper.toJson(entry.getValue()))));
    }

    @Override
    public void deleteUserConfigField(ConfigField configField) {
        Assert.notNull(configField.getUnitId(), "unitId can not be null! ");
        Assert.notNull(configField.getUserId(), "userId can not be null! ");
        Assert.notNull(configField.getTenantId(), "tenantId can not be null !");

        customizeConfigFieldMapper.delete(configField);
    }

    @Override
    public void deleteConfigFieldTl(List<Long> configFieldIds) {
        customizeConfigFieldMapper.deleteConfigFieldTl(configFieldIds);
    }

    @Override
    public Map<String, ConfigField> getUserConfigFieldCache(Long tenantId, Long userId, String unitCode) {
        Assert.notNull(userId, "userId can not be null !");
        Assert.notNull(tenantId, "tenantId can not be null!");
        Assert.notNull(unitCode, "unitCode can not be null!");
        String cacheKey = ConfigField.cacheKey(tenantId, userId, unitCode);
        Map<String, String> cache = redisHelper.hshGetAll(cacheKey);
        if (MapUtils.isNotEmpty(cache)) {
            if (cache.size() == 1 && cache.containsKey(CustomizeConstants.NILL)) {
                return Collections.emptyMap();
            }
            return ConfigField.translateMap(cache.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
                    redisHelper.fromJson(entry.getValue(), ConfigField.class))));
        }

        Unit unit = unitRepository.getUnitCache(unitCode);

        ConfigField userSearch = new ConfigField();
        userSearch.setTenantId(tenantId);
        userSearch.setUnitId(unit.getId());
        userSearch.setUserId(userId);
        //从数据库里查询
        List<ConfigField> configFields = this.selectUserConfigField(userSearch);
        if (CollectionUtils.isEmpty(configFields)) {
            Map<String, String> map = new HashMap<>(1);
            map.put(CustomizeConstants.NILL, CustomizeConstants.NILL);
            redisHelper.hshPutAll(cacheKey, map);
            return Collections.emptyMap();
        }

        Map<String, ConfigField> userConfigFields = configFields.stream().collect(Collectors.toMap(ConfigField::cacheHashKey,
                ConfigField::cacheConvert));
        cacheUserConfigField(tenantId, userId, unitCode, userConfigFields);
        return ConfigField.translateMap(userConfigFields);
    }

    @Override
    public void delUserConfigFieldCache(Long tenantId, Long userId, String unitCode) {
        Assert.notNull(userId, "userId can not be null !");
        Assert.notNull(tenantId, "tenantId can not be null!");
        Assert.notNull(unitCode, "unitCode can not be null!");
        String cacheKey = ConfigField.cacheKey(tenantId, userId, unitCode);
        redisHelper.delKey(cacheKey);
    }

}
