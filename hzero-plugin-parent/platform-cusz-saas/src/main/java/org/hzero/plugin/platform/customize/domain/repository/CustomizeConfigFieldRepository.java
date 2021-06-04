package org.hzero.plugin.platform.customize.domain.repository;

import org.hzero.boot.customize.dto.CustomizeConfigField;
import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.api.dto.ConfigFieldLovDTO;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitRelatedDTO;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author : peng.yu01@hand-china.com 2019/12/19 10:56
 */
public interface CustomizeConfigFieldRepository extends BaseRepository<ConfigField> {

    /**
     * 查询指定单元已配置的模型字段信息
     *
     * @param tenantId 租户ID
     * @param unitId   单元ID
     * @return
     */
    List<ConfigField> selectConfigFieldByUnitId(Long tenantId, Long unitId);


    /**
     * 根据ID查询，同时带出模型字段信息
     *
     * @param id
     * @return
     */
    ConfigField selectById(Long id);

    /**
     * 查询已配置的模型字段信息
     *
     * @param tenantId 租户ID
     * @param unitId   单元ID
     * @return 返回结果不包含字段组件信息
     */
    List<ConfigFieldLovDTO> selectSimpleConfigFieldByUnitId(Long tenantId, Long unitId, FieldCommonSearchDTO searchDTO);


    /**
     * 删除缓存
     *
     * @param tenantId
     * @param unitCode
     */
    void cacheDel(Long tenantId, String unitCode);

    /**
     * 根据租户ID，个性化单元ID，字段ID获取配置
     *
     * @param tenantId
     * @param unitCode
     * @param fieldId
     * @return
     */
    ConfigField getConfigFieldCache(Long tenantId, String unitCode, Long fieldId);

    /**
     * 删除缓存 - 单条
     *
     * @param field
     */
    void delConfigFieldCache(ConfigField field);

    /**
     * 根据租户ID，单元ID，获取个性化单元配置字段
     *
     * @param tenantId
     * @param unitCode
     * @return
     */
    Map<String, ConfigField> getConfigFieldsCacheMap(Long tenantId, String unitCode);

    /**
     * 缓存时查询多语言
     *
     * @param configFieldId
     * @return
     */
    List<Map<String, String>> selectFieldTL(Long configFieldId);

    /**
     * 启动时初始化ConfigField至缓存
     */
//    void initConfigFieldCache();

    /**
     * 根据单元id和字段id查询
     *
     * @param field
     * @return
     */
    List<ConfigField> selectByUnitIdAndFieldId(ConfigField field);

    /**
     * 获取指定单元配置的字段信息
     *
     * @param tenantId
     * @param unitIds
     * @return
     */
    List<ConfigField> selectByUnitIdAndTenantId(Long tenantId, Collection<Long> unitIds);

    /**
     * 查询字段及其组件信息
     *
     * @param unitCodes
     * @param tenantId
     * @return
     */
    List<UnitRelatedDTO> selectWithWdgByUnitId(Collection<String> unitCodes, Long tenantId);

    /**
     * 查询所有单元ID以及租户ID
     *
     * @return
     */
    List<ConfigField> selectAllUnitIdAndTenant();

    /**
     * 查询用户级个性化字段配置
     *
     * @param field
     * @return
     */
    List<ConfigField> selectUserConfigField(ConfigField field);


    /**
     * 删除用户在某租户下某单元的所有个性化配置
     *
     * @param configField
     */
    void deleteUserConfigField(ConfigField configField);

    /**
     * 删除
     *
     * @param configFieldIds
     */
    void deleteConfigFieldTl(List<Long> configFieldIds);

    /**
     * 从缓存中读取用户个性化配置
     *
     * @param tenantId
     * @param userId
     * @param unitCode
     * @return
     */
    Map<String, ConfigField> getUserConfigFieldCache(Long tenantId, Long userId, String unitCode);

    /**
     * 从缓存中读取用户个性化配置
     *
     * @param tenantId
     * @param userId
     * @param unitCode
     * @return
     */
    void delUserConfigFieldCache(Long tenantId, Long userId, String unitCode);

    /**
     * 查询租户个性化配字段配置
     *
     * @param tenantId 租户ID
     * @param unitCode 单元编码
     * @return 返回结果包含对应的模型字段信息、组件信息、映射信息
     */
    List<ConfigField> selectByUnitCode(Long tenantId, String unitCode);

    /**
     * 单元字段别名更新
     * @param unitField
     * @return
     */
    int updateFieldAliasByUnitField(UnitField unitField);

    /**
     * 查询个性化字段
     * @param configFieldIds
     * @return
     */
    List<ConfigField> selectConfigFieldAndUnitCode(Collection<Long> configFieldIds);

    /**
     * 根据单元编码获取个性化配置字段明细
     *
     * @param tenantId 租户ID
     * @param unitId   单元Id
     * @return
     */
    List<CustomizeConfigField> selectConfigModelFieldsByUnitId(Long tenantId, Long unitId);

}
