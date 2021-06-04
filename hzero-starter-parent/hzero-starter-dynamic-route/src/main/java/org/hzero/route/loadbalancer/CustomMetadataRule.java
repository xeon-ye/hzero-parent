package org.hzero.route.loadbalancer;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import io.choerodon.core.exception.CommonException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.variable.RequestVariableHolder;
import org.hzero.route.DynamicRouteProperties;
import org.hzero.route.metadata.parser.MetadataParser;
import org.hzero.route.rule.repository.NodeGroupRepository;
import org.hzero.route.rule.vo.NodeGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

import static org.hzero.route.constant.RouteConstants.META_NODE_GROUP_RULE;


/**
 * 根据标签和权重选择目标server <p></p>
 * 加入根据规则节点组规则寻找节点
 * 
 * @author crock
 * @author bojiangzhou
 */
public class CustomMetadataRule extends ZoneAvoidanceRule {
    private static final String META_DATA_KEY_LABEL = "GROUP";
    private static final String META_DATA_KEY_WEIGHT = "WEIGHT";
    private static final String LABEL_SPLIT = ",";

    private Random random = new Random();

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomMetadataRule.class);

    @Autowired
    private NodeGroupRepository nodeGroupRepository;
    @Autowired
    private DynamicRouteProperties properties;
    @Autowired
    private MetadataParser metadataParser;

    @Override
    public Server choose(Object key) {
        List<String> labels = getSourceLabel();
        ILoadBalancer balancer = getLoadBalancer();

        // 选择节点组
        List<Server> servers = chooseNodeServer(balancer, key);

        // 根据权重选择Server
        Map<Server, Integer> maxLabelServers = new HashMap<>(8);
        int maxLabelNumber = -1;
        int totalWeight = 0;
        TreeSet<String> labelSet = new TreeSet<>();
        for (Server server : servers) {
            int weight = getTargetWeight(extractMetadata(server));
            List<String> targetLabels = getTargetLabel(extractMetadata(server));
            if (labels.isEmpty() && targetLabels.isEmpty()) {
                return server;
            }
            labelSet.addAll(labels);
            labelSet.retainAll(targetLabels);
            int labelNumber = labelSet.size();
            if (labelNumber > maxLabelNumber) {
                maxLabelServers.clear();
                maxLabelServers.put(server, weight);
                maxLabelNumber = labelNumber;
                totalWeight = weight;
            } else if (labelNumber == maxLabelNumber) {
                maxLabelServers.put(server, weight);
                totalWeight += weight;
            }
        }
        if (maxLabelServers.isEmpty()) {
            return null;
        }
        int randomWight = random.nextInt(totalWeight);
        int current = 0;
        for (Map.Entry<Server, Integer> entry : maxLabelServers.entrySet()) {
            current += entry.getValue();
            if (randomWight <= current) {
                return entry.getKey();
            }
        }
        return null;
    }
    public List<Server> chooseNodeServer(ILoadBalancer balancer, Object key) {
        List<Server> allServerList = this.getPredicate().getEligibleServers(balancer.getAllServers(), key);
        List<Server> availableNodeServerList;

        if (!HystrixRequestContext.isCurrentThreadInitialized()) {
            HystrixRequestContext.initializeContext();
        }
        Long tenantId = RequestVariableHolder.TENANT_ID.get();
        Long userId = RequestVariableHolder.USER_ID.get();

        String serviceName = ((DynamicServerListLoadBalancer) balancer).getName().toLowerCase();
        if (properties.enableDebugLogger()) {
            LOGGER.debug(">>>>> choose server before: serviceName={}, allServerList={}", serviceName, allServerList.toString());
        }

        if (tenantId == null) {
            if (properties.enableDebugLogger()) {
                LOGGER.debug(">>>>> tenantId is null, directly choose common node");
                return getAvailableServerList(allServerList, null);
            }
        }

        // URL 的规则优先级最高
        Set<String> nodeGroupRules = RouteRequestVariable.NODE_GROUP_ID.get();

        // 然后用户规则、最后是租户规则
        if (nodeGroupRules.isEmpty()) {
            NodeGroup nodeGroup = new NodeGroup();
            nodeGroup
                    .setServiceName(serviceName)
                    .setTenantId(tenantId)
                    .setUserId(userId);
            nodeGroupRules.addAll(nodeGroupRepository.getNodeGroupId(nodeGroup));
        }

        // 规则排序
        List<String> sortedNodeGroupRules = nodeGroupRules.stream()
                .map(rule -> rule.split(BaseConstants.Symbol.SIGH))
                .sorted(Comparator.comparingInt(arr -> Integer.parseInt(arr[1])))
                .map(arr -> arr[0])
                .collect(Collectors.toList());

        if (properties.enableDebugLogger()) {
            LOGGER.debug(">>>>> choose server info, serviceName={}, nodeGroupRules={}, sortedNodeGroupRules={}", serviceName,
                    nodeGroupRules.toString(), sortedNodeGroupRules.toString());
        }

        // 获取可访问节点
        availableNodeServerList = getAvailableServerList(allServerList, sortedNodeGroupRules);

        if (properties.enableDebugLogger()) {
            LOGGER.debug(">>>>> choose server after: serviceName={}, availableNodeServerList={}", serviceName, availableNodeServerList.toString());
        }
        if (CollectionUtils.isEmpty(availableNodeServerList)) {
            String message = String.format(">>>>> server node not found: serviceName=%s,nodeGroupRule=%s,tenantId=%s,userId=%s",
                    serviceName, nodeGroupRules.toString(), String.valueOf(tenantId), String.valueOf(userId) );
            LOGGER.error(message);
            throw new CommonException(message);
        }
        return availableNodeServerList;
    }

    private List<Server> getAvailableServerList(List<Server> allServerList, List<String> nodeGroupRules) {
        Map<String, String> map;

        List<Server> commonServers = new ArrayList<>();
        // 如果规则为空，则取通用节点
        if (CollectionUtils.isEmpty(nodeGroupRules)) {
            for (Server svr : allServerList) {
                map = extractMetadata(svr);
                if (properties.enableDebugLogger()) {
                    LOGGER.debug(">>>>> server meta info: {}", new HashMap<>(map));
                }
                String serverRules = map.get(META_NODE_GROUP_RULE);
                // 节点上没有规则就认为是通用节点
                if (StringUtils.isBlank(serverRules)) {
                    commonServers.add(svr);
                }
            }
            return commonServers;
        }

        // 全大写
        nodeGroupRules = nodeGroupRules.stream().map(String::trim).map(String::toUpperCase).collect(Collectors.toList());
        Map<String, List<Server>> ruleServers = new HashMap<>(nodeGroupRules.size());

        for (Server svr : allServerList) {
            map = extractMetadata(svr);
            if (properties.enableDebugLogger()) {
                LOGGER.debug(">>>>> server meta info: {}", new HashMap<>(map));
            }
            String serverRules = map.get(META_NODE_GROUP_RULE);
            if (StringUtils.isNotBlank(serverRules)) {
                Set<String> rules = Arrays.stream(serverRules.split(",")).map(String::trim).map(String::toUpperCase).collect(Collectors.toSet());
                for (String nodeGroupRule : nodeGroupRules) {
                    if (rules.contains(nodeGroupRule)) {
                        ruleServers.putIfAbsent(nodeGroupRule, new ArrayList<>());
                        ruleServers.get(nodeGroupRule).add(svr);
                        break;
                    }
                }
            }
        }

        if (properties.enableDebugLogger()) {
            for (String nodeGroupRule : nodeGroupRules) {
                LOGGER.debug(">>>>> choose server for rule, nodeGroupRule={}, corresponding serverList={}", nodeGroupRule, ruleServers.get(nodeGroupRule));
            }
        }

        for (String nodeGroupRule : nodeGroupRules) {
            // 按规则先后取
            if (CollectionUtils.isNotEmpty(ruleServers.get(nodeGroupRule))) {
                return ruleServers.get(nodeGroupRule);
            }
        }

        return Collections.emptyList();
    }

    private List<String> getSourceLabel() {
        String sourceLabel = null;
        if (HystrixRequestContext.isCurrentThreadInitialized()) {
            sourceLabel = RequestVariableHolder.LABEL.get();
        }
        List<String> labels = Collections.emptyList();
        if (sourceLabel != null) {
            labels = Arrays.asList(sourceLabel.split(LABEL_SPLIT));
        }
        return labels;
    }

    private int getTargetWeight(Map<String, String> metadata) {
        String weightString = metadata.get(META_DATA_KEY_WEIGHT);
        int weight = 100;
        if (weightString != null) {
            weight = Integer.parseInt(weightString);
        }
        return weight;
    }

    private List<String> getTargetLabel(Map<String, String> metadata) {
        String label = metadata.get(META_DATA_KEY_LABEL);
        List<String> targetLabels = Collections.emptyList();
        if (label != null) {
            targetLabels = Arrays.asList(label.split(LABEL_SPLIT));
        }
        return targetLabels;
    }

    private Map<String,String> extractMetadata(Server server) {
        return metadataParser.parse(server);
    }

}
