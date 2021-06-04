package org.hzero.sso.oauth.config;

import java.util.Collections;
import org.hzero.sso.core.domain.repository.DomainRepository;
import org.hzero.sso.oauth.filter.Auth2AuthenticationFilter;
import org.hzero.sso.oauth.provider.Auth2AuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Order(org.springframework.boot.autoconfigure.security.SecurityProperties.BASIC_AUTH_ORDER - 1)
@Configuration
public class OauthWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
    
    @Autowired(required = false)
    private DomainRepository domainRepository;

    @Autowired(required = false)
    private Auth2AuthenticationProvider auth2AuthenticationProvider;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	http
        	.antMatcher("/login/auth2/**")
        	.authorizeRequests()
        	.anyRequest()
        	.permitAll()
        	.and()
        	.addFilterAt(auth2AuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
        	.csrf()
        	.disable()
        	;

    }
   
    private Auth2AuthenticationFilter auth2AuthenticationFilter() {
        Auth2AuthenticationFilter auth2AuthenticationFilter = new Auth2AuthenticationFilter(domainRepository);
        ProviderManager providerManager = new ProviderManager(Collections.singletonList(auth2AuthenticationProvider));
        auth2AuthenticationFilter.setAuthenticationManager(providerManager);
        return auth2AuthenticationFilter;
    }

}
