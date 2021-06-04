package org.hzero.sso.azure.token;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;

/**
*
* @author minghui.qiu@hand-china.com
*/
public class AzureAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    public AzureAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public AzureAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }
}
