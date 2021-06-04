package org.hzero.route.metadata.parser;

import java.util.Map;

import org.springframework.cloud.alibaba.nacos.ribbon.NacosServer;

import com.netflix.loadbalancer.Server;

/**
 * @author XCXCXCXCX
 * @since 1.0
 */
public class NacosMetadataParser implements MetadataParser {

    @Override
    public Map<String, String> parse(Server server) {
        return ((NacosServer)server).getMetadata();
    }
}
