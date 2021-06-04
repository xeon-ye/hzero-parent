package org.hzero.route.metadata.parser;

import com.alibaba.cloud.nacos.ribbon.NacosServer;
import com.netflix.loadbalancer.Server;

import java.util.Map;

/**
 * @author XCXCXCXCX
 * @version 1.0
 * @date 2019/11/12 9:56 上午
 */
public class NacosV2MetadataParser implements MetadataParser {

    @Override
    public Map<String, String> parse(Server server) {
        return ((NacosServer)server).getMetadata();
    }
}
