package org.hzero.report.infra.enums;

/**
 * 报表类型
 *
 * @author xianzhi.chen@hand-china.com 2018年10月17日下午4:23:55
 */
public enum ReportTypeEnum {

    /**
     * 简单表格
     */
    SIMPLE_TABLE("ST"),
    /**
     * 复杂表格
     */
    TABLE("T"),
    /**
     * 图表报表
     */
    CHART("C"),
    /**
     * 模板报表
     */
    DOCUMENT("D");

    private final String value;

    ReportTypeEnum(final String value) {
        this.value = value;
    }

    public static ReportTypeEnum valueOf2(String arg) {
        switch (arg) {
            case "ST":
                return SIMPLE_TABLE;
            case "T":
                return TABLE;
            case "C":
                return CHART;
            case "D":
                return DOCUMENT;
            default:
                return TABLE;
        }
    }

    public static boolean isInEnum(String value) {
        for (ReportTypeEnum reportTypeEnum : ReportTypeEnum.values()) {
            if (reportTypeEnum.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public String getValue() {
        return this.value;
    }
}
