package org.hzero.sso.oauth.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import org.hzero.sso.core.security.service.SsoUserAccountService;
import org.hzero.sso.core.security.service.SsoUserDetailsBuilder;
import org.hzero.sso.core.type.SsoRegister;
import org.hzero.sso.oauth.config.AuthSsoRegister;
import org.hzero.sso.oauth.provider.Auth2AuthenticationProvider;
import org.hzero.sso.oauth.service.Auth2UserDetailsService;

@Configuration
@ComponentScan(value = {
		        "org.hzero.sso.oauth",
		})
public class OauthAutoConfiguration {

	@Bean
	public SsoRegister authSsoRegister()  {
		return new AuthSsoRegister();
	}

	@Bean
	@ConditionalOnMissingBean(Auth2UserDetailsService.class)
	public Auth2UserDetailsService auth2UserDetailsService(SsoUserAccountService userAccountService,
	                SsoUserDetailsBuilder userDetailsBuilder) {
		return new Auth2UserDetailsService(userAccountService, userDetailsBuilder);
	}
	
	@Bean
    @ConditionalOnMissingBean(Auth2AuthenticationProvider.class)
    public Auth2AuthenticationProvider auth2AuthenticationProvider(Auth2UserDetailsService auth2UserDetailsService) {
        return new Auth2AuthenticationProvider(auth2UserDetailsService);
    }

}
