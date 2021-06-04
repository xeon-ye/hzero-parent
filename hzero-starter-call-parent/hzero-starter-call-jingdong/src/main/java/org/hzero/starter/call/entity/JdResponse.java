package org.hzero.starter.call.entity;

/**
 * Created by wushuai on 2021/5/27
 */
public class JdResponse {
    private String timestamp;
    private String messageId;

    public String getTimestamp() {
        return timestamp;
    }

    public JdResponse setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String getMessageId() {
        return messageId;
    }

    public JdResponse setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }
}
