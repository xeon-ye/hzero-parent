package org.hzero.boot.oauth.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import org.hzero.core.message.MessageAccessor;

@Component
public class PasswordInitializeConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // 加入消息文件
        MessageAccessor.addBasenames("classpath:messages/messages_pwd");
    }
}
