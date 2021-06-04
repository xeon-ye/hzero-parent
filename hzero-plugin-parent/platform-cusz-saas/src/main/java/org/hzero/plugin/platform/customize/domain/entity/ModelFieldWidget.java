package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author : peng.yu01@hand-china.com 2019/12/15 13:20
 */
@ModifyAudit
/*@VersionAudit*/
@Table(name = "hpfm_cusz_model_field_wdg")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ModelFieldWidget extends AuditDomain {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_FIELD_ID = "fieldId";
    public static final String FIELD_FIELD_WIDGET = "fieldWidget";
    public static final String FIELD_TEXT_MAX_LENGTH = "textMaxLength";
    public static final String FIELD_TEXT_MIN_LENGTH = "textMinLength";
    public static final String FIELD_TEXT_AREA_MAX_LINE = "textAreaMaxLine";
    public static final String FIELD_SOURCE_CODE = "sourceCode";
    public static final String FIELD_DATE_FORMAT = "dateFormat";
    public static final String FIELD_NUMBER_DECIMAL = "numberPrecision";
    public static final String FIELD_NUMBER_MIN = "numberMin";
    public static final String FIELD_NUMBER_MAX = "numberMax";
    public static final String FIELD_BUCKET_NAME = "bucketName";
    public static final String FIELD_BUCKET_DIRECTORY = "bucketDirectory";
    public static final String FIELD_LINK_TITLE = "linkTitle";
    public static final String FIELD_LINK_HREF = "linkHref";
    public static final String FIELD_LINK_NEW_WINDOW = "linkNewWindow";
    public static final String FIELD_DEFAULT_VALUE = "defaultValue";

    public static final String[] UPDATE_FIELD_COMMON_LIST = {FIELD_FIELD_WIDGET, FIELD_TEXT_MAX_LENGTH, FIELD_TEXT_AREA_MAX_LINE, FIELD_SOURCE_CODE, FIELD_DATE_FORMAT, FIELD_NUMBER_DECIMAL,
            FIELD_NUMBER_MIN, FIELD_NUMBER_MAX, FIELD_BUCKET_NAME, FIELD_BUCKET_DIRECTORY, FIELD_TEXT_MIN_LENGTH, FIELD_LINK_TITLE, FIELD_LINK_HREF, FIELD_LINK_NEW_WINDOW,FIELD_DEFAULT_VALUE};

    @Id
    @GeneratedValue
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_FIELD_WDG)
    private Long id;
    private Long tenantId = 0L;
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_FIELD)
    private Long fieldId;
    /**
     * 字段控件类型
     */
    private String fieldWidget;
    /**
     * TEXT类型 最大长度
     */
    private Integer textMaxLength;

    /**
     * TEXT类型 最大长度
     */
    private Integer textMinLength;

    /**
     * 文本域组件 最大行数
     */
    private Integer textAreaMaxLine;
    /**
     * 值集或者值集视图编码
     */
    private String sourceCode;
    /**
     * DATE类型 日期格式
     */
    private String dateFormat;
    /**
     * NUMBER类型 数值精度
     */
    @Column(name = "number_precision")
    private Integer numberDecimal;
    /**
     * NUMBER类型 数值最小值
     */
    private Long numberMin;
    /**
     * NUMBER类型 数值最大值
     */
    private Long numberMax;
    /**
     * 桶名
     */
    private String bucketName;
    /**
     * 桶目录
     */
    private String bucketDirectory;
    /**
     * 链接标题
     */
    private String linkTitle;
    /**
     * 链接地址
     */
    private String linkHref;
    /**
     * 是否打开新窗口
     */
    private Integer linkNewWindow;
    /**
     * 组件默认值
     */
    private String defaultValue;

    public ModelFieldWidget(Long fieldId) {
        this.fieldId = fieldId;
    }

    public ModelFieldWidget() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    public String getFieldWidget() {
        return fieldWidget;
    }

    public void setFieldWidget(String fieldWidget) {
        this.fieldWidget = fieldWidget;
    }

    public Integer getTextMaxLength() {
        return textMaxLength;
    }

    public void setTextMaxLength(Integer textMaxLength) {
        this.textMaxLength = textMaxLength;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public Integer getNumberDecimal() {
        return numberDecimal;
    }

    public void setNumberDecimal(Integer numberDecimal) {
        this.numberDecimal = numberDecimal;
    }

    public Long getNumberMin() {
        return numberMin;
    }

    public void setNumberMin(Long numberMin) {
        this.numberMin = numberMin;
    }

    public Long getNumberMax() {
        return numberMax;
    }

    public void setNumberMax(Long numberMax) {
        this.numberMax = numberMax;
    }


    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBucketDirectory() {
        return bucketDirectory;
    }

    public void setBucketDirectory(String bucketDirectory) {
        this.bucketDirectory = bucketDirectory;
    }

    public Integer getTextAreaMaxLine() {
        return textAreaMaxLine;
    }

    public void setTextAreaMaxLine(Integer textAreaMaxLine) {
        this.textAreaMaxLine = textAreaMaxLine;
    }

    public Integer getTextMinLength() {
        return textMinLength;
    }

    public void setTextMinLength(Integer textMinLength) {
        this.textMinLength = textMinLength;
    }

    public String getLinkTitle() {
        return linkTitle;
    }

    public void setLinkTitle(String linkTitle) {
        this.linkTitle = linkTitle;
    }

    public String getLinkHref() {
        return linkHref;
    }

    public void setLinkHref(String linkHref) {
        this.linkHref = linkHref;
    }

    public Integer getLinkNewWindow() {
        return linkNewWindow;
    }

    public void setLinkNewWindow(Integer linkNewWindow) {
        this.linkNewWindow = linkNewWindow;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
