package org.hzero.boot.platform.templateconfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.hzero.boot.platform.templateconfig.constant.TemplateConfigConstants;
import org.hzero.boot.platform.templateconfig.vo.TemplateConfigVO;
import org.hzero.common.HZeroService;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.redis.RedisHelper;

/**
 * 模板配置客户端
 *
 * @author xiaoyu.zhao@hand-china.com 2019/07/11 11:33
 */
@Component
public class TemplateConfigClient {

    @Autowired
    private RedisHelper redisHelper;

    private TemplateConfigClient() {}

    /**
     * 查询缓存获取某域名下指定模版编码的模板配置值
     */
    public Set<TemplateConfigVO> getTemplateConfigValues(String domainUrl, String sourceType, String templateCode,
                    String configCode) {
        return this.getTemplateConfigValue(domainUrl, sourceType, templateCode, configCode);
    }

    /**
     * 查询缓存获取域名管理下指定模版编码的模板配置值
     */
    public Set<TemplateConfigVO> getTemplateConfigValues(String domainUrl, String templateCode, String configCode) {
        return this.getTemplateConfigValue(domainUrl, TemplateConfigConstants.SSO_SOURCE_TYPE, templateCode,
                        configCode);
    }

    /**
     * 查询缓存获取某域名下指定模版编码的单一模板配置值
     */
    public TemplateConfigVO getOneTemplateConfigValue(String domainUrl, String sourceType, String templateCode,
                    String configCode) {
        Set<TemplateConfigVO> templateConfigValue =
                        this.getTemplateConfigValue(domainUrl, sourceType, templateCode, configCode);
        if (CollectionUtils.isNotEmpty(templateConfigValue)) {
            return templateConfigValue.iterator().next();
        }
        return null;
    }

    /**
     * 查询缓存获取某域名下指定模版编码的单一模板配置值
     */
    public TemplateConfigVO getOneTemplateConfigValue(String domainUrl, String templateCode, String configCode) {
        Set<TemplateConfigVO> templateConfigValue = this.getTemplateConfigValue(domainUrl,
                        TemplateConfigConstants.SSO_SOURCE_TYPE, templateCode, configCode);
        if (CollectionUtils.isNotEmpty(templateConfigValue)) {
            return templateConfigValue.iterator().next();
        }
        return null;
    }

    /**
     * 获取默认模板缓存值
     */
    public Set<TemplateConfigVO> getDefaultTplConfigValues(String domainUrl, String sourceType, String configCode) {
        return this.getTemplateConfigValue(domainUrl, sourceType, null, configCode);
    }

    /**
     * 获取默认模板缓存值
     */
    public Set<TemplateConfigVO> getDefaultTplConfigValues(String domainUrl, String configCode) {
        return this.getTemplateConfigValue(domainUrl, TemplateConfigConstants.SSO_SOURCE_TYPE, null, configCode);
    }

    /**
     * 获取默认模板单一缓存值
     */
    public TemplateConfigVO getOneDefaultTplConfigValue(String domainUrl, String sourceType, String configCode) {
        Set<TemplateConfigVO> templateConfigValue =
                        this.getTemplateConfigValue(domainUrl, sourceType, null, configCode);
        if (CollectionUtils.isNotEmpty(templateConfigValue)) {
            return templateConfigValue.iterator().next();
        }
        return null;
    }

    /**
     * 获取默认模板单一缓存值
     */
    public TemplateConfigVO getOneDefaultTplConfigValue(String domainUrl, String configCode) {
        Set<TemplateConfigVO> templateConfigValue =
                this.getTemplateConfigValue(domainUrl, TemplateConfigConstants.SSO_SOURCE_TYPE, null, configCode);
        if (CollectionUtils.isNotEmpty(templateConfigValue)) {
            return templateConfigValue.iterator().next();
        }
        return null;
    }



    /**
     * 生成缓存Key
     * 
     * @return key
     */
    private String generateCacheKey(String domainUrl, String sourceType, String templateCode, String configCode) {
        // 解析domainUrl，去除http://(https://)后面的'//'
        String tempDomainUrl = StringUtils.replace(domainUrl, BaseConstants.Symbol.DOUBLE_SLASH, StringUtils.EMPTY);
        return StringUtils.join(TemplateConfigConstants.TEMPLATE_CACHE_KEY, sourceType, BaseConstants.Symbol.COLON,
                        tempDomainUrl, BaseConstants.Symbol.COLON, templateCode, BaseConstants.Symbol.COLON,
                        configCode);
    }

    /**
     * 生成默认模板缓存Key 默认模板缓存key hpfm:default-template:{SSO}:{domainUrl}:{configCode}
     *
     * @return 默认模板缓存Key
     */
    private String generateDefaultTplCacheKey(String domainUrl, String sourceType, String configCode) {
        String tempDomainUrl = StringUtils.replace(domainUrl, BaseConstants.Symbol.DOUBLE_SLASH, StringUtils.EMPTY);
        // 格式hpfm:default-template:SSO:domainUrl:configCode
        return StringUtils.join(TemplateConfigConstants.DEFAULT_TEMPLATE_CACHE_KEY, sourceType,
                        BaseConstants.Symbol.COLON, tempDomainUrl, BaseConstants.Symbol.COLON, configCode);

    }

    /**
     * 获取缓存值
     *
     * @return Set<TemplateConfigVO>
     */
    private Set<TemplateConfigVO> getTemplateConfigValue(String domainUrl, String sourceType, String templateCode,
                    String configCode) {
        Set<String> configSet;
        try {
            redisHelper.setCurrentDatabase(HZeroService.Platform.REDIS_DB);
            String uniqueKey = this.generateCacheKey(domainUrl, sourceType, templateCode, configCode);
            configSet = redisHelper.zSetRangeByScore(uniqueKey, 0D, 100D);
            if (CollectionUtils.isEmpty(configSet)) {
                // 获取默认模板缓存Key
                String defaultTplCacheKey = this.generateDefaultTplCacheKey(domainUrl, sourceType, configCode);
                configSet = redisHelper.zSetRangeByScore(defaultTplCacheKey, 0D, 100D);
                if (CollectionUtils.isEmpty(configSet)) {
                    return Collections.emptySet();
                }
            }
        } finally {
            redisHelper.clearCurrentDatabase();
        }

        Set<TemplateConfigVO> resultSet = new HashSet<>();
        configSet.forEach(config -> {
            TemplateConfigVO templateConfigVO = redisHelper.fromJson(config, TemplateConfigVO.class);
            resultSet.add(templateConfigVO);
        });
        return resultSet;
    }

}
