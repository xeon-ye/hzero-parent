package org.hzero.sso.saml.provider;

import org.hzero.sso.core.exception.AccountNotExistsException;
import org.hzero.sso.saml.token.CustomSamlAuthenticationToken;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLRuntimeException;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLAuthenticationToken;
import org.springframework.security.saml.SAMLConstants;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.util.Assert;

public class SamlAuthenticationProvider extends SAMLAuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(SamlAuthenticationProvider.class);
    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    private AuthenticationUserDetailsService<CustomSamlAuthenticationToken> authenticationUserDetailsService;

    public SamlAuthenticationProvider(
            AuthenticationUserDetailsService<CustomSamlAuthenticationToken> authenticationUserDetailsService) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if (!supports(authentication.getClass())) {
            throw new IllegalArgumentException(
                    "Only SAMLAuthenticationToken is supported, " + authentication.getClass() + " was attempted");
        }

        SAMLAuthenticationToken token = (SAMLAuthenticationToken) authentication;
        SAMLMessageContext context = token.getCredentials();

        if (context == null) {
            throw new AuthenticationServiceException(
                    "SAML message context is not available in the authentication token");
        }

        SAMLCredential credential;

        try {
            if (SAMLConstants.SAML2_WEBSSO_PROFILE_URI.equals(context.getCommunicationProfileId())) {
                credential = consumer.processAuthenticationResponse(context);
            } else if (SAMLConstants.SAML2_HOK_WEBSSO_PROFILE_URI.equals(context.getCommunicationProfileId())) {
                credential = hokConsumer.processAuthenticationResponse(context);
            } else {
                throw new SAMLException(
                        "Unsupported profile encountered in the context " + context.getCommunicationProfileId());
            }
        } catch (SAMLRuntimeException e) {
            log.debug("Error validating SAML message", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message", e);
        } catch (SAMLException e) {
            log.debug("Error validating SAML message", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message", e);
        } catch (ValidationException e) {
            log.debug("Error validating signature", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message signature", e);
        } catch (org.opensaml.xml.security.SecurityException e) {
            log.debug("Error validating signature", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message signature", e);
        } catch (DecryptionException e) {
            log.debug("Error decrypting SAML message", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error decrypting SAML message", e);
        }
        //SAMLPrincipal sAMLPrincipal = (SAMLPrincipal) getUserDetails().loadUserBySAML(credential);
        String username = credential.getNameID().getValue();
        final CustomSamlAuthenticationToken authenticationToken = new CustomSamlAuthenticationToken(username, "");
        UserDetails user = this.authenticationUserDetailsService.loadUserDetails(authenticationToken);
        if (user == null) {
            throw new AccountNotExistsException("User account is Not Exists");
        }
        Assert.notNull(user, "retrieveUser returned null - a violation of the interface contract");
        return createSuccessAuthentication(user, authentication, user);
    }

    protected Authentication createSuccessAuthentication(Object principal, Authentication authentication,
            UserDetails user) {
        CustomSamlAuthenticationToken result = new CustomSamlAuthenticationToken(principal, authentication.getCredentials(),
                authoritiesMapper.mapAuthorities(user.getAuthorities()));
        result.setDetails(authentication.getDetails());
        return result;
    }
}
