package org.hzero.boot.platform.entity.autoconfigure;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.hzero.boot.platform.entity.feign.EntityRegistFeignClient;

/**
 * description
 *
 * @author xingxing.wu@hand-china.com 2019/07/08 16:10
 */
@Configuration
@EnableAsync
@ComponentScan("org.hzero.boot.platform.entity")
@EnableFeignClients(basePackageClasses = EntityRegistFeignClient.class)
@EnableConfigurationProperties({EntityRegistProperties.class})
@EntityRegistScan(basePackages = {EntityRegistScannerRegistrar.DEFAULT_BASE_PACKAGE})
public class EntityRegistAutoConfiguration {

    @Bean
    @Qualifier("entityRegistAsyncTaskExecutor")
    public AsyncTaskExecutor entityRegistAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("entityRegistAsyncTaskExecutor");
        executor.setMaxPoolSize(3);
        executor.setCorePoolSize(2);
        return executor;
    }
}
