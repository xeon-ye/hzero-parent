package org.hzero.route;

import org.hzero.core.redis.RedisHelper;
import org.hzero.route.gateway.GatewayRouteConfiguration;
import org.hzero.route.interceptor.flux.WebFluxConfiguration;
import org.hzero.route.interceptor.mvc.WebMvcConfiguration;
import org.hzero.route.rule.repository.NodeGroupRepository;
import org.hzero.route.rule.repository.TenantUrlRepository;
import org.hzero.route.rule.repository.UrlMappingRepository;
import org.hzero.route.rule.repository.impl.NodeGroupServiceImpl;
import org.hzero.route.rule.repository.impl.TenantUrlServiceImpl;
import org.hzero.route.rule.repository.impl.UrlMappingServiceImpl;
import org.hzero.route.zuul.ZuulRouteConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author XCXCXCXCX
 * @date 2019/9/9
 * @project hzero-gateway
 */
@Configuration
@EnableConfigurationProperties(DynamicRouteProperties.class)
@Import({
        WebMvcConfiguration.class,
        WebFluxConfiguration.class,
        ZuulRouteConfiguration.class,
        GatewayRouteConfiguration.class,
        InnerInvokeConfiguration.class
})
public class DynamicRouteAutoConfiguration {

    @Autowired
    private RedisHelper redisHelper;

    @Bean
    @ConditionalOnMissingBean
    public NodeGroupRepository nodeGroupRepository() {
        return new NodeGroupServiceImpl(redisHelper);
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantUrlRepository tenantUrlRepository() {
        return new TenantUrlServiceImpl(redisHelper);
    }

    @Bean
    @ConditionalOnMissingBean
    public UrlMappingRepository urlMappingRepository() {
        return new UrlMappingServiceImpl(redisHelper);
    }

}
