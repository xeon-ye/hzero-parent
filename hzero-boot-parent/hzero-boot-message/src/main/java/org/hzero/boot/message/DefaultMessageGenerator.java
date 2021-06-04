package org.hzero.boot.message;

import java.util.HashMap;
import java.util.Map;

import org.hzero.boot.message.config.MessageClientProperties;
import org.hzero.boot.message.entity.Message;
import org.hzero.boot.message.entity.MessageTemplate;
import org.hzero.boot.message.feign.MessageRemoteService;
import org.hzero.boot.message.service.MessageGenerator;
import org.hzero.boot.message.util.VelocityUtils;
import org.hzero.common.HZeroService;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.redis.RedisHelper;
import org.hzero.core.util.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author qingsheng.chen@hand-china.com 2019-06-20 15:04
 */
public class DefaultMessageGenerator implements MessageGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageGenerator.class);
    private static final String MESSAGE_TEMPLATE_PREFIX = HZeroService.Message.CODE + ":message:template:";

    private final MessageClientProperties messageClientProperties;
    private final MessageRemoteService messageRemoteService;
    private final RedisHelper redisHelper;

    public DefaultMessageGenerator(DefaultMessageGenerator that) {
        this.messageClientProperties = that.messageClientProperties;
        this.messageRemoteService = that.messageRemoteService;
        this.redisHelper = that.redisHelper;
    }

    public DefaultMessageGenerator(MessageClientProperties messageClientProperties,
                                   MessageRemoteService messageRemoteService,
                                   RedisHelper redisHelper) {
        this.messageClientProperties = messageClientProperties;
        this.messageRemoteService = messageRemoteService;
        this.redisHelper = redisHelper;
    }

    @Override
    public Message generateMessage(long tenantId, String templateCode, String serverTypeCode, Map<String, String> args, boolean sqlEnable, String lang) {
        // 获取消息模板
        lang = getLang(lang);
        MessageTemplate messageTemplate = getMessageTemplate(tenantId, templateCode, lang);
        if (messageTemplate == null || BaseConstants.Flag.NO.equals(messageTemplate.getEnabledFlag())) {
            return null;
        }
        // 拼接消息内容
        Map<String, Object> objectArgs = new HashMap<>(args.size());
        objectArgs.putAll(args);
        return generateMessage(generateMessage(messageTemplate, templateCode, lang, tenantId), objectArgs);
    }

    @Override
    public Message generateMessageObjectArgs(long tenantId, String templateCode, String serverTypeCode, Map<String, Object> objectArgs, boolean sqlEnable, String lang) {
        // 获取消息模板
        lang = getLang(lang);
        MessageTemplate messageTemplate = getMessageTemplate(tenantId, templateCode, lang);
        if (messageTemplate == null || BaseConstants.Flag.NO.equals(messageTemplate.getEnabledFlag())) {
            return null;
        }
        // 拼接消息内容
        return generateMessage(generateMessage(messageTemplate, templateCode, lang, tenantId), objectArgs);
    }

    Message generateMessage(Message message, Map<String, Object> args) {
        // 从消息内容中替换获取的参数
        if (!CollectionUtils.isEmpty(args)) {
            message.setContent(VelocityUtils.parseObject(message.getContent(), args)).setSubject(VelocityUtils.parseObject(message.getSubject(), args));
        }
        return message;
    }

    Message generateMessage(MessageTemplate messageTemplate, String templateCode, String lang, long tenantId) {
        return new Message().setMessageTypeCode(messageTemplate.getTemplateTypeCode())
                .setSubject(messageTemplate.getTemplateTitle())
                .setContent(messageTemplate.getTemplateContent())
                .setTemplateCode(templateCode)
                .setLang(lang)
                .setTenantId(tenantId)
                .setExternalCode(messageTemplate.getExternalCode())
                .setServerTypeCode(messageTemplate.getServerTypeCode());
    }

    MessageTemplate getMessageTemplate(long tenantId, String templateCode, String lang) {
        // 如果启用动态Redis，切换设置的Redis
        redisHelper.setCurrentDatabase(messageClientProperties.getMessageRedisDatabase());
        // 先尝试取缓存
        MessageTemplate messageTemplate = redisHelper.strGet(getRedisKey(tenantId, templateCode, lang), MessageTemplate.class);
        // 如果启用动态Redis，清除设置的Redis
        redisHelper.clearCurrentDatabase();
        if (messageTemplate != null) {
            return messageTemplate;
        }
        return ResponseUtils.getResponse(messageRemoteService.queryMessageTemplate(tenantId, templateCode, lang), MessageTemplate.class);
    }

    String getLang(String lang) {
        if (!StringUtils.hasText(lang)) {
            return messageClientProperties.getDefaultLang();
        }
        return lang;
    }

    public static String getRedisKey(long tenantId, String templateCode, String lang) {
        return MESSAGE_TEMPLATE_PREFIX + tenantId +
                "." + templateCode +
                "." + lang;
    }
}
