package org.hzero.route.rule.repository.impl;

import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.redis.RedisHelper;
import org.hzero.route.constant.RouteConstants;
import org.hzero.route.rule.handler.TargetServiceUrlContext;
import org.hzero.route.rule.repository.UrlMappingRepository;
import org.hzero.route.rule.vo.UrlMappingCacheVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author 11838 liguo.wang 2020/4/4
 */
public class UrlMappingServiceImpl implements UrlMappingRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlMappingServiceImpl.class);

    private RedisHelper redisHelper;

    public UrlMappingServiceImpl(RedisHelper redisHelper) {
        this.redisHelper = redisHelper;
    }

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 获取缓存值
     * 用户级 、 租户级、 平台级
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return Map
     */
    private Map<String, String> getRedisUrlMapping(Long userId, Long tenantId) {
        String redisKey;
        redisKey = RouteConstants.UrlMapping.HADM_UM;
        Map<String, String> map = new HashMap<>(redisHelper.hshGetAll(redisKey));

        if (tenantId != null) {
            redisKey = StringUtils.join(RouteConstants.UrlMapping.HADM_UM, BaseConstants.Symbol.COLON, tenantId);
            map.putAll(redisHelper.hshGetAll(redisKey));
        }

        if (userId != null && tenantId != null) {
            redisKey = StringUtils.join(RouteConstants.UrlMapping.HADM_UM, BaseConstants.Symbol.COLON, tenantId, BaseConstants.Symbol.COLON, userId);
            map.putAll(redisHelper.hshGetAll(redisKey));
        }
        return map;
    }

    @Override
    public List<UrlMappingCacheVO> getUrlMapping(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return new ArrayList<>();
        }
        redisHelper.setCurrentDatabase(RouteConstants.Admin.REDIS_DB);
        Map<String, String> map = getRedisUrlMapping(userId, tenantId);
        List<UrlMappingCacheVO> urlMappingCacheVOList = new ArrayList<>(map.size());
        try {
            if (!CollectionUtils.isEmpty(map)) {
                Collection<String> mappingList = map.values();
                if (!CollectionUtils.isEmpty(mappingList)) {
                    // 获取url映射对象集合
                    buildUrlMapping(mappingList, urlMappingCacheVOList);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Get url mapping redis data error:" + e);
        } finally {
            redisHelper.clearCurrentDatabase();
        }
        return urlMappingCacheVOList;
    }

    @Override
    public void getTargetServiceUrl(TargetServiceUrlContext context, List<UrlMappingCacheVO> urlMappings) {
        if (!CollectionUtils.isEmpty(urlMappings)) {
            // 按sourceUrl从短到长排序
            urlMappings.sort(Comparator.comparing((UrlMappingCacheVO mappingVO) -> {
                if (mappingVO.getSourceUrl() == null) {
                    return 0;
                }
                return mappingVO.getSourceUrl().length();
            }));
            String path = context.getSourceUrl();
            for (UrlMappingCacheVO urlMappingCacheVO : urlMappings) {
                String sourceUrl = urlMappingCacheVO.getSourceUrl();
                String targetUrl = urlMappingCacheVO.getTargetUrl();
                if (StringUtils.isEmpty(sourceUrl) || StringUtils.isEmpty(targetUrl)) {
                    if (context.getSourceService() != null && context.getSourceService().equals(urlMappingCacheVO.getSourceService())) {
                        context.setTargetService(urlMappingCacheVO.getTargetService());
                    }
                } else {
                    // 替换变量值 ep:/{id} <-> /{1}
                    replaceVariable(path, sourceUrl, targetUrl);
                    // 匹配URL 替换模糊值/* /** 暂只支持在最后 ep：select/**  <-> select/self
                    if (pathMatcher.match(sourceUrl, path)) {
                        String blurryValue = pathMatcher.extractPathWithinPattern(sourceUrl, path);
                        if (StringUtils.isNotEmpty(blurryValue) && targetUrl.contains(BaseConstants.Symbol.STAR)) {
                            targetUrl = targetUrl.replaceAll("\\*", "") + blurryValue;
                        }
                        if (!targetUrl.contains(BaseConstants.Symbol.STAR)) {
                            context.setTargetUrl(targetUrl);
                            if (context.getSourceService() != null && context.getSourceService().equals(urlMappingCacheVO.getSourceService())) {
                                context.setTargetService(urlMappingCacheVO.getTargetService());
                            }
                        }
                    }
                }
            }
        }
    }

    private void replaceVariable(String path, String sourceUrl, String targetUrl) {
        Map<String, String> variableValue = null;
        try {
            variableValue = pathMatcher.extractUriTemplateVariables(sourceUrl, path);
        } catch (Exception e) {
            LOGGER.debug(">>>>> url: {}; templateUrl: {}", sourceUrl, path);
        }
        if (!CollectionUtils.isEmpty(variableValue)) {
            for (Map.Entry<String, String> entry : variableValue.entrySet()) {
                sourceUrl = sourceUrl.replace(StringUtils.join(BaseConstants.Symbol.LEFT_BIG_BRACE, entry.getKey(), BaseConstants.Symbol.RIGHT_BIG_BRACE), entry.getValue());
                targetUrl = targetUrl.replace(StringUtils.join(BaseConstants.Symbol.LEFT_BIG_BRACE, entry.getKey(), BaseConstants.Symbol.RIGHT_BIG_BRACE), entry.getValue());
            }
        }
    }

    private void buildUrlMapping(Collection<String> mappingList, List<UrlMappingCacheVO> urlMappingCacheVOList) {
        // 缓存样式： A@/B/b,C@D/d
        for (String str : mappingList) {
            if (str.contains(BaseConstants.Symbol.COMMA) && str.contains(BaseConstants.Symbol.AT)) {
                String[] um = str.split(BaseConstants.Symbol.COMMA);
                String[] serviceUrl1 = um[0].split(BaseConstants.Symbol.AT);
                String[] serviceUrl2 = um[1].split(BaseConstants.Symbol.AT);
                String sourceService = serviceUrl1[0];
                String targetService = serviceUrl2[0];
                UrlMappingCacheVO urlMappingCacheVO = new UrlMappingCacheVO();
                urlMappingCacheVO.setSourceService(sourceService);
                urlMappingCacheVO.setTargetService(targetService);
                if (serviceUrl1.length > BaseConstants.Digital.ONE) {
                    urlMappingCacheVO.setSourceUrl(serviceUrl1[1]);
                }
                if (serviceUrl2.length > BaseConstants.Digital.ONE) {
                    urlMappingCacheVO.setTargetUrl(serviceUrl2[1]);
                }
                urlMappingCacheVOList.add(urlMappingCacheVO);
            }
        }
    }


}
