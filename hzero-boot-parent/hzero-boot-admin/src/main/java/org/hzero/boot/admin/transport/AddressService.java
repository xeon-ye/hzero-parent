package org.hzero.boot.admin.transport;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * Created by wushuai on 2021/5/21
 */
public interface AddressService {

    List<ServiceInstance> getInstances();
}
