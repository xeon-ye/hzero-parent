package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.customize.constant.ModelConstant;
import org.hzero.boot.customize.dto.ModelFieldWdgMetaData;
import org.hzero.boot.customize.util.CustomizeUtils;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.hzero.boot.platform.lov.dto.LovValueDTO;
import org.hzero.boot.platform.lov.dto.LovViewDTO;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : peng.yu01@hand-china.com 2019/12/15 13:06
 */
@ModifyAudit
/*@VersionAudit*/
@Table(name = "hpfm_cusz_config_field_wdg")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ConfigFieldWidget extends AuditDomain {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_CONFIG_FIELD_ID = "configFieldId";
    public static final String FIELD_FIELD_WIDGET = "fieldWidget";
    public static final String FIELD_TEXT_MAX_LENGTH = "textMaxLength";
    public static final String FIELD_TEXT_MIN_LENGTH = "textMinLength";
    public static final String FIELD_TEXT_AREA_MAX_LINE = "textAreaMaxLine";
    public static final String FIELD_SOURCE_CODE = "sourceCode";
    public static final String FIELD_DATE_FORMAT = "dateFormat";
    public static final String FIELD_NUMBER_DECIMAL = "numberDecimal";
    public static final String FIELD_NUMBER_MIN = "numberMin";
    public static final String FIELD_NUMBER_MAX = "numberMax";
    public static final String FIELD_BUCKET_NAME = "bucketName";
    public static final String FIELD_BUCKET_DIRECTORY = "bucketDirectory";
    public static final String FIELD_LINK_TITLE = "linkTitle";
    public static final String FIELD_LINK_HREF = "linkHref";
    public static final String FIELD_LINK_NEW_WINDOW = "linkNewWindow";
    public static final String FIELD_DEFAULT_VALUE = "defaultValue";
    public static final String FIELD_MULTIPLE_FLAG = "multipleFlag";

    public static final String[] UPDATE_FIELD_COMMON_LIST = {FIELD_FIELD_WIDGET, FIELD_TEXT_MAX_LENGTH, FIELD_TEXT_AREA_MAX_LINE, FIELD_SOURCE_CODE, FIELD_DATE_FORMAT, FIELD_NUMBER_DECIMAL,
            FIELD_NUMBER_MIN, FIELD_NUMBER_MAX, FIELD_BUCKET_NAME, FIELD_BUCKET_DIRECTORY, FIELD_TEXT_MIN_LENGTH, FIELD_LINK_TITLE, FIELD_LINK_HREF, FIELD_LINK_NEW_WINDOW, FIELD_DEFAULT_VALUE, FIELD_MULTIPLE_FLAG};

    @Id
    @GeneratedValue
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_CONFIG_FIELD_WDG)
    private Long id;
    private Long tenantId;
    /**
     * config_field表id
     */
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_CONFIG_FIELD)
    private Long configFieldId;
    /**
     * 字段控件类型
     */
    private String fieldWidget;
    /**
     * TEXT类型 最大长度
     */
    @Column(name = "text_max_length")
    private Integer textMaxLength;
    /**
     * TEXT类型 最小长度
     */
    @Column(name = "text_min_length")
    private Integer textMinLength;
    /**
     * 文本域组件 最大行数
     */
    @Column(name = "text_area_max_line")
    private Integer textAreaMaxLine;
    /**
     * LOV类型 值集编码
     */
    @Column(name = "source_code")
    private String sourceCode;
    /**
     * DATE类型 日期格式
     */
    @Column(name = "date_format")
    private String dateFormat;
    /**
     * NUMBER类型 数值精度
     */
    @Column(name = "number_precision")
    private Integer numberDecimal;
    /**
     * NUMBER类型 数值最小值
     */
    @Column(name = "number_min")
    private Long numberMin;
    /**
     * NUMBER类型 数值最大值
     */
    @Column(name = "number_max")
    private Long numberMax;
    /**
     * 桶名
     */
    @Column(name = "bucket_name")
    private String bucketName;
    /**
     * 桶目录
     */
    @Column(name = "bucket_directory")
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

    /**
     * 组件默认值翻译
     */
    private Object defaultValueMeaning;

    /**
     * 多选组件标记
     */
    private Integer multipleFlag;

    public ConfigFieldWidget() {
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

    public Long getConfigFieldId() {
        return configFieldId;
    }

    public void setConfigFieldId(Long configFieldId) {
        this.configFieldId = configFieldId;
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

    public Integer getMultipleFlag() {
        return multipleFlag;
    }

    public void setMultipleFlag(Integer multipleFlag) {
        this.multipleFlag = multipleFlag;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }


    public ConfigFieldWidget cacheConvert() {
        ConfigFieldWidget widget = new ConfigFieldWidget();

        BeanUtils.copyProperties(this, widget, FIELD_TENANT_ID, FIELD_CONFIG_FIELD_ID, FIELD_CREATION_DATE, FIELD_CREATED_BY, FIELD_LAST_UPDATE_DATE,
                FIELD_LAST_UPDATED_BY, FIELD_OBJECT_VERSION_NUMBER, "_token");

        return widget;
    }

    public Object getDefaultValueMeaning() {
        return defaultValueMeaning;
    }

    public void setDefaultValueMeaning(Object defaultValueMeaning) {
        this.defaultValueMeaning = defaultValueMeaning;
    }

    public void translateLov(LovAdapter lovAdapter, Long tenantId) {
        String lovCode = null;
        if (StringUtils.isBlank(getSourceCode()) || StringUtils.isBlank(getDefaultValue())) {
            return;
        }

        if (ModelConstant.WidgetType.LOV.equals(getFieldWidget())) {
            LovViewDTO lovViewDTO = lovAdapter.queryLovViewInfo(getSourceCode(), tenantId);
            lovCode = lovViewDTO.getLovCode();
        }
        if (ModelConstant.WidgetType.SELECT.equals(getFieldWidget())) {
            lovCode = getSourceCode();
        }

        if (StringUtils.isNotBlank(lovCode) && StringUtils.isNotBlank(getDefaultValue()) && StringUtils.isNotBlank(getSourceCode())) {
            this.setDefaultValueMeaning(this.getDefaultValue());
            List<String> param = new ArrayList<>(1);
            param.addAll(CustomizeUtils.subList(new ArrayList<>(
                    org.springframework.util.StringUtils.commaDelimitedListToSet(this.getDefaultValue())), 5));
            List<LovValueDTO> result;
            try {
                result = lovAdapter.queryLovValue(lovCode, tenantId, param);
            } catch (Exception e) {
                //ignore
                result = new ArrayList<>();
            }
            //多选组件，翻译多个值
            Map<String, String> translateMap = result.stream().filter(t -> StringUtils.isNotBlank(t.getValue())).collect(Collectors.toMap(LovValueDTO::getValue, LovValueDTO::getMeaning));
            //单选组件
            if (ObjectUtils.notEqual(this.getMultipleFlag(), 1)) {
                if (!CollectionUtils.isEmpty(result)) {
                    if (translateMap.containsKey(getDefaultValue())) {
                        this.setDefaultValueMeaning(translateMap.get(getDefaultValue()));
                    }
                }
            } else {
                //如果-1，默认全选，不翻译
                if ("-1".equals(this.getDefaultValue())) {
                    return;
                }
                Map<String, String> translateResult = new HashMap<>(param.size());
                param.forEach(t -> translateResult.put(t, translateMap.getOrDefault(t, t)));
                this.setDefaultValueMeaning(translateResult);
            }
        }
    }

    public static ConfigFieldWidget readFromModelWdg(ModelFieldWdgMetaData modelFieldWdgMetaData) {
        ConfigFieldWidget configFieldWidget = new ConfigFieldWidget();
        configFieldWidget.setFieldWidget(modelFieldWdgMetaData.getFieldWidget());
        configFieldWidget.setSourceCode(modelFieldWdgMetaData.getSourceCode());
        return configFieldWidget;
    }
}
