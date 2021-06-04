package org.hzero.sso.cas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import java.util.List;

import org.hzero.core.user.UserType;
import org.hzero.sso.cas.token.CasUsernamePasswordAuthenticationToken;
import org.hzero.sso.core.domain.entity.SsoUser;
import org.hzero.sso.core.security.service.SsoUserAccountService;
import org.hzero.sso.core.security.service.SsoUserDetailsBuilder;

/**
 * Cas UserDetailsService
 *
 * @author bojiangzhou 2019/02/25
 */
public class CasUserDetailsService implements AuthenticationUserDetailsService<CasUsernamePasswordAuthenticationToken> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CasUserDetailsService.class);

    private SsoUserAccountService userAccountService;
    private SsoUserDetailsBuilder userDetailsBuilder;

    public CasUserDetailsService(SsoUserAccountService userAccountService,
                                 SsoUserDetailsBuilder userDetailsBuilder) {
        this.userAccountService = userAccountService;
        this.userDetailsBuilder = userDetailsBuilder;
    }

    @Override
    public UserDetails loadUserDetails(CasUsernamePasswordAuthenticationToken token) throws UsernameNotFoundException {
        String username = token.getName();
        Long tenantId = Long.valueOf(String.valueOf(token.getCredentials()));
        LOGGER.debug("load cas user, username={}, tenantId={}, token={}", username, tenantId, token);
        SsoUser user = userAccountService.findLoginUser(username, UserType.ofDefault());
        Assert.notNull(user, "User is Not Exists");
        List<Long> organizationIdList = userAccountService.findUserLegalOrganization(user.getId());
        if (!organizationIdList.contains(tenantId)){
          	throw new UsernameNotFoundException("Permission not match！");
        } 
        return userDetailsBuilder.buildUserDetails(user);
    }

}
