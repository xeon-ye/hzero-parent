package org.hzero.route.interceptor.flux;

import org.hzero.route.DynamicRouteProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author XCXCXCXCX
 * @version 1.0
 * @date 2019/11/12 10:41 上午
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@Configuration
public class WebFluxConfiguration {

    @Bean
    public HandlerFilterFunction<ServerResponse, ServerResponse> requestHeaderFilterFunction(DynamicRouteProperties dynamicRouteProperties){
        return new RequestHeaderFilterFunction(dynamicRouteProperties);
    }

}
