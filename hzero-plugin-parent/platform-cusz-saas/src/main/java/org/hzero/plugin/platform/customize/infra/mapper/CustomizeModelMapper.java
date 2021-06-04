package org.hzero.plugin.platform.customize.infra.mapper;

import java.util.List;

import io.choerodon.mybatis.common.BaseMapper;
import org.hzero.plugin.platform.customize.domain.entity.Model;
import org.hzero.plugin.platform.customize.domain.entity.ModelObjectPub;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 11:16
 */
public interface CustomizeModelMapper extends BaseMapper<ModelObjectPub> {

    /**
     * 根据条件查询模型列表，
     *
     * @param model 查询条件
     * @return
     */
    List<Model> selectByOption(Model model);


}
