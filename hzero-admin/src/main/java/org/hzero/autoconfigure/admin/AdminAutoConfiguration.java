package org.hzero.autoconfigure.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import io.choerodon.resource.annoation.EnableChoerodonResourceServer;
import org.hzero.admin.api.controller.v1.ServiceInitRegistryEndpoint;
import org.hzero.admin.config.ConfigProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author XCXCXCXCX
 * @date 2019/9/6
 * @project hzero-admin
 */
@ComponentScan(value = {
        "org.hzero.admin.api",
        "org.hzero.admin.app",
        "org.hzero.admin.config",
        "org.hzero.admin.domain",
        "org.hzero.admin.infra",
        "org.hzero.plugin.admin"
})
@EnableAdminServer
@EnableAsync
@EnableFeignClients(basePackages = "org.hzero.admin.infra.feign")
@EnableConfigurationProperties({
        ConfigProperties.class
})
@EnableChoerodonResourceServer
@Configuration
public class AdminAutoConfiguration {

    @Bean
    public ServiceInitRegistryEndpoint serviceInitRegistryEndpoint() {
        return new ServiceInitRegistryEndpoint();
    }

    @Bean("async-gateway-notifier")
    public ThreadPoolTaskExecutor asyncGatewayNotifier() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setDaemon(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("async-context-refresh-notifier")
    public ThreadPoolTaskExecutor asyncContextRefreshNotifier() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setDaemon(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}
