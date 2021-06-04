package org.hzero.starter.call.service;

import org.hzero.starter.call.entity.CallConfig;
import org.hzero.starter.call.entity.CallMessage;
import org.hzero.starter.call.entity.CallReceiver;

import java.util.List;
import java.util.Map;

/**
 * 语音发送方法
 * Created by wushuai on 2021/5/27
 */
public interface CallService {

    /**
     * 获取服务类型
     *
     * @return 服务类型
     */
    String serverType();

    /**
     * 语音消息发送
     *
     * @param receiverAddressList 接收人地址
     * @param callConfig          配置
     * @param message             消息内容
     * @param args                参数
     */
    void callSend(List<CallReceiver> receiverAddressList, CallConfig callConfig, CallMessage message, Map<String, String> args);
}
