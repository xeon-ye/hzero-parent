package org.hzero.route.interceptor.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import org.hzero.core.variable.RequestVariableHolder;

import org.hzero.route.DynamicRouteProperties;

/**
 * 服务中拦截并缓存当前用户ID和租户ID
 *
 * @author bojiangzhou 2018/09/28
 */
public class RequestHeaderInterceptor extends HandlerInterceptorAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHeaderInterceptor.class);

    private final DynamicRouteProperties properties;

    public RequestHeaderInterceptor(DynamicRouteProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!HystrixRequestContext.isCurrentThreadInitialized()) {
            HystrixRequestContext.initializeContext();
        }

        String labelString = request.getHeader(RequestVariableHolder.HEADER_LABEL);
        if (properties.enableDebugLogger()) {
            LOGGER.debug("X-Eureka-Label:{}", labelString);
        }
        RequestVariableHolder.LABEL.set(labelString);

        CustomUserDetails details = DetailsHelper.getUserDetails();
        if (details != null) {
            RequestVariableHolder.TENANT_ID.set(details.getOrganizationId());
            RequestVariableHolder.USER_ID.set(details.getUserId());
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                    ModelAndView modelAndView) throws Exception {
        if (HystrixRequestContext.isCurrentThreadInitialized()) {
            HystrixRequestContext.getContextForCurrentThread().shutdown();
        }
    }
}
