package org.hzero.sso.core.type;

import java.util.Set;

/**
 * SSO 类型
 *
 * @author bojiangzhou 2020/04/15
 */
public interface SsoRegister {

    /**
     * @return 返回SSO注册ID
     */
    Set<String> ids();
}
