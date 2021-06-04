package org.hzero.sso.cas.provider;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorageImpl;
import org.jasig.cas.client.validation.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.*;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.cas.authentication.NullStatelessTicketCache;
import org.springframework.security.cas.authentication.StatelessTicketCache;
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.*;
import org.springframework.util.Assert;

import org.hzero.core.util.DomainUtils;
import org.hzero.sso.cas.filter.CasAuthenticationFilter;
import org.hzero.sso.cas.token.CasUsernamePasswordAuthenticationToken;
import org.hzero.sso.core.constant.SsoConstant;
import org.hzero.sso.core.domain.entity.Domain;
import org.hzero.sso.core.domain.repository.DomainRepository;


/**
*
* @author minghui.qiu@hand-china.com
*/
public class CasAuthenticationProvider implements AuthenticationProvider, InitializingBean, MessageSourceAware {
    // ~ Static fields/initializers
    // =====================================================================================

    private static final Log logger = LogFactory.getLog(CasAuthenticationProvider.class);

    // ~ Instance fields
    // ================================================================================================

    private AuthenticationUserDetailsService<CasUsernamePasswordAuthenticationToken> authenticationUserDetailsService;
    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private StatelessTicketCache statelessTicketCache = new NullStatelessTicketCache();
    private String key;
    private TicketValidator ticketValidator;
    private ServiceProperties serviceProperties;
    private DomainRepository domainRepository;
    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    // ~ Methods
    // ========================================================================================================

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.authenticationUserDetailsService, "An authenticationUserDetailsService must be set");
        //Assert.notNull(this.ticketValidator, "A ticketValidator must be set");
        Assert.notNull(this.statelessTicketCache, "A statelessTicketCache must be set");
        Assert.hasText(this.key,
                "A Key is required so CasAuthenticationProvider can identify tokens it previously authenticated");
        Assert.notNull(this.messages, "A message source must be set");
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }
        if (authentication instanceof UsernamePasswordAuthenticationToken
                && (!CasAuthenticationFilter.CAS_STATEFUL_IDENTIFIER.equals(authentication.getPrincipal().toString())
                        && !CasAuthenticationFilter.CAS_STATELESS_IDENTIFIER
                                .equals(authentication.getPrincipal().toString()))) {
            // UsernamePasswordAuthenticationToken not CAS related
            return null;
        }
        // If an existing CasAuthenticationToken, just check we created it
        if (authentication instanceof CasAuthenticationToken) {
            if (this.key.hashCode() == ((CasAuthenticationToken) authentication).getKeyHash()) {
                return authentication;
            } else {
                throw new BadCredentialsException(messages.getMessage("CasAuthenticationProvider.incorrectKey",
                        "The presented CasAuthenticationToken does not contain the expected key"));
            }
        }
        // Ensure credentials are presented
        if ((authentication.getCredentials() == null) || "".equals(authentication.getCredentials())) {
            throw new BadCredentialsException(messages.getMessage("CasAuthenticationProvider.noServiceTicket",
                    "Failed to provide a CAS service ticket to validate"));
        }
        boolean stateless = false;
        if (authentication instanceof UsernamePasswordAuthenticationToken
                && CasAuthenticationFilter.CAS_STATELESS_IDENTIFIER.equals(authentication.getPrincipal())) {
            stateless = true;
        }
        CasAuthenticationToken result = null;
        if (stateless) {
            // Try to obtain from cache
            result = statelessTicketCache.getByTicketId(authentication.getCredentials().toString());
        }
        if (result == null) {
            result = this.customAuthenticateNow(authentication);

            if (result == null) {
                result = this.authenticateNow(authentication);
            }

            result.setDetails(authentication.getDetails());
        }
        if (stateless) {
            // Add to cache
            statelessTicketCache.putTicketInCache(result);
        }
        return result;
    }

    /**
     * 自定义的认证，返回 null 将执行默认的 authenticateNow.
     */
    protected CasAuthenticationToken customAuthenticateNow (final Authentication authentication) throws AuthenticationException {
        return null;
    }

    private CasAuthenticationToken authenticateNow(final Authentication authentication) throws AuthenticationException {
        try {

            Map<String, String> param = DomainUtils.getQueryMap(this.getServiceUrl(authentication));

            String serverUrl = param.get(SsoConstant.UrlParamKey.SERVER_URL);
            Long tenantId = Long.parseLong(param.get(SsoConstant.UrlParamKey.TENANT_ID));
            String ssoTypeCode = param.get(SsoConstant.UrlParamKey.CAS_VERSION);

            // 根据配置选择合适的校验器
            AbstractUrlBasedTicketValidator ticketValidator;
            if (SsoConstant.CAS.equalsIgnoreCase(ssoTypeCode)) {
                ticketValidator = new Cas10TicketValidator(serverUrl);
            } else {
                Cas20ProxyTicketValidator proxyTicketValidator = null;
                if (SsoConstant.CAS2.equalsIgnoreCase(ssoTypeCode)) {
                    proxyTicketValidator = new Cas20ProxyTicketValidator(serverUrl);
                } else if (SsoConstant.CAS3.equalsIgnoreCase(ssoTypeCode)) {
                    proxyTicketValidator = new Cas30ProxyTicketValidator(serverUrl);
                }

                if (proxyTicketValidator == null) {
                    throw new AuthenticationServiceException("Obtain ticketValidator was failed.");
                }

                proxyTicketValidator.setAcceptAnyProxy(true);
                proxyTicketValidator.setProxyGrantingTicketStorage(new ProxyGrantingTicketStorageImpl());
                ticketValidator = proxyTicketValidator;
            }

            // 配置的sso domainUrl 为https 时转化为https
            String serviceUrl = this.getServiceUrl(authentication);
            if ("true".equals(param.get(SsoConstant.UrlParamKey.SSO_HTTPS)) && serviceUrl.startsWith(DomainUtils.HTTP)) {
                serviceUrl = serviceUrl.replace(DomainUtils.HTTP, DomainUtils.HTTPS);
            }

            Assertion assertion = ticketValidator.validate(authentication.getCredentials().toString(), serviceUrl);

            final UserDetails userDetails = loadUserByAssertion(assertion, tenantId, param);

            userDetailsChecker.check(userDetails);

            return new CasAuthenticationToken(this.key, userDetails, authentication.getCredentials(),
                    authoritiesMapper.mapAuthorities(userDetails.getAuthorities()), userDetails, assertion);

        } catch (final TicketValidationException e) {
            throw new BadCredentialsException(e.getMessage(), e);
        }
    }

    /**
     * Gets the serviceUrl. If the {@link Authentication#getDetails()} is an
     * instance of {@link ServiceAuthenticationDetails}, then
     * {@link ServiceAuthenticationDetails#getServiceUrl()} is used. Otherwise,
     * the {@link ServiceProperties#getService()} is used.
     *
     * @param authentication
     * @return
     */
    private String getServiceUrl(Authentication authentication) {
        String serviceUrl;
        if (authentication.getDetails() instanceof ServiceAuthenticationDetails) {
            serviceUrl = ((ServiceAuthenticationDetails) authentication.getDetails()).getServiceUrl();
        } else if (serviceProperties == null) {
            throw new IllegalStateException(
                    "serviceProperties cannot be null unless Authentication.getDetails() implements ServiceAuthenticationDetails.");
        } else if (serviceProperties.getService() == null) {
            throw new IllegalStateException(
                    "serviceProperties.getService() cannot be null unless Authentication.getDetails() implements ServiceAuthenticationDetails.");
        } else {
            serviceUrl = serviceProperties.getService();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("serviceUrl = " + serviceUrl);
        }
        return serviceUrl;
    }

    /**
     * Template method for retrieving the UserDetails based on the assertion.
     * Default is to call configured userDetailsService and pass the username.
     * Deployers can override this method and retrieve the user based on any
     * criteria they desire.
     *
     * @param assertion
     *            The CAS Assertion.
     * @return the UserDetails.
     */
    protected UserDetails loadUserByAssertion(final Assertion assertion,Long tenantId, Map<String, String> params) {
        String loginNameField = params.get(SsoConstant.UrlParamKey.LOGIN_NAME_FIELD);
        String username = null;
        if (StringUtils.isNotBlank(loginNameField) && !"null".equalsIgnoreCase(loginNameField)) {
            Map<String, Object> attributes = assertion.getPrincipal().getAttributes();
            if (MapUtils.isNotEmpty(attributes) && attributes.get(loginNameField) != null) {
                username = attributes.get(loginNameField).toString();
            }
        }
        if (StringUtils.isBlank(username)) {
            username = assertion.getPrincipal().getName();
        }

        final CasUsernamePasswordAuthenticationToken token = new CasUsernamePasswordAuthenticationToken(username, tenantId);
        return this.authenticationUserDetailsService.loadUserDetails(token);
    }
    
    /**
     * 根据请求获取域名信息
     */
    protected Domain checkCasServer(String casServer) {
        java.net.URL url;
        try {
            url = new java.net.URL(casServer);
        } catch (MalformedURLException e) {
            return null;
        }
        String casServerHost = url.getHost();
        if (StringUtils.isBlank(casServerHost) ) {
            return null;
        }
        // 查询域名
        List<Domain> domains = domainRepository.selectAllDomain();
        if (domains == null || domains.size() == 0) {
            return null;
        }
        for(Domain dto : domains){
            if(dto.getSsoServerUrl()!=null  && dto.getSsoServerUrl().contains(casServerHost)){
                return dto;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    /**
     * Sets the UserDetailsService to use. This is a convenience method to
     * invoke
     */
    public void setUserDetailsService(final UserDetailsService userDetailsService) {
        this.authenticationUserDetailsService = new UserDetailsByNameServiceWrapper(userDetailsService);
    }

    public void setAuthenticationUserDetailsService(
            final AuthenticationUserDetailsService<CasUsernamePasswordAuthenticationToken> authenticationUserDetailsService) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
    }

    public void setServiceProperties(final ServiceProperties serviceProperties) {
        this.serviceProperties = serviceProperties;
    }

    protected String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public StatelessTicketCache getStatelessTicketCache() {
        return statelessTicketCache;
    }

    protected TicketValidator getTicketValidator() {
        return ticketValidator;
    }

    public void setMessageSource(final MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    public void setStatelessTicketCache(final StatelessTicketCache statelessTicketCache) {
        this.statelessTicketCache = statelessTicketCache;
    }

    public void setTicketValidator(final TicketValidator ticketValidator) {
        this.ticketValidator = ticketValidator;
    }

    public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
        this.authoritiesMapper = authoritiesMapper;
    }
    
    public DomainRepository getDomainRepository() {
      return domainRepository;
    }

    public void setDomainRepository(final DomainRepository domainRepository) {
      this.domainRepository = domainRepository;
    }

    public boolean supports(final Class<?> authentication) {
        return (CasUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication))
                || (CasAuthenticationToken.class.isAssignableFrom(authentication))
                || (CasAssertionAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
