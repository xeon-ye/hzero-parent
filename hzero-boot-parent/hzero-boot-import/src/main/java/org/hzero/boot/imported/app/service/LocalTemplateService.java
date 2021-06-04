package org.hzero.boot.imported.app.service;

import org.hzero.boot.imported.domain.entity.LocalTemplate;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * 应用服务
 *
 * @author shuangfei.zhu@hand-china.com 2018-12-18 16:00:39
 */
public interface LocalTemplateService {

    /**
     * 通过模板头主键分页查询模板行
     *
     * @param tenantId     租户Id
     * @param templateCode 模板编码
     * @param pageRequest  分页
     * @return 模板信息
     */
    Page<LocalTemplate> pageTemplate(Long tenantId, String templateCode, PageRequest pageRequest);

    /**
     * 根据主键查询
     *
     * @param templateId 主键
     * @return 模板
     */
    LocalTemplate detailTemplateJson(Long templateId);

    /**
     * 根据唯一索引查询
     *
     * @param tenantId     租户Id
     * @param templateCode 模板编码
     * @return 模板
     */
    LocalTemplate detailTemplateJsonByCode(Long tenantId, String templateCode);

    /**
     * 创建与更新
     *
     * @param localTemplate 模板信息
     * @return 更新后的信息
     */
    LocalTemplate save(LocalTemplate localTemplate);

    /**
     * 删除本地模板
     *
     * @param localTemplate 本地模板
     */
    void deleteTemplateJson(LocalTemplate localTemplate);
}
