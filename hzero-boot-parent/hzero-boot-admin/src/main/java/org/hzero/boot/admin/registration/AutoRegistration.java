package org.hzero.boot.admin.registration;

/**
 * Created by wushuai on 2021/5/21
 */
public interface AutoRegistration {
    /**
     * 注册
     */
    void register(Registration registration);

    /**
     * 注销
     */
    void unregister(Registration registration);
}
