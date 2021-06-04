package org.hzero.sso.idm.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import org.hzero.sso.core.security.service.SsoUserAccountService;
import org.hzero.sso.core.security.service.SsoUserDetailsBuilder;
import org.hzero.sso.core.type.SsoRegister;
import org.hzero.sso.idm.config.IdmSsoRegister;
import org.hzero.sso.idm.provider.IdmAuthenticationProvider;
import org.hzero.sso.idm.service.IdmUserDetailsService;

@Configuration
@ComponentScan(value = {
		        "org.hzero.sso.idm",
		})
public class IdmAutoConfiguration {

	@Bean
	public SsoRegister idmSsoRegister()  {
		return new IdmSsoRegister();
	}

	@Bean
	@ConditionalOnMissingBean(IdmUserDetailsService.class)
	public IdmUserDetailsService idmUserDetailsService(SsoUserAccountService userAccountService,
	                SsoUserDetailsBuilder userDetailsBuilder) {
		return new IdmUserDetailsService(userAccountService, userDetailsBuilder);
	}
	
	@Bean
    @ConditionalOnMissingBean(IdmAuthenticationProvider.class)
    public IdmAuthenticationProvider idmAuthenticationProvider(IdmUserDetailsService idmUserDetailsService) {
        return new IdmAuthenticationProvider(idmUserDetailsService);
    }

}
