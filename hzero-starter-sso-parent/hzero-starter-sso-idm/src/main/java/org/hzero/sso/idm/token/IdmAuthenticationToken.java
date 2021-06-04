package org.hzero.sso.idm.token;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
*
* @author minghui.qiu@hand-china.com
*/
public class IdmAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    public IdmAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public IdmAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }
}
