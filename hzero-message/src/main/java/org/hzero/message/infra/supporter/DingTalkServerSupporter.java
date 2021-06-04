package org.hzero.message.infra.supporter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hzero.boot.message.entity.DingTalkSender;
import org.hzero.dd.dto.MarkDownFormat;
import org.hzero.dd.dto.SendWorkMarkDownMessageDTO;
import org.hzero.dd.dto.SendWorkMessageResultDTO;
import org.hzero.dd.service.DingCorpMessageService;
import org.hzero.message.domain.entity.Message;
import org.hzero.message.infra.exception.SendMessageException;
import org.hzero.message.infra.util.ClearHtmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import io.choerodon.core.convertor.ApplicationContextHelper;
import io.choerodon.core.exception.CommonException;


/**
 * 钉钉消息
 *
 * @author zifeng.ding@hand-china.com 2019/11/14 16:46
 */
public class DingTalkServerSupporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DingTalkServerSupporter.class);
    private static final ObjectMapper OBJECT_MAPPER = ApplicationContextHelper.getContext().getBean(ObjectMapper.class);
    private static final String MARKDOWN_MESSAGE = "markdown";

    private DingTalkServerSupporter() {
    }


    private static SendWorkMarkDownMessageDTO generateMessage(List<String> userList, Message message) {
        SendWorkMarkDownMessageDTO markdownMessage = new SendWorkMarkDownMessageDTO();
        try {
            markdownMessage.setUserid_list(buildParam(userList));
            Map<String, String> map = OBJECT_MAPPER.readValue(message.getSendArgs(), new TypeReference<Map<String, String>>() {
            });
            String agentStr = map.get(DingTalkSender.FIELD_AGENT_ID);
            markdownMessage.setAgent_id(Objects.equals(agentStr, "null") ? 0L : Long.parseLong(agentStr));
            if (map.containsKey(DingTalkSender.FIELD_DEPT_ID_LIST)) {
                markdownMessage.setDept_id_list(
                        buildParam(OBJECT_MAPPER.readValue(map.get(DingTalkSender.FIELD_DEPT_ID_LIST), new TypeReference<List<String>>() {
                        })));
            }
            if (map.containsKey(DingTalkSender.FIELD_TO_ALL_USER)) {
                markdownMessage.setTo_all_user(
                        OBJECT_MAPPER.readValue(map.get(DingTalkSender.FIELD_TO_ALL_USER), new TypeReference<Boolean>() {
                        }));
            }
            MarkDownFormat msg = new MarkDownFormat();
            MarkDownFormat.MarkdownBean markdown = new MarkDownFormat.MarkdownBean();
            markdown.setTitle(message.getSubject());
            markdown.setText(String.format("## %s%n %s", message.getSubject(), ClearHtmlUtils.clearHtml(message.getContent())));
            msg.setMsgtype(MARKDOWN_MESSAGE);
            msg.setMarkdown(markdown);
            markdownMessage.setMsg(msg);
            return markdownMessage;
        } catch (IOException e) {
            LOGGER.error("Incorrect parameter format");
            throw new CommonException(e);
        }
    }

    private static String buildParam(List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        list.forEach(item -> sb.append(item).append(","));
        return sb.toString().endsWith(",") ? sb.substring(0, sb.length() - 1) : null;
    }

    /**
     * 发送钉钉模板消息
     */
    public static void sendMessage(DingCorpMessageService messageService, String token, List<String> userList,
                                   Message message) {
        SendWorkMarkDownMessageDTO workMessage = generateMessage(userList, message);
        SendWorkMessageResultDTO response = messageService.sendWorkMarkDownMessage(token, workMessage);
        if (response.getErrcode() != 0) {
            throw new SendMessageException(String.format("dingtalk message send failed! code: [%s] , message: [%s]",
                    response.getErrcode(), response.getErrmsg()));
        }
    }


}
