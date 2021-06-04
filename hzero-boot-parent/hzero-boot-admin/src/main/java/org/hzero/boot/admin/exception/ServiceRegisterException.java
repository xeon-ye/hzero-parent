package org.hzero.boot.admin.exception;

/**
 * Created by wushuai on 2021/5/21
 */
public class ServiceRegisterException extends RuntimeException {
    public ServiceRegisterException() {
    }

    public ServiceRegisterException(String message) {
        super(message);
    }

    public ServiceRegisterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceRegisterException(Throwable cause) {
        super(cause);
    }

    public ServiceRegisterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
