package org.hzero.route.metadata.parser;

import java.util.Map;

import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

/**
 * @author XCXCXCXCX
 * @since 1.0
 */
public class EurekaMetadataParser implements MetadataParser {

    @Override
    public Map<String, String> parse(Server server) {
        return ((DiscoveryEnabledServer)server).getInstanceInfo().getMetadata();
    }
}
