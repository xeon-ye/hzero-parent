package org.hzero.boot.imported.app.service.impl;

import java.io.IOException;

import org.hzero.boot.imported.app.service.LocalTemplateService;
import org.hzero.boot.imported.domain.entity.LocalTemplate;
import org.hzero.boot.imported.domain.entity.Template;
import org.hzero.boot.imported.domain.repository.LocalTemplateRepository;
import org.hzero.boot.imported.infra.constant.HimpBootConstants;
import org.hzero.mybatis.domian.Condition;
import org.hzero.mybatis.util.Sqls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * 应用服务默认实现
 *
 * @author shuangfei.zhu@hand-china.com 2018-12-18 16:00:39
 */
@Service
public class LocalTemplateServiceImpl implements LocalTemplateService {

    @Autowired
    private LocalTemplateRepository localTemplateRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Page<LocalTemplate> pageTemplate(Long tenantId, String templateCode, PageRequest pageRequest) {
        Page<LocalTemplate> page = PageHelper.doPageAndSort(pageRequest, () -> localTemplateRepository.selectByCondition(
                Condition.builder(LocalTemplate.class)
                        .andWhere(Sqls.custom().andEqualTo(LocalTemplate.FIELD_TENANT_ID, tenantId, true)
                                .andLike(LocalTemplate.FIELD_TEMPLATE_CODE, templateCode, true))
                        .build()));
        page.forEach(this::readTemplate);
        return page;
    }

    @Override
    public LocalTemplate detailTemplateJson(Long templateId) {
        return readTemplate(localTemplateRepository.selectByPrimaryKey(templateId));
    }

    @Override
    public LocalTemplate detailTemplateJsonByCode(Long tenantId, String templateCode) {
        return readTemplate(localTemplateRepository.selectOne(new LocalTemplate()
                .setTenantId(tenantId)
                .setTemplateCode(templateCode)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LocalTemplate save(LocalTemplate localTemplate) {
        try {
            localTemplate.setTemplateJson(objectMapper.writeValueAsString(localTemplate.getTemplate()));
        } catch (JsonProcessingException e) {
            throw new CommonException(e);
        }
        if (localTemplate.getId() == null) {
            // 唯一性校验
            Assert.isTrue(localTemplateRepository.selectCount(new LocalTemplate()
                            .setTenantId(localTemplate.getTenantId())
                            .setTemplateCode(localTemplate.getTemplateCode())) == 0,
                    HimpBootConstants.ErrorCode.LOCAL_TEMPLATE_EXISTS);
            localTemplateRepository.insertSelective(localTemplate);
            return localTemplate;
        } else {
            localTemplateRepository.updateOptional(localTemplate, LocalTemplate.FIELD_TEMPLATE_JSON);
            return localTemplate;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplateJson(LocalTemplate localTemplate) {
        localTemplateRepository.deleteByPrimaryKey(localTemplate);
    }

    private LocalTemplate readTemplate(LocalTemplate template) {
        if (template != null) {
            try {
                template.setTemplate(objectMapper.readValue(template.getTemplateJson(), Template.class));
            } catch (IOException e) {
                throw new CommonException(e);
            }
        }
        return template;
    }
}
