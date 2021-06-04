package org.hzero.starter.call.entity;

/**
 * 表单配置的额外参数
 * Created by wushuai on 2021/5/27
 */
public class ExtParam {
    private String accountCode;
    private String userId;
    private String showNum;
    private String playTimes;

    public String getAccountCode() {
        return accountCode;
    }

    public ExtParam setAccountCode(String accountCode) {
        this.accountCode = accountCode;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public ExtParam setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getShowNum() {
        return showNum;
    }

    public ExtParam setShowNum(String showNum) {
        this.showNum = showNum;
        return this;
    }

    public String getPlayTimes() {
        return playTimes;
    }

    public ExtParam setPlayTimes(String playTimes) {
        this.playTimes = playTimes;
        return this;
    }
}
