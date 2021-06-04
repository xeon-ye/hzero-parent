package org.hzero.sso.idm.filter;

import java.io.IOException;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.*;
import org.springframework.util.Assert;
import org.hzero.sso.idm.token.IdmAuthenticationToken;
import org.hzero.sso.core.domain.entity.Domain;
import org.hzero.sso.core.domain.repository.DomainRepository;

/**
 * <p>
 * idm过滤器
 * </p>
 *
 * @author minghui.qiu@hand-china.com
 */
public class IdmAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	private DomainRepository domainRepository;
	private RememberMeServices rememberMeServices = new NullRememberMeServices();
	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	public IdmAuthenticationFilter(DomainRepository domainRepository) {
		super("/login/idm/**");
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
		// this.successHandler.onAuthenticationSuccess(request, response,
		// authResult);
	}

	@Override
	public Authentication attemptAuthentication(final HttpServletRequest request, final HttpServletResponse response)
	                throws AuthenticationException {

		Domain domain = getSsoDomain(request);
		Assert.notNull(domain, "Domain is Not Exists");
		//Idm默认从header获取USER_NAME
		String username = request.getHeader("USER_NAME");
		Assert.notNull(username, "IDM USER_NAME Not Exists");
		IdmAuthenticationToken authRequest = new IdmAuthenticationToken(username, domain.getTenantId());
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

	protected void setDetails(HttpServletRequest request, IdmAuthenticationToken authRequest) {
		authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
	}

}