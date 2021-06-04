package org.hzero.plugin.platform.customize.infra.mapper;

import java.util.List;

import io.choerodon.mybatis.common.BaseMapper;
import org.hzero.boot.customize.dto.CustomizeConfigField;
import org.hzero.plugin.platform.customize.domain.entity.Config;

/**
 * 个性化配置mapper
 *
 * @author : xiangyu.qi01@hand-china.com 2019/12/13
 */
public interface CustomizeConfigMapper extends BaseMapper<Config> {


    /**
     * 初始化缓存时调用， 查询所有的Config，含有所属单元编码
     *
     * @return
     */
    List<Config> selectAllWithUnitCode();

}
