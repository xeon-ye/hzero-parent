package org.hzero.sso.core.config;

import org.springframework.beans.factory.InitializingBean;

import org.hzero.core.message.MessageAccessor;

public class SsoInitializeConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // 加入消息文件
        MessageAccessor.addBasenames("classpath:messages/message_sso");
    }
}
