package org.hzero.route.metadata.parser;

import java.util.Map;

import com.baidu.formula.discovery.autoconfigure.FormulaDiscoveryServer;
import com.netflix.loadbalancer.Server;

/**
 * @author XCXCXCXCX
 * @since 1.0
 */
public class FormulaMetadataParser implements MetadataParser {
    @Override
    public Map<String, String> parse(Server server) {
        return ((FormulaDiscoveryServer)server).getMetadata();
    }
}
