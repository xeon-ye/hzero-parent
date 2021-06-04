package org.hzero.plugin.platform.customize.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import org.hzero.plugin.platform.customize.api.dto.FieldConValidTlDTO;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionValid;

import java.util.Collection;
import java.util.List;

/**
 * @author peng.yu01@hand-china.com on 2020-04-10
 */
public interface CustomizeFieldConditionValidRepository extends BaseRepository<FieldConditionValid> {

    List<FieldConValidTlDTO> selectValidTl(Collection<Long> validId);

}
