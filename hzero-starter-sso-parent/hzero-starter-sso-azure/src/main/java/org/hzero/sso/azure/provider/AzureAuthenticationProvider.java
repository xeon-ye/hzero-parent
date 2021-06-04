package org.hzero.sso.azure.provider;

import org.hzero.sso.azure.token.AzureAuthenticationToken;
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
public class AzureAuthenticationProvider implements AuthenticationProvider {

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();
    
    private AuthenticationUserDetailsService<AzureAuthenticationToken> authenticationUserDetailsService;

    public AzureAuthenticationProvider(AuthenticationUserDetailsService<AzureAuthenticationToken> authenticationUserDetailsService) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(AzureAuthenticationToken.class, authentication,
                        "Only AzureAuthenticationToken is supported");
        Assert.notNull(authentication.getPrincipal(), "User account is Not Exists");
        String userName = authentication.getName();
        final AzureAuthenticationToken authenticationToken = new AzureAuthenticationToken(userName, authentication.getCredentials());
        UserDetails user = this.authenticationUserDetailsService.loadUserDetails(authenticationToken);
        Assert.notNull(user, "User account is Not Exists");
        return createSuccessAuthentication(user, authentication, user);
    }

    protected Authentication createSuccessAuthentication(Object principal, Authentication authentication,
                                                         UserDetails user) {
        AzureAuthenticationToken result =
                        new AzureAuthenticationToken(principal,authentication.getCredentials(), authoritiesMapper.mapAuthorities(user.getAuthorities()));
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (AzureAuthenticationToken.class.isAssignableFrom(authentication));
    }

    public AuthenticationUserDetailsService<AzureAuthenticationToken> getAuthenticationUserDetailsService() {
        return authenticationUserDetailsService;
    }

    public void setAuthenticationUserDetailsService(AuthenticationUserDetailsService<AzureAuthenticationToken> authenticationUserDetailsService) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
    }


}
