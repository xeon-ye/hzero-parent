package org.hzero.sso.saml.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.hzero.core.user.UserType;
import org.hzero.sso.core.domain.entity.SsoUser;
import org.hzero.sso.core.security.service.SsoUserAccountService;
import org.hzero.sso.core.security.service.SsoUserDetailsBuilder;
import org.hzero.sso.saml.token.CustomSamlAuthenticationToken;
import org.hzero.sso.core.exception.LoginExceptions;

public class SamlUserDetailsService implements AuthenticationUserDetailsService<CustomSamlAuthenticationToken> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SamlUserDetailsService.class);

    private SsoUserAccountService userAccountService;
    private SsoUserDetailsBuilder userDetailsBuilder;

    public SamlUserDetailsService(SsoUserAccountService userAccountService, SsoUserDetailsBuilder userDetailsBuilder) {
        this.userAccountService = userAccountService;
        this.userDetailsBuilder = userDetailsBuilder;
    }
    
    @Override
    public UserDetails loadUserDetails(CustomSamlAuthenticationToken token) throws UsernameNotFoundException {
        String username = token.getName();
        LOGGER.debug("load auth2 user, username={}, token={}", username, token);
        SsoUser user = userAccountService.findLoginUser(username, UserType.ofDefault());
        if (user == null) {
            throw new UsernameNotFoundException(LoginExceptions.USERNAME_NOT_FOUND.value());
        }
        return userDetailsBuilder.buildUserDetails(user);
    }

}
