package org.hzero.sso.cas.config;

import java.util.Collections;
import java.util.List;

import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.session.SingleSignOutFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetailsSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.hzero.sso.cas.filter.CasAuthenticationFilter;
import org.hzero.sso.cas.provider.CasAuthenticationProvider;
import org.hzero.sso.core.custom.processor.cas.login.CasLoginProcessor;
import org.hzero.sso.core.custom.processor.cas.logout.CasLogoutProcessor;
import org.hzero.sso.core.domain.repository.DomainRepository;

@Order(org.springframework.boot.autoconfigure.security.SecurityProperties.BASIC_AUTH_ORDER - 4)
@Configuration
public class CasWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
	
    @Autowired(required = false)
    private DomainRepository domainRepository;
    
    @Autowired(required = false)
    private ServiceProperties serviceProperties;

    @Autowired(required = false)
    private ProxyGrantingTicketStorage proxyGrantingTicketStorage;
    
    @Autowired(required = false)
    private CasAuthenticationProvider casAuthenticationProvider;

    @Autowired(required = false)
    private SavedRequestAwareAuthenticationSuccessHandler authenticationSuccessHandler;
    @Autowired(required = false)
    private AuthenticationFailureHandler authenticationFailureHandler;
    @Autowired(required = false)
    private List<CasLoginProcessor> casLoginProcessors;
    @Autowired(required = false)
    private List<CasLogoutProcessor> casLogoutProcessors;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	http
        	.antMatcher("/login/cas/**")
        	.authorizeRequests()
        	.anyRequest()
        	.permitAll()
        	.and()
        	.addFilterAt(casAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
        	.csrf()
        	.disable()
        	;

    }
    
    private CasAuthenticationFilter casAuthenticationFilter() throws Exception {
    	ProviderManager providerManager = new ProviderManager(Collections.singletonList(casAuthenticationProvider));
        CasAuthenticationFilter casAuthenticationFilter = new CasAuthenticationFilter();
        casAuthenticationFilter.setAuthenticationManager(providerManager);
        casAuthenticationFilter.setServiceProperties(serviceProperties);
        casAuthenticationFilter.setProxyGrantingTicketStorage(proxyGrantingTicketStorage);
        casAuthenticationFilter.setAuthenticationDetailsSource(new ServiceAuthenticationDetailsSource(serviceProperties));
        casAuthenticationFilter.setDomainRepository(domainRepository);
        casAuthenticationFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        casAuthenticationFilter.setAuthenticationFailureHandler(authenticationFailureHandler);
        casAuthenticationFilter.setProxyAuthenticationFailureHandler(authenticationFailureHandler);
        return casAuthenticationFilter;
    }
    
    @Bean
    public FilterRegistrationBean<SingleSignOutFilter> singleSignOutFilter(){
        FilterRegistrationBean<SingleSignOutFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(casSingleSingOutFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        // 顺序一定要放到 SessionRepositoryFilter 后面
        filterRegistrationBean.setOrder(Integer.MIN_VALUE + 100);
        return filterRegistrationBean;
    }

    private SingleSignOutFilter casSingleSingOutFilter(){
        SingleSignOutFilter singleSignOutFilter = new SingleSignOutFilter();
        singleSignOutFilter.setCasServerUrlPrefix("empty_cas_prefix");
        singleSignOutFilter.setCasLoginProcessors(casLoginProcessors);
        singleSignOutFilter.setCasLogoutProcessors(casLogoutProcessors);
        return singleSignOutFilter;
    }

}
