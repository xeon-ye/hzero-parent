package org.hzero.boot.platform.lov.adapter.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.hzero.boot.platform.lov.constant.LovConstants;
import org.hzero.boot.platform.lov.dto.LovDTO;
import org.hzero.boot.platform.lov.dto.LovValueDTO;
import org.hzero.boot.platform.lov.dto.LovViewDTO;
import org.hzero.boot.platform.lov.feign.LovFeignClient;
import org.hzero.common.HZeroCacheKey;
import org.hzero.common.HZeroService;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.redis.RedisHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

/**
 * 值集值接口适配器默认实现类
 *
 * @author gaokuo.dai@hand-china.com 2018年6月30日下午6:52:53
 */
public class DefaultLovAdapter implements LovAdapter {

    private RedisHelper redisHelper;
    private LovFeignClient lovFeignClient;
    private ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(DefaultLovAdapter.class);

    public DefaultLovAdapter(RedisHelper redisHelper, LovFeignClient lovFeignClient, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.redisHelper = redisHelper;
        this.lovFeignClient = lovFeignClient;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    /**
     * Hzero平台HTTP协议,默认http
     */
    @Value("${hzero.platform.httpProtocol:http}")
    private String hzeroPlatformHttpProtocol;

    @Override
    public LovDTO queryLovInfo(String lovCode, Long tenantId) {
        Assert.notNull(lovCode, BaseConstants.ErrorCode.DATA_INVALID);
        if (tenantId == null) {
            tenantId = BaseConstants.DEFAULT_TENANT_ID;
        }
        String lang = this.getLang();
        logger.debug("query lov define using lov code [{}] and tenant id [{}]", lovCode, tenantId);
        // 查询缓存
        logger.debug("try to bind redis db [{}]", HZeroService.Platform.REDIS_DB);
        this.redisHelper.setCurrentDatabase(HZeroService.Platform.REDIS_DB);
        String cacheKey = HZeroCacheKey.Lov.HEADER_KEY_PREFIX + lovCode;
        String hashKey = hashKey(tenantId, lang);
        logger.debug("query organization lov define from redis using key [{}]-[{}]", cacheKey, hashKey);
        List<String> lovJsons = this.redisHelper.hshMultiGet(cacheKey, listHashKey(tenantId, lang));
        String bestMatch = getLatestAndRemove(lovJsons);
        // 有fail fast
        if (AccessStatus.FORBIDDEN.name().equals(bestMatch)) {
            logger.debug("the redis key [{}]-[{}] is in organization blacklist", cacheKey, hashKey);
            // try to get lov info in organization redis cache
            // check if this request can fail fast
            // try to get lov info in global redis cache
            if (!CollectionUtils.isEmpty(lovJsons)) {
                hashKey = hashKey(BaseConstants.DEFAULT_TENANT_ID, lang);
                logger.debug("query global lov define from redis using key [{}]-[{}]", cacheKey, hashKey);
                bestMatch = getLatestAndRemove(lovJsons);
                if (AccessStatus.FORBIDDEN.name().equals(bestMatch)) {
                    logger.warn("the redis key [{}]-[{}] is in global blacklist", cacheKey, hashKey);
                    logger.debug("Unbind redis db [{}]", HZeroService.Platform.REDIS_DB);
                    this.redisHelper.clearCurrentDatabase();
                    return null;
                }
            } else {
                return null;
            }
        }
        if (StringUtils.isEmpty(bestMatch)) {
            // redis cache missing, query data from remote service
            logger.debug("can not get lov define [{}] from redis cache, try to query from remote service...", lovCode);
            logger.debug("Unbind redis db [{}]", HZeroService.Platform.REDIS_DB);
            this.redisHelper.clearCurrentDatabase();
            return this.lovFeignClient.queryLovInfo(lovCode, tenantId);
        } else {
            // redis cache found
            logger.debug("get lov define in redis cache: [{}]", bestMatch);
            logger.debug("Unbind redis db [{}]", HZeroService.Platform.REDIS_DB);
            this.redisHelper.clearCurrentDatabase();
            return this.redisHelper.fromJson(bestMatch, LovDTO.class);
        }
    }

    private <T> T getLatestAndRemove(List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        T latest = list.get(list.size() - 1);
        list.remove(latest);
        return latest;
    }

    @Override
    public LovViewDTO queryLovViewInfo(String lovViewCode, Long tenantId) {
        Assert.notNull(lovViewCode, BaseConstants.ErrorCode.DATA_INVALID);
        if (tenantId == null) {
            tenantId = BaseConstants.DEFAULT_TENANT_ID;
        }
        String lang = this.getLang();
        logger.debug("query lov view define using lov view code [{}] and tenant id [{}]", lovViewCode, tenantId);
        // 查询缓存
        logger.debug("try to bind redis db [{}]", HZeroService.Platform.REDIS_DB);
        this.redisHelper.setCurrentDatabase(HZeroService.Platform.REDIS_DB);
        String cacheKey = HZeroCacheKey.Lov.VIEW_KEY_PREFIX + lovViewCode;
        String hashKey = hashKey(tenantId, lang);
        logger.debug("query organization lov view define from redis using key [{}]-[{}]", cacheKey, hashKey);
        List<String> lovViewJsons = this.redisHelper.hshMultiGet(cacheKey, listHashKey(tenantId, lang));
        String bestMatch = getLatestAndRemove(lovViewJsons);
        // 有fail fast
        if (AccessStatus.FORBIDDEN.name().equals(bestMatch)) {
            logger.debug("the redis key [{}]-[{}] is in organization blacklist", cacheKey, hashKey);
            // try to get lov info in organization redis cache
            // check if this request can fail fast
            // try to get lov info in global redis cache
            if (!CollectionUtils.isEmpty(lovViewJsons)) {
                hashKey = hashKey(BaseConstants.DEFAULT_TENANT_ID, lang);
                logger.debug("query global lov define from redis using key [{}]-[{}]", cacheKey, hashKey);
                bestMatch = getLatestAndRemove(lovViewJsons);
                if (AccessStatus.FORBIDDEN.name().equals(bestMatch)) {
                    logger.warn("the redis key [{}]-[{}] is in global blacklist", cacheKey, bestMatch);
                    logger.debug("Unbind redis db [{}]", HZeroService.Platform.REDIS_DB);
                    this.redisHelper.clearCurrentDatabase();
                    return null;
                }
            } else {
                return null;
            }
        }
        if (StringUtils.isEmpty(bestMatch)) {
            // redis cache missing, query data from remote service
            logger.debug("can not get lov define [{}] from redis cache, try to query from remote service...", lovViewCode);
            logger.debug("Unbind redis db [{}]", HZeroService.Platform.REDIS_DB);
            this.redisHelper.clearCurrentDatabase();
            return this.lovFeignClient.queryLovViewInfo(lovViewCode, tenantId);
        } else {
            // redis cache found
            logger.debug("get lov define in redis cache: [{}]", bestMatch);
            logger.debug("Unbind redis db [{}]", HZeroService.Platform.REDIS_DB);
            this.redisHelper.clearCurrentDatabase();
            return this.redisHelper.fromJson(bestMatch, LovViewDTO.class);
        }
    }

    @Override
    public List<LovValueDTO> queryLovValue(String lovCode, Long tenantId) {
        return queryLovValue(lovCode, tenantId, this.getLang());
    }

    @Override
    public List<LovValueDTO> queryLovValue(String lovCode, Long tenantId, String lang) {
        List<LovValueDTO> result = null;
        // data valid
        Assert.notNull(lovCode, BaseConstants.ErrorCode.DATA_INVALID);
        logger.debug("query lov values using lov code [{}] and tenant id [{}]", lovCode, tenantId);
        // select cache db
        logger.debug("try to bind redis db [{}]", HZeroService.Platform.REDIS_DB);
        this.redisHelper.setCurrentDatabase(HZeroService.Platform.REDIS_DB);
        // check if this request can fail fast
        String cacheKey = HZeroCacheKey.Lov.VALUE_KEY_PREFIX + lovCode;
        String hashKey = hashKey(tenantId, lang);
        List<String> lovValueJsons = this.redisHelper.hshMultiGet(cacheKey, listHashKey(tenantId, lang));
        String bestMatch = getLatestAndRemove(lovValueJsons);
        if (AccessStatus.FORBIDDEN.name().equals(bestMatch)) {
            logger.debug("the redis key [{}]-[{}] is in organization blacklist", cacheKey, hashKey);
            hashKey = hashKey(BaseConstants.DEFAULT_TENANT_ID, lang);
            if (!CollectionUtils.isEmpty(lovValueJsons)) {
                bestMatch = getLatestAndRemove(lovValueJsons);
                if (AccessStatus.FORBIDDEN.name().equals(bestMatch)) {
                    logger.warn("the redis key [{}]-[{}] is in global blacklist", cacheKey, hashKey);
                    logger.debug("Unbind redis db [{}]", HZeroService.Platform.REDIS_DB);
                    this.redisHelper.clearCurrentDatabase();
                    return Collections.emptyList();
                }
            } else {
                return Collections.emptyList();
            }
        }
        // try to get data from cache
        // find data in cache, convert json to DTO
        // cache missed, try to get data from platform service
        if (StringUtils.isBlank(bestMatch) && !CollectionUtils.isEmpty(lovValueJsons)) {
            bestMatch = getLatestAndRemove(lovValueJsons);
        }
        if (StringUtils.isNotBlank(bestMatch)) {
            logger.debug("get lov define in redis cache: [{}]", bestMatch);
            // find data in cache, convert json to DTO
            result = redisHelper.fromJsonList(bestMatch, LovValueDTO.class);
        } else {
            logger.debug("can not get lov values [{}] from global redis cache, try to query from remote service...", lovCode);
            // cache missed, try to get data from platform service
            result = this.lovFeignClient.queryLovValueWithLanguage(lovCode, tenantId, lang);
        }
        logger.debug("Unbind redis db [{}]", HZeroService.Platform.REDIS_DB);
        this.redisHelper.clearCurrentDatabase();
        return result;
    }

    @Override
    public List<LovValueDTO> queryLovValue(String lovCode, Long tenantId, List<String> params) {
        return queryLovValue(lovCode, tenantId, params, this.getLang());
    }

    @Override
    public List<LovValueDTO> queryLovValue(String lovCode, Long tenantId, List<String> params, String lang) {
        LovDTO lovDTO = queryLovInfo(lovCode, tenantId);
        if (lovDTO == null) {
            return Collections.emptyList();
        }
        // 这里不用考虑租户覆盖，lovDTO已经做了覆盖查询，tenantId是lovDTO的租户Id
        // 默认是IDP
        if (lovDTO.getLovTypeCode() == null) {
            lovDTO.setLovTypeCode(LovConstants.LovTypes.IDP);
        }
        switch (lovDTO.getLovTypeCode()) {
            case LovConstants.LovTypes.IDP:
                return queryLovValue(lovCode, lovDTO.getTenantId(), lang);
            case LovConstants.LovTypes.SQL:
            case LovConstants.LovTypes.URL:
                return sqlList(lovDTO, params);
            default:
                return Collections.emptyList();
        }
    }

    private List<LovValueDTO> sqlList(LovDTO lovDTO, List<String> params) {
        // restTemplate调用查询结果
        ResponseEntity<String> responseEntity;
        if (CollectionUtils.isNotEmpty(params)) {
            String url = this.hzeroPlatformHttpProtocol + "://" + getServerName(lovDTO.getRouteName()) +
                    "/v1/" + lovDTO.getTenantId() + "/lovs/translation-sql/data?lovCode={lovCode}&params={params}";
            StringBuilder sb = new StringBuilder();
            params.forEach(item -> sb.append(item).append(BaseConstants.Symbol.COMMA));
            responseEntity = restTemplate.getForEntity(url, String.class, lovDTO.getLovCode(), sb.substring(0, sb.length() - 1));
        } else {
            String url = this.hzeroPlatformHttpProtocol + "://" + getServerName(lovDTO.getRouteName()) +
                    "/v1/" + lovDTO.getTenantId() + "/lovs/translation-sql/data?lovCode={lovCode}";
            responseEntity = restTemplate.getForEntity(url, String.class, lovDTO.getLovCode());
        }
        String body = responseEntity.getBody();
        List<Map<String, Object>> result = null;
        try {
            result = objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (IOException e) {
            logger.error("get translation data error.");
        }
        if (CollectionUtils.isNotEmpty(result)) {
            List<LovValueDTO> value = new ArrayList<>();
            String k = lovDTO.getValueField();
            String v = lovDTO.getDisplayField();
            result.forEach(item -> {
                LovValueDTO lovValueDTO = new LovValueDTO();
                lovValueDTO.setMetadata(item);
                if (item.containsKey(k)) {
                    lovValueDTO.setValue(String.valueOf(item.get(k)));
                    if (item.containsKey(v)) {
                        lovValueDTO.setMeaning(String.valueOf(item.get(v)));
                    }
                }
                value.add(lovValueDTO);
            });
            return value;
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * 获取服务全名
     *
     * @param serverCode 服务简码
     * @return 服务全称
     */
    private String getServerName(String serverCode) {
        this.redisHelper.setCurrentDatabase(HZeroService.Admin.REDIS_DB);
        String serverName = redisHelper.hshGet(HZeroService.Admin.CODE + ":routes", serverCode);
        this.redisHelper.clearCurrentDatabase();
        return serverName;
    }

    /**
     * 获取当前登录用户的语言
     *
     * @return 当前语言
     */
    private String getLang() {
        CustomUserDetails user = DetailsHelper.getUserDetails();
        String lang = BaseConstants.DEFAULT_LOCALE_STR;
        if (user != null && user.getLanguage() != null) {
            lang = user.getLanguage();
        }
        return lang;
    }

    @Override
    public String queryLovMeaning(String lovCode, Long tenantId, String value) {
        return queryLovMeaning(lovCode, tenantId, value, this.getLang());
    }

    @Override
    public String queryLovMeaning(String lovCode, Long tenantId, String value, String lang) {
        List<LovValueDTO> list = queryLovValue(lovCode, tenantId, lang);
        for (LovValueDTO dto : list) {
            if (Objects.equals(dto.getValue(), value)) {
                return dto.getMeaning();
            }
        }
        return value;
    }

    private String hashKey(Long tenantId, String lang) {
        if (StringUtils.isBlank(lang)) {
            return String.valueOf(tenantId);
        }
        return tenantId + "-" + lang;
    }

    private Collection<String> listHashKey(Long tenantId, String lang) {
        List<String> hashKeys = new LinkedList<>();
        if (!BaseConstants.DEFAULT_TENANT_ID.equals(tenantId)) {
            hashKeys.add(hashKey(BaseConstants.DEFAULT_TENANT_ID, lang));
        }
        hashKeys.add(hashKey(tenantId, lang));
        return hashKeys;
    }

    /**
     * 值集可访问状态
     *
     * @author gaokuo.dai@hand-china.com 2019年3月1日上午12:09:24
     */
    public enum AccessStatus {
        /**
         * 可访问
         */
        ACCESS,
        /**
         * 禁止访问
         */
        FORBIDDEN,
        /**
         * 未找到
         */
        NOT_FOUND
    }
}
