package org.hzero.sso.idm.config;

import java.util.Collections;
import org.hzero.sso.core.domain.repository.DomainRepository;
import org.hzero.sso.idm.filter.IdmAuthenticationFilter;
import org.hzero.sso.idm.provider.IdmAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Order(org.springframework.boot.autoconfigure.security.SecurityProperties.BASIC_AUTH_ORDER - 3)
@Configuration
public class IdmWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
    
    @Autowired(required = false)
    private DomainRepository domainRepository;

    @Autowired(required = false)
    private IdmAuthenticationProvider idmAuthenticationProvider;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	http
        	.antMatcher("/login/idm/**")
        	.authorizeRequests()
        	.anyRequest()
        	.permitAll()
        	.and()
        	.addFilterAt(auth2AuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
        	.csrf()
        	.disable()
        	;

    }
   
    private IdmAuthenticationFilter auth2AuthenticationFilter() {
        IdmAuthenticationFilter auth2AuthenticationFilter = new IdmAuthenticationFilter(domainRepository);
        ProviderManager providerManager = new ProviderManager(Collections.singletonList(idmAuthenticationProvider));
        auth2AuthenticationFilter.setAuthenticationManager(providerManager);
        return auth2AuthenticationFilter;
    }

}
