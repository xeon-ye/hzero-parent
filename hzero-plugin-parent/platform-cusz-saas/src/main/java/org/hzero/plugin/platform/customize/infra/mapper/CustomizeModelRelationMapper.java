package org.hzero.plugin.platform.customize.infra.mapper;

import java.util.List;

import org.hzero.plugin.platform.customize.domain.entity.ModelRelation;

/**
 * @author : peng.yu01@hand-china.com 2019/12/13 14:01
 */
public interface CustomizeModelRelationMapper {

    /**
     * 查询与指定模型关联的模型关系
     *
     * @param modelCode 模型ID，可能为主模型ID或从模型ID
     * @return
     */
    List<ModelRelation> selectRelations(String modelCode);

    /**
     * 查询所有模型关联的模型关系
     *
     * @return
     */
    List<ModelRelation> selectAllRelations();

}
