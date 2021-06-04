package org.hzero.route.loadbalancer;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableDefault;

import java.util.HashSet;
import java.util.Set;

/**
 * 线程级变量
 *
 * @author bojiangzhou 2018/09/28
 */
public class RouteRequestVariable {

    /**
     * 在Feign处，如果用户有个性化节点组，存储当前用户的节点组ID
     */
    public static final HystrixRequestVariableDefault<Set<String>> NODE_GROUP_ID = new HystrixRequestVariableDefault<Set<String>>() {
        @Override
        public Set<String> initialValue() {
            return new HashSet<>();
        }
    };

    /**
     * 在Zuul层，存储当前访问URL的方法
     */
    public static final HystrixRequestVariableDefault<String> URL_METHOD = new HystrixRequestVariableDefault<>();

    private RouteRequestVariable() {}
}
