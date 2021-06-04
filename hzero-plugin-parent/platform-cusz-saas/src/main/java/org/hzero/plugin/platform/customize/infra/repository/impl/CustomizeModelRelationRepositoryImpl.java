package org.hzero.plugin.platform.customize.infra.repository.impl;

import java.util.List;

import org.hzero.boot.customize.dto.ModelMetaData;
import org.hzero.core.redis.RedisHelper;
import org.hzero.plugin.platform.customize.domain.entity.BaseModelRelationField;
import org.hzero.plugin.platform.customize.domain.entity.ModelRelation;
import org.hzero.plugin.platform.customize.domain.entity.ModelRelationFieldPub;
import org.hzero.plugin.platform.customize.domain.entity.ModelRelationPub;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelRelationRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelRepository;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeModelRelationMapper;
import org.hzero.plugin.platform.customize.infra.mapper.ModelRelationFieldPubMapper;
import org.hzero.plugin.platform.customize.infra.mapper.ModelRelationPubMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : peng.yu01@hand-china.com 2019/12/13 14:02
 */
@Component
public class CustomizeModelRelationRepositoryImpl implements CustomizeModelRelationRepository {

    @Autowired
    private CustomizeModelRelationMapper relationMapper;
    @Autowired
    private ModelRelationPubMapper relationPubMapper;
    @Autowired
    private ModelRelationFieldPubMapper relationFieldPubMapper;
    @Autowired
    private CustomizeModelRepository customizeModelRepository;

    @Autowired
    private RedisHelper redisHelper;

    @Override
    public List<ModelRelation> selectAllRelations() {
        return relationMapper.selectAllRelations();
    }

    @Override
    public void insertRelation(ModelRelationPub modelRelationPub) {
        relationPubMapper.insertSelective(modelRelationPub);
        for (BaseModelRelationField modelRelationFieldPub : modelRelationPub.getModelRelationFields()) {
            modelRelationFieldPub.setRelationId(modelRelationPub.getId());
            relationFieldPubMapper.insertSelective((ModelRelationFieldPub) modelRelationFieldPub);
        }
    }

    @Override
    public List<ModelRelation> selectRelations(Long modelId) {
        ModelMetaData modelMetaData = customizeModelRepository.getModelCache(modelId);
        return relationMapper.selectRelations(modelMetaData.getModelCode());
    }

    @Override
    public void cacheRelation(ModelRelation relation) {
        redisHelper.hshPut(ModelRelation.CACHE_KEY_PREFIX + relation.getMasterModelId().toString(), relation.getSlaveModelId().toString(), redisHelper.toJson(relation));
        redisHelper.hshPut(ModelRelation.CACHE_KEY_PREFIX + relation.getSlaveModelId().toString(), relation.getMasterModelId().toString(), redisHelper.toJson(relation));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRelation(Long relationId) {
        relationPubMapper.deleteByPrimaryKey(relationId);
        ModelRelationFieldPub modelRelationFieldPub = new ModelRelationFieldPub();
        modelRelationFieldPub.setRelationId(relationId);
        relationFieldPubMapper.delete(modelRelationFieldPub);
        return 1;
    }

}
