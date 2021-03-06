package org.hzero.message.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.service.Tag;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * <p>
 *
 * </p>
 *
 * @author qingsheng.chen 2018/7/30 ζζδΈ 14:26
 */
@Configuration
public class MessageSwaggerApiConfig {
    public static final String EMAIL_FILTER = "Email Filter";
    public static final String EMAIL_FILTER_SITE = "Email Filter(Site Level)";
    public static final String EMAIL_SERVER = "Email Server";
    public static final String EMAIL_SERVER_SITE = "Email Server(Site Level)";
    public static final String MESSAGE = "Message";
    public static final String MESSAGE_SITE = "Message(Site Level)";
    public static final String MESSAGE_EVENT = "Message event";
    public static final String MESSAGE_EVENT_SITE = "Message event(Site Level)";
    public static final String MESSAGE_GENERATOR = "Message Generator";
    public static final String MESSAGE_GENERATOR_SITE = "Message Generator(Site Level)";
    public static final String MESSAGE_RECEIVER = "Message Receiver";
    public static final String MESSAGE_RECEIVER_SITE = "Message Receiver(Site Level)";
    public static final String MESSAGE_TEMPLATE = "Message Template";
    public static final String MESSAGE_TEMPLATE_SITE = "Message Template(Site Level)";
    public static final String RECEIVE_CONFIG = "Receive Config";
    public static final String RECEIVE_CONFIG_SITE = "Receive Config(Site Level)";
    public static final String RECEIVER_TYPE = "Receiver Type";
    public static final String RECEIVER_TYPE_SITE = "Receiver Type(Site Level)";
    public static final String REL_MESSAGE = "Relation Send Message";
    public static final String REL_MESSAGE_SITE = "Relation Send Message(Site Level)";
    public static final String EMAIL_MESSAGE = "Send Email Message";
    public static final String EMAIL_MESSAGE_SITE = "Send Email Message(Site Level)";
    public static final String SMS_MESSAGE = "Send SMS Message";
    public static final String SMS_MESSAGE_SITE = "Send SMS Message(Site Level)";
    public static final String WEB_MESSAGE = "Send Web Message";
    public static final String WEB_MESSAGE_SITE = "Send Web Message(Site Level)";
    public static final String SMS_SERVER = "SMS Server";
    public static final String SMS_SERVER_SITE = "SMS Server(Site Level)";
    public static final String TEMPLATE_ARG = "Template Arg";
    public static final String TEMPLATE_ARG_SITE = "Template Arg(Site Level)";
    public static final String TEMPLATE_SERVER = "Template Maintain";
    public static final String TEMPLATE_SERVER_SITE = "Template Maintain(Site Level)";
    public static final String USER_MESSAGE = "User Message";
    public static final String USER_RECEIVE_CONFIG = "User Receive Config";
    public static final String NOTICE = "Notice";
    public static final String NOTICE_V2 = "Notice V2";
    public static final String NOTICE_SITE = "Notice(Site Level)";
    public static final String NOTICE_PUBLISH = "Notice_Publish";
    public static final String NOTICE_PUBLISH_SITE = "Notice_Publish(Site Level)";
    public static final String NOTICE_RECEIVER = "Notice_Receiver";
    public static final String NOTICE_RECEIVER_SITE = "Notice_Receiver(Site Level)";
    public static final String WE_CHAT_OFFICIAL = "WeChat Official";
    public static final String WE_CHAT_OFFICIAL_SITE = "WeChat Official(Site Level)";
    public static final String WE_CHAT_MESSAGE = "Send WeChat Message";
    public static final String WE_CHAT_MESSAGE_SITE = "Send WeChat Message(Site Level)";
    public static final String WE_CHAT_ENTERPRISE = "WeChat EnterPrise";
    public static final String WE_CHAT_ENTERPRISE_SITE = "WeChat EnterPrise(Site Level)";
    public static final String DING_TALK_SERVER = "DingTalk Server";
    public static final String DING_TALK_SERVER_SITE = "DingTalk Server(Site Level)";
    public static final String DING_TALK_MESSAGE = "Send DingTalk Message";
    public static final String DING_TALK_MESSAGE_SITE = "Send DingTalk Message(Site Level)";
    public static final String CALL_SERVER = "Call Server";
    public static final String CALL_SERVER_SITE = "Call Server(Site Level)";
    public static final String CALL_MESSAGE = "SMS Server";
    public static final String CALL_MESSAGE_SITE = "SMS Server(Site Level)";
    public static final String WEBHOOK_MESSAGE = "WebHook Server";
    public static final String WEBHOOK_MESSAGE_SITE = "WebHook Server(Site Level)";
    public static final String SEND_WEBHOOK = "Send WebHook Message";
    public static final String SEND_WEBHOOK_SITE = "Send WebHook Message(Site Level)";
    public static final String TEMPLATE_SERVER_WH = "WebHook Template Server Config";
    public static final String TEMPLATE_SERVER_WH_SITE = "WebHook Template Server Config(Site Level)";

    @Autowired
    public MessageSwaggerApiConfig(Docket docket) {
        docket.tags(
                new Tag(NOTICE, "ε¬ε"),
                new Tag(NOTICE_V2, "ε¬ε(V2)"),
                new Tag(NOTICE_SITE, "ε¬ε(εΉ³ε°ηΊ§)"),
                new Tag(NOTICE_PUBLISH, "ε¬εεεΈθ?°ε½"),
                new Tag(NOTICE_RECEIVER, "ε¬εζ₯ζΆθ?°ε½"),
                new Tag(EMAIL_FILTER, "ι?η?±θ΄¦ζ·ι»η½εε"),
                new Tag(EMAIL_FILTER_SITE, "ι?η?±θ΄¦ζ·ι»η½εε(εΉ³ε°ηΊ§)"),
                new Tag(EMAIL_SERVER, "ι?η?±ζε‘η?‘η"),
                new Tag(EMAIL_SERVER_SITE, "ι?η?±ζε‘η?‘η(εΉ³ε°ηΊ§)"),
                new Tag(MESSAGE, "ζΆζ―δΏ‘ζ―η?‘η"),
                new Tag(MESSAGE_SITE, "ζΆζ―δΏ‘ζ―η?‘η(εΉ³ε°ηΊ§)"),
                new Tag(MESSAGE_EVENT, "ζΆζ―δΊδ»Ά"),
                new Tag(MESSAGE_EVENT_SITE, "ζΆζ―δΊδ»Ά(εΉ³ε°ηΊ§)"),
                new Tag(MESSAGE_GENERATOR, "ζΆζ―εε?Ήηζ"),
                new Tag(MESSAGE_GENERATOR_SITE, "ζΆζ―εε?Ήηζ(εΉ³ε°ηΊ§)"),
                new Tag(MESSAGE_RECEIVER, "ζΆζ―ζ₯ζΆδΊΊθ·ε"),
                new Tag(MESSAGE_RECEIVER_SITE, "ζΆζ―ζ₯ζΆδΊΊθ·ε(εΉ³ε°ηΊ§)"),
                new Tag(MESSAGE_TEMPLATE, "ζΆζ―ζ¨‘ζΏη?‘η"),
                new Tag(MESSAGE_TEMPLATE_SITE, "ζΆζ―ζ¨‘ζΏη?‘η(εΉ³ε°ηΊ§)"),
                new Tag(RECEIVE_CONFIG, "ζ₯ζΆιη½?"),
                new Tag(RECEIVE_CONFIG_SITE, "ζ₯ζΆιη½?(εΉ³ε°ηΊ§)"),
                new Tag(RECEIVER_TYPE, "ζ₯ζΆθη±»ε"),
                new Tag(RECEIVER_TYPE_SITE, "ζ₯ζΆθη±»ε(εΉ³ε°ηΊ§)"),
                new Tag(REL_MESSAGE, "ε³θειζΆζ―"),
                new Tag(REL_MESSAGE_SITE, "ε³θειζΆζ―(εΉ³ε°ηΊ§)"),
                new Tag(EMAIL_MESSAGE, "ι?η?±ζΆζ―ει"),
                new Tag(EMAIL_MESSAGE_SITE, "ι?η?±ζΆζ―ει(εΉ³ε°ηΊ§)"),
                new Tag(SMS_MESSAGE, "η­δΏ‘ζΆζ―ει"),
                new Tag(SMS_MESSAGE_SITE, "η­δΏ‘ζΆζ―ει(εΉ³ε°ηΊ§)"),
                new Tag(WEB_MESSAGE, "η«εζΆζ―ει"),
                new Tag(WEB_MESSAGE_SITE, "η«εζΆζ―ει(εΉ³ε°ηΊ§)"),
                new Tag(SMS_SERVER, "η­δΏ‘ζε‘η?‘η"),
                new Tag(SMS_SERVER_SITE, "η­δΏ‘ζε‘η?‘η(εΉ³ε°ηΊ§)"),
                new Tag(TEMPLATE_ARG, "ζΆζ―ζ¨‘ζΏεζ°η?‘η"),
                new Tag(TEMPLATE_ARG_SITE, "ζΆζ―ζ¨‘ζΏεζ°η?‘η(εΉ³ε°ηΊ§)"),
                new Tag(TEMPLATE_SERVER, "ι?η?±θ΄¦ζ·δΈζ¨‘ζΏε³η³»η»΄ζ€"),
                new Tag(TEMPLATE_SERVER_SITE, "ι?η?±θ΄¦ζ·δΈζ¨‘ζΏε³η³»η»΄ζ€(εΉ³ε°ηΊ§)"),
                new Tag(USER_MESSAGE, "η¨ζ·ζΆζ―"),
                new Tag(USER_RECEIVE_CONFIG, "η¨ζ·ζ₯ζΆιη½?"),
                new Tag(NOTICE_PUBLISH, "ε¬εεεΈθ?°ε½"),
                new Tag(NOTICE_PUBLISH_SITE, "ε¬εεεΈθ?°ε½(εΉ³ε°ηΊ§)"),
                new Tag(NOTICE_RECEIVER, "ε¬εζ₯ζΆθ?°ε½"),
                new Tag(NOTICE_RECEIVER_SITE, "ε¬εζ₯ζΆθ?°ε½(εΉ³ε°ηΊ§)"),
                new Tag(WE_CHAT_OFFICIAL, "εΎ?δΏ‘ε¬δΌε·ιη½?"),
                new Tag(WE_CHAT_OFFICIAL_SITE, "εΎ?δΏ‘ε¬δΌε·ιη½?(εΉ³ε°ηΊ§)"),
                new Tag(WE_CHAT_MESSAGE, "εΎ?δΏ‘ζΆζ―ει"),
                new Tag(WE_CHAT_MESSAGE_SITE, "εΎ?δΏ‘ζΆζ―ει(εΉ³ε°ηΊ§)"),
                new Tag(WE_CHAT_ENTERPRISE, "δΌδΈεΎ?δΏ‘ιη½?η»΄ζ€"),
                new Tag(WE_CHAT_ENTERPRISE_SITE, "δΌδΈεΎ?δΏ‘ιη½?η»΄ζ€(εΉ³ε°ηΊ§)"),
                new Tag(DING_TALK_SERVER, "ιιιη½?"),
                new Tag(DING_TALK_SERVER_SITE, "ιιιη½?(εΉ³ε°ηΊ§)"),
                new Tag(DING_TALK_MESSAGE, "ιιζΆζ―ει"),
                new Tag(DING_TALK_MESSAGE_SITE, "ιιζΆζ―ει(εΉ³ε°ηΊ§)"),
                new Tag(CALL_SERVER, "θ―­ι³ζε‘ιη½?"),
                new Tag(CALL_SERVER_SITE, "θ―­ι³ζε‘ιη½?(εΉ³ε°ηΊ§)"),
                new Tag(CALL_MESSAGE, "θ―­ι³ζΆζ―"),
                new Tag(CALL_MESSAGE_SITE, "θ―­ι³ζΆζ―(εΉ³ε°ηΊ§)"),
                new Tag(WEBHOOK_MESSAGE, "WEBHOOK ζΆζ―"),
                new Tag(WEBHOOK_MESSAGE_SITE, "WEBHOOK ζΆζ―(εΉ³ε°ηΊ§)"),
                new Tag(TEMPLATE_SERVER_WH, "WEBHOOK ζΆζ―ειιη½?"),
                new Tag(TEMPLATE_SERVER_WH_SITE, "WEBHOOK ζΆζ―ειιη½?(εΉ³ε°ηΊ§)")
        );
    }
}
