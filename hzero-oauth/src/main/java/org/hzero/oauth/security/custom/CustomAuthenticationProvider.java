package org.hzero.oauth.security.custom;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.naming.directory.DirContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.SearchScope;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.choerodon.core.ldap.DirectoryType;

import org.hzero.boot.oauth.domain.entity.BaseClient;
import org.hzero.boot.oauth.domain.entity.BaseLdap;
import org.hzero.boot.oauth.domain.entity.BasePasswordPolicy;
import org.hzero.boot.oauth.domain.repository.BaseLdapRepository;
import org.hzero.boot.oauth.util.CustomBCryptPasswordEncoder;
import org.hzero.common.HZeroService;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.captcha.CaptchaImageHelper;
import org.hzero.core.user.UserType;
import org.hzero.core.util.EncryptionUtils;
import org.hzero.oauth.domain.entity.User;
import org.hzero.oauth.domain.repository.UserRepository;
import org.hzero.oauth.infra.encrypt.EncryptClient;
import org.hzero.oauth.security.config.SecurityProperties;
import org.hzero.oauth.security.constant.LoginException;
import org.hzero.oauth.security.constant.LoginSource;
import org.hzero.oauth.security.constant.LoginType;
import org.hzero.oauth.security.exception.AccountNotExistsException;
import org.hzero.oauth.security.exception.CustomAuthenticationException;
import org.hzero.oauth.security.exception.ErrorWithTimesException;
import org.hzero.oauth.security.exception.LoginExceptions;
import org.hzero.oauth.security.service.LoginRecordService;
import org.hzero.oauth.security.service.UserAccountService;
import org.hzero.oauth.security.util.LoginUtil;
import org.hzero.oauth.security.util.PasswordDecode;

public class CustomAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    private final CustomUserDetailsService userDetailsService;
    private final BaseLdapRepository baseLdapRepository;
    private final UserAccountService userAccountService;
    private final LoginRecordService loginRecordService;
    private final CaptchaImageHelper captchaImageHelper;
    private final SecurityProperties securityProperties;
    private final EncryptClient encryptClient;
    private final UserRepository userRepository;
    private PasswordEncoder passwordEncoder = new CustomBCryptPasswordEncoder();

    public CustomAuthenticationProvider(CustomUserDetailsService userDetailsService,
                                        BaseLdapRepository baseLdapRepository,
                                        UserAccountService userAccountService,
                                        LoginRecordService loginRecordService,
                                        CaptchaImageHelper captchaImageHelper,
                                        SecurityProperties securityProperties,
                                        EncryptClient encryptClient,
                                        UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.baseLdapRepository = baseLdapRepository;
        this.userAccountService = userAccountService;
        this.loginRecordService = loginRecordService;
        this.captchaImageHelper = captchaImageHelper;
        this.securityProperties = securityProperties;
        this.encryptClient = encryptClient;
        this.userRepository = userRepository;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        if (passwordEncoder != null) {
            this.passwordEncoder = passwordEncoder;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return super.supports(authentication) &&
                UsernamePasswordAuthenticationToken.class.getTypeName().equals(authentication.getTypeName());
    }

    @Override
    public UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) {
        // ??????????????????????????????
        String loginField = null;
        String userType = null;
        Object details = authentication.getDetails();
        User user = null;

        if (details instanceof CustomWebAuthenticationDetails) {
            loginField = ((CustomWebAuthenticationDetails) details).getLoginField();
            userType = ((CustomWebAuthenticationDetails) details).getUserType();
        } else if (details instanceof Map) {
            loginField = (String) ((Map) details).get(LoginUtil.FIELD_LOGIN_FIELD);
            userType = (String) ((Map) details).get(UserType.PARAM_NAME);
        }
        if (loginField != null) {
            user = userAccountService.findLoginUser(loginField, username, UserType.ofDefault(userType));
        } else {
            user = userAccountService.findLoginUser(LoginType.ACCOUNT, username, UserType.ofDefault(userType));
        }
        if (user == null) {
            throw new AccountNotExistsException(LoginExceptions.USERNAME_OR_PASSWORD_ERROR.value());
        }

        loginRecordService.saveLocalLoginUser(user);

        // ???????????????????????????
        userAccountService.checkLoginUser(user);

        return getUserDetailsService().loadUserByUsername(username);
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication) {
        // ???????????????
        checkCaptcha(userDetails, authentication);
        BaseClient client = userAccountService.findCurrentClient();
        if (client == null) {
            throw new AuthenticationServiceException(LoginExceptions.CLIENT_NOT_FOUND.value());
        }
        // ??????????????????
        checkReplayPassword(authentication, client);
        // ????????????
        checkPassword(userDetails, authentication);
        // ??????????????????????????????
        checkAccessClient(userDetails, client);
    }

    /**
     * ??????????????????
     */
    protected void checkReplayPassword(UsernamePasswordAuthenticationToken authentication, BaseClient client) {
        if (!BaseConstants.Flag.YES.equals(client.getPwdReplayFlag())) {
            return;
        }

        String md5Pass = EncryptionUtils.MD5.encrypt((String) authentication.getCredentials());
        boolean absent = loginRecordService.savePassIfAbsent(client.getName(), md5Pass,
                securityProperties.getLogin().getPassReplayExpire(), TimeUnit.DAYS);

        if (absent) {
            throw new AuthenticationServiceException(LoginExceptions.DUPLICATE_PASSWORD.value());
        }
    }

    /**
     * ?????????????????????????????????
     */
    protected void checkAccessClient(UserDetails userDetails, BaseClient client) {
        // ???????????????ID
        String accessRoles = client.getAccessRoles();
        if (StringUtils.isEmpty(accessRoles)) {
            return;
        }
        User loginUser = loginRecordService.getLocalLoginUser();
        // ????????????ID
        List<Long> roleIds = userRepository.selectUserRole(loginUser.getId());
        String[] roleIdArr = StringUtils.split(accessRoles, BaseConstants.Symbol.COMMA);
        for (String roleId : roleIdArr) {
            if (roleIds.contains(Long.parseLong(roleId))) {
                return;
            }
        }
        throw new CustomAuthenticationException(LoginExceptions.USER_NOT_ACCESS_CLIENT.value());
    }

    /**
     * ???????????????
     */
    protected void checkCaptcha(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {
        // Web ??????
        if (authentication.getDetails() instanceof CustomWebAuthenticationDetails) {
            CustomWebAuthenticationDetails webDetails = (CustomWebAuthenticationDetails) authentication.getDetails();

            String captchaCode = webDetails.getCacheCaptcha();
            String captcha = webDetails.getInputCaptcha();

            checkCaptcha(captcha, captchaCode);
        }
        // ????????????
        else if (authentication.getDetails() instanceof Map) {
            Map parameters = (Map) authentication.getDetails();
            String sourceType = getParameterFromMap(parameters, LoginUtil.FIELD_SOURCE_TYPE);

            // ??????????????? source_type ???????????????????????????????????????OAuth2.0 ????????????
            if (StringUtils.isBlank(sourceType) ||
                    (LoginSource.APP.value().equalsIgnoreCase(sourceType) && !securityProperties.isEnableAppCaptcha())) {
                return;
            }
            String captcha = getParameterFromMap(parameters, LoginUtil.FIELD_CAPTCHA);
            String captchaKey = getParameterFromMap(parameters, LoginUtil.FIELD_CAPTCHA_KEY);
            String captchaCode = captchaImageHelper.getCaptcha(HZeroService.Oauth.CODE, captchaKey);

            checkCaptcha(captcha, captchaCode);
        }
    }

    private String getParameterFromMap(Map parameters, String key) {
        return Optional.ofNullable(parameters.get(key)).map(Object::toString).orElse(null);
    }

    private void checkCaptcha(String captcha, String captchaCode) {
        User user = loginRecordService.getLocalLoginUser();
        if (userAccountService.isNeedCaptcha(user)) {
            if (StringUtils.isBlank(captcha)) {
                throw new AuthenticationServiceException(LoginExceptions.CAPTCHA_NULL.value());
            }
            if (StringUtils.isBlank(captchaCode) || !StringUtils.equalsIgnoreCase(captchaCode, captcha)) {
                throw new AuthenticationServiceException(LoginExceptions.CAPTCHA_ERROR.value());
            }
        }
    }

    /**
     * ????????????
     */
    protected void checkPassword(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {
        String rawPassword = getDecryptPassword(authentication);

        boolean passed;
        User user = loginRecordService.getLocalLoginUser();
        if (user.getLdap()) {
            passed = ldapAuthentication(user.getOrganizationId(), userDetails.getUsername(), rawPassword);
        } else {
            passed = passwordEncoder.matches(rawPassword, userDetails.getPassword());
            // ??????????????????????????????
            if (passed) {
                checkPasswordModified(user);
            }
        }
        if (passed) {
            processPasswordLoginSuccess();
            return;
        }

        processPasswordLoginError();
    }

    /**
     * ?????????????????????????????????
     */
    protected String getDecryptPassword(UsernamePasswordAuthenticationToken authentication) {
        String credentials = null;
        try {
            // RSA ???????????????
            if (securityProperties.getPassword().isEnableEncrypt()) {
                credentials = encryptClient.decrypt((String) authentication.getCredentials());
            }
            // Base64 ??????
            else {
                credentials = new String(PasswordDecode.decode((String) authentication.getCredentials()), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.error("decode password error. ex={}", e.getMessage());
            throw new AuthenticationServiceException(LoginExceptions.DECODE_PASSWORD_ERROR.value());
        }
        return credentials;
    }

    /**
     * ????????????????????????
     */
    protected void processPasswordLoginSuccess() {
        User user = loginRecordService.getLocalLoginUser();
        loginRecordService.loginSuccess(user);
    }

    /**
     * ????????????????????????
     */
    protected void processPasswordLoginError() {
        // password error
        User loginUser = loginRecordService.getLocalLoginUser();
        long residualTimes = loginRecordService.loginError(loginUser);
        // ???????????? ?????????
        if (loginUser.getLocked()) {
            throw new CustomAuthenticationException(LoginExceptions.LOGIN_ERROR_MORE_THEN_MAX_TIME.value());
        }
        // ???????????? ??????????????????
        else {
            if (residualTimes == -1) {
                throw new CustomAuthenticationException(LoginExceptions.USERNAME_OR_PASSWORD_ERROR.value());
            } else {
                ErrorWithTimesException ex = new ErrorWithTimesException(LoginExceptions.PASSWORD_ERROR.value(), residualTimes);
                ex.setErrorTimes(loginRecordService.getErrorTimes(loginUser));
                ex.setSurplusTimes(residualTimes);
                throw ex;
            }
        }
    }


    /**
     * ??????????????????????????????
     *
     * @param user ??????
     */
    protected void checkPasswordModified(User user) {
        BasePasswordPolicy basePasswordPolicy = userAccountService.getPasswordPolicyByUser(user);
        // ????????????????????????
        if (basePasswordPolicy == null) {
            return;
        }
        if (userAccountService.isPasswordExpired(basePasswordPolicy, user)) {
            throw new CustomAuthenticationException(LoginExceptions.PASSWORD_EXPIRED.value());
        }

        // ????????????????????????????????????????????????????????????????????????????????????
        if (userAccountService.isNeedForceModifyPassword(basePasswordPolicy, user)) {
            throw new CustomAuthenticationException(LoginExceptions.PASSWORD_FORCE_MODIFY.value());
        }
    }

    private boolean ldapAuthentication(Long organizationId, String loginName, String credentials) {
        BaseLdap ldap = baseLdapRepository.selectLdap(organizationId);
        if (ldap != null && ldap.getEnabled()) {
            LdapContextSource contextSource = new LdapContextSource();
            String url = ldap.getServerAddress() + ":" + ldap.getPort();
            contextSource.setUrl(url);
            contextSource.setBase(ldap.getBaseDn());
            contextSource.afterPropertiesSet();
            LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
            // ad????????????????????????
            if (DirectoryType.MICROSOFT_ACTIVE_DIRECTORY.value().equals(ldap.getDirectoryType())) {
                ldapTemplate.setIgnorePartialResultException(true);
            }
            String userDn = null;
            boolean anonymousFetchFailed = false;
            try {
                List<String> names = ldapTemplate.search(
                        query()
                                .searchScope(SearchScope.SUBTREE)
                                .where("objectclass")
                                .is(ldap.getObjectClass())
                                .and(ldap.getLoginNameField())
                                .is(loginName),
                        new AbstractContextMapper<String>() {
                            @Override
                            protected String doMapFromContext(DirContextOperations ctx) {
                                return ctx.getNameInNamespace();
                            }
                        });
                userDn = getUserDn(names, ldap.getLoginNameField(), loginName);
            } catch (Exception e) {
                anonymousFetchFailed = true;
                LOGGER.error("ldap anonymous search objectclass = {}, {} = {} failed, exception {}",
                        ldap.getObjectClass(), ldap.getLoginNameField(), loginName, e);
            }
            if (anonymousFetchFailed) {
                userDn = accountAsUserDn2Authentication(loginName, ldap, contextSource, userDn);
            }
            return authentication(credentials, contextSource, userDn);
        } else {
            throw new AuthenticationServiceException(LoginException.LDAP_IS_DISABLE.value());
        }
    }

    private String accountAsUserDn2Authentication(String loginName, BaseLdap ldap, LdapContextSource contextSource, String userDn) {
        contextSource.setUserDn(ldap.getAccount());
        contextSource.setPassword(ldap.getLdapPassword());
        contextSource.afterPropertiesSet();
        LdapTemplate template = new LdapTemplate(contextSource);
        if (DirectoryType.MICROSOFT_ACTIVE_DIRECTORY.value().equals(ldap.getDirectoryType())) {
            template.setIgnorePartialResultException(true);
        }
        try {
            List<String> names = template.search(
                    query()
                            .searchScope(SearchScope.SUBTREE)
                            .where("objectclass")
                            .is(ldap.getObjectClass())
                            .and(ldap.getLoginNameField())
                            .is(loginName),
                    new AbstractContextMapper<String>() {
                        @Override
                        protected String doMapFromContext(DirContextOperations ctx) {
                            return ctx.getNameInNamespace();
                        }
                    });
            userDn = getUserDn(names, ldap.getLoginNameField(), loginName);
        } catch (Exception e) {
            LOGGER.error("use ldap account as userDn and password to authentication but search objectclass = {}, {} = {} failed, maybe the account or password is illegal, and check for the ldap config, exception {}",
                    ldap.getObjectClass(), ldap.getLoginNameField(), loginName, e);
        }
        return userDn;
    }

    private String getUserDn(List<String> names, String loginFiled, String loginName) {
        if (names.isEmpty()) {
            LOGGER.warn("user not found");
        } else if (names.size() == 1) {
            return names.get(0);
        } else {
            LOGGER.warn("user {} = {} is not unique", loginFiled, loginName);
        }
        return null;
    }

    private boolean authentication(String credentials, LdapContextSource contextSource, String userDn) {
        if (userDn == null) {
            return false;
        } else {
            DirContext ctx = null;
            try {
                ctx = contextSource.getContext(userDn, credentials);
                return true;
            } catch (Exception e) {
                logger.error("Login failed, userDn or credentials may be wrong, exception {}", e);
                return false;
            } finally {
                // It is imperative that the created DirContext instance is always closed
                LdapUtils.closeContext(ctx);
            }

        }
    }

    //
    // getter
    // ------------------------------------------------------------------------------


    public UserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    public BaseLdapRepository getBaseLdapRepository() {
        return baseLdapRepository;
    }

    public UserAccountService getUserAccountService() {
        return userAccountService;
    }

    public LoginRecordService getLoginRecordService() {
        return loginRecordService;
    }
}
