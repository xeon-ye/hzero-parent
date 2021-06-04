package org.hzero.route.rule.vo;


import org.hzero.route.constant.RouteConstants;

/**
 * 租户定制化URL
 *
 * @author bojiangzhou 2018/09/28
 */
public class TenantUrl {

    private String serviceName;
    private String url;
    private String method;
    private Long tenantId;
    private String urlPrefix;

    /**
     * 获取客户化URL的缓存KEY
     * @return nodegroup:url-node:{serviceName}:{tenantId}
     */
    public String getUrlNodeKey() {
        return String.format(RouteConstants.Admin.CODE + ":nodegroup:url-node:%s:%s", serviceName, tenantId);
    }

    /**
     * 获取客制化URL
     */
    public String getCustomUrlPrefix() {
        return urlPrefix;
    }

    public String getUrl() {
        return url;
    }

    public TenantUrl setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public TenantUrl setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getServiceName() {
        return serviceName;
    }

    public TenantUrl setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public TenantUrl setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public TenantUrl setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
        return this;
    }

    @Override
    public String toString() {
        return "TenantUrl{" +
                "serviceName='" + serviceName + '\'' +
                ", url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", tenantId=" + tenantId +
                ", urlPrefix='" + urlPrefix + '\'' +
                '}';
    }
}
