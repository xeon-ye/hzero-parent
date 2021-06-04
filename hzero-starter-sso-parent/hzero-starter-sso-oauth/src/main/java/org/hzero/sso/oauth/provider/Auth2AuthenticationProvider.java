package org.hzero.sso.oauth.provider;

import org.hzero.sso.oauth.token.Auth2AuthenticationToken;
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
public class Auth2AuthenticationProvider implements AuthenticationProvider {

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();
    
    private AuthenticationUserDetailsService<Auth2AuthenticationToken> authenticationUserDetailsService;

    public Auth2AuthenticationProvider(AuthenticationUserDetailsService<Auth2AuthenticationToken> authenticationUserDetailsService) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(Auth2AuthenticationToken.class, authentication,
                        "Only Auth2AuthenticationToken is supported");
        Assert.notNull(authentication.getPrincipal(), "User account is Not Exists");
        String userName = authentication.getName();
        final Auth2AuthenticationToken authenticationToken = new Auth2AuthenticationToken(userName, authentication.getCredentials());
        UserDetails user = this.authenticationUserDetailsService.loadUserDetails(authenticationToken);
        Assert.notNull(user, "User account is Not Exists");
        return createSuccessAuthentication(user, authentication, user);
    }

    protected Authentication createSuccessAuthentication(Object principal, Authentication authentication,
                                                         UserDetails user) {
        Auth2AuthenticationToken result =
                        new Auth2AuthenticationToken(principal,authentication.getCredentials(), authoritiesMapper.mapAuthorities(user.getAuthorities()));
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (Auth2AuthenticationToken.class.isAssignableFrom(authentication));
    }

    public AuthenticationUserDetailsService<Auth2AuthenticationToken> getAuthenticationUserDetailsService() {
        return authenticationUserDetailsService;
    }

    public void setAuthenticationUserDetailsService(AuthenticationUserDetailsService<Auth2AuthenticationToken> authenticationUserDetailsService) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
    }


}
