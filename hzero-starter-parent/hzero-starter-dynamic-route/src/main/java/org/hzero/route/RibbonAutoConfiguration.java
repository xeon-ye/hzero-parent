package org.hzero.route;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.IRule;
import org.hzero.route.loadbalancer.CustomMetadataRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author XCXCXCXCX
 * @date 2020/5/25 8:51 下午
 */
@Configuration
@ConditionalOnMissingBean(IRule.class)
@AutoConfigureBefore(org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration.class)
@RibbonClients(defaultConfiguration = CustomMetadataRule.class)
public class RibbonAutoConfiguration {

    @Autowired(required = false)
    private IClientConfig config;

    @Bean
    public CustomMetadataRule customMetadataRule() {
        CustomMetadataRule rule = new CustomMetadataRule();
        rule.initWithNiwsConfig(config);
        return rule;
    }

}
