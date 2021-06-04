package org.hzero.plugin.platform.customize.api.dto;

import java.util.List;
import java.util.Map;

/**
 * @author peng.yu01@hand-china.com on 2020-04-10
 */
public class FieldConValidTlDTO {

    private Long conValidId;

    private Long conHeaderId;

    private Long configFieldId;

    private List<Map<String, String>> tlMaps;

    public List<Map<String, String>> getTlMaps() {
        return tlMaps;
    }

    public void setTlMaps(List<Map<String, String>> tlMaps) {
        this.tlMaps = tlMaps;
    }

    public Long getConHeaderId() {
        return conHeaderId;
    }

    public void setConHeaderId(Long conHeaderId) {
        this.conHeaderId = conHeaderId;
    }

    public Long getConfigFieldId() {
        return configFieldId;
    }

    public void setConfigFieldId(Long configFieldId) {
        this.configFieldId = configFieldId;
    }

    public Long getConValidId() {
        return conValidId;
    }

    public void setConValidId(Long conValidId) {
        this.conValidId = conValidId;
    }
}
