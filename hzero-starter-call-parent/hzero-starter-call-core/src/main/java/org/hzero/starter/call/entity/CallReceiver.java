package org.hzero.starter.call.entity;

/**
 * 语音消息接收人
 * Created by wushuai on 2021/5/27
 */
public class CallReceiver {
    /**
     * 语音消息接收者：电话号码
     */
    private String phone;
    /**
     * 语音消息接收者：国际冠码
     */
    private String idd = "+86";

    public String getPhone() {
        return phone;
    }

    public CallReceiver setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getIdd() {
        return idd;
    }

    public CallReceiver setIdd(String idd) {
        this.idd = idd;
        return this;
    }
}
