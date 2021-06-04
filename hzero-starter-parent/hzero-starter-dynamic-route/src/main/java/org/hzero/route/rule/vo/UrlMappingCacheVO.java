package org.hzero.route.rule.vo;

/**
 * @author 11838
 */
public class UrlMappingCacheVO {

    private String sourceService;

    private String targetService;

    private String sourceUrl;

    private String targetUrl;

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public String getTargetService() {
        return targetService;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    @Override
    public String toString() {
        return "UrlMappingVO{" +
                "sourceService='" + sourceService + '\'' +
                ", targetService='" + targetService + '\'' +
                ", sourceUrl='" + sourceUrl + '\'' +
                ", targetUrl='" + targetUrl + '\'' +
                '}';
    }
}
