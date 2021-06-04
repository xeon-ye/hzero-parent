package org.hzero.boot.admin.transport;

/**
 * Created by wushuai on 2021/5/21
 */
public class Response<T> {

    private boolean success;

    private T body;

    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
