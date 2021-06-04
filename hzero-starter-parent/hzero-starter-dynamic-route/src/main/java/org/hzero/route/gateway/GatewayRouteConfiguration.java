package org.hzero.route.gateway;

import org.hzero.core.helper.DetailsExtractor;
import org.hzero.route.DynamicRouteProperties;
import org.hzero.route.rule.repository.TenantUrlRepository;
import org.hzero.route.rule.repository.UrlMappingRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author XCXCXCXCX
 * @version 1.0
 * @date 2019/11/12 10:26 上午
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
@Configuration
public class GatewayRouteConfiguration {

    @Bean
    public DynamicRouteToRequestUrlFilter dynamicRouteToRequestUrlFilter(DynamicRouteProperties dynamicRouteProperties,
                                                                         TenantUrlRepository tenantUrlRepository,
                                                                         UrlMappingRepository urlMappingRepository,
                                                                         DetailsExtractor detailsExtractor){
        return new DynamicRouteToRequestUrlFilter(dynamicRouteProperties, tenantUrlRepository, urlMappingRepository, detailsExtractor);
    }
}
