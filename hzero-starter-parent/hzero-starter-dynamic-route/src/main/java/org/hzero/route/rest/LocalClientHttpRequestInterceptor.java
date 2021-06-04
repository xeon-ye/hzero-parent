package org.hzero.route.rest;

import org.hzero.core.variable.RequestVariableHolder;
import org.hzero.route.rule.handler.TargetServiceUrlContext;
import org.hzero.route.rule.repository.TenantUrlRepository;
import org.hzero.route.rule.repository.UrlMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * resTemplate 拦截器
 *
 * @author 11838
 */
@Component
public class LocalClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalClientHttpRequestInterceptor.class);

    private UrlMappingRepository urlMappingRepository;

    private TenantUrlRepository tenantUrlRepository;

    private final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
    private ClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

    public LocalClientHttpRequestInterceptor(UrlMappingRepository urlMappingRepository, TenantUrlRepository tenantUrlRepository) {
        this.urlMappingRepository = urlMappingRepository;
        this.tenantUrlRepository = tenantUrlRepository;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // 判断有路由或者url映射，则重构request
        URI uri = request.getURI();
        HttpHeaders httpHeaders = request.getHeaders();
        InterceptingClientHttpRequestFactory factory = new InterceptingClientHttpRequestFactory(requestFactory, interceptors);
        // rest外部请求可能带端口
        String host = uri.getAuthority();
        String path = uri.getPath();
        // try cache 或者 排除特殊path
        Long tenantId = null;
        Long userId = null;
        try {
            tenantId = RequestVariableHolder.TENANT_ID.get();
            userId = RequestVariableHolder.USER_ID.get();
        } catch (Exception e) {
            LOGGER.warn("TENANT_ID and USER_ID is not saved");
            return execution.execute(request, body);
        }

        // 动态路由、url
        TargetServiceUrlContext context = new TargetServiceUrlContext(tenantId, userId, host, path);
        context.setTargetServiceUrl(urlMappingRepository);

        // url前缀
        context.setUrlPrefix(tenantUrlRepository, request.getMethodValue());

        // set
        path = context.getTargetUrl();
        host = context.getTargetService();

        // 重构request
        if (!uri.getPath().equals(path) || !uri.getAuthority().equals(host)) {
            // url加参数
            path = path + (uri.getQuery() == null ? "" : '?' + uri.getQuery());
            try {
                uri = new URI(uri.getScheme(), host, path, uri.getFragment());
            } catch (URISyntaxException e) {
                LOGGER.error("new URI error", e);
            }
            request = factory.createRequest(uri, request.getMethod());
            request.getHeaders().addAll(httpHeaders);
        }
        // end
        return execution.execute(request, body);
    }
}
