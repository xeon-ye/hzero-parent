package org.hzero.starter.sms.support;

import com.github.qcloudsms.SmsMultiSender;
import com.github.qcloudsms.SmsMultiSenderResult;
import io.choerodon.core.exception.CommonException;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants;
import org.hzero.starter.sms.entity.SmsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * 腾讯短信发送支持
 * Created by wushuai on 2021/5/27
 */
public class QcloudSmsSupporter {

    private static final Logger logger = LoggerFactory.getLogger(QcloudSmsSupporter.class);

    private QcloudSmsSupporter() {
    }

    /**
     * 腾讯短信
     */
    public static SmsMultiSender multiSender(SmsConfig smsConfig) {
        return new SmsMultiSender(Integer.parseInt(smsConfig.getAccessKey()), smsConfig.getAccessKeySecret());
    }

    /**
     * 腾讯发送短信  群发一次请求最多支持 200 个号码
     */
    public static SmsMultiSenderResult sendSms(SmsMultiSender smsMultiSender, String idd, String[] receiverAddress, String templateId, String[] params, String smsSign) {
        Assert.isTrue(StringUtils.isNotBlank(templateId), BaseConstants.ErrorCode.DATA_INVALID);
        try {
            return smsMultiSender.sendWithParam(idd, receiverAddress, Integer.parseInt(templateId), params, smsSign, "", "");
        } catch (Exception e) {
            logger.error("Unable to send sms : {0}", e.fillInStackTrace());
            throw new CommonException(e);
        }
    }
}
