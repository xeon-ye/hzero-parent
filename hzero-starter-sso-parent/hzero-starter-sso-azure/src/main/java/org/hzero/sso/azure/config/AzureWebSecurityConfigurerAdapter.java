package org.hzero.sso.azure.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.hzero.sso.azure.filter.AzureAuthenticationFilter;
import org.hzero.sso.azure.provider.AzureAuthenticationProvider;
import org.hzero.sso.core.domain.repository.DomainRepository;

@Order(org.springframework.boot.autoconfigure.security.SecurityProperties.BASIC_AUTH_ORDER - 5)
@Configuration
public class AzureWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
    
    @Autowired(required = false)
    private DomainRepository domainRepository;

    @Autowired(required = false)
    private AzureAuthenticationProvider azureAuthenticationProvider;
    @Autowired
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	http
        	.antMatcher("/login/azure/**")
        	.authorizeRequests()
        	.anyRequest()
        	.permitAll()
        	.and()
        	.addFilterAt(azureAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
        	.csrf()
        	.disable()
        	;

    }
   
    private AzureAuthenticationFilter azureAuthenticationFilter() {
        AzureAuthenticationFilter azureAuthenticationFilter = new AzureAuthenticationFilter(domainRepository);
        ProviderManager providerManager = new ProviderManager(Collections.singletonList(azureAuthenticationProvider));
        azureAuthenticationFilter.setAuthenticationManager(providerManager);
        azureAuthenticationFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        return azureAuthenticationFilter;
    }

}
