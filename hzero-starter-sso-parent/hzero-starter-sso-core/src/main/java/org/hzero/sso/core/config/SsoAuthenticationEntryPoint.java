package org.hzero.sso.core.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Timer;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.lang3.StringUtils;
import org.jasig.cas.client.util.CommonUtils;
import org.opensaml.common.SAMLException;
import org.opensaml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.util.URLBuilder;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.transport.http.HTTPInTransport;
import org.opensaml.ws.transport.http.HTTPOutTransport;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml.SAMLConstants;
import org.springframework.security.saml.SAMLDiscovery;
import org.springframework.security.saml.context.SAMLContextProvider;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLLogger;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.saml.util.SAMLUtil;
import org.springframework.security.saml.websso.WebSSOProfile;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.*;
import org.springframework.security.web.util.RedirectUrlBuilder;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.filter.GenericFilterBean;

import org.hzero.core.exception.IllegalOperationException;
import org.hzero.core.util.DomainUtils;
import org.hzero.sso.core.constant.SsoConstant;
import org.hzero.sso.core.domain.entity.Domain;
import org.hzero.sso.core.domain.repository.DomainRepository;
import org.hzero.sso.core.type.SsoAuthenticationLocator;

public class SsoAuthenticationEntryPoint extends GenericFilterBean
        implements InitializingBean, AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(SsoAuthenticationEntryPoint.class);
    private static final String AUTHORIZE_URI = "/oauth/oauth/authorize";
    private final ServiceProperties serviceProperties = new ServiceProperties();
    private final PortResolver portResolver = new PortResolverImpl();
    private final DomainRepository domainRepository;
    private final String loginFormUrl;
    private final String disableSsoParameter;
    private final boolean forceHttps;
    // saml properties
    protected WebSSOProfileOptions defaultOptions;
    protected WebSSOProfile webSSOprofile;
    protected WebSSOProfile webSSOprofileECP;
    protected WebSSOProfile webSSOprofileHoK;
    protected KeyManager keyManager;
    protected SAMLLogger samlLogger;
    protected MetadataManager metadata;
    protected SAMLContextProvider contextProvider;
    protected SAMLDiscovery samlDiscovery;
    protected Timer backgroundTaskTimer = new Timer(true);
    protected HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());;
    protected ParserPool parserPool;
    protected ExtendedMetadata extendedMetadata;
    protected SsoAuthenticationLocator ssoAuthenticationLocator;

    /**
     * Url this filter should get activated on.
     */
    protected String filterProcessesUrl = FILTER_URL;
    /**
     * Default name of path suffix which will invoke this filter.
     */
    public static final String FILTER_URL = "/saml/login";
    /**
     * Name of parameter of HttpRequest telling entry point that the login should use specified idp.
     */
    public static final String IDP_PARAMETER = "idp";
    /**
     * Parameter is used to indicate response from IDP discovery service. When present IDP discovery is not invoked
     * again.
     */
    public static final String DISCOVERY_RESPONSE_PARAMETER = "disco";


    public SsoAuthenticationEntryPoint(DomainRepository domainRepository, SsoProperties ssoProperties) {
        this.domainRepository = domainRepository;
        this.forceHttps = ssoProperties.getLogin().isEnableHttps();
        this.loginFormUrl = ssoProperties.getLogin().getPage();
        this.disableSsoParameter = ssoProperties.getSso().getDisableSsoParameter();
    }

    @Override
    public void commence(final HttpServletRequest request, final HttpServletResponse response,
            final AuthenticationException authException) throws IOException, ServletException {
        String redirectUrl;
        Domain ssoDomain = getSsoDomain(request, response);
        if (ssoDomain == null || ssoDomain.getSsoTypeCode() == null || SsoConstant.NULL.equals(ssoDomain.getSsoTypeCode())) {
            redirectUrl = buildRedirectUrlToLoginPage(request, response, authException);
        } else if (ssoDomain.getSsoTypeCode() != null && SsoConstant.SAML.equals(ssoDomain.getSsoTypeCode())) {
            checkSsoRegister(ssoDomain.getSsoTypeCode());
            samlCommence(request, response, authException,ssoDomain);
            return;
        } else if (ssoDomain.getSsoTypeCode() != null && SsoConstant.IDM.equals(ssoDomain.getSsoTypeCode())) {
            checkSsoRegister(ssoDomain.getSsoTypeCode());
            redirectUrl = ssoDomain.getClientHostUrl();
        } else {
            checkSsoRegister(ssoDomain.getSsoTypeCode());
            redirectUrl = buildRedirectUrlToSsoPage(ssoDomain, request, response);
        }
        response.sendRedirect(redirectUrl);
    }

    private void checkSsoRegister(String ssoId) {
        if (!ssoAuthenticationLocator.ssoRegister(ssoId)) {
            logger.error("Sso [{}] not enabled, you need to add the dependency of [hzero-starter-sso-{}] to enabled it.", ssoId, ssoId.toLowerCase());
            throw new IllegalOperationException("Sso [" + ssoId + "] is not enabled.");
        }
    }

    /**
     * 根据请求获取域名信息
     */
    protected Domain getSsoDomain(final HttpServletRequest request, final HttpServletResponse response) {
        String disable = request.getParameter(disableSsoParameter);
        if (StringUtils.isNotBlank(disable) && !("0".equals(disable) || "false".equals(disable))) {
            return null;
        }

        boolean authorize = request.getRequestURI().contains(AUTHORIZE_URI);
        String redirectUrl = request.getParameter("redirect_uri");
        java.net.URL url;
        String redirectUri = "";
        try {
            url = new java.net.URL(redirectUrl);
            if(url.getPort()>0){
                redirectUri = url.getHost() + ":" + url.getPort();
            }else{
                redirectUri = url.getHost();
            }
        } catch (MalformedURLException e) {
            return null;
        }
        if (StringUtils.isBlank(redirectUri) || !authorize) {
            return null;
        }
        // 查询域名
        List<Domain> domains = domainRepository.selectAllDomain();
        if (domains == null || domains.size() == 0) {
            return null;
        }

        String finalRedirectUri = redirectUri;
        return domains.stream().filter(d -> d.getSsoTypeCode() != null && d.getDomainUrl().contains(finalRedirectUri))
                .findFirst().orElse(null);
    }

    /**
     * 构建重定向到 SSO 登录页面的地址
     */
    protected String buildRedirectUrlToSsoPage(Domain ssoDomain, HttpServletRequest request,
            HttpServletResponse response) {
    	if(ssoDomain.getSsoTypeCode().contains(SsoConstant.CAS)){
    		final String urlEncodedService = createServiceUrl(ssoDomain, request, response);
            return createRedirectUrl(ssoDomain, urlEncodedService);
    	}else if(ssoDomain.getSsoTypeCode().contains(SsoConstant.AUTH)){
    		return ssoDomain.getSsoLoginUrl();
    	}else{
    		return null;
    	}
    }

    /**
     * 构建Cas登录后重定向回来的地址
     */
    protected String createServiceUrl(final Domain domain, final HttpServletRequest request,
            final HttpServletResponse response) {

        StringBuilder service = new StringBuilder();
        boolean ssoHttps = domain.getDomainUrl().startsWith(DomainUtils.HTTPS);

        service.append(domain.getClientHostUrl()).append("?")
                .append(SsoConstant.UrlParamKey.TENANT_ID).append("=").append(domain.getTenantId())
                .append("&").append(SsoConstant.UrlParamKey.SERVER_URL).append("=").append(domain.getSsoServerUrl())
                .append("&").append(SsoConstant.UrlParamKey.CAS_VERSION).append("=").append(domain.getSsoTypeCode())
                .append("&").append(SsoConstant.UrlParamKey.LOGIN_NAME_FIELD).append("=").append(domain.getLoginNameField())
                .append("&").append(SsoConstant.UrlParamKey.SSO_HTTPS).append("=").append(ssoHttps);

        return CommonUtils.constructServiceUrl(null, response, service.toString(), null,
                this.serviceProperties.getServiceParameter(), this.serviceProperties.getArtifactParameter(), true);
    }

    /**
     * 构建重定向到 Cas 的地址
     */
    protected String createRedirectUrl(final Domain domain, final String serviceUrl) {
        return CommonUtils.constructRedirectUrl(domain.getSsoLoginUrl(), this.serviceProperties.getServiceParameter(),
                serviceUrl, this.serviceProperties.isSendRenew(), false);
    }

    protected String buildRedirectUrlToLoginPage(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) {
        String loginForm = determineUrlToUseForThisRequest(request, response, authException);

        if (UrlUtils.isAbsoluteUrl(loginForm)) {
            return loginForm;
        }
        int serverPort = portResolver.getServerPort(request);
        String scheme = request.getScheme();

        RedirectUrlBuilder urlBuilder = new RedirectUrlBuilder();

        String rootPath = request.getHeader("H-Root-Path");
        if (org.springframework.util.StringUtils.isEmpty(rootPath) || "/".equals(rootPath)) {
            rootPath = "";
        } else if (!rootPath.startsWith("/")) {
            rootPath = "/" + rootPath;
        }

        urlBuilder.setScheme(scheme);
        urlBuilder.setServerName(request.getServerName());
        urlBuilder.setPort(serverPort);
        urlBuilder.setContextPath(rootPath + request.getContextPath());
        urlBuilder.setPathInfo(loginForm);

        if (forceHttps && "http".equals(scheme)) {
            urlBuilder.setScheme("https");
        }

        return urlBuilder.getUrl();
    }

    protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) {

        return getLoginFormUrl();
    }

    public String getLoginFormUrl() {
        return loginFormUrl;
    }

    protected boolean processFilter(HttpServletRequest request) {
        return SAMLUtil.processFilter(filterProcessesUrl, request);
    }

    public void samlCommence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e,Domain ssoDomain)
            throws IOException, ServletException {
        try {
        	boolean addFlag = true;
        	for(ExtendedMetadataDelegate dto : metadata.getAvailableProviders()){
             	if(dto.getDelegate() instanceof HTTPMetadataProvider ) {
             		HTTPMetadataProvider provider  = (HTTPMetadataProvider) dto.getDelegate();
             		if(provider.getMetadataURI().equals(ssoDomain.getSamlMetaUrl()) && !StringUtils.isBlank(metadata.getDefaultIDP())) {
             			addFlag = false;
             		}else {
             			 metadata.removeMetadataProvider(dto);
             		}
             	}
            }
        	if (addFlag) {
            	ExtendedMetadataDelegate newMetadataDelegate = newExtendedMetadataProvider(ssoDomain);
            	metadata.addMetadataProvider(newMetadataDelegate);
            	metadata.afterPropertiesSet();
            }
            SAMLMessageContext context = contextProvider.getLocalAndPeerEntity(request, response);

//            if (isECP(context)) {
//                initializeECP(context, e);
//            } else if (isDiscovery(context)) {
//                initializeDiscovery(context);
//            } else {
//                initializeSSO(context, e);
//            }
            initializeSSO(context, e);

        } catch (SAMLException e1) {
            logger.debug("Error initializing entry point", e1);
            throw new ServletException(e1);
        } catch (MetadataProviderException e1) {
            logger.debug("Error initializing entry point", e1);
            throw new ServletException(e1);
        } catch (MessageEncodingException e1) {
            logger.debug("Error initializing entry point", e1);
            throw new ServletException(e1);
        }

    }

    /**
     * Initializes ECP profile.
     * <p>
     * Subclasses can alter the initialization behaviour.
     *
     * @param context saml context, also containing wrapped request and response objects
     * @param e       exception causing the entry point to be invoked (if any)
     * @throws MetadataProviderException in case metadata can't be queried
     * @throws SAMLException             in case message sending fails
     * @throws MessageEncodingException  in case SAML message encoding fails
     */
    protected void initializeECP(SAMLMessageContext context, AuthenticationException e)
            throws MetadataProviderException, SAMLException, MessageEncodingException {

        WebSSOProfileOptions options = getProfileOptions(context, e);

        logger.debug("Processing SSO using ECP profile");
        webSSOprofileECP.sendAuthenticationRequest(context, options);
        samlLogger.log(SAMLConstants.AUTH_N_REQUEST, SAMLConstants.SUCCESS, context);

    }

    /**
     * WebSSO profile or WebSSO Holder-of-Key profile. Selection is made based on the settings of the Service Provider.
     * In case Enhanced Client/Proxy is enabled and the request claims to support this profile it is used. Otherwise it is verified what is the binding
     * and profile specified for the assertionConsumerIndex in the WebSSOProfileOptions. In case it is HoK the WebSSO Holder-of-Key profile is used,
     * otherwise the ordinary WebSSO.
     * <p>
     * Subclasses can alter the initialization behaviour.
     *
     * @param context saml context, also containing wrapped request and response objects
     * @param e       exception causing the entry point to be invoked (if any)
     * @throws MetadataProviderException in case metadata can't be queried
     * @throws SAMLException             in case message sending fails
     * @throws MessageEncodingException  in case SAML message encoding fails
     */
    protected void initializeSSO(SAMLMessageContext context, AuthenticationException e)
            throws MetadataProviderException, SAMLException, MessageEncodingException {

        // Generate options for the current SSO request
        WebSSOProfileOptions options = getProfileOptions(context, e);

        // Determine the assertionConsumerService to be used
        AssertionConsumerService consumerService = SAMLUtil.getConsumerService(
                (SPSSODescriptor) context.getLocalEntityRoleMetadata(), options.getAssertionConsumerIndex());

        // HoK WebSSO
        if (SAMLConstants.SAML2_HOK_WEBSSO_PROFILE_URI.equals(consumerService.getBinding())) {
            if (webSSOprofileHoK == null) {
                logger.warn(
                        "WebSSO HoK profile was specified to be used, but profile is not configured in the EntryPoint, HoK will be skipped");
            } else {
                logger.debug("Processing SSO using WebSSO HolderOfKey profile");
                webSSOprofileHoK.sendAuthenticationRequest(context, options);
                samlLogger.log(SAMLConstants.AUTH_N_REQUEST, SAMLConstants.SUCCESS, context);
                return;
            }
        }

        // Ordinary WebSSO
        logger.debug("Processing SSO using WebSSO profile");
        webSSOprofile.sendAuthenticationRequest(context, options);
        samlLogger.log(SAMLConstants.AUTH_N_REQUEST, SAMLConstants.SUCCESS, context);

    }

    /**
     * Method initializes IDP Discovery Profile as defined in http://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-idp-discovery.pdf
     * It is presumed that metadata of the local Service Provider contains discovery return address.
     *
     * @param context saml context also containing request and response objects
     * @throws ServletException          error
     * @throws IOException               io error
     * @throws MetadataProviderException in case metadata of the local entity can't be populated
     */
    protected void initializeDiscovery(SAMLMessageContext context)
            throws ServletException, IOException, MetadataProviderException {

        String discoveryURL = context.getLocalExtendedMetadata().getIdpDiscoveryURL();

        if (discoveryURL != null) {

            URLBuilder urlBuilder = new URLBuilder(discoveryURL);
            List<Pair<String, String>> queryParams = urlBuilder.getQueryParams();
            queryParams.add(new Pair<String, String>(SAMLDiscovery.ENTITY_ID_PARAM, context.getLocalEntityId()));
            queryParams.add(new Pair<String, String>(SAMLDiscovery.RETURN_ID_PARAM, IDP_PARAMETER));
            discoveryURL = urlBuilder.buildURL();

            logger.debug("Using discovery URL from extended metadata");

        } else {

            String discoveryUrl = SAMLDiscovery.FILTER_URL;
            if (samlDiscovery != null) {
                discoveryUrl = samlDiscovery.getFilterProcessesUrl();
            }

            String contextPath = (String) context.getInboundMessageTransport()
                    .getAttribute(SAMLConstants.LOCAL_CONTEXT_PATH);
            discoveryURL = contextPath + discoveryUrl + "?" + SAMLDiscovery.RETURN_ID_PARAM + "=" + IDP_PARAMETER + "&"
                    + SAMLDiscovery.ENTITY_ID_PARAM + "=" + context.getLocalEntityId();

            logger.debug("Using local discovery URL");

        }

        logger.debug("Redirecting to discovery URL =" + discoveryURL);
        HTTPOutTransport response = (HTTPOutTransport) context.getOutboundMessageTransport();
        response.sendRedirect(discoveryURL);

    }

    /**
     * Method is supposed to populate preferences used to construct the SAML message. Method can be overridden to provide
     * logic appropriate for given application. In case defaultOptions object was set it will be used as basis for construction
     * and request specific values will be update (idp field).
     *
     * @param context   containing local entity
     * @param exception exception causing invocation of this entry point (can be null)
     * @return populated webSSOprofile
     * @throws MetadataProviderException in case metadata loading fails
     */
    protected WebSSOProfileOptions getProfileOptions(SAMLMessageContext context, AuthenticationException exception)
            throws MetadataProviderException {

        WebSSOProfileOptions ssoProfileOptions;
        if (defaultOptions != null) {
            ssoProfileOptions = defaultOptions.clone();
        } else {
            ssoProfileOptions = new WebSSOProfileOptions();
        }

        return ssoProfileOptions;

    }

    /**
     * Sets object which determines default values to be used as basis for construction during getProfileOptions call.
     *
     * @param defaultOptions default object to use for options construction
     */
    public void setDefaultProfileOptions(WebSSOProfileOptions defaultOptions) {
        if (defaultOptions != null) {
            this.defaultOptions = defaultOptions.clone();
        } else {
            this.defaultOptions = null;
        }
    }

    public void setDefaultProfileOptions() {
        WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
        webSSOProfileOptions.setIncludeScoping(false);
        if (defaultOptions != null) {
            this.defaultOptions = defaultOptions.clone();
        } else {
            this.defaultOptions = null;
        }
    }

    /**
     * Determines whether IDP Discovery should be initialized. By default no user-selected IDP must be present in the context,
     * IDP Discovery must be enabled and the request mustn't be a response from IDP Discovery in order for the method
     * to return true.
     *
     * @param context context
     * @return true if IDP Discovery should get initialized
     */
    protected boolean isDiscovery(SAMLMessageContext context) {
        return !context.isPeerUserSelected() && context.getLocalExtendedMetadata().isIdpDiscoveryEnabled()
                && !isDiscoResponse(context);
    }

    /**
     * Determines whether ECP profile should get initialized. By default ECP is used when request declares supports for ECP
     * and ECP is allowed for the current service provider. In case ECP is enabled but webSSOprofileECP wasn't set a warning
     * is logged and ECP is not used.
     *
     * @param context context
     * @return true if ECP profile should get initialized
     */
    protected boolean isECP(SAMLMessageContext context) {
        HttpServletRequest request = ((HttpServletRequestAdapter) context.getInboundMessageTransport())
                .getWrappedRequest();
        boolean ecp = context.getLocalExtendedMetadata().isEcpEnabled() && SAMLUtil.isECPRequest(request);
        if (ecp) {
            if (webSSOprofileECP == null) {
                logger.warn(
                        "ECP profile was specified to be used, but profile is not configured in the EntryPoint, ECP will be skipped");
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * True value indicates that request is a response from the discovery profile. We use the value to
     * prevent repeated invocation of the discovery service upon failure.
     *
     * @param context context with request and response included
     * @return true if this HttpRequest is a response from IDP discovery profile.
     */
    private boolean isDiscoResponse(SAMLMessageContext context) {
        HTTPInTransport request = (HTTPInTransport) context.getInboundMessageTransport();
        String disco = request.getParameterValue(DISCOVERY_RESPONSE_PARAMETER);
        return (disco != null && "true".equals(disco.toLowerCase().trim()));
    }

    @Autowired
    public void setSsoAuthenticationLocator(SsoAuthenticationLocator ssoAuthenticationLocator) {
        this.ssoAuthenticationLocator = ssoAuthenticationLocator;
    }

    /**
     * Profile for consumption of processed messages, cannot be null, must be set.
     *
     * @param webSSOprofile profile
     */
    @Autowired(required = false)
    @Qualifier("webSSOprofile")
    public void setWebSSOprofile(WebSSOProfile webSSOprofile) {
        this.webSSOprofile = webSSOprofile;
    }

    @Autowired(required = false)
    @Qualifier("ecpprofile")
    public void setWebSSOprofileECP(WebSSOProfile webSSOprofileECP) {
        this.webSSOprofileECP = webSSOprofileECP;
    }

    @Autowired(required = false)
    @Qualifier("hokWebSSOProfile")
    public void setWebSSOprofileHoK(WebSSOProfile webSSOprofileHoK) {
        this.webSSOprofileHoK = webSSOprofileHoK;
    }

    /**
     * Logger for SAML events, cannot be null, must be set.
     *
     * @param samlLogger logger
     */
    @Autowired(required = false)
    public void setSamlLogger(SAMLLogger samlLogger) {
        this.samlLogger = samlLogger;
    }

    /**
     * Dependency for loading of discovery URL
     * @param samlDiscovery saml discovery endpoint
     */
    @Autowired(required = false)
    public void setSamlDiscovery(SAMLDiscovery samlDiscovery) {
        this.samlDiscovery = samlDiscovery;
    }

    /**
     * Sets entity responsible for populating local entity context data.
     *
     * @param contextProvider provider implementation
     */
    @Autowired(required = false)
    public void setContextProvider(SAMLContextProvider contextProvider) {
        //Assert.notNull(contextProvider, "Context provider can't be null");
        this.contextProvider = contextProvider;
    }

    @Autowired(required = false)
    @Qualifier("samlHttpClient")
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Autowired(required = false)
    public void setParserPool(ParserPool parserPool) {
        this.parserPool = parserPool;
    }

    @Autowired(required = false)
    public void setExtendedMetadata(ExtendedMetadata extendedMetadata) {
        this.extendedMetadata = extendedMetadata;
    }

    /**
     * Metadata manager, cannot be null, must be set.
     *
     * @param metadata manager
     * @throws MetadataProviderException 
     */
    @Autowired(required = false)
    public void setMetadata(MetadataManager metadata) throws MetadataProviderException {
        this.metadata = metadata;
    }
    
    @Autowired(required = false)
    public void setKeyManager(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    public ExtendedMetadataDelegate newExtendedMetadataProvider(Domain ssoDomain) throws MetadataProviderException {
        String idpSSOCircleMetadataURL = ssoDomain.getSamlMetaUrl();
        HTTPMetadataProvider httpMetadataProvider = new HTTPMetadataProvider(this.backgroundTaskTimer, this.httpClient,
                idpSSOCircleMetadataURL);
        httpMetadataProvider.setParserPool(this.parserPool);
        ExtendedMetadataDelegate extendedMetadataDelegate = new ExtendedMetadataDelegate(httpMetadataProvider,
                this.extendedMetadata);
        extendedMetadataDelegate.setMetadataTrustCheck(false);
        extendedMetadataDelegate.setMetadataRequireSignature(false);
        this.backgroundTaskTimer.purge();
        return extendedMetadataDelegate;
    }

    /**
     * @return filter URL
     */
    public String getFilterProcessesUrl() {
        return filterProcessesUrl;
    }

    /**
     * Custom filter URL which overrides the default. Filter url determines URL where filter starts processing.
     *
     * @param filterProcessesUrl filter URL
     */
    public void setFilterProcessesUrl(String filterProcessesUrl) {
        this.filterProcessesUrl = filterProcessesUrl;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        FilterInvocation fi = new FilterInvocation(request, response, chain);

        if (!processFilter(fi.getRequest())) {
            chain.doFilter(request, response);
            return;
        }
        commence(fi.getRequest(), fi.getResponse(), null);
    }
}
