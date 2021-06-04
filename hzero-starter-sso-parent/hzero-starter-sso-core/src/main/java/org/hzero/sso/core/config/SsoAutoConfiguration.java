package org.hzero.sso.core.config;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import org.hzero.sso.core.domain.repository.DomainRepository;
import org.hzero.sso.core.infra.repository.impl.DomainRepositoryImpl;
import org.hzero.sso.core.type.SsoAuthenticationLocator;
import org.hzero.sso.core.type.SsoRegister;

@ComponentScan(value = {
    "org.hzero.sso.core",
})
@Configuration
@EnableConfigurationProperties(SsoProperties.class)
public class SsoAutoConfiguration {

    @Autowired
    private SsoProperties ssoProperties;

    @Bean
    @ConditionalOnMissingBean(DomainRepository.class)
    public DomainRepository domainRepository() {
        return new DomainRepositoryImpl();
    }

    @Bean
    public SsoInitializeConfig ssoInitializeConfig() {
        return new SsoInitializeConfig();
    }

    @Bean
    public SsoAuthenticationLocator ssoAuthenticationLocator(Optional<List<SsoRegister>> optional) {
        Set<String> ssoIds = optional.orElse(new ArrayList<>()).stream().map(SsoRegister::ids)
                .flatMap(Collection::stream).collect(Collectors.toSet());
        return new SsoAuthenticationLocator(ssoIds);
    }

    @Bean
    @ConditionalOnMissingBean(SsoAuthenticationEntryPoint.class)
    public SsoAuthenticationEntryPoint ssoAuthenticationEntryPoint(DomainRepository domainRepository) {
        SsoAuthenticationEntryPoint ssoAuthenticationEntryPoint = new SsoAuthenticationEntryPoint(domainRepository, ssoProperties);
        ssoAuthenticationEntryPoint.setDefaultProfileOptions();
        return ssoAuthenticationEntryPoint;
    }

}
