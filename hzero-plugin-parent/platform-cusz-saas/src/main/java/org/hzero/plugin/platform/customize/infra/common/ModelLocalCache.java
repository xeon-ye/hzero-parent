package org.hzero.plugin.platform.customize.infra.common;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.hzero.boot.customize.dto.ModelFieldMetaData;
import org.hzero.boot.customize.dto.ModelMetaData;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelFieldRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelRepository;
import org.springframework.util.Assert;

/**
 * 模型以及模型字段本地缓存，需要每次new
 *
 * @author xiangyu.qi01@hand-china.com on 2020-01-14.
 */
public class ModelLocalCache {

    private ModelMetaData NULL_MODEL = new ModelMetaData();

    private ModelFieldMetaData NULL_FIELD = new ModelFieldMetaData();

    public ModelLocalCache(CustomizeModelRepository modelRepository, CustomizeModelFieldRepository modelFieldRepository) {
        Assert.notNull(modelRepository, "must set modelRepository");
        Assert.notNull(modelFieldRepository, "must set modelFieldRepository");
        this.modelRepository = modelRepository;
        this.modelFieldRepository = modelFieldRepository;
    }

    private CustomizeModelRepository modelRepository;

    private CustomizeModelFieldRepository modelFieldRepository;

    private Map<Long, ModelMetaData> modelCache = new HashMap<>(4);
    private Map<Long, Map<Long, ModelFieldMetaData>> modelFieldCache = new HashMap<>(4);


    public ModelMetaData getModel(Long modelId) {
        if (modelId == -1) {
            return NULL_MODEL;
        }
        return modelCache.computeIfAbsent(modelId, key -> modelRepository.getModelCache(key));
    }

    public ModelFieldMetaData getModelField(Long modelId, Long fieldId) {
        if (modelId == -1 && fieldId == -1) {
            return NULL_FIELD;
        }
        Map<Long, ModelFieldMetaData> modelFieldMetaDataMap = modelFieldCache.computeIfAbsent(modelId,
                key -> modelFieldRepository.getFieldCacheList(modelId).stream().collect(Collectors.toMap(ModelFieldMetaData::getFieldId, v -> v)));
        if(!modelFieldMetaDataMap.containsKey(fieldId)){
            return NULL_FIELD;
        }
        return modelFieldMetaDataMap.get(fieldId);
    }

}
