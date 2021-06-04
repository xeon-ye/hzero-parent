package org.hzero.admin.api.dto;

import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author XCXCXCXCX
 * @date 2020/6/1 2:45 下午
 */
public class Report {

    private Boolean success = true;

    private Map<String, String> instanceErrorMessages = new HashMap<>();

    private String message;

    public Report() {
    }

    public Report(Map<String, String> instanceErrorMessages) {
        if (!CollectionUtils.isEmpty(instanceErrorMessages)) {
            this.success = false;
            this.instanceErrorMessages = instanceErrorMessages;
        }
    }

    private String merge(Map<String, String> instanceErrorMessages) {
        StringBuilder builder = new StringBuilder();
        int sum = instanceErrorMessages.size();
        builder.append("Summary[")
                .append(sum)
                .append("] instances notify failed.\n ");
        for (Map.Entry<String, String> entry : instanceErrorMessages.entrySet()) {
            builder.append("instance [host = ")
                    .append(entry.getKey())
                    .append("] ")
                    .append("cause [ ")
                    .append(entry.getValue())
                    .append(" ].")
                    .append("\n ");
        }
        return builder.toString();
    }

    public Boolean getSuccess() {
        return success;
    }

    public Map<String, String> getInstanceErrorMessages() {
        return instanceErrorMessages;
    }

    public String getMessage() {
        return merge(instanceErrorMessages);
    }

}
