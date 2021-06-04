package org.hzero.route.metadata.parser;

import java.util.Map;

import com.netflix.loadbalancer.Server;

/**
 * @author XCXCXCXCX
 * @since 1.0
 */
public interface MetadataParser {

    Map<String, String> parse(Server server);

}
