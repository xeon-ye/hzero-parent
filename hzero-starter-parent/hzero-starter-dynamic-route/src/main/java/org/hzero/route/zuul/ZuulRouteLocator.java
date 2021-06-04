package org.hzero.route.zuul;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants.Symbol;
import org.hzero.core.variable.RequestVariableHolder;
import org.hzero.route.DynamicRouteProperties;
import org.hzero.route.constant.RouteConstants;
import org.hzero.route.loadbalancer.RouteRequestVariable;
import org.hzero.route.rule.handler.TargetServiceUrlContext;
import org.hzero.route.rule.repository.TenantUrlRepository;
import org.hzero.route.rule.repository.UrlMappingRepository;
import org.hzero.route.rule.vo.TenantUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 刷新路由 增加功能：获取到路由后，根据配置的路由规则修改路径
 * zuul 已不用，现都已用使用getaway，后续类似拦截修改可不修改zuul
 *
 * @author bojiangzhou
 */
public class ZuulRouteLocator extends SimpleRouteLocator implements RefreshableRouteLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZuulRouteLocator.class);

    private AtomicReference<Map<String, ZuulRoute>> routes = new AtomicReference<>();

    private static final int ORDER = -5;

    private final ZuulProperties properties;
    private final TenantUrlRepository tenantUrlRepository;
    private final UrlMappingRepository urlMappingRepository;
    private final DynamicRouteProperties dynamicRouteProperties;


    public ZuulRouteLocator(String servletPath,
                            ZuulProperties properties,
                            TenantUrlRepository tenantUrlRepository,
                            UrlMappingRepository urlMappingRepository,
                            DynamicRouteProperties dynamicRouteProperties) {
        super(servletPath, properties);
        this.properties = properties;
        this.tenantUrlRepository = tenantUrlRepository;
        this.urlMappingRepository = urlMappingRepository;
        this.dynamicRouteProperties = dynamicRouteProperties;
    }

    /**
     * 获取到路由后，根据配置的路由规则修改路径
     */
    @Override
    public Route getMatchingRoute(String path) {
        Route route = super.getMatchingRoute(path);

        if (route != null && HystrixRequestContext.isCurrentThreadInitialized()) {
            Long tenantId = RequestVariableHolder.TENANT_ID.get();
            Long userId = RequestVariableHolder.USER_ID.get();

            // 动态路由、url
            TargetServiceUrlContext context = new TargetServiceUrlContext(tenantId, userId, route.getLocation(), route.getPath());
            context.setTargetServiceUrl(urlMappingRepository);
            // set
            String newUrl = context.getTargetUrl();
            String serviceName = context.getTargetService();

            // 加URL前缀
            if (serviceName != null) {
                TenantUrl tenantUrl = new TenantUrl();
                tenantUrl
                        .setTenantId(tenantId)
                        .setServiceName(serviceName.toLowerCase())
                        .setUrl(newUrl)
                        .setMethod(RouteRequestVariable.URL_METHOD.get())
                ;
                if (dynamicRouteProperties.enableDebugLogger()) {
                    LOGGER.debug(">>>>> matching route: {}", route);
                    LOGGER.debug(">>>>> zuul custom url apply: tenantUrl={}", tenantUrl);
                }
                String urlPrefix = tenantUrlRepository.getTenantCustomUrlPrefix(tenantUrl);
                if (StringUtils.isNotBlank(urlPrefix)) {
                    if (route.isPrefixStripped()) {
                        newUrl = insertPath(newUrl, urlPrefix, 0);
                    } else {
                        newUrl = insertPath(newUrl, urlPrefix, 1);
                    }
                    route.setPath(newUrl);
                    route.setFullPath(route.getPrefix() + route.getPath());
                }
            }
        }
        return route;
    }

    private String insertPath(String originalPath, String insertPath, int index) {
        return Symbol.SLASH + StringUtils.join(ArrayUtils.insert(index, StringUtils.split(originalPath, Symbol.SLASH), insertPath), Symbol.SLASH);
    }

    @Override
    protected Map<String, ZuulRoute> locateRoutes() {
        return Collections.emptyMap();
    }

    @Override
    public List<Route> getRoutes() {
        return Collections.emptyList();
    }

    @Override
    public void refresh() {
        this.routes.set(routes());
    }

    @Override
    protected Map<String, ZuulRoute> getRoutesMap() {
        if (this.routes.get() == null) {
            this.routes.set(routes());
        }
        return this.routes.get();
    }

    protected Map<String, ZuulRoute> routes() {
        LinkedHashMap<String, ZuulRoute> routesMap = new LinkedHashMap<>();
        for (ZuulRoute route : this.properties.getRoutes().values()) {
            routesMap.put(route.getPath(), route);
        }
        return routesMap;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

}
