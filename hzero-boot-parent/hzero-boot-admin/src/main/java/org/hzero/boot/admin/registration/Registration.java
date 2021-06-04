package org.hzero.boot.admin.registration;

import java.util.Map;

/**
 * Created by wushuai on 2021/5/21
 */
public interface Registration {

    String getServiceName();

    String getVersion();

    String getHealthUrl();

    Map<String, String> getMetadata();
}
