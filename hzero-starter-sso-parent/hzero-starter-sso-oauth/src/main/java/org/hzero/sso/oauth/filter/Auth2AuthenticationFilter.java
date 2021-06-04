package org.hzero.sso.oauth.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.hzero.sso.core.domain.entity.Domain;
import org.hzero.sso.core.domain.repository.DomainRepository;
import org.hzero.sso.core.util.JsonMapper;
import org.hzero.sso.oauth.token.Auth2AuthenticationToken;

/**
 * <p>
 * oauth过滤器
 * </p>
 *
 * @author minghui.qiu@hand-china.com
 */
public class Auth2AuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	private DomainRepository domainRepository;
	private RememberMeServices rememberMeServices = new NullRememberMeServices();
	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private final RestTemplate restTemplate = new RestTemplate();
	private final JsonMapper jsonMapper = new JsonMapper();

	public Auth2AuthenticationFilter(DomainRepository domainRepository) {
		super("/login/auth2/**");
		this.domainRepository = domainRepository;
		setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler());
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Assert.notNull(domainRepository, "domain repository should not be null.");
	}

	@Override
	protected final void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
	                FilterChain chain, Authentication authResult) throws IOException, ServletException {
		if (logger.isDebugEnabled()) {
			logger.debug("Authentication success. Updating SecurityContextHolder to contain: " + authResult);
		}
		SecurityContextHolder.getContext().setAuthentication(authResult);
		rememberMeServices.loginSuccess(request, response, authResult);
		// Fire event
		if (this.eventPublisher != null) {
			eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(authResult, this.getClass()));
		}
		Domain domain = getSsoDomain(request);
		redirectStrategy.sendRedirect(request, response, domain.getDomainUrl());
	}

	@Override
	public Authentication attemptAuthentication(final HttpServletRequest request, final HttpServletResponse response)
	                throws AuthenticationException {

		Domain domain = getSsoDomain(request);
		Assert.notNull(domain, "Domain is Not Exists");
		String code = request.getParameter("code");
		String url = domain.getSsoServerUrl() + "/oauth/token?grant_type=authorization_code&code=" + code
		                + "&redirect_uri=" + domain.getClientHostUrl();
		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		HttpHeaders headers = new HttpHeaders();
		String authStr = domain.getSsoClientId() + ":" + domain.getSsoClientPwd();
		String authorization = "Basic " + new String(Base64.encodeBase64(authStr.getBytes(StandardCharsets.UTF_8)),
		                StandardCharsets.UTF_8);
		headers.add("Authorization", authorization);
		HttpEntity<MultiValueMap<String, String>> formEntity = new HttpEntity<MultiValueMap<String, String>>(params,
		                headers);
		String result = restTemplate.postForObject(url, formEntity, String.class);
		Map<String, Object> authRes = jsonMapper.convertToMap(result);
		String username = null;
		String loginNameField = domain.getLoginNameField();
		if (authRes.get("access_token") != null) {
			HttpHeaders userHeaders = new HttpHeaders();
			userHeaders.add("Authorization", "bearer " + authRes.get("access_token"));
			HttpEntity<MultiValueMap<String, String>> userFormEntity = new HttpEntity<MultiValueMap<String, String>>(
			                params, userHeaders);
			ResponseEntity<String> userResult = restTemplate.exchange(domain.getSsoUserInfo(), HttpMethod.GET,
			                userFormEntity, String.class, new Object());
			Map<String, Object> userInfo = jsonMapper.convertToMap(userResult.getBody());

			if (StringUtils.isNotBlank(loginNameField) && !"null".equalsIgnoreCase(loginNameField) && userInfo.containsKey(loginNameField)) {
				username = userInfo.get(loginNameField).toString();
			}
			if (StringUtils.isBlank(username)) {
				if (userInfo.get("username") != null) {
					username = String.valueOf(userInfo.get("username"));
				} else {
					for (String key : userInfo.keySet()) {
						if (key.contains("name")) {
							username = String.valueOf(userInfo.get(key));
							break;
						}
					}
				}
			}
		}
		Auth2AuthenticationToken authRequest = new Auth2AuthenticationToken(username, domain.getTenantId());
		setDetails(request, authRequest);
		return this.getAuthenticationManager().authenticate(authRequest);
	}

	protected Domain getSsoDomain(HttpServletRequest request) {
		String[] urls = request.getRequestURL().toString().split("/");
		List<Domain> list = domainRepository.selectAllDomain();
		if (urls.length > 0) {
			return list.stream().filter(d -> d.getDomainUrl().contains(urls[urls.length - 1])).findFirst()
			                .orElse(new Domain());
		} else {
			return null;
		}
	}

	protected void setDetails(HttpServletRequest request, Auth2AuthenticationToken authRequest) {
		authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
	}
}