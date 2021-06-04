package org.hzero.plugin.platform.customize.app.service;

import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.plugin.platform.customize.api.dto.*;
import org.hzero.plugin.platform.customize.domain.entity.Config;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldMap;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionHeader;

import java.util.List;
import java.util.Map;

/**
 * @author : peng.yu01@hand-china.com 2019/12/19 19:49
 */
public interface CustomizeConfigService {

    /**
     * 保存单元头配置
     *
     * @param config
     */
    void saveUnitConfig(Config config);

    /**
     * 保存个性化单元字段配置
     *
     * @param configField
     */
    void saveUnitConfigFieldAndWdg(ConfigField configField);

    /**
     * 查询个性化单元字段列表
     *
     * @param tenantId 租户ID
     * @param unitId   单元ID
     * @return
     */
    List<ConfigField> selectUnitConfig(Long tenantId, Long unitId);

    /**
     * 删除ConfigField
     *
     * @param configField
     */
    void deleteConfigFieldById(ConfigField configField);

    /**
     * 获取ConfigField的简要信息
     *
     * @param tenantId
     * @param unitId
     * @param searchDTO
     * @param pageRequest
     * @return
     */
    List<ConfigFieldLovDTO> selectSimpleConfigField(Long tenantId, Long unitId, FieldCommonSearchDTO searchDTO, PageRequest pageRequest);

    /**
     * 查询LOV映射
     *
     * @param configFieldId
     * @return
     */
    List<ConfigFieldMap> selectFieldMapping(Long configFieldId);

    /**
     * 删除指定单元个性化已配置的模型字段，包含模型字段及组件缓存
     */
    void deleteConfigField(ConfigField configField);

    /**
     * 查询指定字段配置的条件列表
     *
     * @return 返回结果包含条件行
     */
    List<FieldConditionHeader> selectCondHeaderByFieldId(FieldConditionHeader search, Long unitId, boolean needConValid);

    /**
     * 查询字段及其组件信息
     *
     * @param unitId
     * @param tenantId
     * @return
     */
    List<UnitRelatedDTO> selectWithWdgByUnitId(Long unitId, Long tenantId);

    /**
     * 返回用户级个性化列配置
     *
     * @param unitCodes
     * @return
     */
    Map<String, UserConfigDTO> userConfigByUnitCodes(Long tenantId, String[] unitCodes);

    /**
     * 保存用户个性化配置
     *
     * @param tenantId
     * @param configDTO
     * @return
     */
    UserConfigDTO saveUserConfigFields(Long tenantId, UserConfigDTO configDTO);

    /**
     * 删除用户个性化数据
     *
     * @param tenantId
     * @param unitId
     * @return
     */
    int deleteUserConfigField(Long tenantId, Long unitId);

    /**
     * 查询个性化配置头、行
     *
     * @param tenantId
     * @param unitId
     * @return
     */
    UnitCompositeDTO selectConfigDetails(Long tenantId, Long unitId);

    /**
     * 查询指定字段的高级校验配置
     *
     * @return
     */
    FieldConditionHeader selectConValidByFieldId(FieldConditionHeader search, Long unitId);

    /**
     * 保存高级校验配置
     *
     * @param conditionHeader
     * @param unitId
     */
    void saveConValid(FieldConditionHeader conditionHeader, Long unitId);
}
