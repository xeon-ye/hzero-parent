package org.hzero.sso.azure.filter;

import java.net.URI;
import java.util.List;
import java.util.concurrent.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.aad.msal4j.AuthorizationCodeParameters;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.util.Assert;

import org.hzero.sso.azure.token.AzureAuthenticationToken;
import org.hzero.sso.core.domain.entity.Domain;
import org.hzero.sso.core.domain.repository.DomainRepository;
import org.hzero.sso.core.exception.Auth2RequestException;

/**
 * <p>
 * oauth过滤器
 * </p>
 *
 * @author minghui.qiu@hand-china.com
 */
public class AzureAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(AzureAuthenticationFilter.class);

	private DomainRepository domainRepository;

	private RememberMeServices rememberMeServices = new NullRememberMeServices();
	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private final ExecutorService executorService;

	private int executeCorePoolSize = 20;
	private int executeMaximumPoolSize = 200;

	public AzureAuthenticationFilter(DomainRepository domainRepository) {
		super("/login/azure/**");

		this.domainRepository = domainRepository;

		setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler());

		// auth2 请求线程池
		executorService = new ThreadPoolExecutor(executeCorePoolSize, executeMaximumPoolSize, 30, TimeUnit.MINUTES,
		                new ArrayBlockingQueue<>(512), (r) -> new Thread("auth2-request"));
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Assert.notNull(domainRepository, "domain repository should not be null.");
	}

	@Override
	public Authentication attemptAuthentication(final HttpServletRequest request, final HttpServletResponse response)
	                throws AuthenticationException {
		Domain domain = getSsoDomain(request);
		if (domain == null) {
			throw new Auth2RequestException("hoth.warn.auth2.getUserError");
		}
		String code = request.getParameter("code");
		String currentUri = domain.getClientHostUrl();
		String authority = domain.getSsoServerUrl();

		IAuthenticationResult result = null;
		ConfidentialClientApplication app;
		try {
			app = ConfidentialClientApplication.builder(domain.getSsoClientId(), ClientCredentialFactory.createFromSecret(domain.getSsoClientPwd())).
					authority(authority).
					build();

			AuthorizationCodeParameters parameters = AuthorizationCodeParameters.builder(
					code,
					new URI(currentUri)).
					build();
			Future<IAuthenticationResult> future = app.acquireToken(parameters);
			result = future.get();

		} catch (Exception e) {
			LOGGER.warn("auth2 request failure, domain is {}", domain, e);
			throw new Auth2RequestException("hoth.warn.auth2.getUserError");
		}

		String username = result.account().username();
		AzureAuthenticationToken authRequest = new AzureAuthenticationToken(username, domain.getTenantId());
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

	protected void setDetails(HttpServletRequest request, AzureAuthenticationToken authRequest) {
		authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
	}

	public void setExecuteCorePoolSize(int executeCorePoolSize) {
		this.executeCorePoolSize = executeCorePoolSize;
	}

	public void setExecuteMaximumPoolSize(int executeMaximumPoolSize) {
		this.executeMaximumPoolSize = executeMaximumPoolSize;
	}
}