package org.hzero.route.feign;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import feign.RequestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.variable.RequestVariableHolder;
import org.hzero.feign.aspect.FeignVariableHolder;
import org.hzero.feign.interceptor.FeignRequestInterceptor;
import org.hzero.route.DynamicRouteProperties;
import org.hzero.route.rule.repository.TenantUrlRepository;
import org.hzero.route.rule.repository.UrlMappingRepository;
import org.hzero.route.rule.vo.TenantUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 传递租户id，并且动态修改url
 *
 * @author bojiangzhou 2018/09/28
 */
public class FeignRouteInterceptor implements FeignRequestInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeignRouteInterceptor.class);

    private TenantUrlRepository tenantUrlRepository;
    private UrlMappingRepository urlMappingRepository;
    private DynamicRouteProperties properties;


    public FeignRouteInterceptor(TenantUrlRepository tenantUrlRepository, UrlMappingRepository urlMappingRepository, DynamicRouteProperties properties) {
        this.tenantUrlRepository = tenantUrlRepository;
        this.urlMappingRepository = urlMappingRepository;
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public void apply(RequestTemplate template) {

        if (!HystrixRequestContext.isCurrentThreadInitialized()) {
            if (properties.enableDebugLogger()) {
                LOGGER.debug(">>>>> FeignClientAspect may not intercept the FeignClient, please check your FeignClient if in the package <*.infra.feign.*> .");
            }
            return;
        }
        // 获取服务名称
        String serviceName = FeignVariableHolder.FEIGN_SERVICE_NAME.get();
        Long tenantId = RequestVariableHolder.TENANT_ID.get();

        if (tenantId != null && StringUtils.isNotBlank(serviceName)) {
            TenantUrl tenantUrl = new TenantUrl();
            tenantUrl
                    .setTenantId(tenantId)
                    .setServiceName(serviceName)
                    .setUrl(template.url())
                    .setMethod(template.method())
            ;
            if (properties.enableDebugLogger()) {
                LOGGER.debug(">>>>> feign custom url apply: tenantUrl={}", tenantUrl);
            }
            // URL前缀
            /*String urlPrefix = tenantUrlRepository.getTenantCustomUrlPrefix(tenantUrl);
            if (StringUtils.isNotEmpty(urlPrefix)) {
                template.insert(0, urlPrefix);
                if (properties.enableDebugLogger()) {
                    LOGGER.debug(">>>>> feign custom url apply: serviceName={}, tenantId={}, urlPrefix={}, newUrl={}",
                        serviceName, tenantId, urlPrefix, template.url());
                }
            }*/

        }
    }

}
