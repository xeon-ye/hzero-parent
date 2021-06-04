package org.hzero.plugin.platform.customize.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionHeader;

import java.util.Collection;
import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2020/2/7 10:03
 */
public interface CustomizeFieldConditionHeaderRepository extends BaseRepository<FieldConditionHeader> {

    /**
     * 查询指定字段条件列表
     *
     * @param configFieldIds 字段ID
     * @return 返回结果包含行数据
     */
    List<FieldConditionHeader> selectWithLineByFieldId(Collection<Long> configFieldIds, Collection<String> conTypes, boolean needValid);

    /**
     * 查询所有条件  初始化缓存使用
     *
     * @return
     */
    List<FieldConditionHeader> selectAllConditions(Collection<Long> configFieldIds);


}
