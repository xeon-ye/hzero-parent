package org.hzero.route.interceptor.mvc;

import org.hzero.route.DynamicRouteProperties;
import org.hzero.route.interceptor.mvc.RequestHeaderInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author XCXCXCXCX
 * @version 1.0
 * @date 2019/11/12 10:20 上午
 */
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private DynamicRouteProperties dynamicRouteProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(new RequestHeaderInterceptor(dynamicRouteProperties))
                .addPathPatterns("/**")
        ;
    }
}
