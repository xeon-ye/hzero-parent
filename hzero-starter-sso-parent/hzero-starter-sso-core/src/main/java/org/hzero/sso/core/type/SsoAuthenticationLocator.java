package org.hzero.sso.core.type;

import java.util.Set;

/**
 *
 * @author bojiangzhou 2020/04/15
 */
public class SsoAuthenticationLocator {

    private Set<String> ssoIds;

    public SsoAuthenticationLocator(Set<String> ssoIds) {
        this.ssoIds = ssoIds;
    }

    public boolean ssoRegister(String ssoId) {
        return ssoIds.contains(ssoId);
    }

}
