package org.hzero.route.rule.handler;

import org.apache.commons.lang3.StringUtils;
import org.hzero.route.rule.repository.TenantUrlRepository;
import org.hzero.route.rule.repository.UrlMappingRepository;
import org.hzero.route.rule.vo.TenantUrl;
import org.hzero.route.rule.vo.UrlMappingCacheVO;

import java.util.List;

/**
 * @author 11838
 */
public class TargetServiceUrlContext {

    private Long tenantId;

    private Long userId;

    private String sourceService;

    private String sourceUrl;

    private String targetService;

    private String targetUrl;

    public TargetServiceUrlContext(Long tenantId, Long userId, String sourceService, String sourceUrl) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.sourceService = sourceService;
        this.sourceUrl = sourceUrl;
        this.targetService = sourceService;
        this.targetUrl = sourceUrl;
    }

    public String getTargetService() {
        return targetService;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public void setTargetServiceUrl(UrlMappingRepository urlMappingRepository) {

        // 获取缓存
        List<UrlMappingCacheVO> urlMappingCacheVOList = urlMappingRepository.getUrlMapping(userId, tenantId);

        /*
         * 动态路由、url
         */
        urlMappingRepository.getTargetServiceUrl(this, urlMappingCacheVOList);

    }

    /**
     * 目标url前缀
     *
     * @param tenantUrlRepository tenantUrlRepository
     * @param method              method
     */
    public void setUrlPrefix(TenantUrlRepository tenantUrlRepository, String method) {
        TenantUrl tenantUrl = new TenantUrl();
        tenantUrl.setTenantId(tenantId)
                .setServiceName(targetService)
                .setUrl(targetUrl)
                .setMethod(method);
        String urlPrefix = tenantUrlRepository.getTenantCustomUrlPrefix(tenantUrl);
        if (StringUtils.isNotEmpty(urlPrefix)) {
            targetUrl = tenantUrlRepository.insertPath(targetUrl, urlPrefix, 0);
        }
    }
}
