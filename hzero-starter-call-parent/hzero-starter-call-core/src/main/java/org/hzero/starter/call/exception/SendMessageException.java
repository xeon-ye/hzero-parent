package org.hzero.starter.call.exception;

/**
 * Created by wushuai on 2021/5/27
 */
public class SendMessageException extends RuntimeException {
    public SendMessageException(String message) {
        super(message);
    }
}
