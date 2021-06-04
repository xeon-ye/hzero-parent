package org.hzero.route.rule.vo;

import org.hzero.route.constant.RouteConstants;

/**
 * 节点组
 *
 * @author bojiangzhou 2018/09/28
 */
public class NodeGroup {

    private Long nodeGroupId;
    private Long tenantId;
    private String serviceName;
    private Long userId;

    /**
     * 获取租户用户节点key
     * @return nodegroup:user-node:{serviceName}:{tenantId}
     */
    public String getUserNodeKey() {
        return String.format(RouteConstants.Admin.CODE +":nodegroup:user-node:%s", tenantId);
    }

    /**
     * 获取租户节点key
     * @return nodegroup:tenant-node:{serviceName}
     */
    public String getTenantNodeKey() {
        return RouteConstants.Admin.CODE +":nodegroup:tenant-node";
    }

    public Long getNodeGroupId() {
        return nodeGroupId;
    }

    public NodeGroup setNodeGroupId(Long nodeGroupId) {
        this.nodeGroupId = nodeGroupId;
        return this;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public NodeGroup setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public String getServiceName() {
        return serviceName;
    }

    public NodeGroup setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public NodeGroup setUserId(Long userId) {
        this.userId = userId;
        return this;
    }
}
