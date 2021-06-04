package org.hzero.sso.core.exception;

import org.springframework.security.authentication.InternalAuthenticationServiceException;

/**
 * 账户不存在异常
 *
 * @author bojiangzhou 2019/02/25
 */
public class AccountNotExistsException extends InternalAuthenticationServiceException {

	private static final long serialVersionUID = 7152748074391013712L;

	public AccountNotExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountNotExistsException(String message) {
        super(message);
    }

}
