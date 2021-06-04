package org.hzero.boot.admin.transport;

import org.hzero.boot.admin.exception.TransportException;
import org.hzero.boot.admin.registration.Registration;

/**
 * Created by wushuai on 2021/5/21
 */
public interface Transport {

    /**
     * 发起请求的通讯方法
     * 会抛异常，如果无法通讯成功，则服务反复报错，直到通讯成功
     *
     * @throws TransportException
     */
    void transport(Registration registration) throws TransportException;
}
