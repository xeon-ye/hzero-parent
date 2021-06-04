package org.hzero.plugin.platform.customize.infra.repository.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.hzero.boot.customize.dto.ModelFieldMetaData;
import org.hzero.boot.customize.dto.ModelMetaData;
import org.hzero.core.redis.RedisHelper;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.api.dto.ConfigFieldLovDTO;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.domain.entity.*;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelFieldRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelFieldWidgetRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeUnitFieldRepository;
import org.hzero.plugin.platform.customize.infra.common.ModelLocalCache;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeUnitFieldMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author : peng.yu01@hand-china.com 2019/12/21 11:01
 */
@Component
public class CustomizeUnitFieldRepositoryImpl extends BaseRepositoryImpl<UnitField> implements CustomizeUnitFieldRepository {

    private final Logger logger = LoggerFactory.getLogger(CustomizeUnitFieldRepositoryImpl.class);

    public static final String UNIT_FIELD_SELECT_ALL_SQL_ID = "org.hzero.plugin.platform.customize.infra.mapper.CustomizeUnitFieldMapper.selectAllUnitField";

    @Autowired
    private CustomizeUnitFieldMapper customizeUnitFieldMapper;
    @Autowired
    private CustomizeModelRepository modelRepository;
    @Autowired
    private CustomizeModelFieldRepository modelFieldRepository;
    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private SqlSessionFactory sqlSessionFactory;
    @Autowired
    private CustomizeModelFieldWidgetRepository modelFieldWidgetRepository;

    @Override
    public List<ConfigField> selectUnitFieldByUnitId(Long unitId) {
        List<ConfigField> configFields = customizeUnitFieldMapper.selectByUnitId(unitId);
        if (CollectionUtils.isEmpty(configFields)) {
            return Collections.emptyList();
        }
        //处理模型数据
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        configFields.forEach(t -> {
            if (t.isNotTableField()) {
                return;
            }
            ModelField modelField = ModelField.convertFormFieldMetadata(modelLocalCache.getModelField(t.getModelId(), t.getFieldId()));
            ModelMetaData model = modelLocalCache.getModel(t.getModelId());
            modelField.setModelCode(model.getModelCode());
            modelField.setModelName(model.getModelName());
            t.setField(modelField);
        });


        return configFields;
    }

    @Override
    public List<ConfigFieldLovDTO> selectSimpleUnitFieldByUnitId(Long unitId, FieldCommonSearchDTO searchDTO) {
        List<ConfigFieldLovDTO> dtos = customizeUnitFieldMapper.selectSimpleByUnitId(unitId, searchDTO);
        if (CollectionUtils.isEmpty(dtos)) {
            return Collections.emptyList();
        }
        //处理模型数据
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        dtos.forEach(t -> {
            ModelField modelField = ModelField.convertFormFieldMetadata(modelLocalCache.getModelField(t.getModelId(), t.getFieldId()));
            t.setFieldCategory(modelField.getFieldCategory());
            t.setFieldType(modelField.getFieldType());
        });
        return dtos;
    }

    @Override
    public List<UnitField> selectUnitFieldsByUnitId(Long unitId) {
        List<UnitField> unitFields = customizeUnitFieldMapper.selectWithParamByUnitId(unitId);
        if (CollectionUtils.isEmpty(unitFields)) {
            return Collections.emptyList();
        }
        Map<Long, ModelFieldWidget> fieldWidgetMap = new HashMap<>(128);
        Set<Long> fieldIds = unitFields.stream().filter(field -> !field.isNotTableField()).map(UnitField::getFieldId).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(fieldIds)) {
            //获取字段的组件配置
            List<ModelFieldWidget> fieldWidgets = modelFieldWidgetRepository.selectByFieldIds(fieldIds);
            if (CollectionUtils.isNotEmpty(fieldWidgets)) {
                fieldWidgetMap = fieldWidgets.stream().collect(Collectors.toMap(ModelFieldWidget::getFieldId, v -> v));
            }
        }
        Map<Long, ModelFieldWidget> finalFieldWidgetMap = fieldWidgetMap;

        //处理数据
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        unitFields.forEach(t -> {
            ModelMetaData metaData = modelLocalCache.getModel(t.getModelId());
            ModelFieldMetaData fieldMetaData = modelLocalCache.getModelField(t.getModelId(), t.getFieldId());
            ModelField field = new ModelField();
            field.setFieldCode(fieldMetaData.getFieldCode());
            field.setFieldCategory(fieldMetaData.getFieldCategory());
            field.setFieldType(fieldMetaData.getFieldType());
            Model model = new Model();
            field.setModelCode(metaData.getModelCode());
            field.setModelName(metaData.getModelName());
            model.setModelTable(metaData.getModelTable());
            model.setSupportMultiLang(Boolean.TRUE.equals(metaData.getSupportMultiLang()) ? 1 : 0);
            model.setModelId(t.getModelId());
            field.setModel(model);
            t.setField(field);
            if (StringUtils.isEmpty(t.getFieldName())) {
                t.setFieldName(fieldMetaData.getFieldName());
            }
            //设置组件
            if (finalFieldWidgetMap.containsKey(t.getFieldId())) {
                t.getField().setWdg(finalFieldWidgetMap.get(t.getFieldId()));
            }
        });
        return unitFields;
    }

    @Override
    public List<ModelField> selectNotConfigField(Long unitId, FieldCommonSearchDTO searchDTO) {
        return customizeUnitFieldMapper.selectNotConfigField(unitId, searchDTO);
    }

    @Override
    public void cacheUnitField(UnitField unitField) {
        Assert.notNull(unitField.getFieldId(), "fieldId can not be null!");
        Assert.notNull(unitField.getUnitCode(), "unitCode can not be null!");
        //缓存操作列等非实体字段key = fieldAlias
        redisHelper.hshPut(UnitField.cacheKey(unitField.getUnitCode()), unitField.cacheHashKey(), redisHelper.toJson(unitField.cacheConvert()));
    }


    @Override
    public void delUnitFieldCache(String unitCode, String fieldId) {
        Assert.notNull(fieldId, "fieldId can not be null!");
        Assert.notNull(unitCode, "unitCode can not be null!");
        redisHelper.hshDelete(UnitField.cacheKey(unitCode), fieldId);
    }

    @Override
    public Map<String, UnitField> getUnitFieldsCacheMap(String unitCode) {
        Assert.notNull(unitCode, "unitCode can not be null!");
        Map<String, String> cache = redisHelper.hshGetAll(UnitField.cacheKey(unitCode));
        if (MapUtils.isNotEmpty(cache)) {
            return UnitField.translateMap(cache.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                    v -> redisHelper.fromJson(v.getValue(), UnitField.class))));
        }
        return Collections.emptyMap();
    }

    /**
     * TODO : 后期以单元组分类
     */
    @Override
    public void initUnitFieldCache() {

        logger.info("Cache UnitField : start");
        List<UnitField> allResult = customizeUnitFieldMapper.selectAllUnitField();
        if (CollectionUtils.isEmpty(allResult)) {
            return;
        }
        Map<String, Map<String, String>> unitFieldGroup = new HashMap<>(allResult.size() / 5);
        allResult.forEach(field -> {
            Map<String, String> innerMap = unitFieldGroup.computeIfAbsent(field.getUnitCode(), v -> new HashMap<>(64));
            innerMap.put(field.cacheHashKey(), redisHelper.toJson(field.cacheConvert()));
        });
        //先删除后插入
        unitFieldGroup.forEach((k, v) -> {
            redisHelper.delKey(UnitField.cacheKey(k));
            redisHelper.hshPutAll(UnitField.cacheKey(k), v);
        });
        logger.info("Cache UnitField : finished");
    }

    @Override
    public UnitField selectWithUnitCodeById(Long unitFieldId) {
        return customizeUnitFieldMapper.selectWithUnitCodeById(unitFieldId);
    }

    @Override
    public List<UnitField> selectByUnitCode(String unitCode) {
        return customizeUnitFieldMapper.selectByUnitCode(unitCode);
    }

}
