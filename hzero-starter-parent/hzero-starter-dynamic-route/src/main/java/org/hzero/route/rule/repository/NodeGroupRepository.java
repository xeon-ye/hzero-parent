package org.hzero.route.rule.repository;

import org.hzero.route.rule.vo.NodeGroup;

import java.util.Set;

/**
 * 获取节点组
 *
 * @author bojiangzhou 2018/09/28
 */
public interface NodeGroupRepository {

    /**
     * 判断节点是否存在
     *
     * @param nodeGroup 节点组
     * @return String 节点组ID
     */
    Set<String> getNodeGroupId(NodeGroup nodeGroup);
}
