package org.hzero.plugin.platform.customize.app.service;

import java.util.List;

import org.hzero.boot.customize.dto.TableMetaData;
import org.hzero.plugin.platform.customize.domain.entity.Model;
import org.hzero.plugin.platform.customize.domain.entity.ModelRelation;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 11:10
 */
public interface CustomizeModelService {

    /**
     * 创建模型
     *
     * @param model 模型参数
     * @return
     */
    Model createModel(Model model);

    /**
     * 通过服务名获取该服务下的所有表名
     *
     * @param serviceName 服务名
     * @return 关联的表名列表
     */
    List<TableMetaData> getTables(String serviceName);

    /**
     * 获取指定单元关联的模型及模型关联的模型列表
     *
     * @param unitId  单元ID
     * @param modelId 模型ID
     * @return
     */
    List<Model> selectAssociatedModels(Long unitId, Long modelId);

    /**
     * 更新模型名称
     *
     * @param model
     * @return
     */
    Model updateByOptions(Model model);

    /**
     * 根据模型ID获取关联的模型关系，包括作为主模型和从模型
     *
     * @param modelId 模型ID
     * @return
     */
    List<ModelRelation> selectAssociateRelation(Long modelId);

    /**
     * 创建模型关系
     *
     * @param relation
     * @return
     */
    ModelRelation createRelation(ModelRelation relation);

}
