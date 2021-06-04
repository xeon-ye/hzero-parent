package org.hzero.sso.cas.autoconfigure;

import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorageImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.util.Assert;

import org.hzero.sso.cas.config.CasSsoRegister;
import org.hzero.sso.cas.provider.CasAuthenticationProvider;
import org.hzero.sso.cas.service.CasUserDetailsService;
import org.hzero.sso.core.config.SsoProperties;
import org.hzero.sso.core.domain.repository.DomainRepository;
import org.hzero.sso.core.security.service.SsoUserAccountService;
import org.hzero.sso.core.security.service.SsoUserDetailsBuilder;
import org.hzero.sso.core.type.SsoRegister;

@Configuration
@ComponentScan(value = {
		        "org.hzero.sso.cas",
		})
@ConditionalOnProperty(prefix = SsoProperties.PREFIX, name = "sso.enabled", havingValue = "true")
public class CasAutoConfiguration {
	
	private SsoProperties ssoProperties;
	
	public CasAutoConfiguration(SsoProperties ssoProperties) {
        this.ssoProperties = ssoProperties;
    }

    @Bean
    public SsoRegister casSsoRegister()  {
	    return new CasSsoRegister();
    }

    @Bean
    @ConditionalOnMissingBean(ProxyGrantingTicketStorage.class)
    public ProxyGrantingTicketStorage proxyGrantingTicketStorage() {
        return new ProxyGrantingTicketStorageImpl();
    }
	
    @Bean
    @ConditionalOnMissingBean(ServiceProperties.class)
    public ServiceProperties serviceProperties() {
        String serviceBaseUrl = ssoProperties.getSso().getService().getBaseUrl();
        Assert.notNull(serviceBaseUrl, "cas service base url not be null.");
        ServiceProperties serviceProperties = new ServiceProperties();
        serviceProperties.setService(serviceBaseUrl + ssoProperties.getSso().getService().getLoginPath());
        serviceProperties.setAuthenticateAllArtifacts(true);
        return serviceProperties;
    }
	
	@Bean
    @ConditionalOnMissingBean(CasUserDetailsService.class)
    public CasUserDetailsService casUserDetailsService(SsoUserAccountService userAccountService,
            SsoUserDetailsBuilder userDetailsBuilder) {
        return new CasUserDetailsService(userAccountService, userDetailsBuilder);
    }
    
    

    @Bean
    @ConditionalOnMissingBean(CasAuthenticationProvider.class)
    public CasAuthenticationProvider casAuthenticationProvider(DomainRepository domainRepository,
    														   ServiceProperties serviceProperties,
    														   CasUserDetailsService casUserDetailsService) {
        CasAuthenticationProvider authenticationProvider = new CasAuthenticationProvider();
        authenticationProvider.setKey(ssoProperties.getSso().getProvider().getKey());
        authenticationProvider.setServiceProperties(serviceProperties);
        authenticationProvider.setAuthenticationUserDetailsService(casUserDetailsService);
        authenticationProvider.setDomainRepository(domainRepository);
        return authenticationProvider;
    }

}
