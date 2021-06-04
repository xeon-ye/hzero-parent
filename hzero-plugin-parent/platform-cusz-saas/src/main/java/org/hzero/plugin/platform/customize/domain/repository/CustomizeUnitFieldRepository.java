package org.hzero.plugin.platform.customize.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.api.dto.ConfigFieldLovDTO;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;

import java.util.List;
import java.util.Map;

/**
 * @author : peng.yu01@hand-china.com 2019/12/21 10:56
 */
public interface CustomizeUnitFieldRepository extends BaseRepository<UnitField> {

    List<ConfigField> selectUnitFieldByUnitId(Long unitId);

    List<ConfigFieldLovDTO> selectSimpleUnitFieldByUnitId(Long unitId, FieldCommonSearchDTO searchDTO);

    List<UnitField> selectUnitFieldsByUnitId(Long unitId);

    List<ModelField> selectNotConfigField(Long unitId, FieldCommonSearchDTO searchDTO);

    /**
     * 根据主键查询单元字段，包含单元编码
     *
     * @param unitFieldId
     * @return
     */
    UnitField selectWithUnitCodeById(Long unitFieldId);

    /**
     * 缓存个性化单元字段
     *
     * @param unitField
     */
    void cacheUnitField(UnitField unitField);


    /**
     * 删除缓存 - 单条
     *
     * @param unitCode
     * @param fieldId
     */
    void delUnitFieldCache(String unitCode, String fieldId);

    /**
     * 根据个性化单元编码获取字段缓存集合
     *
     * @param unitCode
     * @return
     */
    Map<String, UnitField> getUnitFieldsCacheMap(String unitCode);

    /**
     * 初始化缓存
     */
    void initUnitFieldCache();

    /**
     * 根据单元编码查询
     *
     * @param unitCode
     * @return
     */
    List<UnitField> selectByUnitCode(String unitCode);

}
