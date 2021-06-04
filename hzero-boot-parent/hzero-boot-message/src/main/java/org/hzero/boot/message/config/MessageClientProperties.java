package org.hzero.boot.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 消息客户端配置
 * </p>
 *
 * @author qingsheng.chen 2018/8/7 星期二 17:33
 */
@Component
@ConfigurationProperties(prefix = "hzero.message")
public class MessageClientProperties {
    private Integer messageRedisDatabase = 1;

    private String defaultLang = "zh_CN";

    public Integer getMessageRedisDatabase() {
        return messageRedisDatabase;
    }

    public MessageClientProperties setMessageRedisDatabase(Integer messageRedisDatabase) {
        this.messageRedisDatabase = messageRedisDatabase;
        return this;
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public MessageClientProperties setDefaultLang(String defaultLang) {
        this.defaultLang = defaultLang;
        return this;
    }
}
