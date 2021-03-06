package org.hzero.boot.admin.transport;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

/**
 * Created by wushuai on 2021/5/21
 */
public class DiscoveryAddressService implements AddressService {

    private DiscoveryClient discoveryClient;

    private AdminClientProperties properties;

    public DiscoveryAddressService(DiscoveryClient discoveryClient, AdminClientProperties properties) {
        this.discoveryClient = discoveryClient;
        this.properties = properties;
    }

    @Override
    public List<ServiceInstance> getInstances() {
        return discoveryClient.getInstances(properties.getDiscovery().getServiceId());
    }
}
