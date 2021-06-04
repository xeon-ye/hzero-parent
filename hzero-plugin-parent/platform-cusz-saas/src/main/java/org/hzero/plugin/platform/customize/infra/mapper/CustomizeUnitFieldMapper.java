package org.hzero.plugin.platform.customize.infra.mapper;

import io.choerodon.mybatis.common.BaseMapper;
import org.hzero.plugin.platform.customize.api.dto.ConfigFieldLovDTO;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;

import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2019/12/21 11:02
 */
public interface CustomizeUnitFieldMapper extends BaseMapper<UnitField> {

    /**
     * 查询指定单元的字段列表，包含组件信息、所属模型信息
     *
     * @param unitId
     * @return
     */
    List<ConfigField> selectByUnitId(Long unitId);

    /**
     * 查询指定单元符合条件的字段简要信息
     *
     * @param unitId
     * @param field
     * @return
     */
    List<ConfigFieldLovDTO> selectSimpleByUnitId(Long unitId, FieldCommonSearchDTO field);

    /**
     * 查询指定单元下的字段列表，包含组件信息和所属模型信息
     *
     * @param unitId
     * @return
     */
    List<UnitField> selectUnitFieldsByUnitId(Long unitId);

    /**
     * 查询模型字段列表，排除已配置在个性化单元的
     *
     * @param unitId 个性化单元ID
     * @param field  查询条件
     * @return
     */
    List<ModelField> selectNotConfigField(Long unitId, FieldCommonSearchDTO field);

    /**
     * 查询个性化单元字段配置，初始化缓存时调用
     *
     * @return
     */
    List<UnitField> selectAllUnitField();

    /**
     * 查询个性化单元字段配置，包含所属单元编码
     *
     * @param unitFieldId
     * @return
     */
    UnitField selectWithUnitCodeById(Long unitFieldId);

    /**
     * 根据单元编码查询
     *
     * @param unitCode
     * @return
     */
    List<UnitField> selectByUnitCode(String unitCode);

    /**
     * 查询单元脑子短，包含字段参数配置
     * @param unitId
     * @return
     */
    List<UnitField> selectWithParamByUnitId(Long unitId);

}
