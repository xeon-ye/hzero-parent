package org.hzero.route;

import feign.Client;
import org.hzero.core.HZeroAutoConfiguration;
import org.hzero.core.redis.RedisHelper;
import org.hzero.route.feign.FeignRouteInterceptor;
import org.hzero.route.rest.LocalClientHttpRequestInterceptor;
import org.hzero.route.rule.repository.NodeGroupRepository;
import org.hzero.route.rule.repository.TenantUrlRepository;
import org.hzero.route.rule.repository.UrlMappingRepository;
import org.hzero.route.rule.repository.impl.NodeGroupServiceImpl;
import org.hzero.route.rule.repository.impl.TenantUrlServiceImpl;
import org.hzero.route.rule.repository.impl.UrlMappingServiceImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.openfeign.ribbon.LocalLoadBalancerFeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置
 *
 * @author bojiangzhou 2018/09/28
 */
@Configuration
@AutoConfigureAfter(HZeroAutoConfiguration.class)
public class InnerInvokeConfiguration {

    /**
     * 网关层不启用
     */
    @Bean
    @ConditionalOnMissingClass({
            "com.netflix.zuul.ZuulFilter",
            "org.springframework.cloud.gateway.filter.GlobalFilter"
    })
    public FeignRouteInterceptor tenantInterceptor(TenantUrlRepository tenantUrlRepository, UrlMappingRepository urlMappingRepository, DynamicRouteProperties properties) {
        return new FeignRouteInterceptor(tenantUrlRepository, urlMappingRepository, properties);
    }

    /**
     * feign + restTemplate
     */
    @Bean
    public BeanPostProcessor feignBeanPostProcessor(CachingSpringLoadBalancerFactory lbClientFactory,
                                                    SpringClientFactory clientFactory,
                                                    UrlMappingRepository urlMappingRepository,
                                                    TenantUrlRepository tenantUrlRepository) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(@Nonnull Object bean, String beanName) throws BeansException {
                if (bean instanceof LoadBalancerFeignClient) {
                    return new LocalLoadBalancerFeignClient(new Client.Default(null, null), lbClientFactory, clientFactory, urlMappingRepository, tenantUrlRepository);
                }
                if (bean instanceof RestTemplate) {
                    RestTemplate restTemplate = ((RestTemplate) bean);
                    List<ClientHttpRequestInterceptor> list = new ArrayList<>();
                    for (ClientHttpRequestInterceptor clientHttpRequestInterceptor : restTemplate.getInterceptors()) {
                        if (!(clientHttpRequestInterceptor instanceof LocalClientHttpRequestInterceptor)) {
                            list.add(clientHttpRequestInterceptor);
                        }
                    }
                    list.add(new LocalClientHttpRequestInterceptor(urlMappingRepository, tenantUrlRepository));
                    restTemplate.setInterceptors(list);
                }
                return bean;
            }
        };
    }

    /**
     * restTemplate
     *
     * @param urlMappingRepository
     * @return
     */
    @Bean
    @LoadBalanced
    @ConditionalOnMissingBean
    public RestTemplate restTemplate(UrlMappingRepository urlMappingRepository, TenantUrlRepository tenantUrlRepository) {

        RestTemplate restTemplate = new RestTemplate();
        // 把自定义的ClientHttpRequestInterceptor添加到RestTemplate，可添加多个
        restTemplate.setInterceptors(Collections.singletonList(new LocalClientHttpRequestInterceptor(urlMappingRepository, tenantUrlRepository)));
        return restTemplate;

    }

}
