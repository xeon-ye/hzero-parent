package org.hzero.boot.admin.exception;

/**
 * Created by wushuai on 2021/5/21
 */
public class ServiceUnregisterException extends RuntimeException{
    public ServiceUnregisterException() {
    }

    public ServiceUnregisterException(String message) {
        super(message);
    }

    public ServiceUnregisterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceUnregisterException(Throwable cause) {
        super(cause);
    }

    public ServiceUnregisterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
