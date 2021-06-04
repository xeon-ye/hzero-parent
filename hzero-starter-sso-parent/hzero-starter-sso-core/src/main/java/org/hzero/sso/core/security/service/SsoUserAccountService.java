package org.hzero.sso.core.security.service;

import java.util.List;

import org.hzero.core.user.UserType;
import org.hzero.sso.core.domain.entity.SsoUser;


public interface SsoUserAccountService {

 
    /**
     * 查询登录用户
     *
     * @param account loginName/phone/email
     */
    SsoUser findLoginUser(String account, UserType userType);

    /**
     * 查询用户所属租户
     *
     * @param userId 用户ID
     */
    List<Long> findUserLegalOrganization(Long userId);

}
