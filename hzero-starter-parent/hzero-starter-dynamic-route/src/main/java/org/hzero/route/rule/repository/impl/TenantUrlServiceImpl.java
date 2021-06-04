package org.hzero.route.rule.repository.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.base.BaseConstants.Symbol;
import org.hzero.core.redis.RedisHelper;
import org.hzero.route.constant.RouteConstants;
import org.hzero.route.loadbalancer.RouteRequestVariable;
import org.hzero.route.rule.repository.TenantUrlRepository;
import org.hzero.route.rule.vo.TenantUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.Set;

/**
 * 获取客户化URL
 *
 * @author bojiangzhou 2018/09/28
 */
public class TenantUrlServiceImpl implements TenantUrlRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantUrlServiceImpl.class);

    private RedisHelper redisHelper;

    public TenantUrlServiceImpl(RedisHelper redisHelper) {
        this.redisHelper = redisHelper;
    }

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public String getTenantCustomUrlPrefix(TenantUrl tenantUrl) {
        String urlNodeKey = tenantUrl.getUrlNodeKey();

        redisHelper.setCurrentDatabase(RouteConstants.Admin.REDIS_DB);
        // 获取所有 url key，之后匹配地址
        try {
            Set<String> urlNodeHashKeys = redisHelper.hshKeys(urlNodeKey);
            String urlNodeHashKey = null;
            for (String nodeHashKey : urlNodeHashKeys) {
                String[] arr = nodeHashKey.split(Symbol.WELL);
                if (StringUtils.equalsIgnoreCase(arr[1], tenantUrl.getMethod()) && pathMatcher.match(arr[0], tenantUrl.getUrl())) {
                    urlNodeHashKey = nodeHashKey;
                    break;
                }
            }

            if (StringUtils.isBlank(urlNodeHashKey)) {
                return null;
            }

            String nodeGroupRules = redisHelper.hshGet(urlNodeKey, urlNodeHashKey);
            String urlPrefix = urlNodeHashKey.split(Symbol.WELL)[2];
            if (StringUtils.isNotBlank(nodeGroupRules)) {
                // URL 前缀
                tenantUrl.setUrlPrefix(urlPrefix);
                // 缓存客制化URL对应的节点组
                RouteRequestVariable.NODE_GROUP_ID.get().addAll(Arrays.asList(nodeGroupRules.split(Symbol.COMMA)));
                return tenantUrl.getCustomUrlPrefix();
            }
        } catch (Exception e) {
            LOGGER.error("get tenant custom url prefix error. ex = {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            redisHelper.clearCurrentDatabase();
        }

        return null;
    }

    @Override
    public String insertPath(String path, String urlPrefix, int index) {
        return Symbol.SLASH + StringUtils.join(ArrayUtils.insert(index, StringUtils.split(path, Symbol.SLASH), urlPrefix), Symbol.SLASH);
    }

}
