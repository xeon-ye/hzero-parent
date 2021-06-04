package org.hzero.plugin.platform.customize.infra.mapper;

import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.hzero.boot.customize.dto.CustomizeConfigField;
import org.hzero.plugin.platform.customize.api.dto.ConfigFieldLovDTO;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitRelatedDTO;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author : peng.yu01@hand-china.com 2019/12/19 11:45
 */
public interface CustomizeConfigFieldMapper extends BaseMapper<ConfigField> {


    /**
     * 根据主键查询，包含单元编码、关联的模型字段信息
     *
     * @param configFieldId
     * @return
     */
    ConfigField selectById(Long configFieldId);

    /**
     * 查询配置的字段信息，不包含组件信息
     *
     * @param tenantId
     * @param unitId
     * @param field
     * @return
     */
    List<ConfigFieldLovDTO> selectSimpleByUnitId(Long tenantId, Long unitId, FieldCommonSearchDTO field);

    /**
     * 查询所有的配置字段，包含所属模型信息、关联的模型字段信息、组件信息、映射信息、所属单元信息
     *
     * @return
     */
    List<ConfigField> selectConfigFieldByUnitId(Long unitId, Long tenantId);

    /**
     * 查询指定配置字段的多语言信息
     *
     * @param configFieldId
     * @return
     */
    List<Map<String, String>> selectFieldTL(Long configFieldId);

    /**
     * 查询配置的字段信息，含有所属个性化单元编码
     *
     * @param field
     * @return
     */
    List<ConfigField> selectByUnitIdAndFieldId(ConfigField field);

    List<ConfigField> selectByUnitIdAndTenantId(Long tenantId, @Param("unitIds") Collection<Long> unitIds);

    /**
     * 查询单元关联字段
     *
     * @param unitCodes
     * @param tenantId
     * @return
     */
    List<UnitRelatedDTO> selectRelatedUnitAndField(@Param("list") Collection<String> unitCodes, Long tenantId);

    /**
     * 查询字段别名不为null的字段
     *
     * @param tenantId
     * @return
     */
    List<ConfigField> selectWithAliasNotNullByTenantId(Long tenantId);

    /**
     * 查询所有单元ID以及租户ID
     *
     * @return
     */
    List<ConfigField> selectAllUnitIdAndTenant();

    /**
     * 查询租户个性化字段配置
     *
     * @param tenantId 租户ID
     * @param unitCode 单元编码
     * @return 返回结果包含对应的模型字段信息、组件信息、映射信息
     */
    List<ConfigField> selectWithModelFieldByUnitCode(Long tenantId, String unitCode);

    /**
     * 删除租户个性化字段多语言
     *
     * @param configFieldIds
     */
    void deleteConfigFieldTl(@Param("list") Collection<Long> configFieldIds);

    List<ConfigField> selectUserConfigField(ConfigField configField);


    /**
     * 根据单元字段更新别名
     * @param unitField
     * @return
     */
    int updateFieldAliasByUnitField(UnitField unitField);

    /**
     * 查询个性化字段和单元编码
     * @param configFieldIds
     * @return
     */
    List<ConfigField> selectConfigFieldAndUnitCode(@Param("list") Collection<Long> configFieldIds);

    /**
     * 根据单元编码获取个性化配置字段明细
     *
     * @param tenantId 租户ID
     * @param unitId   单元Id
     * @return
     */
    List<CustomizeConfigField> selectConfigModelFieldsByUnitId(Long tenantId, Long unitId);

}
