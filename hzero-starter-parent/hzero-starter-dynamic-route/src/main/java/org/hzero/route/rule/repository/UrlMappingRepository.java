package org.hzero.route.rule.repository;

import org.hzero.route.rule.handler.TargetServiceUrlContext;
import org.hzero.route.rule.vo.UrlMappingCacheVO;

import java.util.List;
import java.util.Map;

/**
 * @author 11838
 */
public interface UrlMappingRepository {

    /**
     * 获取url映射对象
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return UrlMappingVO
     */
    List<UrlMappingCacheVO> getUrlMapping(Long userId, Long tenantId);

    /**
     * 获取目标服务、url
     *
     * @param context     context
     * @param urlMappings 映射关系
     */
    void getTargetServiceUrl(TargetServiceUrlContext context, List<UrlMappingCacheVO> urlMappings);

}
