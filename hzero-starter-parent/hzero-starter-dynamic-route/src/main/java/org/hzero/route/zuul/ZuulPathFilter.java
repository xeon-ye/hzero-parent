package org.hzero.route.zuul;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.choerodon.core.oauth.CustomUserDetails;
import org.hzero.core.helper.DetailsExtractor;
import org.hzero.core.variable.RequestVariableHolder;
import org.hzero.route.loadbalancer.RouteRequestVariable;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;

import javax.servlet.http.HttpServletRequest;

/**
 * Zuul 层缓存当前用户ID和用户ID，以及请求的URL
 *
 * @author bojiangzhou 2018/09/28
 */
public class ZuulPathFilter extends ZuulFilter {

    private final DetailsExtractor detailsExtractor;

    private static final int AFTER_HEADER_WRAPPER_FILTER = 0;

    public ZuulPathFilter(DetailsExtractor detailsExtractor) {
        this.detailsExtractor = detailsExtractor;
    }

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return AFTER_HEADER_WRAPPER_FILTER;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean shouldFilter() {
        RequestContext requestContext = RequestContext.getCurrentContext();

        return requestContext.getZuulRequestHeaders().containsKey(RequestVariableHolder.HEADER_JWT.toLowerCase());
    }

    /**
     * 客制化URL
     */
    @Override
    public Object run() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        CustomUserDetails details = detailsExtractor.extractDetails(request);


        if (details == null) {
            return null;
        }

        if (!HystrixRequestContext.isCurrentThreadInitialized()) {
            HystrixRequestContext.initializeContext();
        }

        Long tenantId = details.getOrganizationId();
        if (tenantId != null) {
            RequestVariableHolder.TENANT_ID.set(tenantId);
        }

        Long userId = details.getUserId();
        if (userId != null) {
            RequestVariableHolder.USER_ID.set(userId);
        }

        RouteRequestVariable.URL_METHOD.set(request.getMethod().toLowerCase());

        return null;
    }


}
