package org.hzero.starter.integrate.constant;

/**
 * Hr组织信息同步授权类型
 * Created by wushuai on 2021/5/26
 */
public class HrSyncAuthType {
    private HrSyncAuthType() {
    }

    /**
     * 用户凭证授权
     */
    public static final String SELF = "SELF";
    /**
     * 第三方授权
     */
    public static final String THIRD = "THIRD";
}
