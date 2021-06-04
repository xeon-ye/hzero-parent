package io.choerodon.core.swagger;

/**
 * @author wuguokai
 */
public class ChoerodonRouteData {

    private Long id;

    private String name;

    private String path;

    private String serviceId;

    private String url;

    private Boolean stripPrefix = true;

    private Boolean retryable;

    private String sensitiveHeaders;

    private Boolean customSensitiveHeaders = true;

    private String helperService;

    private Boolean builtIn;

    /**
     * 必须指定，表示属于该服务的包路径
     */
    private String packages;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getStripPrefix() {
        return stripPrefix;
    }

    public void setStripPrefix(Boolean stripPrefix) {
        this.stripPrefix = stripPrefix;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public String getSensitiveHeaders() {
        return sensitiveHeaders;
    }

    public void setSensitiveHeaders(String sensitiveHeaders) {
        this.sensitiveHeaders = sensitiveHeaders;
    }

    public Boolean getCustomSensitiveHeaders() {
        return customSensitiveHeaders;
    }

    public void setCustomSensitiveHeaders(Boolean customSensitiveHeaders) {
        this.customSensitiveHeaders = customSensitiveHeaders;
    }

    public String getHelperService() {
        return helperService;
    }

    public void setHelperService(String helperService) {
        this.helperService = helperService;
    }

    public Boolean getBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(Boolean builtIn) {
        this.builtIn = builtIn;
    }

    public String getPackages() {
        return packages;
    }

    public void setPackages(String packages) {
        this.packages = packages;
    }

    @Override
    public String toString() {
        return "ChoerodonRouteData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", url='" + url + '\'' +
                ", stripPrefix=" + stripPrefix +
                ", retryable=" + retryable +
                ", sensitiveHeaders='" + sensitiveHeaders + '\'' +
                ", customSensitiveHeaders=" + customSensitiveHeaders +
                ", helperService='" + helperService + '\'' +
                ", builtIn=" + builtIn +
                ", packages='" + packages + '\'' +
                '}';
    }
}
