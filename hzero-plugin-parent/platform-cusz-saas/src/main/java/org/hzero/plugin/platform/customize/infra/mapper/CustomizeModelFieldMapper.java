package org.hzero.plugin.platform.customize.infra.mapper;

import java.util.List;

import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 13:48
 */
public interface CustomizeModelFieldMapper {

    /**
     * 条件模糊查询 ，包含组件信息
     *
     * @param search 查询条件
     * @return
     */
    List<ModelField> selectFieldAndWdg(FieldCommonSearchDTO search);

    /**
     * 查询未配置在ConfigField中的模型字段
     *
     * @param field    字段ID
     * @param tenantId 租户ID
     * @param unitId   单元ID
     * @return 结果已过滤已配置的模型字段，即 hpfm_cusz_config_field 中存在的
     */
    List<ModelField> selectUnConfigByModelId(FieldCommonSearchDTO field, Long tenantId, Long unitId);

    /**
     * 初始化缓存时调用，查询所有模型字段，包含组件配置信息
     *
     * @return
     */
    List<ModelField> selectAllFieldWithWdg();
}
