package org.hzero.plugin.platform.customize.domain.repository;

import java.util.Collection;
import java.util.List;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionLine;

/**
 * @author : peng.yu01@hand-china.com 2020/2/7 11:42
 */
public interface CustomizeFieldConditionLineRepository extends BaseRepository<FieldConditionLine> {

    int deleteByIds(Collection<Long> ids);

    /**
     * 根据单元ID和字段ID查询被关联的条件行
     * @param unitId
     * @param fieldId
     * @return
     */
    List<FieldConditionLine> selectByUnitIdAndFieldId(Long unitId, Long fieldId);
}
