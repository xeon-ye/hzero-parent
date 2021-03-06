package org.hzero.message.app.service.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.message.entity.*;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.convert.CommonConverter;
import org.hzero.message.api.dto.UserMessageDTO;
import org.hzero.message.app.service.MessageGeneratorService;
import org.hzero.message.app.service.MessageReceiverService;
import org.hzero.message.app.service.WebHookSendService;
import org.hzero.message.config.MessageConfigProperties;
import org.hzero.message.domain.entity.Message;
import org.hzero.message.domain.entity.MessageReceiver;
import org.hzero.message.domain.entity.MessageTransaction;
import org.hzero.message.domain.entity.WebhookServer;
import org.hzero.message.domain.repository.MessageReceiverRepository;
import org.hzero.message.domain.repository.MessageRepository;
import org.hzero.message.domain.repository.MessageTransactionRepository;
import org.hzero.message.domain.repository.WebhookServerRepository;
import org.hzero.message.infra.constant.HmsgConstant;
import org.hzero.mybatis.helper.DataSecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import io.choerodon.core.convertor.ApplicationContextHelper;
import io.choerodon.core.exception.CommonException;

/**
 * webHook????????????
 *
 * @author xiaoyu.zhao@hand-china.com 2020-04-26 19:57:46
 */
@Service
public class WebHookSendServiceImpl extends AbstractSendService implements WebHookSendService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebHookSendServiceImpl.class);
    private static final String URL_REGEX = "(http[s]{0,1}|ftp)://[a-zA-Z0-9\\.\\-]+\\.([a-zA-Z]{2,4})(:\\d+)?([a-zA-Z0-9\\.\\-~!@#$%^&*+?:_/=<>[\\u4E00-\\u9FA5\\uF900-\\uFA2D]*]*)+";
    private static final String CHINESE_REGEX = "[\\u4E00-\\u9FA5\\uF900-\\uFA2D]+";

    private final MessageRepository messageRepository;
    private final MessageReceiverService messageReceiverService;
    private final MessageConfigProperties messageConfigProperties;
    private final MessageGeneratorService messageGeneratorService;
    private final WebhookServerRepository webhookServerRepository;
    private final MessageTransactionRepository messageTransactionRepository;
    private final MessageReceiverRepository messageReceiverRepository;

    @Autowired
    public WebHookSendServiceImpl(MessageRepository messageRepository, MessageReceiverService messageReceiverService,
                                  MessageConfigProperties messageConfigProperties, MessageGeneratorService messageGeneratorService,
                                  WebhookServerRepository webhookServerRepository, MessageTransactionRepository messageTransactionRepository,
                                  MessageReceiverRepository messageReceiverRepository) {
        this.messageRepository = messageRepository;
        this.messageReceiverService = messageReceiverService;
        this.messageConfigProperties = messageConfigProperties;
        this.messageGeneratorService = messageGeneratorService;
        this.webhookServerRepository = webhookServerRepository;
        this.messageTransactionRepository = messageTransactionRepository;
        this.messageReceiverRepository = messageReceiverRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message sendWebHookMessage(WebHookSender messageSender) {
        if (messageConfigProperties.isAsync()) {
            ApplicationContextHelper.getContext().getBean(WebHookSendService.class).asyncSendMessage(messageSender);
            return null;
        } else {
            return sendMessage(messageSender);
        }
    }

    @Override
    @Async("commonAsyncTaskExecutor")
    public void asyncSendMessage(WebHookSender messageSender) {
        sendMessage(messageSender);
    }

    private Message sendMessage(WebHookSender messageSender) {
        Assert.notNull(messageSender.getMessageCode(), BaseConstants.ErrorCode.DATA_INVALID);
        Assert.notNull(messageSender.getServerCode(), BaseConstants.ErrorCode.DATA_INVALID);
        Assert.notNull(messageSender.getTenantId(), BaseConstants.ErrorCode.DATA_INVALID);
        // ?????????????????????webHook??????
        WebhookServer webhookServer = webhookServerRepository.selectOne(new WebhookServer()
                .setTenantId(messageSender.getTenantId())
                .setServerCode(messageSender.getServerCode())
                .setEnabledFlag(BaseConstants.Flag.YES));
        if (webhookServer == null && !messageSender.getTenantId().equals(BaseConstants.DEFAULT_TENANT_ID)) {
            webhookServer = webhookServerRepository.selectOne(new WebhookServer()
                    .setTenantId(BaseConstants.DEFAULT_TENANT_ID)
                    .setServerCode(messageSender.getServerCode())
                    .setEnabledFlag(BaseConstants.Flag.YES));
        }
        // ??????WebHook????????????
        Assert.notNull(webhookServer, HmsgConstant.ErrorCode.WEBHOOK_NOT_EXISTS);
        if (StringUtils.isNotBlank(webhookServer.getSecret())) {
            webhookServer.setSecret(this.decryptSecret(webhookServer.getSecret()));
        }
        // ??????????????????
        Message message = createMessage(messageSender, HmsgConstant.MessageType.WEB_HOOK);
        try {
            // ??????????????????
            message = messageGeneratorService.generateMessage(messageSender, message);
            // ?????????????????????
            MessageSender ms = messageReceiverService
                    .queryReceiver(new MessageSender().setTenantId(messageSender.getTenantId())
                            .setReceiverTypeCode(messageSender.getReceiverTypeCode())
                            .setReceiverAddressList(messageSender.getReceiverAddressList()));
            // ????????????
            messageSender.setMessage(CommonConverter.beanConvert(org.hzero.boot.message.entity.Message.class, message))
                    .setWebhookAddress(webhookServer.getWebhookAddress())
                    .setSecret(webhookServer.getSecret())
                    .setServerType(webhookServer.getServerType())
                    .setReceiverAddressList(ms.getReceiverAddressList())
                    .setServerCode(webhookServer.getServerCode());
            // ????????????
            messageRepository.updateByPrimaryKeySelective(message);
            Long messageId = message.getMessageId();
            Long tenantId = message.getTenantId();
            // ????????????????????????TODO ??????????????????????????????????????????
            if (CollectionUtils.isNotEmpty(ms.getReceiverAddressList())
                    && messageSender.getServerType().equals(HmsgConstant.WebHookServerType.DING_TALK)) {
                ms.getReceiverAddressList().forEach(receiver -> {
                    if (!StringUtils.isBlank(receiver.getPhone())) {
                        messageReceiverRepository.insertSelective(new MessageReceiver()
                                .setMessageId(messageId)
                                .setTenantId(tenantId)
                                .setIdd(receiver.getIdd())
                                .setReceiverAddress(receiver.getPhone())
                        );
                    }
                });
            }
            sendWHMessage(messageSender, message);
            // ????????????
            messageRepository.updateByPrimaryKeySelective(message.setSendFlag(BaseConstants.Flag.YES));
            MessageTransaction transaction = new MessageTransaction().setMessageId(message.getMessageId())
                    .setTrxStatusCode(HmsgConstant.TransactionStatus.S)
                    .setTenantId(messageSender.getTenantId());
            messageTransactionRepository.insertSelective(transaction);
            message.setTransactionId(transaction.getTransactionId());
        } catch (Exception e) {
            // ????????????
            failedProcess(message, e);
        }
        return message;
    }

    /**
     * ??????webhook?????? TODO ???????????????????????????????????????markdown?????????????????????????????????????????????
     *
     * @param messageSender WebHookSender
     * @param message       ???????????????
     */
    private void sendWHMessage(WebHookSender messageSender, Message message) {
        // ??????WebHook?????????????????????????????????????????????
        switch (messageSender.getServerType()) {
            case HmsgConstant.WebHookServerType.JSON:
                sendWHJsonMessage(messageSender);
                break;
            case HmsgConstant.WebHookServerType.DING_TALK:
                // ???????????????
                List<Receiver> receivers = messageSender.getReceiverAddressList();
                Set<String> phoneSet = new HashSet<>();
                // ????????????????????????????????????@?????????
                if (CollectionUtils.isNotEmpty(receivers)) {
                    for (Receiver receiver : receivers) {
                        if (StringUtils.isNotBlank(receiver.getPhone())) {
                            // ??????????????????????????????webhook??????????????????????????????????????????????????????????????????
                            String[] split = StringUtils.split(receiver.getPhone(), BaseConstants.Symbol.COMMA);
                            for (String phone : split) {
                                // ??????????????????
                                if (StringUtils.isNotBlank(receiver.getIdd())) {
                                    phoneSet.add(StringUtils.join(receiver.getIdd(), BaseConstants.Symbol.MIDDLE_LINE,
                                            phone));
                                } else {
                                    phoneSet.add(phone);
                                }
                            }
                        }
                    }
                }
                sendWHDingTalkMessage(messageSender, message, phoneSet);
                break;
            case HmsgConstant.WebHookServerType.WE_CHAT:
                sendWHWeChatMessage(messageSender, message);
                break;
            default:
                throw new CommonException(HmsgConstant.ErrorCode.WEBHOOK_TYPE_ILLEGAL);
        }
    }

    /**
     * ??????????????????WebHook??????
     *
     * @param messageSender WebHookSender
     * @param message       ???????????????
     */
    private void sendWHWeChatMessage(WebHookSender messageSender, Message message) {
        String content = message.getContent()
                .replaceAll("<(?!font|/font).*?>", "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&amp;", "&");
        content = this.urlEncode(content);
        //this.urlEncode();
        WhWeChatSend whWeChatSend = new WhWeChatSend();
        WhWeChatSend.WhWeChatMarkdown weChatMarkdown = new WhWeChatSend.WhWeChatMarkdown();
        weChatMarkdown.setContent(content);
        whWeChatSend.setMsgtype("markdown");
        whWeChatSend.setMarkdown(weChatMarkdown);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(messageSender.getWebhookAddress(), whWeChatSend, String.class);
    }

    /**
     * ????????????WebHook??????
     *
     * @param messageSender WebHookSender
     * @param message       ???????????????
     * @param receivers     ??????@????????????
     */
    private void sendWHDingTalkMessage(WebHookSender messageSender, Message message, Set<String> receivers) {
        WhDingTalkSend whDingTalkSend = new WhDingTalkSend();
        WhDingTalkSend.WhDingAt whDingAt = new WhDingTalkSend.WhDingAt();
        WhDingTalkSend.WhDingMarkdown whDingMarkdown = new WhDingTalkSend.WhDingMarkdown();
        // ??????????????????
        String contentText = String.format("%s", message.getContent()
                .replaceAll("<[^>]+>", "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&amp;", "&"));
        //contentText = this.urlEncode(contentText);
        if (CollectionUtils.isNotEmpty(receivers)) {
            for (String receiver : receivers) {
                contentText = StringUtils.join(contentText, BaseConstants.Symbol.SPACE, BaseConstants.Symbol.AT,
                        receiver);
            }
            whDingAt.setAtAll(false);
            whDingAt.setAtMobiles(receivers.toArray(new String[0]));
        } else {
            whDingAt.setAtAll(true);
        }
        // 1.??????@??????
        whDingTalkSend.setAt(whDingAt);
        // 2.?????????????????????????????????uri?????????????????????uri?????????String???????????????http?????????RestTemplate
        // ????????????????????????????????????String???????????????????????????????????????????????????????????????sign????????????????????????
        long timestamp = System.currentTimeMillis();
        URI uri;
        try {
            uri = new URI(messageSender.getWebhookAddress() + "&timestamp=" + timestamp + "&sign="
                    + addSignature(messageSender.getSecret(), timestamp));
        } catch (URISyntaxException e) {
            throw new CommonException(e);
        }
        // 3.??????????????????
        whDingTalkSend.setMsgtype("markdown");

        // 4.??????????????????
        whDingMarkdown.setText(contentText);
        whDingMarkdown.setTitle(message.getSubject());
        whDingTalkSend.setMarkdown(whDingMarkdown);
        // 5.????????????
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(uri, whDingTalkSend, String.class);
    }

    /**
     * ??????Json??????
     * Json ???????????????????????????????????????????????????Authorization:bearer 8ebe3da9-0337-4e0a-866a-671f8fa90f9c
     *
     * @param messageSender WebHookSender
     */
    private void sendWHJsonMessage(WebHookSender messageSender) {
        sendWHHttpMessage(messageSender.getWebhookAddress(), messageSender);
    }

    private void sendWHHttpMessage(String path, WebHookSender messageSender) {
        HttpHeaders httpHeaders = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        httpHeaders.setContentType(type);
        if (messageSender.getSecret() != null) {
            String[] split = StringUtils.split(messageSender.getSecret(), ":", 2);
            if (split.length == BaseConstants.Digital.TWO) {
                httpHeaders.add(split[0], split[1]);
            }
        }
        HttpEntity<String> request = new HttpEntity<>(messageSender.getMessage().getContent(), httpHeaders);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(path, request, String.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message resendWebHookMessage(UserMessageDTO message) {
        WebhookServer dbWebHook = webhookServerRepository.selectOne(
                new WebhookServer().setServerCode(message.getServerCode()).setTenantId(message.getTenantId()));
        if (dbWebHook == null && !message.getTenantId().equals(BaseConstants.DEFAULT_TENANT_ID)) {
            dbWebHook = webhookServerRepository.selectOne(new WebhookServer()
                    .setTenantId(BaseConstants.DEFAULT_TENANT_ID).setServerCode(message.getServerCode()));
        }
        Assert.notNull(dbWebHook, BaseConstants.ErrorCode.NOT_NULL);
        if (StringUtils.isNotBlank(dbWebHook.getSecret())) {
            dbWebHook.setSecret(this.decryptSecret(dbWebHook.getSecret()));
        }
        List<Receiver> receivers = new ArrayList<>();
        try {
            if (CollectionUtils.isNotEmpty(message.getMessageReceiverList())) {
                message.getMessageReceiverList().forEach(receiver -> {
                    if (!org.springframework.util.StringUtils.hasText(receiver.getReceiverAddress())
                            || receiver.getTenantId() == null) {
                        throw new CommonException(
                                "Sending a message error because no recipient or target tenant is specified : "
                                        + receiver.toString());
                    }
                    Receiver rc = new Receiver();
                    rc.setIdd(receiver.getIdd());
                    rc.setPhone(receiver.getReceiverAddress());
                    receivers.add(rc);
                });
            }
            // ????????????
            WebHookSender webHookSender = new WebHookSender()
                    .setMessage(CommonConverter.beanConvert(org.hzero.boot.message.entity.Message.class,
                            message.getMessage()))
                    .setServerCode(message.getServerCode())
                    .setReceiverAddressList(message.getReceiverAddressList());
            webHookSender.setMessageCode(message.getMessageCode());
            webHookSender.setTenantId(message.getTenantId());
            webHookSender.setLang(message.getLang());
            webHookSender.setServerType(dbWebHook.getServerType());
            webHookSender.setWebhookAddress(dbWebHook.getWebhookAddress());
            webHookSender.setSecret(dbWebHook.getSecret());
            webHookSender.setReceiverAddressList(receivers);
            // ??????????????????
            sendWHMessage(webHookSender, message);
            successProcessUpdate(message);
        } catch (Exception e) {
            failedProcessUpdate(message, e);
        }
        return message;
    }

    /**
     * ?????????????????? ???????????????timestamp+"\n"+????????????????????????????????????HmacSHA256?????????????????????????????????Base64
     * encode????????????????????????????????????urlEncode???????????????????????????????????????UTF-8????????????
     *
     * @param secret    ??????
     * @param timestamp ?????????
     * @return ???????????????
     */
    private String addSignature(String secret, Long timestamp) {
        // ???????????????timestamp+"\n"+???????????????????????????
        String stringToSign = timestamp + "\n" + secret;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(new String(Base64.encodeBase64(signData)), "UTF-8");
        } catch (Exception e) {
            LOGGER.error(">>>SENDING_WEBHOOK_ERROR>>> An error occurred while adding the signature {}", e.getMessage());
            return null;
        }
    }

    /**
     * ??????Secret
     *
     * @return ????????????Secret
     */
    private String decryptSecret(String secret) {
        try {
            // ????????????????????????
            return DataSecurityHelper.decrypt(secret);
        } catch (Exception e) {
            LOGGER.warn("===>>secret decryption failed, use the original secret to perform message sending<<===");
        }
        return secret;
    }

    /**
     * ??????URL??????????????????
     */
    private String urlEncode(String content) {
        Set<String> urlSet = patternMessageContent(content, URL_REGEX);
        for (String url : urlSet) {
            String encodeUrl = encodeUrlChinese(url);
            content = content.replace(url, encodeUrl);
        }
        return content;
    }

    /**
     * ??????URL??????????????????
     *
     * @param url url??????
     */
    private String encodeUrlChinese(String url) {
        Set<String> chineseSet = patternMessageContent(url, CHINESE_REGEX);
        for (String chinese : chineseSet) {
            try {
                String encodeChinese = URLEncoder.encode(chinese, StandardCharsets.UTF_8.name());
                url = StringUtils.replace(url, chinese, encodeChinese);
            } catch (Exception e) {
                LOGGER.error("url encode failed, exception is : {}", e.getMessage());
                throw new CommonException(BaseConstants.ErrorCode.ERROR);
            }
        }
        return url;
    }

    /**
     * ????????????????????????
     *
     * @param content ??????????????????
     * @param regex   ???????????????
     */
    private Set<String> patternMessageContent(String content, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        Set<String> resultSet = new LinkedHashSet<>();
        while (matcher.find()) {
            resultSet.add(matcher.group());
        }
        return resultSet;
    }

}
