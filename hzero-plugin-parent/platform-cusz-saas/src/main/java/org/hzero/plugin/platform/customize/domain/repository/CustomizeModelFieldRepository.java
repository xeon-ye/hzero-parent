package org.hzero.plugin.platform.customize.domain.repository;

import java.util.List;

import org.hzero.boot.customize.dto.ModelFieldMetaData;
import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;
import org.hzero.plugin.platform.customize.domain.entity.ModelFieldPub;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 13:40
 */
public interface CustomizeModelFieldRepository extends BaseRepository<ModelFieldPub> {

    /**
     * 查询模型下所有模型字段及模型字段组件
     *
     * @param searchDTO 查询条件
     * @return 模型字段列表，包含字段组件信息
     */
    List<ModelField> selectFieldWithWdg(FieldCommonSearchDTO searchDTO);

    /**
     * 查询指定模型下未配置的模型字段
     *
     * @param searchDTO 查询条件
     * @param tenantId  租户ID
     * @param unitId    单元ID
     * @return
     */
    List<ModelField> selectUnConfigFieldByModelId(FieldCommonSearchDTO searchDTO, Long tenantId, Long unitId);

    /**
     * 更新缓存
     *
     * @param field
     */
    void cacheModelField(ModelField field);

    /**
     * 批量缓存
     *
     * @param fieldList
     */
    void cacheModelFieldList(List<ModelField> fieldList);

    /**
     * 删除执行模型的指定字段缓存
     *
     * @param field
     */
    void delCaChe(ModelField field);

    /**
     * 删除指定模型的所有模型字段缓存
     *
     * @param field
     */
    void delCacheList(ModelField field);

    /**
     * 获取指定模型的所有模型字段缓存
     *
     * @param modelId
     * @return
     */

    List<ModelFieldMetaData> getFieldCacheList(Long modelId);

    /**
     * 获取指定模型的指定模型字段的缓存
     *
     * @param field
     * @return
     */
    ModelFieldMetaData getFieldCache(ModelField field);

    /**
     * 初始化缓存
     */
    void initModelFieldCache();
}
