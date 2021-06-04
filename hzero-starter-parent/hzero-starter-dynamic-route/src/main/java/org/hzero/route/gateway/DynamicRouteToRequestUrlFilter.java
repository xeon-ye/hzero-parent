package org.hzero.route.gateway;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import io.choerodon.core.oauth.CustomUserDetails;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.helper.DetailsExtractor;
import org.hzero.core.variable.RequestVariableHolder;
import org.hzero.route.DynamicRouteProperties;
import org.hzero.route.loadbalancer.RouteRequestVariable;
import org.hzero.route.rule.handler.TargetServiceUrlContext;
import org.hzero.route.rule.repository.TenantUrlRepository;
import org.hzero.route.rule.repository.UrlMappingRepository;
import org.hzero.route.rule.vo.TenantUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * @author XCXCXCXCX
 * @version 1.0
 * @date 2019/11/12 2:17 下午
 */
public class DynamicRouteToRequestUrlFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicRouteToRequestUrlFilter.class);

    private final DynamicRouteProperties dynamicRouteProperties;

    private final TenantUrlRepository tenantUrlRepository;

    private final UrlMappingRepository urlMappingRepository;

    private final DetailsExtractor detailsExtractor;

    public DynamicRouteToRequestUrlFilter(DynamicRouteProperties dynamicRouteProperties,
                                          TenantUrlRepository tenantUrlRepository,
                                          UrlMappingRepository urlMappingRepository,
                                          DetailsExtractor detailsExtractor) {
        this.dynamicRouteProperties = dynamicRouteProperties;
        this.tenantUrlRepository = tenantUrlRepository;
        this.urlMappingRepository = urlMappingRepository;
        this.detailsExtractor = detailsExtractor;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        preFilter(exchange.getRequest());
        return doFilter(exchange, chain);
    }

    private void preFilter(ServerHttpRequest request) {
        if (!HystrixRequestContext.isCurrentThreadInitialized()) {
            HystrixRequestContext.initializeContext();
        }

        CustomUserDetails details = detailsExtractor.extractDetails(request);

        if (details == null) {
            return;
        }

        Long tenantId = details.getOrganizationId();
        if (tenantId != null) {
            RequestVariableHolder.TENANT_ID.set(tenantId);
        }

        Long userId = details.getUserId();
        if (userId != null) {
            RequestVariableHolder.USER_ID.set(userId);
        }

        if (request.getMethod() == null) {
            return;
        }

        RouteRequestVariable.URL_METHOD.set(request.getMethod().toString().toLowerCase());

        //初始化用户上下文信息
        String username = details.getUsername();
        if (username != null) {
            UserContext.getContext().setUsername(username);
        }
    }

    private Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain) {

        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return chain.filter(exchange);
        }
        boolean stripPrefix = ifStripPrefix(route);
        LOGGER.trace("DynamicRouteToRequestUrlFilter start");

        URI originUri = exchange.getRequest().getURI();
        URI uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        if (uri == null) {
            LOGGER.error("DynamicRouteToRequestUrlFilter filter failed, GATEWAY_REQUEST_URL_ATTR is not found, RouteToRequestUrlFilter may be invalid.");
            return chain.filter(exchange);
        }

        Long tenantId = RequestVariableHolder.TENANT_ID.get();
        Long userId = RequestVariableHolder.USER_ID.get();

        // 动态路由、url
        TargetServiceUrlContext context = new TargetServiceUrlContext(tenantId, userId, uri.getHost(), uri.getPath());
        context.setTargetServiceUrl(urlMappingRepository);
        // set
        String newUrl = context.getTargetUrl();
        String serviceName = context.getTargetService();

        if (serviceName != null) {
            // 加URL前缀
            TenantUrl tenantUrl = new TenantUrl();
            tenantUrl
                    .setTenantId(tenantId)
                    .setServiceName(serviceName.toLowerCase())
                    .setUrl(newUrl)
                    .setMethod(RouteRequestVariable.URL_METHOD.get());
            if (dynamicRouteProperties.enableDebugLogger()) {
                LOGGER.debug(">>>>> matching route: {}", route);
                LOGGER.debug(">>>>> gateway custom url apply: tenantUrl={}", tenantUrl);
            }
            String urlPrefix = tenantUrlRepository.getTenantCustomUrlPrefix(tenantUrl);
            if (StringUtils.isNotBlank(urlPrefix)) {
                if (stripPrefix) {
                    newUrl = insertPath(newUrl, urlPrefix, 0);
                } else {
                    newUrl = insertPath(newUrl, urlPrefix, 1);
                }
            }
        }

        URI newUri = UriComponentsBuilder.fromUri(originUri)
                .scheme(uri.getScheme())
                .host(serviceName)
                .port(uri.getPort())
                .replacePath(encodeChinese(newUrl))
                .build(isAllEncoded(uri))
                .toUri();

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, newUri);
        // exchange.getRequest()中也要改变url
        ServerHttpRequest newRequest = exchange.getRequest().mutate().path(newUrl).build();
        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
        return chain.filter(newExchange);
    }

    private boolean isAllEncoded(URI uri) {
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null) {
            return false;
        }
        String[] queries = rawQuery.split("&");
        if (queries.length > 0) {
            for (String query : queries) {
                if (!query.contains("%")) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    // [\u4e00-\u9fa5，。？！￥（）—【】；、‘’“”《》]
    private final static Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5\\uff0c\\u3002\\uff1f\\uff01\\uffe5\\uff08\\uff09\\u2014\\u3010\\u3011\\uff1b\\u3001\\u2018\\u2019\\u201c\\u201d\\u300a\\u300b]++");

    private static String encodeChinese(String str) {
        try {
            Matcher matcher = pattern.matcher(str);
            String tmp = "";
            while (matcher.find()) {
                tmp = matcher.group();
                str = str.replaceFirst(tmp, URLEncoder.encode(tmp, Charsets.UTF_8.name()));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    private String insertPath(String originalPath, String insertPath, int index) {
        return BaseConstants.Symbol.SLASH + StringUtils.join(ArrayUtils.insert(index, StringUtils.split(originalPath, BaseConstants.Symbol.SLASH), insertPath), BaseConstants.Symbol.SLASH);
    }

    private boolean ifStripPrefix(Route route) {
        List<GatewayFilter> filters = route.getFilters();
        if (filters.isEmpty()) {
            return false;
        } else {
            for (GatewayFilter filter : filters) {
                if (filter instanceof OrderedGatewayFilter
                        && ((OrderedGatewayFilter) filter).getDelegate().getClass().getName().contains("StripPrefixGatewayFilterFactory")) {
                    return true;
                }
            }
            ;
            return false;
        }
    }

    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }
}
