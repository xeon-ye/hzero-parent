package org.hzero.admin.config;

import org.hzero.core.message.MessageAccessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 初始化配置
 *
 * @author bojiangzhou 2019/07/23
 */
@Component
public class ConfigInitializeConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // 加入消息文件
        MessageAccessor.addBasenames("classpath:messages/messages_hadm");
    }
}
