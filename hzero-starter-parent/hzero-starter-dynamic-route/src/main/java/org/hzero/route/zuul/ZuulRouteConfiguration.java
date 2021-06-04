package org.hzero.route.zuul;

import org.hzero.core.helper.DetailsExtractor;
import org.hzero.route.DynamicRouteProperties;
import org.hzero.route.rule.repository.TenantUrlRepository;
import org.hzero.route.rule.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关层自动配置类
 *
 * @author bojiangzhou 2018/09/28
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "com.netflix.zuul.ZuulFilter")
@Configuration
public class ZuulRouteConfiguration {

    @Autowired
    protected ServerProperties server;
    @Autowired
    private ZuulProperties zuulProperties;
    @Autowired
    private DispatcherServletPath dispatcherServletPath;
    @Autowired
    private TenantUrlRepository tenantUrlRepository;
    @Autowired
    private UrlMappingRepository urlMappingRepository;
    @Autowired
    private DynamicRouteProperties dynamicRouteProperties;

    @Bean
    @ConditionalOnMissingBean
    public ZuulPathFilter zuulPathFilter(DetailsExtractor detailsExtractor) {
        return new ZuulPathFilter(detailsExtractor);
    }

    @Bean
    @ConditionalOnMissingBean
    public ZuulPathOverFilter zuulPathOverFilter() {
        return new ZuulPathOverFilter();
    }

    @Bean
    public RouteLocator memoryRouteLocator() {
        return new ZuulRouteLocator(this.dispatcherServletPath.getPrefix(), this.zuulProperties, tenantUrlRepository, urlMappingRepository, dynamicRouteProperties);
    }
}
