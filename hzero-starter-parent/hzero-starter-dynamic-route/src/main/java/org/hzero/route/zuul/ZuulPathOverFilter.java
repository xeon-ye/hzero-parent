package org.hzero.route.zuul;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.netflix.zuul.ZuulFilter;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;

/**
 * 请求结束，清理线程变量
 *
 * @author bojiangzhou 2018/09/28
 */
public class ZuulPathOverFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.SEND_RESPONSE_FILTER_ORDER + 100;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    /**
     * 清理线程变量
     */
    @Override
    public Object run() {
        if (HystrixRequestContext.isCurrentThreadInitialized()) {
            HystrixRequestContext.getContextForCurrentThread().shutdown();
        }
        return null;
    }


}
