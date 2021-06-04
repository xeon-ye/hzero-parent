package org.hzero.route;

import org.hzero.route.metadata.parser.EurekaMetadataParser;
import org.hzero.route.metadata.parser.FormulaMetadataParser;
import org.hzero.route.metadata.parser.MetadataParser;
import org.hzero.route.metadata.parser.NacosMetadataParser;
import org.hzero.route.metadata.parser.NacosV2MetadataParser;
import org.hzero.route.metadata.parser.ZookeeperMetadataParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author XCXCXCXCX
 * @date 2020/5/25 9:31 下午
 */
@Configuration
public class RibbonMetadataParserAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "com.netflix.niws.loadbalancer.DiscoveryEnabledServer")
    public MetadataParser eurekaMetadataParser() {
        return new EurekaMetadataParser();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.cloud.alibaba.nacos.ribbon.NacosServer")
    public MetadataParser nacosMetadataParser() {
        return new NacosMetadataParser();
    }

    @Bean
    @ConditionalOnClass(name = "com.alibaba.cloud.nacos.ribbon.NacosServer")
    public MetadataParser nacosV2MetadataParser() {
        return new NacosV2MetadataParser();
    }

    @Bean
    @ConditionalOnClass(name = "com.baidu.formula.discovery.autoconfigure.FormulaDiscoveryServer")
    public MetadataParser formulaMetadataParser() {
        return new FormulaMetadataParser();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.cloud.zookeeper.discovery.ZookeeperServer")
    public MetadataParser zookeeperMetadataParser() {
        return new ZookeeperMetadataParser();
    }

}
