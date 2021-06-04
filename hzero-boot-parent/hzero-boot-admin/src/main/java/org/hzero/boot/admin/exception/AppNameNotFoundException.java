package org.hzero.boot.admin.exception;

/**
 * Created by wushuai on 2021/5/21
 */
public class AppNameNotFoundException extends RuntimeException{
    public AppNameNotFoundException() {
    }

    public AppNameNotFoundException(String message) {
        super(message);
    }

    public AppNameNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppNameNotFoundException(Throwable cause) {
        super(cause);
    }

    public AppNameNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
