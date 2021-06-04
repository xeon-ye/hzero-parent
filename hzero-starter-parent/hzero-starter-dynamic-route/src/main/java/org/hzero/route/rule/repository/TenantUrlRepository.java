package org.hzero.route.rule.repository;

import org.hzero.route.rule.vo.TenantUrl;

/**
 * 获取客户化URL
 *
 * @author bojiangzhou 2018/09/28
 */
public interface TenantUrlRepository {

    /**
     * 获取URL客制化的前缀
     *
     * @param tenantUrl URL
     * @return String Custom URL prefix
     */
    String getTenantCustomUrlPrefix(TenantUrl tenantUrl);

    /**
     * url加前缀
     * @param path  url路径
     * @param urlPrefix url前缀
     * @param index  下标
     * @return 加完前缀后的url
     */
    String insertPath(String path, String urlPrefix, int index);
}
