package org.hzero.route.metadata.parser;

import com.netflix.loadbalancer.Server;
import org.springframework.cloud.zookeeper.discovery.ZookeeperServer;

import java.util.Map;

/**
 * @author XCXCXCXCX
 * @version 1.2.0
 * @date 2020/2/21 1:17 下午
 */
public class ZookeeperMetadataParser implements MetadataParser {

    @Override
    public Map<String, String> parse(Server server) {
        return ((ZookeeperServer) server).getInstance().getPayload().getMetadata();
    }
}
