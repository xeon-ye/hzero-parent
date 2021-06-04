package org.hzero.plugin.platform.customize.infra.repository.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.hzero.boot.customize.dto.ModelFieldMetaData;
import org.hzero.boot.customize.util.JdbcToJava;
import org.hzero.core.redis.RedisHelper;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;
import org.hzero.plugin.platform.customize.domain.entity.ModelFieldPub;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelFieldRepository;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeModelFieldMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 13:48
 */
@Component
public class CustomizeModelFieldRepositoryImpl extends BaseRepositoryImpl<ModelFieldPub> implements CustomizeModelFieldRepository {

    public static final String MODEL_FIELD_SELECT_ALL_SQL_ID = "org.hzero.plugin.platform.customize.infra.mapper.CustomizeModelFieldMapper.selectAllFieldWithWdg";

    private final Logger logger = LoggerFactory.getLogger(CustomizeModelFieldRepositoryImpl.class);

    @Autowired
    private CustomizeModelFieldMapper customizeModelFieldMapper;
    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Override
    public List<ModelField> selectFieldWithWdg(FieldCommonSearchDTO searchDTO) {
        List<ModelField> fields = customizeModelFieldMapper.selectFieldAndWdg(searchDTO);
        for (ModelField field : fields) {
            field.setFieldType(JdbcToJava.convertJavaType(field.getFieldType()));
        }
        return fields;
    }

    @Override
    public List<ModelField> selectUnConfigFieldByModelId(FieldCommonSearchDTO searchDTO, Long tenantId, Long unitId) {
        List<ModelField> fields = customizeModelFieldMapper.selectUnConfigByModelId(searchDTO, tenantId, unitId);
        for (ModelField field : fields) {
            field.setFieldType(JdbcToJava.convertJavaType(field.getFieldType()));
        }
        return fields;
    }

    @Override
    public void cacheModelField(ModelField field) {
        redisHelper.hshPut(ModelField.generateKey(field.getModelId()), field.getFieldId().toString(), redisHelper.toJson(field.conversionField()));
    }

    @Override
    public void cacheModelFieldList(List<ModelField> fieldList) {
        if (CollectionUtils.isEmpty(fieldList)) {
            logger.warn("Cache for ModelField. The cache data is null.");
            return;
        }
        Map<String, Map<String, String>> cacheMap = new HashMap<>(128);
        fieldList.forEach(field -> {
            Map<String, String> innerMap = cacheMap.computeIfAbsent(ModelField.generateKey(field.getModelId()), (key) -> new HashMap<>(64));
            innerMap.putIfAbsent(field.getFieldId().toString(), redisHelper.toJson(field.conversionField()));
        });
        for (Map.Entry<String, Map<String, String>> entry : cacheMap.entrySet()) {
            redisHelper.hshPutAll(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void delCaChe(ModelField field) {
        redisHelper.hshDelete(ModelField.generateKey(field.getModelId()), field.getFieldId());
    }

    @Override
    public void delCacheList(ModelField field) {
        redisHelper.delKey(ModelField.generateKey(field.getModelId()));
    }

    @Override
    public List<ModelFieldMetaData>  getFieldCacheList(Long modelId) {
        List<ModelFieldMetaData> cache = new ArrayList<>(32);
        String key = ModelField.generateKey(modelId);
        Map<String, String> map = redisHelper.hshGetAll(key);
        if(MapUtils.isEmpty(map)){
            return cache;
        }
        map.values().forEach(item -> cache.add(redisHelper.fromJson(item, ModelFieldMetaData.class)));
        return cache;
    }

    @Override
    public ModelFieldMetaData getFieldCache(ModelField field) {
        String json = redisHelper.hshGet(ModelField.generateKey(field.getModelId()), field.getFieldId().toString());
        if(StringUtils.isBlank(json)){
            return new ModelFieldMetaData();
        }
        return redisHelper.fromJson(json, ModelFieldMetaData.class);
    }

    @Override
    public void initModelFieldCache() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            logger.info("Cache ModelField : finished");
            Map<String, Map<String, String>> cacheMap = new HashMap<>(128);
            sqlSession.select(MODEL_FIELD_SELECT_ALL_SQL_ID, (resultContext) -> {
                try {
                    ModelField field = (ModelField) resultContext.getResultObject();
                    Map<String, String> innerMap = cacheMap.computeIfAbsent(ModelField.generateKey(field.getModelId()), (key) -> new HashMap<>(64));
                    innerMap.putIfAbsent(field.getFieldId().toString(), redisHelper.toJson(field.conversionField()));
                }catch (Exception e){
                    logger.error("Cache ModelField ignore cause : ",e);
                }
            });
            for (Map.Entry<String, Map<String, String>> entry : cacheMap.entrySet()) {
                redisHelper.hshPutAll(entry.getKey(), entry.getValue());
            }
            logger.info("Cache ModelField : finished");
        }
    }
}
