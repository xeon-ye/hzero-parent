package org.hzero.sso.core.security.service;

import io.choerodon.core.oauth.CustomUserDetails;

import org.hzero.sso.core.domain.entity.SsoUser;


public interface SsoUserDetailsBuilder {

    /**
     * 构建 CustomUserDetails
     * 
     * @param user User
     * @return CustomUserDetails
     */
    CustomUserDetails buildUserDetails(SsoUser user);

}
