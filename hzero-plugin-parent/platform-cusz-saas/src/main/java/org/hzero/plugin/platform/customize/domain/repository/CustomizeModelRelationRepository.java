package org.hzero.plugin.platform.customize.domain.repository;

import java.util.List;

import org.hzero.plugin.platform.customize.domain.entity.ModelRelation;
import org.hzero.plugin.platform.customize.domain.entity.ModelRelationPub;

/**
 * @author : peng.yu01@hand-china.com 2019/12/13 14:02
 */
public interface CustomizeModelRelationRepository {

    /**
     * 查询所有模型关系
     *
     * @return
     */
    List<ModelRelation> selectAllRelations();


    /**
     * 插入模型关系
     *
     * @param modelRelationPub
     */
    void insertRelation(ModelRelationPub modelRelationPub);

    /**
     * 查询出 masterModelId=modelId 或者 slaveModelId=modelId 的列表
     *
     * @param modelId 模型ID
     * @return
     */
    List<ModelRelation> selectRelations(Long modelId);

    /**
     * 缓存模型关系
     *
     * @param relation
     */
    void cacheRelation(ModelRelation relation);

    /**
     * 删除模型关系
     *
     * @param relationId
     * @return
     */
    int deleteRelation(Long relationId);

}
