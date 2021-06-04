package org.hzero.sso.idm.provider;

import org.hzero.sso.core.exception.AccountNotExistsException;
import org.hzero.sso.idm.token.IdmAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

/**
 *
 * @author minghui.qiu@hand-china.com
 */
public class IdmAuthenticationProvider implements AuthenticationProvider {

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();
    
    private AuthenticationUserDetailsService<IdmAuthenticationToken> authenticationUserDetailsService;

    public IdmAuthenticationProvider(AuthenticationUserDetailsService<IdmAuthenticationToken> authenticationUserDetailsService) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(IdmAuthenticationToken.class, authentication,
                        "Only IdmAuthenticationToken is supported");
        Assert.notNull(authentication.getPrincipal(), "User account is Not Exists");
        String userName = authentication.getName();
        final IdmAuthenticationToken authenticationToken = new IdmAuthenticationToken(userName, authentication.getCredentials());
        UserDetails user = this.authenticationUserDetailsService.loadUserDetails(authenticationToken);
        Assert.notNull(user, "User account is Not Exists");
        return createSuccessAuthentication(user, authentication, user);
    }

    protected Authentication createSuccessAuthentication(Object principal, Authentication authentication,
                                                         UserDetails user) {
        IdmAuthenticationToken result =
                        new IdmAuthenticationToken(principal,authentication.getCredentials(), authoritiesMapper.mapAuthorities(user.getAuthorities()));
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (IdmAuthenticationToken.class.isAssignableFrom(authentication));
    }

    public AuthenticationUserDetailsService<IdmAuthenticationToken> getAuthenticationUserDetailsService() {
        return authenticationUserDetailsService;
    }

    public void setAuthenticationUserDetailsService(AuthenticationUserDetailsService<IdmAuthenticationToken> authenticationUserDetailsService) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
    }


}
