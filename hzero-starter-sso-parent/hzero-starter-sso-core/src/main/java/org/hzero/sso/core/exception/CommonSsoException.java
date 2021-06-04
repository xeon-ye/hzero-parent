package org.hzero.sso.core.exception;

import org.springframework.security.core.AuthenticationException;

public class CommonSsoException extends AuthenticationException {

	private static final long serialVersionUID = -490562531370556929L;

	public CommonSsoException(String msg) {
        super(msg);
    }

    public CommonSsoException(String msg, Throwable t) {
        super(msg, t);
    }
}
