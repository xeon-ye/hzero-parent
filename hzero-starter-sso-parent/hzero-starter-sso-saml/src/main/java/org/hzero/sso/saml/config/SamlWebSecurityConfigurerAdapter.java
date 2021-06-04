package org.hzero.sso.saml.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.hzero.sso.core.config.SsoProperties;
import org.hzero.sso.saml.filter.SAMLProcessingFilter;
import org.hzero.sso.saml.provider.SamlAuthenticationProvider;

@Order(org.springframework.boot.autoconfigure.security.SecurityProperties.BASIC_AUTH_ORDER - 2)
@Configuration
public class SamlWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
	
    @Autowired
    private SsoProperties ssoProperties;

    @Autowired(required = false)
    private SamlAuthenticationProvider samlAuthenticationProvider;

    @Autowired(required = false)
    private KeyManager keyManager;

    @Autowired(required = false)
    private ExtendedMetadata extendedMetadata;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	http
        	.antMatcher("/saml/SSO/**")
        	.authorizeRequests()
        	.anyRequest()
        	.permitAll()
        	.and()
        	.addFilterBefore(metadataGeneratorFilter(), UsernamePasswordAuthenticationFilter.class)
        	.addFilterBefore(samlWebSSOProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
        	.csrf()
        	.disable()
        	;
    }
    
    @Bean
    @ConditionalOnMissingBean(MetadataDisplayFilter.class)
    public MetadataDisplayFilter metadataDisplayFilter() {
        return new MetadataDisplayFilter();
    }

    @Bean
    @ConditionalOnMissingBean(SAMLProcessingFilter.class)
    public SAMLProcessingFilter samlWebSSOProcessingFilter() {
        SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter("/saml/SSO");
        ProviderManager providerManager = new ProviderManager(Collections.singletonList(samlAuthenticationProvider));
        samlWebSSOProcessingFilter.setAuthenticationManager(providerManager);
        return samlWebSSOProcessingFilter;
    }

    @Bean
    @ConditionalOnMissingBean(MetadataGeneratorFilter.class)
    public MetadataGeneratorFilter metadataGeneratorFilter() {
        MetadataGenerator metadataGenerator = new MetadataGenerator();
        metadataGenerator.setEntityId(ssoProperties.getSso().getSaml().getEntityId());
        metadataGenerator.setEntityBaseURL(ssoProperties.getSso().getService().getBaseUrl());
        metadataGenerator.setExtendedMetadata(extendedMetadata);
        metadataGenerator.setIncludeDiscoveryExtension(false);
        metadataGenerator.setKeyManager(keyManager);
        return new MetadataGeneratorFilter(metadataGenerator);
    }
}
