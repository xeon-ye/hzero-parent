/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign.ribbon;

import com.netflix.client.ClientException;
import com.netflix.client.config.IClientConfig;
import feign.Client;
import feign.Request;
import feign.Response;
import org.hzero.core.variable.RequestVariableHolder;
import org.hzero.route.rule.handler.TargetServiceUrlContext;
import org.hzero.route.rule.repository.TenantUrlRepository;
import org.hzero.route.rule.repository.UrlMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

import java.io.IOException;
import java.net.URI;

/**
 * @author Dave Syer
 */
public class LocalLoadBalancerFeignClient extends LoadBalancerFeignClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalLoadBalancerFeignClient.class);


    private final Client delegate;
    private CachingSpringLoadBalancerFactory lbClientFactory;
    private SpringClientFactory clientFactory;

    private UrlMappingRepository urlMappingRepository;
    private TenantUrlRepository tenantUrlRepository;

    public LocalLoadBalancerFeignClient(Client delegate,
                                        CachingSpringLoadBalancerFactory lbClientFactory,
                                        SpringClientFactory clientFactory,
                                        UrlMappingRepository urlMappingRepository, TenantUrlRepository tenantUrlRepository) {
        super(delegate, lbClientFactory, clientFactory);
        this.delegate = delegate;
        this.lbClientFactory = lbClientFactory;
        this.clientFactory = clientFactory;
        this.urlMappingRepository = urlMappingRepository;
        this.tenantUrlRepository = tenantUrlRepository;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        URI asUri = URI.create(request.url());
        Long tenantId = null;
        Long userId = null;
        try {
            tenantId = RequestVariableHolder.TENANT_ID.get();
            userId = RequestVariableHolder.USER_ID.get();
        } catch (Exception e) {
            LOGGER.warn("TENANT_ID and USER_ID is not saved");
            return lbClientExecute(request, asUri.getHost(), options);
        }

        // 动态路由、url
        TargetServiceUrlContext context = new TargetServiceUrlContext(tenantId, userId, asUri.getHost(), asUri.getPath());
        context.setTargetServiceUrl(urlMappingRepository);

        // url前缀
        context.setUrlPrefix(tenantUrlRepository, request.method());

        // set
        String newUrl = context.getTargetUrl();
        String clientName = context.getTargetService();
        // url修改后记得加参数
        if (!asUri.getRawPath().equals(newUrl)) {
            newUrl = newUrl + (asUri.getRawQuery() == null ? "" : '?' + asUri.getRawQuery());
            request = Request.create(request.method(), newUrl, request.headers(), request.body(), request.charset());
        }
        return lbClientExecute(request, clientName, options);
    }

    private Response lbClientExecute(Request request, String clientName, Request.Options options) throws IOException {
        URI uriWithoutHost = cleanUrl(request.url(), clientName);
        FeignLoadBalancer.RibbonRequest ribbonRequest = new FeignLoadBalancer.RibbonRequest(
                this.delegate, request, uriWithoutHost);

        IClientConfig requestConfig = getClientConfig(options, clientName);
        try {
            return lbClient(clientName).executeWithLoadBalancer(ribbonRequest,
                    requestConfig).toResponse();
        } catch (ClientException e) {
            IOException io = findIOException(e);
            if (io != null) {
                throw io;
            }
            throw new RuntimeException(e);
        }
    }

    private FeignLoadBalancer lbClient(String clientName) {
        return this.lbClientFactory.create(clientName);
    }
}
