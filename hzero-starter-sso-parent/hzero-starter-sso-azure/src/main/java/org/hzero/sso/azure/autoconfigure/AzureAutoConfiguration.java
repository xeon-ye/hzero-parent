package org.hzero.sso.azure.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import org.hzero.sso.azure.config.AzureSsoRegister;
import org.hzero.sso.azure.provider.AzureAuthenticationProvider;
import org.hzero.sso.azure.service.AzureUserDetailsService;
import org.hzero.sso.core.security.service.SsoUserAccountService;
import org.hzero.sso.core.security.service.SsoUserDetailsBuilder;
import org.hzero.sso.core.type.SsoRegister;

@Configuration
@ComponentScan(value = {
		        "org.hzero.sso.azure",
		})
public class AzureAutoConfiguration {

	@Bean
	public SsoRegister azureSsoRegister()  {
		return new AzureSsoRegister();
	}

	@Bean
	@ConditionalOnMissingBean(AzureUserDetailsService.class)
	public AzureUserDetailsService azureUserDetailsService(SsoUserAccountService userAccountService,
	                SsoUserDetailsBuilder userDetailsBuilder) {
		return new AzureUserDetailsService(userAccountService, userDetailsBuilder);
	}
	
	@Bean
    @ConditionalOnMissingBean(AzureAuthenticationProvider.class)
    public AzureAuthenticationProvider azureAuthenticationProvider(AzureUserDetailsService azureUserDetailsService) {
        return new AzureAuthenticationProvider(azureUserDetailsService);
    }

}
