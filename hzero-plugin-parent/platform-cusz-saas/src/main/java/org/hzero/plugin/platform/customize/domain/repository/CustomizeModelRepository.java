package org.hzero.plugin.platform.customize.domain.repository;

import java.util.List;

import org.hzero.boot.customize.dto.ModelMetaData;
import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.domain.entity.Model;
import org.hzero.plugin.platform.customize.domain.entity.ModelObjectPub;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 11:14
 */
public interface CustomizeModelRepository extends BaseRepository<ModelObjectPub> {

    /**
     * 分页-条件查询模型列表
     *
     * @return
     */
    List<Model> selectByCondition(Model model);

    /**
     * 更新缓存
     *
     * @param model
     */
    void cacheModel(Model model);

    /**
     * 删除缓存
     *
     * @param model
     */
    void delCache(Model model);

    /**
     * 获取缓存
     *
     * @param modelId
     * @return
     */
    ModelMetaData getModelCache(Long modelId);

    void initModelCache();

}
