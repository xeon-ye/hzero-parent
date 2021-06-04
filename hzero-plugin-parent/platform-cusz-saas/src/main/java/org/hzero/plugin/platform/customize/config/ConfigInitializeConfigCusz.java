package org.hzero.plugin.platform.customize.config;

import org.hzero.core.message.MessageAccessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 初始化配置
 *
 * @author xiangyu.qi01@hand-china.com
 */
@Component
public class ConfigInitializeConfigCusz implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // 加入消息文件
        MessageAccessor.addBasenames("classpath:messages/messages_hpfm_cusz");
    }
}
