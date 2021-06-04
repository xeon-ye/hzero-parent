package org.hzero.sso.saml.autoconfigure;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml.SAMLBootstrap;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.metadata.CachingMetadataManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.parser.ParserPoolHolder;
import org.springframework.security.saml.processor.*;
import org.springframework.security.saml.trust.httpclient.TLSProtocolConfigurer;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.*;

import org.hzero.sso.core.config.SsoProperties;
import org.hzero.sso.core.security.service.SsoUserAccountService;
import org.hzero.sso.core.security.service.SsoUserDetailsBuilder;
import org.hzero.sso.core.type.SsoRegister;
import org.hzero.sso.core.util.KeyStoreLocator;
import org.hzero.sso.saml.config.SamlSsoRegister;
import org.hzero.sso.saml.provider.SamlAuthenticationProvider;
import org.hzero.sso.saml.service.SamlUserDetailsService;

@Configuration
@ComponentScan(value = { "org.hzero.sso.saml", })
public class SamlAutoConfiguration {

	@Autowired
	private SsoProperties ssoProperties;

	@Bean
	public SsoRegister samlSsoRegister()  {
		return new SamlSsoRegister();
	}

	@Bean
	@ConditionalOnMissingBean(SamlUserDetailsService.class)
	public SamlUserDetailsService samlUserDetailsService(SsoUserAccountService userAccountService,
	                SsoUserDetailsBuilder userDetailsBuilder) {
		return new SamlUserDetailsService(userAccountService, userDetailsBuilder);
	}

	@Bean
	@ConditionalOnMissingBean(SamlAuthenticationProvider.class)
	public SamlAuthenticationProvider samlAuthenticationProvider(SamlUserDetailsService samlUserDetailsService) {
		SamlAuthenticationProvider samlAuthenticationProvider = new SamlAuthenticationProvider(
		                samlUserDetailsService);
		// samlAuthenticationProvider.setUserDetails(defaultSAMLUserDetailsService);
		samlAuthenticationProvider.setForcePrincipalAsString(false);
		samlAuthenticationProvider.setExcludeCredential(true);
		return samlAuthenticationProvider;
	}

	@Bean(name = "samlHttpClient")
	public HttpClient httpClient() {
		return new HttpClient(new MultiThreadedHttpConnectionManager());
	}

	@Bean
	public ExtendedMetadata extendedMetadata() {
		ExtendedMetadata extendedMetadata = new ExtendedMetadata();
		extendedMetadata.setIdpDiscoveryEnabled(false);
		extendedMetadata.setSignMetadata(true);
		extendedMetadata.setEcpEnabled(true);
		return extendedMetadata;
	}

	@Bean
	@Qualifier("metadata")
	public CachingMetadataManager metadata() throws MetadataProviderException {
		List<MetadataProvider> providers = new ArrayList<>();
		CachingMetadataManager metadataManager = new CachingMetadataManager(providers);
		return metadataManager;
	}

	@Bean
	public VelocityEngine velocityEngine() {
		return VelocityFactory.getEngine();
	}

	@Bean(initMethod = "initialize")
	public ParserPool parserPool() {
		return new StaticBasicParserPool();
	}

	@Bean(name = "parserPoolHolder")
	public ParserPoolHolder parserPoolHolder() {
		return new ParserPoolHolder();
	}

	@Bean
	public SAMLContextProviderImpl contextProvider() {
		return new SAMLContextProviderImpl();
	}

	@Bean
	public JKSKeyManager keyManager() throws InvalidKeySpecException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, XMLStreamException {
		KeyStore keyStore = KeyStoreLocator.createKeyStore(ssoProperties.getSso().getSaml().getPassphrase());
		KeyStoreLocator.addPrivateKey(keyStore, ssoProperties.getSso().getSaml().getEntityId(), ssoProperties.getSso().getSaml().getPrivateKey(), ssoProperties.getSso().getSaml().getCertificate(), ssoProperties.getSso().getSaml().getPassphrase());
		return new JKSKeyManager(keyStore, Collections.singletonMap(ssoProperties.getSso().getSaml().getEntityId(), ssoProperties.getSso().getSaml().getPassphrase()), ssoProperties.getSso().getSaml().getEntityId());
	}

	@Bean
	public WebSSOProfileOptions defaultWebSSOProfileOptions() {
		WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
		webSSOProfileOptions.setIncludeScoping(false);
		return webSSOProfileOptions;
	}

	private ArtifactResolutionProfile artifactResolutionProfile() {
		final ArtifactResolutionProfileImpl artifactResolutionProfile =
				new ArtifactResolutionProfileImpl(httpClient());
		artifactResolutionProfile.setProcessor(new SAMLProcessorImpl(soapBinding()));
		return artifactResolutionProfile;
	}

	@Bean
	public HTTPArtifactBinding artifactBinding(ParserPool parserPool, VelocityEngine velocityEngine) {
		return new HTTPArtifactBinding(parserPool, velocityEngine, artifactResolutionProfile());
	}

	@Bean
	public HTTPSOAP11Binding soapBinding() {
		return new HTTPSOAP11Binding(parserPool());
	}

	@Bean
	public HTTPPostBinding httpPostBinding() {
		return new HTTPPostBinding(parserPool(), velocityEngine());
	}

	@Bean
	public HTTPRedirectDeflateBinding httpRedirectDeflateBinding() {
		return new HTTPRedirectDeflateBinding(parserPool());
	}

	@Bean
	public HTTPSOAP11Binding httpSOAP11Binding() {
		return new HTTPSOAP11Binding(parserPool());
	}

	@Bean
	public HTTPPAOS11Binding httpPAOS11Binding() {
		return new HTTPPAOS11Binding(parserPool());
	}

	@Bean
	public SAMLProcessorImpl processor() {
		Collection<SAMLBinding> bindings = new ArrayList<SAMLBinding>();
		bindings.add(httpRedirectDeflateBinding());
		bindings.add(httpPostBinding());
		bindings.add(artifactBinding(parserPool(), velocityEngine()));
		bindings.add(httpSOAP11Binding());
		bindings.add(httpPAOS11Binding());
		return new SAMLProcessorImpl(bindings);
	}

	// Logger for SAML messages and events
	@Bean
	public SAMLDefaultLogger samlLogger() {
		return new SAMLDefaultLogger();
	}

	// SAML 2.0 WebSSO Assertion Consumer
	@Bean
	public WebSSOProfileConsumer webSSOprofileConsumer() {
		return new WebSSOProfileConsumerImpl();
	}

	// SAML 2.0 Holder-of-Key WebSSO Assertion Consumer
	@Bean
	public WebSSOProfileConsumerHoKImpl hokWebSSOprofileConsumer() {
		return new WebSSOProfileConsumerHoKImpl();
	}


	//SAML 2.0 Web SSO profile
	@Bean
	public WebSSOProfile webSSOprofile() {
		return new WebSSOProfileImpl();
	}

	// SAML 2.0 ECP profile
	@Bean
	public WebSSOProfileECPImpl ecpprofile() {
		return new WebSSOProfileECPImpl();
	}

	@Bean
	public TLSProtocolConfigurer tlsProtocolConfigurer() {
		return new TLSProtocolConfigurer();
	}

	// Initialization of OpenSAML library
	@Bean
	public static SAMLBootstrap sAMLBootstrap() {
		return new SAMLBootstrap();
	}

}
