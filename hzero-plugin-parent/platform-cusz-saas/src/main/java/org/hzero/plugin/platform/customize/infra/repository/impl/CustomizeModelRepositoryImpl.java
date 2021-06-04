package org.hzero.plugin.platform.customize.infra.repository.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hzero.boot.customize.dto.ModelMetaData;
import org.hzero.core.redis.RedisHelper;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.domain.entity.Model;
import org.hzero.plugin.platform.customize.domain.entity.ModelObjectPub;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelRepository;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 11:14
 */
@Component
public class CustomizeModelRepositoryImpl extends BaseRepositoryImpl<ModelObjectPub> implements CustomizeModelRepository {

    @Autowired
    private CustomizeModelMapper customizeModelMapper;
    @Autowired
    private RedisHelper redisHelper;

    @Override
    public List<Model> selectByCondition(Model model) {
        return customizeModelMapper.selectByOption(model);
    }

    @Override
    public void cacheModel(Model model) {
        redisHelper.hshPut(generateKey(), String.valueOf(model.getModelId()), redisHelper.toJson(model.conversionModel()));
    }

    @Override
    public void delCache(Model model) {
        redisHelper.hshDelete(generateKey(), String.valueOf(model.getModelId()));
    }

    @Override
    public ModelMetaData getModelCache(Long modelId) {
        String json = redisHelper.hshGet(generateKey(), String.valueOf(modelId));
        if(StringUtils.isEmpty(json)){
            return new ModelMetaData();
        }
        return redisHelper.fromJson(json, ModelMetaData.class);
    }

    @Override
    public void initModelCache() {
        ModelObjectPub modelObjectPub = new ModelObjectPub();
        modelObjectPub.setAppId(-1L);
        List<ModelObjectPub> all = this.select(modelObjectPub);
        if (CollectionUtils.isEmpty(all)) {
            return;
        }
        Map<String, String> modelMap = all.stream().collect(Collectors.toMap(k -> (String.valueOf(k.getId())),
                v -> redisHelper.toJson(Model.convertFromModelObject(v))));

        redisHelper.hshPutAll(generateKey(), modelMap);
    }

    protected String generateKey() {
        return ModelMetaData.MODEL_CACHE_KEY;
    }
}
