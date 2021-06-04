package org.hzero.plugin.platform.customize.app.service;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;

import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 13:38
 */
public interface CustomizeModelFieldService {

    /**
     * 查询指定模型下的所有模型字段
     *
     * @param searchDTO   查询条件
     * @param pageRequest 分页参数
     * @return 模型字段列表
     */
    List<ModelField> selectFieldByModelId(FieldCommonSearchDTO searchDTO, PageRequest pageRequest);

    /**
     * 创建模型字段
     * 现阶段只能创建虚拟字段
     *
     * @param modelField
     * @return
     */
    ModelField createField(ModelField modelField);

    /**
     * 根据主键修改模型字段
     *
     * @param field 修改参数
     * @return
     */
    ModelField updateFieldById(ModelField field);

    /**
     * 创建模型时插入表中的字段
     *
     * @param modelFields 表中字段数据
     * @param modelId     模型ID
     */
    void insertFieldFromDb(List<ModelField> modelFields, Long modelId);

    /**
     * 同步指定模型字段
     *
     * @param serviceName 模型所属服务
     * @param tableName   模型关联的表名
     * @param modelId     模型ID
     * @return 同步后的模型字段列表
     */
    List<ModelField> syncField(String serviceName, String tableName, Long modelId);

    /**
     * 批量删除
     *
     * @param fieldId 需要删除的字段ID
     */
    void deleteField(Long fieldId);

    /**
     * 查询指定模型下未配置的模型字段
     *
     * @param searchDTO   过滤条件
     * @param tenantId    租户ID
     * @param unitId      单元ID
     * @param pageRequest 分页参数  暂不能分页
     * @return
     */
    Page<ModelField> selectUnConfigFieldByModelId(FieldCommonSearchDTO searchDTO, Long tenantId, Long unitId, PageRequest pageRequest);

    /**
     * 翻译字段类型（fieldType）、字段类别（fieldCategory）、组件类型（fieldWidget）
     *
     * @param modelFields
     * @return
     */
    List<ModelField> translationLov(List<ModelField> modelFields);

    /**
     * 更新组件配置
     *
     * @param field
     */
    void saveWidget(ModelField field);

}
