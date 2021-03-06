package org.hzero.oauth.config;

import org.hzero.oauth.security.config.SecurityProperties;
import org.hzero.oauth.security.custom.CustomAuthorizationCodeTokenGranter;
import org.hzero.oauth.security.custom.CustomClientDetailsService;
import org.hzero.oauth.security.custom.CustomRedirectResolver;
import org.hzero.oauth.security.custom.CustomUserDetailsService;
import org.hzero.oauth.security.custom.processor.authorize.AuthorizeSuccessProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.CompositeTokenGranter;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.client.ClientCredentialsTokenGranter;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeTokenGranter;
import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.implicit.ImplicitTokenGranter;
import org.springframework.security.oauth2.provider.password.ResourceOwnerPasswordTokenGranter;
import org.springframework.security.oauth2.provider.refresh.RefreshTokenGranter;
import org.springframework.security.oauth2.provider.token.TokenStore;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bojiangzhou
 */
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private CustomClientDetailsService clientDetailsService;
    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private TokenStore tokenStore;
    @Autowired
    private SecurityProperties securityProperties;
    @Autowired
    private List<AuthorizeSuccessProcessor> authorizeSuccessProcessors = new ArrayList<>();

    /**
     * ?????????????????????authorization??????????????????token?????????????????????????????????(token services)???
     * <p>
     * authenticationManager: ????????????AuthenticationManager??????password grant?????????
     * userDetailsService: ?????????????????????UserDetailsService,refresh token grant??????????????????????????????????????????????????????????????????
     * authorizationCodeServices: ??????????????????????????????????????????????????? AuthorizationCodeServices ????????????????????????????????? "authorization_code" ????????????????????????
     * CustomTokenStore extends JdbcTokenStore: ???????????????????????????????????????
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints
                .authorizationCodeServices(new JdbcAuthorizationCodeServices(dataSource))
                .tokenStore(tokenStore)
                .userDetailsService(userDetailsService)
                .authenticationManager(authenticationManager)
                .redirectResolver(new CustomRedirectResolver())
                .setClientDetailsService(clientDetailsService)
                .setAuthorizeSuccessProcessors(authorizeSuccessProcessors)
        ;

        endpoints.tokenGranter(tokenGranter(endpoints));
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(clientDetailsService);
    }

    /**
     * ????????????????????????(Token Endpoint)???????????????
     * allowFormAuthenticationForClients:???????????? clientCredentialsTokenEndpointFilter
     * ( clientCredentialsTokenEndpointFilter:
     * ??????request??????client_id???client_secret;?????????UsernamePasswordAuthenticationToken,
     * ????????????UserDetailsService????????????????????????,???????????????password?????????client_credentials
     * )
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) {
        oauthServer
                .tokenKeyAccess("permitAll()")
                .checkTokenAccess("permitAll()")
                .allowFormAuthenticationForClients();
    }

    private TokenGranter tokenGranter(AuthorizationServerEndpointsConfigurer endpoints) {
        return new TokenGranter() {
            private CompositeTokenGranter delegate;

            @Override
            public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {
                if (delegate == null) {
                    delegate = new CompositeTokenGranter(getDefaultTokenGranters(endpoints));
                }
                return delegate.grant(grantType, tokenRequest);
            }
        };
    }

    private List<TokenGranter> getDefaultTokenGranters(AuthorizationServerEndpointsConfigurer endpoints) {
        List<TokenGranter> tokenGranters = new ArrayList<>();
        // ?????????????????? AuthorizationCodeTokenGranter
        // ????????? clientId ?????????
        if (securityProperties.isNotCheckClientEquals()) {
            tokenGranters.add(new CustomAuthorizationCodeTokenGranter(endpoints.getTokenServices(),
                    endpoints.getAuthorizationCodeServices(), endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory()));
        } else {
            tokenGranters.add(new AuthorizationCodeTokenGranter(endpoints.getTokenServices(),
                    endpoints.getAuthorizationCodeServices(), endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory()));
        }

        tokenGranters.add(new RefreshTokenGranter(endpoints.getTokenServices(),
                endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory()));

        tokenGranters.add(new ImplicitTokenGranter(endpoints.getTokenServices(),
                endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory()));

        ClientCredentialsTokenGranter credentialsTokenGranter = new ClientCredentialsTokenGranter(endpoints.getTokenServices(), endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory());
        credentialsTokenGranter.setAllowRefresh(securityProperties.isCredentialsAllowRefresh());
        tokenGranters.add(credentialsTokenGranter);

        if (authenticationManager != null) {
            tokenGranters.add(new ResourceOwnerPasswordTokenGranter(authenticationManager, endpoints.getTokenServices(),
                    endpoints.getClientDetailsService(), endpoints.getOAuth2RequestFactory()));
        }
        return tokenGranters;
    }
}
