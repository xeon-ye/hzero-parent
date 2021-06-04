package org.hzero.plugin.platform.customize.api.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionHeader;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionLine;

/**
 * @author : peng.yu01@hand-china.com 2020/2/9 18:51
 */
public class UnitConfigFieldConditionHeaderDTO {

    private String conType;

    private String conExpression;

    private List<UnitConfigFieldConditionLineDTO> lines;

    public String getConType() {
        return conType;
    }

    public void setConType(String conType) {
        this.conType = conType;
    }

    public String getConExpression() {
        return conExpression;
    }

    public void setConExpression(String conExpression) {
        this.conExpression = conExpression;
    }

    public List<UnitConfigFieldConditionLineDTO> getLines() {
        if (CollectionUtils.isEmpty(this.lines)) {
            return new ArrayList<>();
        }
        return lines;
    }

    public void setLines(List<UnitConfigFieldConditionLineDTO> lines) {
        this.lines = lines;
    }

    public static UnitConfigFieldConditionHeaderDTO convertFromCache(FieldConditionHeader header) {
        UnitConfigFieldConditionHeaderDTO headerDTO = new UnitConfigFieldConditionHeaderDTO();
        headerDTO.setConExpression(header.getConExpression());
        headerDTO.setConType(header.getConType());

        if (CollectionUtils.isNotEmpty(header.getLines())) {
            headerDTO.setLines(header.getLines().stream().map(UnitConfigFieldConditionLineDTO::convertFromCache).collect(Collectors.toList()));
//            for (FieldConditionLine line : header.getLines()) {
//                headerDTO.getLines().add(UnitConfigFieldConditionLineDTO.convertFromCache(line));
//            }
        }
        return headerDTO;
    }
}
