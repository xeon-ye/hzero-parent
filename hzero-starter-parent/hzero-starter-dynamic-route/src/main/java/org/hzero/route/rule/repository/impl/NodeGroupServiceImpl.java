package org.hzero.route.rule.repository.impl;

import org.apache.commons.lang3.StringUtils;
import org.hzero.core.redis.RedisHelper;
import org.hzero.route.constant.RouteConstants;
import org.hzero.route.rule.repository.NodeGroupRepository;
import org.hzero.route.rule.vo.NodeGroup;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bojiangzhou 2018/09/28
 */
public class NodeGroupServiceImpl implements NodeGroupRepository {

    private RedisHelper redisHelper;

    public NodeGroupServiceImpl(RedisHelper redisHelper) {
        this.redisHelper = redisHelper;
    }

    @Override
    public Set<String> getNodeGroupId(NodeGroup nodeGroup) {
        redisHelper.setCurrentDatabase(RouteConstants.Admin.REDIS_DB);
        try {
            // 优先取用户规则
            if (nodeGroup.getUserId() != null) {
                String userNodeKey = nodeGroup.getUserNodeKey();
                String rules = redisHelper.hshGet(userNodeKey, nodeGroup.getUserId().toString());
                if (StringUtils.isNotBlank(rules)) {
                    return Stream.of(rules.split(",")).collect(Collectors.toSet());
                }
            }
            // 再取租户规则
            if (nodeGroup.getTenantId() != null) {
                String tenantNodeKey = nodeGroup.getTenantNodeKey();
                String rules = redisHelper.hshGet(tenantNodeKey, nodeGroup.getTenantId().toString());
                if (StringUtils.isNotBlank(rules)) {
                    return Stream.of(rules.split(",")).collect(Collectors.toSet());
                }
            }
        } finally {
            redisHelper.clearCurrentDatabase();
        }

        return Collections.emptySet();
    }

}
