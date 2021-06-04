package org.hzero.boot.message;

import java.util.*;
import java.util.stream.Collectors;

import org.hzero.boot.message.config.MessageClientProperties;
import org.hzero.boot.message.constant.HmsgBootConstant;
import org.hzero.boot.message.constant.WebSocketConstant;
import org.hzero.boot.message.dto.NoticeDTO;
import org.hzero.boot.message.dto.OnLineUserDTO;
import org.hzero.boot.message.entity.*;
import org.hzero.boot.message.feign.MessageRemoteService;
import org.hzero.boot.message.feign.PlatformRemoteService;
import org.hzero.boot.message.redis.PublishNoticeRedis;
import org.hzero.boot.message.service.*;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.util.ResponseUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.choerodon.core.exception.CommonException;

/**
 * <p>
 * 消息客户端
 * 提供消息生成，接收人获取，消息发送等功能
 * </p>
 *
 * @author qingsheng.chen 2018/8/6 星期一 20:09
 */
public class MessageClient implements MessageGenerator, MessageReceiver, WebMessageSender, EmailSender, SmsSender, RelSender, WeChatMessageSender, DingTalkMessageSender, WebHookMessageSender {
    public static final String ERROR_MESSAGE_EMPTY_RECEIVER = "error.message.empty_receiver";
    private static Object sqlSessionFactory;
    private final MessageRemoteService messageRemoteService;
    private final PlatformRemoteService platformRemoteService;
    private final MessageAsyncService messageAsyncService;
    private final MessageClientProperties messageClientProperties;
    private MessageGenerator messageGenerator;

    private boolean async;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    private String serviceName;

    public MessageClient(MessageRemoteService messageRemoteService,
                         PlatformRemoteService platformRemoteService,
                         MessageAsyncService messageAsyncService,
                         MessageClientProperties messageClientProperties,
                         MessageGenerator messageGenerator,
                         RedisTemplate<String, String> redisTemplate,
                         ObjectMapper objectMapper) {
        this.messageRemoteService = messageRemoteService;
        this.platformRemoteService = platformRemoteService;
        this.messageAsyncService = messageAsyncService;
        this.messageClientProperties = messageClientProperties;
        this.messageGenerator = messageGenerator;
        this.async = false;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public static void setSqlSessionFactory(Object bean) {
        sqlSessionFactory = bean;
    }

    @Override
    public Message generateMessage(long tenantId, String templateCode, String serverTypeCode, Map<String, String> args, boolean sqlEnable, String lang) {
        return getMessageGenerator().generateMessage(tenantId, templateCode, serverTypeCode, args, sqlEnable, lang);
    }

    @Override
    public Message generateMessageObjectArgs(long tenantId, String templateCode, String serverTypeCode, Map<String, Object> objectArgs, boolean sqlEnable, String lang) {
        return getMessageGenerator().generateMessageObjectArgs(tenantId, templateCode, serverTypeCode, objectArgs, sqlEnable, lang);
    }

    private MessageGenerator getMessageGenerator() {
        if (messageGenerator instanceof SqlMessageGenerator) {
            return messageGenerator;
        }
        if (messageGenerator instanceof DefaultMessageGenerator && sqlSessionFactory != null) {
            messageGenerator = new SqlMessageGenerator((DefaultMessageGenerator) messageGenerator, sqlSessionFactory);
        }
        return messageGenerator;
    }

    @Override
    public List<Receiver> receiver(long tenantId, String receiverTypeCode, Map<String, String> args) {
        if (args == null) {
            args = new HashMap<>(1);
        }
        return ResponseUtils.getResponse(messageRemoteService.queryReceiver(tenantId, receiverTypeCode, args), new TypeReference<List<Receiver>>() {
        });
    }

    @Override
    public List<Receiver> openReceiver(long tenantId, String messageType, String receiverTypeCode, Map<String, String> args) {
        String thirdPlatformType;
        switch (messageType) {
            case HmsgBootConstant.MessageType.WC_E:
                thirdPlatformType = HmsgBootConstant.ThirdPlatformType.WX;
                break;
            case HmsgBootConstant.MessageType.DT:
                thirdPlatformType = HmsgBootConstant.ThirdPlatformType.DD;
                break;
            default:
                throw new CommonException("error.receiver.get");
        }
        if (args == null) {
            args = new HashMap<>(1);
        }
        return ResponseUtils.getResponse(messageRemoteService.queryOpenReceiver(tenantId, thirdPlatformType, receiverTypeCode, args), new TypeReference<List<Receiver>>() {
        });
    }

    // ### Web Message ###

    @Override
    public Message sendWebMessage(long tenantId, String messageTemplateCode, String receiverGroupCode, Map<String, String> args) {
        return sendWebMessage(tenantId, messageTemplateCode, messageClientProperties.getDefaultLang(), receiverGroupCode, args);
    }

    @Override
    public Message sendWebMessage(long tenantId, String messageTemplateCode, List<Receiver> receiverList, Map<String, String> args) {
        return sendWebMessage(tenantId, messageTemplateCode, messageClientProperties.getDefaultLang(), receiverList, args);
    }

    @Override
    public Message sendWebMessage(long tenantId, String messageTemplateCode, String lang, String receiverGroupCode, Map<String, String> args) {
        List<Receiver> receiver = receiver(tenantId, receiverGroupCode, args);
        Assert.notEmpty(receiver, ERROR_MESSAGE_EMPTY_RECEIVER);
        return sendWebMessage(tenantId, messageTemplateCode, messageClientProperties.getDefaultLang(), receiver, args);
    }

    @Override
    public Message sendWebMessage(long tenantId, String messageTemplateCode, String lang, List<Receiver> receiverList, Map<String, String> args) {
        MessageSender messageSender = new MessageSender()
                .setTenantId(tenantId)
                .setMessageCode(messageTemplateCode)
                .setLang(lang)
                .setReceiverAddressList(receiverList)
                .setArgs(args)
                .setMessage(generateMessage(tenantId, messageTemplateCode, HmsgBootConstant.MessageType.WEB, args, true, lang));
        if (async) {
            messageAsyncService.sendWebMessage(messageSender);
            return null;
        } else {
            return responseEntityHandler(messageRemoteService.sendWebMessage(tenantId, messageSender));
        }
    }

    // ### Email ###

    @Override
    public Message sendEmail(long tenantId, String serverCode, String messageTemplateCode, String receiverGroupCode, Map<String, String> args, Attachment... attachments) {
        return sendEmail(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), receiverGroupCode, args, attachments);
    }

    @Override
    public Message sendEmail(long tenantId, String serverCode, String messageTemplateCode, String receiverGroupCode, Map<String, String> args, List<String> ccList, List<String> bccList, Attachment... attachments) {
        return sendEmail(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), receiverGroupCode, args, ccList, bccList, attachments);
    }

    @Override
    public Message sendEmail(long tenantId, String serverCode, String messageTemplateCode, List<Receiver> receiverList, Map<String, String> args, Attachment... attachments) {
        return sendEmail(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), receiverList, args, attachments);
    }

    @Override
    public Message sendEmail(long tenantId, String serverCode, String messageTemplateCode, List<Receiver> receiverList, Map<String, String> args, List<String> ccList, List<String> bccList, Attachment... attachments) {
        return sendEmail(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), receiverList, args, ccList, bccList, attachments);
    }

    @Override
    public Message sendEmail(long tenantId, String serverCode, String messageTemplateCode, String lang, String receiverGroupCode, Map<String, String> args, Attachment... attachments) {
        List<Receiver> receiver = receiver(tenantId, receiverGroupCode, args);
        return sendEmail(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), receiver, args, attachments);
    }

    @Override
    public Message sendEmail(long tenantId, String serverCode, String messageTemplateCode, String lang, String receiverGroupCode, Map<String, String> args, List<String> ccList, List<String> bccList, Attachment... attachments) {
        List<Receiver> receiver = receiver(tenantId, receiverGroupCode, args);
        return sendEmail(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), receiver, args, ccList, bccList, attachments);
    }

    @Override
    public Message sendEmail(long tenantId, String serverCode, String messageTemplateCode, String lang, List<Receiver> receiverList, Map<String, String> args, Attachment... attachments) {
        if (attachments == null) {
            attachments = new Attachment[0];
        }
        Assert.notEmpty(receiverList, ERROR_MESSAGE_EMPTY_RECEIVER);
        MessageSender messageSender = new MessageSender()
                .setTenantId(tenantId)
                .setMessageCode(messageTemplateCode)
                .setLang(lang)
                .setServerCode(serverCode)
                .setReceiverAddressList(receiverList)
                .setArgs(args)
                .setMessage(generateMessage(tenantId, messageTemplateCode, HmsgBootConstant.MessageType.EMAIL, args, true, lang));
        if (attachments.length > 0) {
            messageSender.setAttachmentList(Arrays.asList(attachments));
        }
        if (async) {
            messageAsyncService.sendEmail(messageSender);
            return null;
        } else {
            return responseEntityHandler(messageRemoteService.sendEmail(tenantId, messageSender));
        }
    }

    @Override
    public Message sendEmail(long tenantId, String serverCode, String messageTemplateCode, String lang, List<Receiver> receiverList, Map<String, String> args, List<String> ccList, List<String> bccList, Attachment... attachments) {
        if (attachments == null) {
            attachments = new Attachment[0];
        }
        Assert.notEmpty(receiverList, ERROR_MESSAGE_EMPTY_RECEIVER);
        MessageSender messageSender = new MessageSender()
                .setTenantId(tenantId)
                .setMessageCode(messageTemplateCode)
                .setLang(lang)
                .setServerCode(serverCode)
                .setReceiverAddressList(receiverList)
                .setArgs(args)
                .setCcList(ccList)
                .setBccList(bccList)
                .setMessage(generateMessage(tenantId, messageTemplateCode, HmsgBootConstant.MessageType.EMAIL, args, true, lang));
        if (attachments.length > 0) {
            messageSender.setAttachmentList(Arrays.asList(attachments));
        }
        if (async) {
            messageAsyncService.sendEmail(messageSender);
            return null;
        } else {
            return responseEntityHandler(messageRemoteService.sendEmail(tenantId, messageSender));
        }
    }

    @Override
    public Message sendCustomEmail(Long tenantId, String serverCode, String subject, String content, List<Receiver> receiverList, List<String> ccList, List<String> bccList, Attachment... attachments) {
        if (attachments == null) {
            attachments = new Attachment[0];
        }
        Assert.notEmpty(receiverList, ERROR_MESSAGE_EMPTY_RECEIVER);
        Assert.notNull(tenantId, BaseConstants.ErrorCode.DATA_INVALID);
        Message message = new Message().setServerCode(serverCode)
                .setMessageTypeCode("EMAIL")
                .setTemplateCode("CUSTOM")
                .setLang("zh_CN")
                .setTenantId(tenantId)
                .setSubject(subject)
                .setContent(content);
        MessageSender messageSender = new MessageSender()
                .setTenantId(tenantId)
                .setMessageCode("CUSTOM")
                .setServerCode(serverCode)
                .setReceiverAddressList(receiverList)
                .setCcList(ccList)
                .setBccList(bccList)
                .setMessage(message);
        if (attachments.length > 0) {
            messageSender.setAttachmentList(Arrays.asList(attachments));
        }
        if (async) {
            messageAsyncService.sendEmail(messageSender);
            return null;
        } else {
            return responseEntityHandler(messageRemoteService.sendEmail(tenantId, messageSender));
        }
    }

    // ### SMS ###

    @Override
    public Message sendSms(long tenantId, String serverCode, String messageTemplateCode, String receiverGroupCode, Map<String, String> args) {
        return sendSms(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), receiverGroupCode, args);
    }

    @Override
    public Message sendSms(long tenantId, String serverCode, String messageTemplateCode, List<Receiver> receiverList, Map<String, String> args) {
        return sendSms(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), receiverList, args);
    }

    @Override
    public Message sendSms(long tenantId, String serverCode, String messageTemplateCode, String lang, String receiverGroupCode, Map<String, String> args) {
        List<Receiver> receiver = receiver(tenantId, receiverGroupCode, args);
        return sendSms(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), receiver, args);
    }

    @Override
    public Message sendSms(long tenantId, String serverCode, String messageTemplateCode, String lang, List<Receiver> receiverList, Map<String, String> args) {
        Assert.notEmpty(receiverList, ERROR_MESSAGE_EMPTY_RECEIVER);
        MessageSender messageSender = new MessageSender()
                .setTenantId(tenantId)
                .setMessageCode(messageTemplateCode)
                .setLang(lang)
                .setServerCode(serverCode)
                .setReceiverAddressList(receiverList)
                .setArgs(args)
                .setMessage(generateMessage(tenantId, messageTemplateCode, HmsgBootConstant.MessageType.SMS, args, true, lang));
        if (async) {
            messageAsyncService.sendSms(messageSender);
            return null;
        } else {
            return responseEntityHandler(messageRemoteService.sendSms(tenantId, messageSender));
        }
    }

    // ### REL ###

    @Override
    public void sendMessage(long tenantId, String messageCode, List<Receiver> receiverList, Map<String, String> args, Attachment... attachments) {
        sendMessage(tenantId, messageCode, messageClientProperties.getDefaultLang(), receiverList, args, attachments);
    }

    @Override
    public void sendMessage(long tenantId, String messageCode, String receiverGroupCode, Map<String, String> args, Attachment... attachments) {
        sendMessage(tenantId, messageCode, messageClientProperties.getDefaultLang(), receiverGroupCode, args, attachments);
    }

    @Override
    public void sendMessage(long tenantId, String messageCode, List<Receiver> receiverList, Map<String, String> args, List<String> typeCodeList, Attachment... attachments) {
        sendMessage(tenantId, messageCode, messageClientProperties.getDefaultLang(), receiverList, args, typeCodeList, attachments);
    }

    @Override
    public void sendMessage(long tenantId, String messageCode, String receiverGroupCode, Map<String, String> args, List<String> typeCodeList, Attachment... attachments) {
        sendMessage(tenantId, messageCode, messageClientProperties.getDefaultLang(), receiverGroupCode, args, typeCodeList, attachments);
    }

    @Override
    public void sendMessage(long tenantId, String messageCode, String lang, List<Receiver> receiverList, Map<String, String> args, Attachment... attachments) {
        sendMessage(tenantId, messageCode, lang, receiverList, args, null, attachments);
    }

    @Override
    public void sendMessage(long tenantId, String messageCode, String lang, String receiverGroupCode, Map<String, String> args, Attachment... attachments) {
        sendMessage(tenantId, messageCode, lang, receiverGroupCode, args, null, attachments);
    }

    @Override
    public void sendMessage(long tenantId, String messageCode, String lang, String receiverGroupCode, Map<String, String> args, List<String> typeCodeList, Attachment... attachments) {
        List<Receiver> receiver = receiver(tenantId, receiverGroupCode, args);
        sendMessage(tenantId, messageCode, lang, receiver, args, typeCodeList, attachments);
    }

    @Override
    public void sendMessage(long tenantId, String messageCode, String lang, List<Receiver> receiverAddressList, Map<String, String> args, List<String> typeCodeList, Attachment... attachments) {
        if (attachments == null) {
            attachments = new Attachment[0];
        }
        Map<String, Message> messageMap = buildMessageMap(tenantId, messageCode, args, null, lang);
        MessageSender messageSender = new MessageSender()
                .setTenantId(tenantId)
                .setMessageCode(messageCode)
                .setLang(lang)
                .setReceiverAddressList(receiverAddressList)
                .setArgs(args)
                .setTypeCodeList(typeCodeList)
                .setMessageMap(messageMap);
        if (attachments.length > 0) {
            messageSender.setAttachmentList(Arrays.asList(attachments));
        }
        if (async) {
            messageAsyncService.sendMessage(messageSender);
        } else {
            messageRemoteService.sendMessage(tenantId, messageSender);
        }
    }

    @Override
    public void sendMessage(MessageSender messageSender) {
        Assert.notNull(messageSender.getTenantId(), BaseConstants.ErrorCode.DATA_INVALID);
        Assert.isTrue(StringUtils.hasText(messageSender.getMessageCode()), BaseConstants.ErrorCode.DATA_INVALID);
        messageSender.setMessageMap(buildMessageMap(messageSender.getTenantId(), messageSender.getMessageCode(), messageSender.getArgs(), messageSender.getObjectArgs(), messageSender.getLang()));
        if (async) {
            messageAsyncService.sendMessage(messageSender);
        } else {
            messageRemoteService.sendMessage(messageSender.getTenantId(), messageSender);
        }
    }

    @Override
    public void sendMessage(Long tenantId, String receiverGroupCode, MessageSender messageSender, WeChatSender weChatSender) {
        List<Receiver> weChatReceivers = openReceiver(tenantId, HmsgBootConstant.MessageType.WC_E, receiverGroupCode, null);
        if (!CollectionUtils.isEmpty(weChatReceivers)) {
            List<String> userList = weChatReceivers.stream().map(Receiver::getOpenUserId).collect(Collectors.toList());
            weChatSender.setUserIdList(userList);
        }
        sendMessage(messageSender, weChatSender);
    }

    @Override
    public void sendMessage(Long tenantId, String receiverGroupCode, MessageSender messageSender, WeChatSender weChatSender, DingTalkSender dingTalkSender) {
        List<Receiver> weChatReceivers = openReceiver(tenantId, HmsgBootConstant.MessageType.WC_E, receiverGroupCode, null);
        List<Receiver> dingTalkReceivers = openReceiver(tenantId, HmsgBootConstant.MessageType.DT, receiverGroupCode, null);
        if (!CollectionUtils.isEmpty(weChatReceivers)) {
            List<String> userList = weChatReceivers.stream().map(Receiver::getOpenUserId).collect(Collectors.toList());
            weChatSender.setUserIdList(userList);
        }
        if (!CollectionUtils.isEmpty(dingTalkReceivers)) {
            List<String> userList = dingTalkReceivers.stream().map(Receiver::getOpenUserId).collect(Collectors.toList());
            dingTalkSender.setUserIdList(userList);
        }
        sendMessage(messageSender, weChatSender, dingTalkSender);
    }

    @Override
    public void sendMessage(MessageSender messageSender, WeChatSender weChatSender) {
        sendMessage(messageSender, weChatSender, null);
    }

    @Override
    public void sendMessage(MessageSender messageSender, WeChatSender weChatSender, DingTalkSender dingTalkSender) {
        Assert.isTrue(messageSender != null || weChatSender != null, BaseConstants.ErrorCode.DATA_INVALID);
        Long tenantId = messageSender == null ? weChatSender.getTenantId() : messageSender.getTenantId();
        if (messageSender != null) {
            Assert.isTrue(StringUtils.hasText(messageSender.getMessageCode()), BaseConstants.ErrorCode.DATA_INVALID);
            messageSender.setMessageMap(buildMessageMap(messageSender.getTenantId(), messageSender.getMessageCode(), messageSender.getArgs(), messageSender.getObjectArgs(), messageSender.getLang()));
        }
        if (weChatSender != null) {
            Assert.isTrue(StringUtils.hasText(weChatSender.getMessageCode()), BaseConstants.ErrorCode.DATA_INVALID);
        }
        if (dingTalkSender != null) {
            Assert.isTrue(StringUtils.hasText(dingTalkSender.getMessageCode()), BaseConstants.ErrorCode.DATA_INVALID);
            Assert.notNull(dingTalkSender.getAgentId(), BaseConstants.ErrorCode.DATA_INVALID);
        }
        AllSender sender = new AllSender().setMessageSender(messageSender).setWeChatSender(weChatSender).setDingTalkSender(dingTalkSender);
        if (async) {
            messageAsyncService.sendAllMessage(tenantId, sender);
        } else {
            messageRemoteService.sendAllMessage(tenantId, sender);
        }
    }

    private Map<String, Message> buildMessageMap(Long tenantId, String messageCode, Map<String, String> args, Map<String, Object> objectArgs, String lang) {
        Map<String, Message> messageMap = null;
        List<TemplateServerLine> templateServerLines = responseEntityHandler(messageRemoteService.listTemplateServerLine(tenantId, messageCode));
        if (!CollectionUtils.isEmpty(templateServerLines)) {
            messageMap = new HashMap<>(16);
            for (TemplateServerLine templateServerLine : templateServerLines) {
                if (objectArgs == null) {
                    objectArgs = new HashMap<>(16);
                }
                if (!CollectionUtils.isEmpty(args)) {
                    objectArgs.putAll(args);
                }
                messageMap.put(templateServerLine.getTypeCode(), generateMessageObjectArgs(tenantId, templateServerLine.getTemplateCode(), templateServerLine.getTypeCode(), objectArgs, true, lang));
            }
        }
        return messageMap;
    }

    private <T> T responseEntityHandler(ResponseEntity<T> responseEntity) {
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new CommonException("error.message.send");
        }
        return responseEntity.getBody();
    }

    /**
     * 异步发送
     *
     * @return client
     */
    public MessageClient async() {
        this.async = true;
        return this;
    }

    /**
     * 同步发送
     *
     * @return client
     */
    public MessageClient sync() {
        this.async = false;
        return this;
    }

    // ## websocket相关方法 ##

    /**
     * 获取在线用户信息
     *
     * @return 在线用户信息
     */
    public List<OnLineUserDTO> getUser() {
        return ResponseUtils.getResponse(platformRemoteService.listOnlineUser(BaseConstants.DEFAULT_TENANT_ID), new TypeReference<List<OnLineUserDTO>>() {
        });
    }

    /**
     * 指定sessionId发送webSocket消息
     *
     * @param sessionId sessionId
     * @param key       自定义的key
     * @param message   消息内容
     */
    public void sendBySession(String sessionId, String key, String message) {
        Msg msg = new Msg().setSessionId(sessionId).setKey(key).setMessage(message).setType(WebSocketConstant.SendType.SESSION).setService(serviceName);
        try {
            redisTemplate.convertAndSend(WebSocketConstant.CHANNEL, objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) {
            throw new CommonException(e);
        }
    }

    /**
     * 指定用户发送webSocket消息
     *
     * @param userId  用户Id
     * @param key     自定义的key
     * @param message 消息内容
     */
    public void sendByUserId(Long userId, String key, String message) {
        Msg msg = new Msg().setUserId(userId).setKey(key).setMessage(message).setType(WebSocketConstant.SendType.USER).setService(serviceName);
        try {
            redisTemplate.convertAndSend(WebSocketConstant.CHANNEL, objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) {
            throw new CommonException(e);
        }
    }

    /**
     * 向所有用户发送webSocket消息
     *
     * @param key     自定义的key
     * @param message 消息内容
     */
    public void sendToAll(String key, String message) {
        Msg msg = new Msg().setKey(key).setMessage(message).setType(WebSocketConstant.SendType.ALL).setService(serviceName);
        try {
            redisTemplate.convertAndSend(WebSocketConstant.CHANNEL, objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) {
            throw new CommonException(e);
        }
    }

    /**
     * 获取公告消息
     *
     * @param tenantId           租户Id
     * @param noticeCategoryCode 公告类型
     * @param lang               语言
     * @param count              查询总数
     * @return 公告消息
     */
    public List<NoticeCacheVO> obtainNoticeMsg(Long tenantId, String noticeCategoryCode, String lang, int count) {
        List<NoticeCacheVO> notices =
                PublishNoticeRedis.selectLatestPublishedNotice(tenantId, noticeCategoryCode, lang, count);
        if (CollectionUtils.isEmpty(notices)) {
            notices = PublishNoticeRedis.selectLatestPublishedNotice(BaseConstants.DEFAULT_TENANT_ID, noticeCategoryCode,
                    lang, count);
        }
        return notices;
    }

    /**
     * 获取公告信息明细
     *
     * @param noticeId 公告Id
     * @return NoticeDTO
     */
    public NoticeDTO getNoticeDetailsById(Long noticeId) {
        return ResponseUtils.getResponse(messageRemoteService.getNoticeDetailsById(noticeId), NoticeDTO.class);
    }

    @Override
    public Message sendWeChatOfficialMessage(long tenantId, String serverCode, String messageTemplateCode, List<String> userList, Map<String, WeChatFont> data) {
        return sendWeChatOfficialMessage(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), userList, data, null, null);
    }

    @Override
    public Message sendWeChatOfficialMessage(long tenantId, String serverCode, String messageTemplateCode, String lang, List<String> userList, Map<String, WeChatFont> data, String url, Miniprogram miniprogram) {
        Assert.notEmpty(userList, ERROR_MESSAGE_EMPTY_RECEIVER);
        WeChatSender weChatSender = new WeChatSender()
                .setTenantId(tenantId)
                .setMessageCode(messageTemplateCode)
                .setLang(lang)
                .setServerCode(serverCode)
                .setUserList(userList)
                .setData(data)
                .setUrl(url)
                .setMiniprogram(miniprogram);
        if (async) {
            messageAsyncService.sendWeChatOfficial(weChatSender);
            return null;
        } else {
            return responseEntityHandler(messageRemoteService.sendWeChatOfficial(tenantId, weChatSender));
        }
    }

    @Override
    public Message sendWeChatEnterpriseMessage(long tenantId, String serverCode, String messageTemplateCode, Long agentId, List<String> userList, Map<String, String> args) {
        return sendWeChatEnterpriseMessage(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), agentId, userList, null, null, 0, args);
    }

    @Override
    public Message sendWeChatEnterpriseMessage(long tenantId, String serverCode, String messageTemplateCode, String lang, Long agentId, List<String> userList, List<String> partyList, List<String> tagList, Integer safe, Map<String, String> args) {
        WeChatSender weChatSender = new WeChatSender()
                .setTenantId(tenantId)
                .setServerCode(serverCode)
                .setMessageCode(messageTemplateCode)
                .setLang(lang)
                .setAgentId(agentId)
                .setUserIdList(userList)
                .setPartyList(partyList)
                .setTagList(tagList)
                .setSafe(safe)
                .setArgs(args);
        if (async) {
            messageAsyncService.sendWeChatEnterprise(weChatSender);
            return null;
        } else {
            return responseEntityHandler(messageRemoteService.sendWeChatEnterprise(tenantId, weChatSender));
        }
    }

    @Override
    public Message sendWeChatEnterpriseMessage(long tenantId, String serverCode, String messageTemplateCode, String receiverTypeCode, Long agentId, Map<String, String> args) {
        return sendWeChatEnterpriseMessage(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), receiverTypeCode, agentId, args);
    }

    @Override
    public Message sendWeChatEnterpriseMessage(long tenantId, String serverCode, String messageTemplateCode, String lang, String receiverTypeCode, Long agentId, Map<String, String> args) {
        List<Receiver> openReceivers = openReceiver(tenantId, HmsgBootConstant.MessageType.WC_E, receiverTypeCode, args);
        List<String> userList = null;
        if (!CollectionUtils.isEmpty(openReceivers)) {
            userList = openReceivers.stream().map(Receiver::getOpenUserId).collect(Collectors.toList());
        }
        return sendWeChatEnterpriseMessage(tenantId, serverCode, messageTemplateCode, lang, agentId, userList, null, null, 0, args);
    }

    @Override
    public Message sendDingTalkMessage(Long tenantId, String serverCode, String messageTemplateCode, Map<String, String> args, Long agentId, List<String> userIdList) {
        return sendDingTalkMessage(tenantId, serverCode, messageTemplateCode, null, args, agentId, userIdList, null, false);
    }

    @Override
    public Message sendDingTalkMessage(Long tenantId, String serverCode, String messageTemplateCode, String lang, Map<String, String> args, Long agentId, List<String> userIdList, List<String> deptIdList, boolean toAllUser) {
        DingTalkSender dingTalkSender = new DingTalkSender()
                .setTenantId(tenantId)
                .setServerCode(serverCode)
                .setMessageCode(messageTemplateCode)
                .setLang(lang)
                .setAgentId(agentId)
                .setUserIdList(userIdList)
                .setDeptIdList(deptIdList)
                .setArgs(args)
                .setToAllUser(toAllUser);
        if (async) {
            messageAsyncService.sendDingTalkMessage(dingTalkSender);
            return null;
        } else {
            return responseEntityHandler(messageRemoteService.sendDingTalk(tenantId, dingTalkSender));
        }
    }

    @Override
    public Message sendDingTalkMessage(long tenantId, String serverCode, String messageTemplateCode, String receiverTypeCode, Long agentId, Map<String, String> args) {
        return sendDingTalkMessage(tenantId, serverCode, messageTemplateCode, messageClientProperties.getDefaultLang(), receiverTypeCode, agentId, args);
    }

    @Override
    public Message sendDingTalkMessage(long tenantId, String serverCode, String messageTemplateCode, String lang, String receiverTypeCode, Long agentId, Map<String, String> args) {
        List<Receiver> openReceivers = openReceiver(tenantId, HmsgBootConstant.MessageType.DT, receiverTypeCode, args);
        List<String> userList = null;
        if (!CollectionUtils.isEmpty(openReceivers)) {
            userList = openReceivers.stream().map(Receiver::getOpenUserId).collect(Collectors.toList());
        }
        return sendDingTalkMessage(tenantId, serverCode, messageTemplateCode, lang, args, agentId, userList, null, false);
    }

    @Override
    public Message sendWebHookMessage(Long tenantId, String messageCode, String serverCode, List<Receiver> receiverAddressList, Map<String, String> args) {
        return sendWebHookMessage(tenantId, messageCode, serverCode, receiverAddressList, null, args);
    }

    @Override
    public Message sendWebHookMessage(Long tenantId, String messageCode, String serverCode, Map<String, String> args) {
        return sendWebHookMessage(tenantId, messageCode, serverCode, new ArrayList<>(), args);
    }

    @Override
    public Message sendWebHookMessage(Long tenantId, String messageCode, String serverCode, String receiverTypeCode, String lang, Map<String, String> args) {
        List<Receiver> receiver = receiver(tenantId, receiverTypeCode, args);
        return sendWebHookMessage(tenantId, messageCode, serverCode, receiver, lang, args);
    }

    @Override
    public Message sendWebHookMessage(Long tenantId, String messageCode, String serverCode, String receiverTypeCode, Map<String, String> args) {
        List<Receiver> receiver = receiver(tenantId, receiverTypeCode, args);
        return sendWebHookMessage(tenantId, messageCode, serverCode, receiver, null, args);
    }

    @Override
    public Message sendWebHookMessage(Long tenantId, String messageCode, String serverCode, List<Receiver> receiverAddressList, String lang, Map<String, String> args) {
        if (org.apache.commons.lang3.StringUtils.isBlank(lang)) {
            lang = messageClientProperties.getDefaultLang();
        }
        WebHookSender webHookSender = new WebHookSender()
                .setArgs(args)
                .setTenantId(tenantId)
                .setLang(lang)
                .setMessageCode(messageCode)
                .setReceiverAddressList(receiverAddressList)
                .setServerCode(serverCode);
        if (async) {
            messageAsyncService.sendWebHookMessage(webHookSender);
            return null;
        } else {
            return responseEntityHandler(messageRemoteService.sendWebHookMessage(tenantId, webHookSender));
        }
    }
}
