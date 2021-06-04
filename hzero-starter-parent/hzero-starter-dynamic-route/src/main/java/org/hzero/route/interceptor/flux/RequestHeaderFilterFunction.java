package org.hzero.route.interceptor.flux;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import org.hzero.core.variable.RequestVariableHolder;
import org.hzero.route.DynamicRouteProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author XCXCXCXCX
 * @version 1.0
 * @date 2019/11/12 10:48 上午
 */
public class RequestHeaderFilterFunction implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHeaderFilterFunction.class);

    private final DynamicRouteProperties properties;

    public RequestHeaderFilterFunction(DynamicRouteProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {

        initHystrixContext();

        List<String> labelString = request.headers().header(RequestVariableHolder.HEADER_LABEL);
        if (properties.enableDebugLogger()) {
            LOGGER.debug("X-Eureka-Label:{}", labelString);
        }
        if (labelString.size() != 1){
            LOGGER.warn("X-Eureka-Label contain 0 or more than 1 value, ignore it");
            return next.handle(request);
        }

        RequestVariableHolder.LABEL.set(labelString.get(0));

        CustomUserDetails details = DetailsHelper.getUserDetails();
        if (details != null) {
            RequestVariableHolder.TENANT_ID.set(details.getOrganizationId());
            RequestVariableHolder.USER_ID.set(details.getUserId());
        }

        return next.handle(request)
                .doOnSuccess(response -> cleanHystrixContext())
                .doOnError(error -> cleanHystrixContext());
    }

    private void initHystrixContext(){
        if (!HystrixRequestContext.isCurrentThreadInitialized()) {
            HystrixRequestContext.initializeContext();
        }
    }

    private void cleanHystrixContext(){
        if (HystrixRequestContext.isCurrentThreadInitialized()) {
            HystrixRequestContext.getContextForCurrentThread().shutdown();
        }
    }

}
