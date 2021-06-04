package org.hzero.sso.core.domain.entity;

import java.io.Serializable;
import java.util.StringJoiner;

/**
 * 域名配置
 *
 * @author minghui.qiu@hand-china.com 2019-06-27 20:50:16
 */
public class Domain implements Serializable {
    private static final long serialVersionUID = -6625864894934365970L;

    private Long domainId;
    private String domainUrl;
    private String ssoTypeCode;
    private String ssoServerUrl;
    private String ssoLoginUrl;
    private String ssoLogoutUrl;
    private String clientHostUrl;
    private String ssoClientId;
    private String ssoClientPwd;
    private String ssoUserInfo;
    private String samlMetaUrl;
    private Long tenantId;
    private String tenantNum;
    private Long companyId;
    private String loginNameField;

    /**
     * @return
     */
    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }


    /**
     * @return 域名
     */
    public String getDomainUrl() {
        return domainUrl;
    }

    public void setDomainUrl(String domainUrl) {
        this.domainUrl = domainUrl;
    }

    /**
     * @return CAS|CAS2|SAML|IDM|NULL
     */
    public String getSsoTypeCode() {
        return ssoTypeCode;
    }

    public void setSsoTypeCode(String ssoTypeCode) {
        this.ssoTypeCode = ssoTypeCode;
    }

    /**
     * @return 单点认证服务器地址
     */
    public String getSsoServerUrl() {
        return ssoServerUrl;
    }

    public void setSsoServerUrl(String ssoServerUrl) {
        this.ssoServerUrl = ssoServerUrl;
    }

    /**
     * @return 单点登录地址
     */
    public String getSsoLoginUrl() {
        return ssoLoginUrl;
    }

    public void setSsoLoginUrl(String ssoLoginUrl) {
        this.ssoLoginUrl = ssoLoginUrl;
    }

    /**
     * @return 客户端URL
     */
    public String getClientHostUrl() {
        return clientHostUrl;
    }

    public void setClientHostUrl(String clientHostUrl) {
        this.clientHostUrl = clientHostUrl;
    }

    public String getSsoClientId() {
        return ssoClientId;
    }

    public void setSsoClientId(String ssoClientId) {
        this.ssoClientId = ssoClientId;
    }

    public String getSsoClientPwd() {
        return ssoClientPwd;
    }

    public void setSsoClientPwd(String ssoClientPwd) {
        this.ssoClientPwd = ssoClientPwd;
    }

    public String getSsoUserInfo() {
        return ssoUserInfo;
    }

    public void setSsoUserInfo(String ssoUserInfo) {
        this.ssoUserInfo = ssoUserInfo;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Domain.class.getSimpleName() + "[", "]")
                .add("domainId=" + domainId)
                .add("domainUrl='" + domainUrl + "'")
                .add("ssoTypeCode='" + ssoTypeCode + "'")
                .add("ssoServerUrl='" + ssoServerUrl + "'")
                .add("ssoLoginUrl='" + ssoLoginUrl + "'")
                .add("clientHostUrl='" + clientHostUrl + "'")
                .add("ssoClientId='" + ssoClientId + "'")
                .add("ssoClientPwd='" + ssoClientPwd + "'")
                .add("ssoUserInfo='" + ssoUserInfo + "'")
                .add("tenantNum='" + tenantNum + "'")
                .add("loginNameField='" + loginNameField + "'")
                .toString();
    }

    public Long getTenantId() {
      return tenantId;
    }

    public void setTenantId(Long tenantId) {
      this.tenantId = tenantId;
    }

    public Long getCompanyId() {
      return companyId;
    }

    public void setCompanyId(Long companyId) {
      this.companyId = companyId;
    }

    public String getTenantNum() {
        return tenantNum;
    }

    public void setTenantNum(String tenantNum) {
        this.tenantNum = tenantNum;
    }

    public String getSsoLogoutUrl() {
      return ssoLogoutUrl;
    }

    public void setSsoLogoutUrl(String ssoLogoutUrl) {
      this.ssoLogoutUrl = ssoLogoutUrl;
    }

    public String getSamlMetaUrl() {
      return samlMetaUrl;
    }

    public void setSamlMetaUrl(String samlMetaUrl) {
      this.samlMetaUrl = samlMetaUrl;
    }

    public String getLoginNameField() {
        return loginNameField;
    }

    public void setLoginNameField(String loginNameField) {
        this.loginNameField = loginNameField;
    }
}
