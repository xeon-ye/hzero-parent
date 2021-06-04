package org.hzero.route;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * 配置类
 *
 * @author bojiangzhou 2018/12/21
 */
@RefreshScope
@ConfigurationProperties(prefix = DynamicRouteProperties.PREFIX)
public class DynamicRouteProperties {

    static final String PREFIX = "hzero.dynamic-route";

    private boolean enableDebugLogger = false;

    public boolean enableDebugLogger() {
        return enableDebugLogger;
    }

    public void setEnableDebugLogger(boolean enableDebugLogger) {
        this.enableDebugLogger = enableDebugLogger;
    }
}
