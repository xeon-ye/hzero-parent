package org.hzero.admin.infra.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.StringUtils;
import org.hzero.admin.api.dto.swagger.*;
import org.hzero.admin.config.ConfigProperties;
import org.hzero.admin.domain.entity.ServiceRoute;
import org.hzero.admin.domain.entity.Swagger;
import org.hzero.admin.domain.repository.ServiceRouteRepository;
import org.hzero.admin.domain.repository.SwaggerRepository;
import org.hzero.admin.infra.mapper.SwaggerMapper;
import org.hzero.admin.infra.util.MyLinkedList;
import org.hzero.admin.infra.util.VersionUtil;
import org.hzero.common.HZeroService;
import org.hzero.core.base.BaseConstants;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.swagger.web.SwaggerResource;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.swagger.PermissionData;
import io.choerodon.core.swagger.SwaggerExtraData;

/**
 * ???????????????
 *
 * @author bo.he02@hand-china.com 2020-05-09 11:00:41
 */
@Component
public class SwaggerRepositoryImpl extends BaseRepositoryImpl<Swagger> implements SwaggerRepository {
    /**
     * ??????????????????
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerRepositoryImpl.class);

    private static final String DESCRIPTION = "description";
    private static final String TITLE = "title";
    private static final String KEY = "key";
    private static final String CHILDREN = "children";
    private static final String API_TREE_DOC = "api-tree-doc";
    private static final String PATH_DETAIL = "swagger:path-detail";
    private static final String COLON = BaseConstants.Symbol.COLON;
    private static final String UNDERLINE = BaseConstants.Symbol.MIDDLE_LINE;
    private static final String SERVICE = "service";
    private static final String PATHS = "paths";
    private static final String OPERATION_ID = "operationId";
    private static final String ARRAY = "array";
    private static final String OBJECT = "object";
    private static final String CONTEXT = "CONTEXT";
    private static final String API_DOC_URL = "/v2/choerodon/api-docs";
    private static final String LOCATION_SPLIT_REGX = "\\?version=";
    private static final String REF_CONTROLLER = "refController";
    private static final String METHOD = "method";
    private static final String VERSION = "version";
    private static final String SUMMARY = "summary";
    private static final String SERVICE_PREFIX = "servicePrefix";
    private static final String TAGS = "tags";
    private static final String NAME = "name";
    private static final String SUFFIX_CONTROLLER = "-controller";
    private static final String SUFFIX_ENDPOINT = "-endpoint";
    private static final String BASE_PATH = "basePath";
    private static final String DEFINITIONS = "definitions";
    private static final String PROPERTIES = "properties";
    private static final String TYPE = "type";
    private static final String VAR_REF = "$ref";
    private static final String ITEMS = "items";
    private static final String RESPONSES = "responses";
    private static final String SCHEMA = "schema";
    private static final String CONSUMES = "consumes";
    private static final String PRODUCES = "produces";
    private static final String PARAMETERS = "parameters";
    private static final String BODY = "body";
    private static final String INTEGER = "integer";
    private static final String STRING = "string";
    private static final String BOOLEAN = "boolean";

    /**
     * {{name}}:{{serviceId}}
     */
    private static final String SWAGGER_RESOURCE_NAME_TEMPLATE = "%s:%s";
    private static final String SWAGGER_RESOURCE_VERSION = "2.0";
    /**
     * {{name}}:{{version}}
     */
    private static final String SWAGGER_RESOURCE_LOCATION_TEMPLATE = "/docs/%s?version=%s";

    /**
     * ????????????
     */
    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * swagger???mapper??????
     */
    private final SwaggerMapper swaggerMapper;
    /**
     * RestTemplate??????
     */
    private final RestTemplate restTemplate;
    /**
     * RedisTemplate??????
     */
    private final StringRedisTemplate redisTemplate;
    /**
     * ??????????????????
     */
    private final ObjectMapper objectMapper;
    /**
     * DiscoveryClient??????
     */
    private final DiscoveryClient discoveryClient;
    /**
     * ??????????????????
     */
    private final ConfigProperties configProperties;
    /**
     * ???????????????????????????
     */
    private final ServiceRouteRepository serviceRouteRepository;

    /**
     * ????????????
     */
    private final Cache<String, String> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.DAYS).maximumSize(500).build();

    @Autowired
    public SwaggerRepositoryImpl(SwaggerMapper swaggerMapper,
                                 RestTemplate restTemplate,
                                 StringRedisTemplate redisTemplate,
                                 ObjectMapper objectMapper,
                                 DiscoveryClient discoveryClient,
                                 ConfigProperties configProperties,
                                 ServiceRouteRepository serviceRouteRepository) {
        this.swaggerMapper = swaggerMapper;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.discoveryClient = discoveryClient;
        this.configProperties = configProperties;
        this.serviceRouteRepository = serviceRouteRepository;
    }

    @Override
    public String fetchSwaggerJsonByService(String service, String version) {
        Swagger query = new Swagger();
        query.setServiceName(service);
        query.setServiceVersion(version);
        Swagger data = this.swaggerMapper.selectOne(query);
        if (data == null || StringUtils.isEmpty(data.getValue())) {
            String json = this.fetchFromDiscoveryClient(service, version);
            if (json != null && data == null) {
                //insert
                Swagger insertSwagger = new Swagger();
                insertSwagger.setServiceName(service);
                insertSwagger.setServiceVersion(version);
                insertSwagger.setValue(json);
                if (this.swaggerMapper.insertSelective(insertSwagger) != 1) {
                    LOGGER.warn("insert swagger error, swagger : {}", insertSwagger);
                }
            } else if (json != null && StringUtils.isEmpty(data.getValue())) {
                //update
                query.setValue(json);
                if (this.swaggerMapper.updateByPrimaryKeySelective(query) != 1) {
                    LOGGER.warn("update swagger error, swagger : {}", query);
                }
            }
            return json;
        } else {
            return data.getValue();
        }
    }

    @Override
    public List<SwaggerResource> getSwaggerResource() {
        List<SwaggerResource> swaggerResources = this.processSwaggerResource();
        swaggerResources.sort(Comparator.comparing(SwaggerResource::getName));
        return swaggerResources;
    }

    @Override
    public MultiKeyMap<String, Set<String>> getServiceMetaDataMap() {
        MultiKeyMap<String, Set<String>> serviceMap = new MultiKeyMap<>();

        List<SwaggerResource> resources = this.getSwaggerResource();
        for (SwaggerResource resource : resources) {
            String name = resource.getName();
            String[] nameArray = name.split(COLON);
            String location = resource.getLocation();
            String[] locationArray = location.split(LOCATION_SPLIT_REGX);
            if (nameArray.length != 2 || locationArray.length != 2) {
                LOGGER.warn("the resource name is not match xx:xx or location is not match /doc/xx?version=xxx , name : {}, location: {}",
                        name, location);
                continue;
            }
            String routeName = nameArray[0];
            String service = nameArray[1];
            String version = locationArray[1];
            if (!serviceMap.containsKey(routeName, service)) {
                Set<String> versionSet = new HashSet<>();
                versionSet.add(version);
                serviceMap.put(routeName, service, versionSet);
            } else {
                Set<String> versionSet = serviceMap.get(routeName, service);
                versionSet.add(version);
            }
        }

        return serviceMap;
    }

    @Override
    public Map<String, List<Map<String, Object>>> queryTreeMenu() {
        Map<String, List<Map<String, Object>>> treeMenu = new HashMap<>(2);

        MultiKeyMap<String, Set<String>> serviceMetaDataMap = this.getServiceMetaDataMap();
        if (MapUtils.isEmpty(serviceMetaDataMap)) {
            return treeMenu;
        }

        List<Map<String, Object>> serviceApis = new ArrayList<>();
        serviceMetaDataMap.forEach(((multiKey, versions) -> {
            String routeName = multiKey.getKey(0);
            String service = multiKey.getKey(1);

            Map<String, Object> serviceMap = new HashMap<>();
            serviceMap.put(TITLE, service);
            List<Map<String, Object>> children = new ArrayList<>();
            int versionNum = this.processTreeOnVersionNode(routeName, service, versions, children);
            serviceMap.put(CHILDREN, children);
            if (versionNum > 0) {
                serviceApis.add(serviceMap);
            }
        }));

        treeMenu.put(SERVICE, serviceApis);
        this.processKey(treeMenu);
        return treeMenu;
    }

    @Override
    public ControllerDTO queryPathDetail(String serviceName, String version, String controllerName, String operationId) {
        String key = this.getPathDetailRedisKey(serviceName, version, controllerName, operationId);

        // ?????????????????????????????????
        Boolean hasKey = this.redisTemplate.hasKey(key);
        if (Objects.nonNull(hasKey) && Boolean.TRUE.equals(hasKey)) {
            String value = this.redisTemplate.opsForValue().get(key);
            try {
                return objectMapper.readValue(value, ControllerDTO.class);
            } catch (IOException e) {
                LOGGER.error("object mapper read redis cache value {} to ControllerDTO error, so process from db or swagger, exception: {} ",
                        value, e);
            }
        }

        try {
            return this.processPathDetailFromSwagger(serviceName, version, controllerName, operationId, key);
        } catch (IOException e) {
            LOGGER.error("fetch swagger json error, service: {}, version: {}, exception: {}", serviceName, version, e.getMessage());
            throw new CommonException("error.service.not.run", serviceName, version);
        }
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param service ????????????
     * @param version ??????
     * @return ?????????????????????
     */
    private String fetchFromDiscoveryClient(String service, String version) {
        List<ServiceInstance> instances = this.discoveryClient.getInstances(service);
        List<String> mdVersions = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            String mdVersion = instance.getMetadata().get(VersionUtil.METADATA_VERSION);
            mdVersions.add(mdVersion);
            if (StringUtils.isEmpty(mdVersion)) {
                mdVersion = VersionUtil.NULL_VERSION;
            }
            if (version.equals(mdVersion)) {
                return this.fetch(instance);
            }
        }
        LOGGER.warn("service {} running instances {} do not contain the version {} ", service, mdVersions, version);
        return null;
    }

    /**
     * ????????????????????????
     *
     * @param instance ????????????
     * @return ??????????????????
     */
    private String fetch(ServiceInstance instance) {
        ResponseEntity<String> response;
        String contextPath = instance.getMetadata().get(CONTEXT);
        if (contextPath == null) {
            contextPath = "";
        }
        LOGGER.info("service: {} metadata : {}", instance.getServiceId(), instance.getMetadata());
        try {
            response = this.restTemplate.getForEntity(
                    instance.getUri() + contextPath + API_DOC_URL,
                    String.class);
        } catch (RestClientException e) {
            String msg = "fetch failed, instance:" + instance.getServiceId() + ", uri: " + instance.getUri() + ", contextPath: " + contextPath;
            throw new RemoteAccessException(msg);
        } catch (IllegalStateException e) {
            // ????????????????????????????????????????????????????????????,?????????????????????
            return null;
        }
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RemoteAccessException("fetch failed : " + response);
        }
        return response.getBody();
    }

    /**
     * ??????swagger??????
     *
     * @return swagger??????
     */
    private List<SwaggerResource> processSwaggerResource() {
        List<SwaggerResource> resources = new LinkedList<>();
        //key1:????????? key2:?????? value:route
        MultiKeyMap<String, List<ServiceRoute>> allRunningInstances = Optional
                .ofNullable(this.serviceRouteRepository.getAllRunningInstances()).orElse(new MultiKeyMap<>());
        allRunningInstances.forEach(((multiKey, serviceRoutes) -> {
            String serviceId = serviceRoutes.get(0).getServiceCode();
            String name = serviceRoutes.get(0).getPath().replace("/**", "").replace("/", "");
            if (serviceId != null) {
                boolean isSkip = Arrays.stream(this.configProperties.getRoute().getSkipParseServices())
                        .anyMatch(t -> this.matcher.match(t, serviceId));
                if (!isSkip) {
                    SwaggerResource resource = new SwaggerResource();
                    resource.setName(String.format(SWAGGER_RESOURCE_NAME_TEMPLATE, name, serviceId));
                    resource.setSwaggerVersion(SWAGGER_RESOURCE_VERSION);
                    resource.setLocation(String.format(SWAGGER_RESOURCE_LOCATION_TEMPLATE, name, multiKey.getKey(1)));
                    resources.add(resource);
                }
            }
        }));

        return resources;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param routeName ????????????
     * @param service   ????????????
     * @param versions  ????????????
     * @param children  ?????????
     * @return ???????????????
     */
    private int processTreeOnVersionNode(String routeName, String service, Set<String> versions, List<Map<String, Object>> children) {
        int versionNum = versions.size();
        for (String version : versions) {
            boolean legalVersion;
            Map<String, Object> versionMap = new HashMap<>(16);
            versionMap.put(TITLE, version);
            List<Map<String, Object>> versionChildren = new ArrayList<>();
            versionMap.put(CHILDREN, versionChildren);
            String apiTreeDocKey = this.getApiTreeDocKey(service, version);
            if (this.cache.getIfPresent(apiTreeDocKey) != null) {
                String childrenStr = this.cache.getIfPresent(apiTreeDocKey);
                try {
                    List<Map<String, Object>> list = this.objectMapper.readValue(childrenStr,
                            new TypeReference<List<Map<String, Object>>>() {
                            });
                    versionChildren.addAll(list);
                    legalVersion = true;
                } catch (IOException e) {
                    LOGGER.error("object mapper read redis cache value {} to List<Map<String, Object>> error, so process children version from db or swagger, exception: {} ", childrenStr, e);
                    legalVersion = this.processChildrenFromSwaggerJson(routeName, service, version, versionChildren);
                }
            } else {
                legalVersion = this.processChildrenFromSwaggerJson(routeName, service, version, versionChildren);
            }
            if (legalVersion) {
                children.add(versionMap);
            } else {
                versionNum--;
            }
        }
        return versionNum;
    }

    /**
     * ??????key
     *
     * @param map ????????????
     */
    private void processKey(Map<String, List<Map<String, Object>>> map) {
        List<Map<String, Object>> serviceList = map.get(SERVICE);
        int serviceCount = 0;
        for (Map<String, Object> service : serviceList) {
            String serviceKey = serviceCount + "";
            service.put(KEY, serviceKey);
            List<Map<String, Object>> versions = this.getChildren(service);
            this.recursion(serviceKey, versions);
            serviceCount++;
        }
    }

    /**
     * ????????????
     *
     * @param key  key
     * @param list list
     */
    private void recursion(String key, List<Map<String, Object>> list) {
        int count = 0;
        for (Map<String, Object> map : list) {
            String mapKey = key + UNDERLINE + count;
            map.put(KEY, mapKey);
            if (map.get(CHILDREN) != null) {
                List<Map<String, Object>> children = this.getChildren(map);
                recursion(mapKey, children);
            }
            count++;
        }
    }

    /**
     * ??????swagger???json?????????????????????
     *
     * @param routeName       ??????
     * @param service         ??????
     * @param version         ??????
     * @param versionChildren ???????????????
     * @return ?????????????????????????????????
     */
    private boolean processChildrenFromSwaggerJson(String routeName, String service, String version,
                                                   List<Map<String, Object>> versionChildren) {
        boolean done = false;
        try {
            String json = this.fetchSwaggerJsonByService(service, version);
            if (StringUtils.isBlank(json)) {
                LOGGER.warn("the swagger json of service {} version {} is empty, skip", service, version);
            } else {
                JsonNode node = this.objectMapper.readTree(json);
                this.processTreeOnControllerNode(routeName, service, version, node, versionChildren);
            }
            done = true;
        } catch (IOException e) {
            LOGGER.error("object mapper read tree error, service: {}, version: {}", service, version);
        } catch (RemoteAccessException e) {
            LOGGER.error(e.getMessage());
        }
        return done;
    }

    /**
     * ?????????????????????Controller??????
     *
     * @param routeName ??????
     * @param service   ??????
     * @param version   ??????
     * @param node      ??????
     * @param children  ?????????
     */
    private void processTreeOnControllerNode(String routeName, String service, String version, JsonNode node,
                                             List<Map<String, Object>> children) {
        Map<String, Map<String, Object>> controllerMap = this.processControllerNode(node);
        Map<String, List<Map<String, Object>>> pathMap = this.processPathMap(routeName, service, version, node);

        controllerMap.forEach((name, nodeData) -> {
            List<Map<String, Object>> controllerChildren = this.getChildren(nodeData);
            List<Map<String, Object>> list = pathMap.get(name);
            if (list != null) {
                children.add(nodeData);
                String refControllerName = name.replaceAll(BaseConstants.Symbol.SPACE,
                        BaseConstants.Symbol.MIDDLE_LINE);
                for (Map<String, Object> path : list) {
                    path.put(REF_CONTROLLER, refControllerName);
                    controllerChildren.add(path);
                }
            }
        });

        try {
            String key = this.getApiTreeDocKey(service, version);
            String value = this.objectMapper.writeValueAsString(children);
            this.cache.put(key, value);
        } catch (JsonProcessingException e) {
            LOGGER.warn("read object to string error while caching to redis, exception", e);
        }
    }

    /**
     * ??????api??????????????????key
     *
     * @param service ??????
     * @param version ??????
     * @return key
     */
    private String getApiTreeDocKey(String service, String version) {
        return API_TREE_DOC + COLON + service + COLON + version;
    }

    /**
     * ??????path??????
     *
     * @param routeName ??????
     * @param service   ??????
     * @param version   ??????
     * @param node      ????????????
     * @return path?????????
     */
    private Map<String, List<Map<String, Object>>> processPathMap(String routeName, String service,
                                                                  String version, JsonNode node) {
        Map<String, List<Map<String, Object>>> pathMap = new HashMap<>(16);
        JsonNode pathNode = node.get(PATHS);
        Iterator<String> urlIterator = pathNode.fieldNames();
        while (urlIterator.hasNext()) {
            String url = urlIterator.next();
            JsonNode methodNode = pathNode.get(url);
            Iterator<String> methodIterator = methodNode.fieldNames();
            while (methodIterator.hasNext()) {
                String method = methodIterator.next();
                JsonNode jsonNode = methodNode.findValue(method);
                if (jsonNode.get(DESCRIPTION) == null) {
                    continue;
                }
                Map<String, Object> path = new HashMap<>(16);
                path.put(TITLE, url);
                path.put(METHOD, method);
                path.put(OPERATION_ID, Optional.ofNullable(jsonNode.get(OPERATION_ID)).map(JsonNode::asText).orElse(null));
                path.put(SERVICE, service);
                path.put(VERSION, version);
                path.put(DESCRIPTION, Optional.ofNullable(jsonNode.get(SUMMARY)).map(JsonNode::asText).orElse(null));
                path.put(SERVICE_PREFIX, routeName);
                JsonNode tagNode = jsonNode.get(TAGS);
                for (int i = 0; i < tagNode.size(); i++) {
                    String tag = tagNode.get(i).asText();
                    if (pathMap.get(tag) == null) {
                        List<Map<String, Object>> list = new ArrayList<>();
                        list.add(path);
                        pathMap.put(tag, list);
                    } else {
                        pathMap.get(tag).add(path);
                    }
                }
            }
        }
        return pathMap;
    }

    /**
     * ??????Controller??????
     *
     * @param node ??????
     * @return ????????????   key ---> value === name ---> nodeData
     */
    private Map<String, Map<String, Object>> processControllerNode(JsonNode node) {
        Map<String, Map<String, Object>> controllerMap = new HashMap<>(16);
        JsonNode tagNodes = node.get(TAGS);
        for (JsonNode jsonNode : tagNodes) {
            String name = jsonNode.findValue(NAME).asText();
            Map<String, Object> controller = new HashMap<>(16);
            controller.put(TITLE, name);
            controller.put(CHILDREN, new ArrayList<>());

            controllerMap.put(name, controller);
        }
        return controllerMap;
    }

    /**
     * ??????swagger???json??????
     *
     * @param name    ????????????
     * @param version ????????????
     * @return ????????????json??????
     */
    private String getSwaggerJson(String name, String version) {
        String serviceName = this.getRouteName(name);
        String json = this.fetchSwaggerJsonByService(serviceName, version);
        try {
            if (json != null) {
                //???????????????swaggerJson
                json = this.expandSwaggerJson(name, version, json);
            }
        } catch (IOException e) {
            LOGGER.error("fetch swagger json error, service: {}, version: {}, exception: {}", name, version, e.getMessage());
            throw new CommonException(e, "error.service.not.run", name, version);
        }
        return json;
    }

    /**
     * ????????????
     *
     * @param name ????????????
     * @return ??????
     */
    private String getRouteName(String name) {
        String serviceName;
        List<ServiceRoute> serviceRoutes = this.serviceRouteRepository.select(ServiceRoute.FIELD_SERVICE_CODE, name);
        if (CollectionUtils.isEmpty(serviceRoutes)) {
            throw new CommonException("error.route.not.found.routeName{" + name + "}");
        } else {
            serviceName = serviceRoutes.get(0).getServiceCode();
        }
        return serviceName;
    }

    /**
     * ????????????swagger???json??????
     *
     * @param name    ??????
     * @param version ??????
     * @param json    json??????
     * @return ????????????
     * @throws IOException ????????????
     */
    public String expandSwaggerJson(String name, String version, String json) throws IOException {
        MultiKeyMap<String, List<ServiceRoute>> allRunningInstances = this.serviceRouteRepository.getAllRunningInstances();
        List<ServiceRoute> serviceRoutes = allRunningInstances.get(name, version);
        if (CollectionUtils.isEmpty(serviceRoutes)) {
            return "";
        }
        String basePath = serviceRoutes.get(0).getPath().replace("/**", "");
        ObjectNode root = (ObjectNode) this.objectMapper.readTree(json);
        root.put(BASE_PATH, basePath);
        return this.objectMapper.writeValueAsString(root);
    }

    /**
     * ???swagger????????????????????????????????????
     *
     * @param name           ??????
     * @param version        ??????
     * @param controllerName controller??????
     * @param operationId    ??????ID
     * @param key            redis??????key
     * @return ???????????????controller????????????
     * @throws IOException ????????????
     */
    private ControllerDTO processPathDetailFromSwagger(String name, String version, String controllerName,
                                                       String operationId, String key) throws IOException {
        String json = this.getSwaggerJson(name, version);
        if (StringUtils.isBlank(json)) {
            throw new CommonException("error.controller.not.found", controllerName);
        }

        JsonNode node = this.objectMapper.readTree(json);
        List<ControllerDTO> controllers = this.processControllers(node);
        List<ControllerDTO> targetControllers = controllers.stream()
                .filter(c -> StringUtils.isNotBlank(c.getName()))
                .filter(c -> controllerName.equals(c.getName().replaceAll(BaseConstants.Symbol.SPACE,
                        BaseConstants.Symbol.MIDDLE_LINE)))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(targetControllers)) {
            throw new CommonException("error.controller.not.found", controllerName);
        }

        Map<String, Map<String, FieldDTO>> map = this.processDefinitions(node);
        Map<String, String> dtoMap = this.convertMap2JsonWithComments(map);
        JsonNode pathNode = node.get(PATHS);
        String basePath = node.get(BASE_PATH).asText();
        ControllerDTO controller = this.queryPathDetailByOptions(name, pathNode, targetControllers, operationId, dtoMap, basePath);
        this.cache2Redis(key, controller);
        return controller;
    }

    /**
     * ??????????????????redis???
     *
     * @param key   ?????????key
     * @param value ????????????
     */
    private void cache2Redis(String key, Object value) {
        try {
            //??????10???
            this.redisTemplate.opsForValue().set(key, this.objectMapper.writeValueAsString(value), 10, TimeUnit.DAYS);
        } catch (JsonProcessingException e) {
            LOGGER.warn("read object to string error while caching to redis, exception", e);
        }
    }

    /**
     * ?????????????????????redis??????key
     *
     * @param name           ????????????
     * @param version        ??????
     * @param controllerName controller??????
     * @param operationId    ??????ID
     * @return ??????key
     */
    private String getPathDetailRedisKey(String name, String version, String controllerName, String operationId) {
        return HZeroService.Admin.CODE + COLON +
                PATH_DETAIL + COLON +
                name + COLON +
                version + COLON +
                controllerName + COLON +
                operationId;
    }

    /**
     * ???map??????????????????????????????json??????
     *
     * @param map map??????
     * @return ???????????????json??????
     */
    private Map<String, String> convertMap2JsonWithComments(Map<String, Map<String, FieldDTO>> map) {
        Map<String, String> returnMap = new HashMap<>();
        map.forEach((className, value) -> {
            StringBuilder sb = new StringBuilder();
            //dto????????????????????????????????????????????????
            MyLinkedList<String> linkedList = new MyLinkedList<>();
            linkedList.addNode(className);
            this.process2String(className, map, sb, linkedList);
            returnMap.put(className, sb.toString());
        });
        return returnMap;
    }

    /**
     * ?????????????????????????????????
     *
     * @param ref        ????????????
     * @param map        ?????????
     * @param sb         ?????????????????????
     * @param linkedList ???????????????????????????????????????
     */
    private void process2String(String ref, Map<String, Map<String, FieldDTO>> map, StringBuilder sb,
                                MyLinkedList<String> linkedList) {
        String className = this.subString4ClassName(ref);
        if (map.containsKey(className)) {
            sb.append("{\n");
            Map<String, FieldDTO> fields = map.get(className);
            //???????????????????????????
            if (fields != null) {
                for (Map.Entry<String, FieldDTO> entry1 : fields.entrySet()) {
                    String field = entry1.getKey();
                    FieldDTO dto = entry1.getValue();
                    //??????????????????????????????????????????????????????
                    String type = dto.getType();
                    if (ARRAY.equals(type)) {
                        //??????????????????????????????type???array
                        if (dto.getComment() != null) {
                            sb.append("//");
                            sb.append(dto.getComment());
                            sb.append("\n");
                        }
                        this.appendField(sb, field);
                        sb.append("[\n");
                        if (dto.getRef() != null) {
                            this.processRefField(map, sb, linkedList, dto);
                        } else {
                            sb.append(type);
                            sb.append("\n");
                        }
                        sb.append("]\n");
                    } else if (StringUtils.isEmpty(type)) {
                        //????????????????????????????????????ref
                        if (dto.getRef() != null) {
                            if (dto.getComment() != null) {
                                sb.append("//");
                                sb.append(dto.getComment());
                                sb.append("\n");
                            }
                            this.appendField(sb, field);
                            this.processRefField(map, sb, linkedList, dto);
                        } else {
                            sb.append("{}\n");
                        }
                    } else {
                        if (INTEGER.equals(type) || STRING.equals(type) || BOOLEAN.equals(type)) {
                            this.appendField(sb, field);
                            sb.append("\"");
                            sb.append(type);
                            sb.append("\"");
                            //?????????
                            this.appendComment(sb, dto);
                            sb.append("\n");
                        }
                        if (OBJECT.equals(type)) {
                            this.appendField(sb, field);
                            sb.append("\"{}\"");
                            //?????????
                            this.appendComment(sb, dto);
                            sb.append("\n");
                        }
                    }
                }
            }
            sb.append("}");
        }
    }

    /**
     * ??????????????????
     *
     * @param map        ?????????
     * @param sb         ?????????????????????
     * @param linkedList ???????????????????????????????????????
     * @param dto        ????????????dto
     */
    private void processRefField(Map<String, Map<String, FieldDTO>> map, StringBuilder sb,
                                 MyLinkedList<String> linkedList, FieldDTO dto) {
        String refClassName = this.subString4ClassName(dto.getRef());
        //linkedList?????????????????????????????????????????????????????????????????????????????????
        MyLinkedList<String> copyLinkedList = linkedList.deepCopy();
        copyLinkedList.addNode(refClassName);
        //??????????????????????????????
        if (copyLinkedList.isLoop()) {
            sb.append("{}");
        } else {
            //????????????
            this.process2String(refClassName, map, sb, copyLinkedList);
        }
    }

    /**
     * ????????????????????????
     *
     * @param ref ??????
     * @return ??????
     */
    private String subString4ClassName(String ref) {
        //??????#/definitions/RouteDTO????????????????????????
        String[] arr = ref.split("/");
        return arr.length > 1 ? arr[arr.length - 1] : arr[0];
    }

    /**
     * ????????????
     *
     * @param sb    ?????????????????????
     * @param field ??????
     */
    private void appendField(StringBuilder sb, String field) {
        sb.append("\"");
        sb.append(field);
        sb.append("\"");
        sb.append(COLON);
    }

    /**
     * ????????????
     *
     * @param sb  ?????????????????????
     * @param dto ??????????????????
     */
    private void appendComment(StringBuilder sb, FieldDTO dto) {
        if (dto.getComment() != null) {
            sb.append(" //");
            sb.append(dto.getComment());
        }
    }

    /**
     * ??????Controoler??????
     *
     * @param node ??????
     * @return Controller????????????
     */
    private List<ControllerDTO> processControllers(JsonNode node) {
        List<ControllerDTO> controllers = new ArrayList<>();
        JsonNode tagNodes = node.get(TAGS);
        for (JsonNode jsonNode : tagNodes) {
            String name = jsonNode.findValue(NAME).asText();
            String description = jsonNode.findValue(DESCRIPTION).asText();
            ControllerDTO controller = new ControllerDTO();
            controller.setName(name);
            controller.setDescription(description);
            controller.setPaths(new ArrayList<>());
            controllers.add(controller);
        }
        return controllers;
    }

    /**
     * ??????????????????
     *
     * @param node ??????
     * @return Definitions
     */
    private Map<String, Map<String, FieldDTO>> processDefinitions(JsonNode node) {
        Map<String, Map<String, FieldDTO>> map = new HashMap<>();
        //definitions?????????controller???????????????json??????
        JsonNode definitionNodes = node.get(DEFINITIONS);
        if (definitionNodes != null) {
            Iterator<String> classNameIterator = definitionNodes.fieldNames();
            while (classNameIterator.hasNext()) {
                String className = classNameIterator.next();
                JsonNode jsonNode = definitionNodes.get(className);
                JsonNode propertyNode = jsonNode.get(PROPERTIES);
                if (propertyNode == null) {
                    String type = jsonNode.get(TYPE).asText();
                    if (OBJECT.equals(type)) {
                        map.put(className, null);
                    }
                    continue;
                }
                Iterator<String> filedNameIterator = propertyNode.fieldNames();
                Map<String, FieldDTO> fieldMap = new HashMap<>();
                while (filedNameIterator.hasNext()) {
                    FieldDTO field = new FieldDTO();
                    String filedName = filedNameIterator.next();
                    JsonNode fieldNode = propertyNode.get(filedName);
                    String type = Optional.ofNullable(fieldNode.get(TYPE)).map(JsonNode::asText).orElse(null);
                    field.setType(type);
                    String description = Optional.ofNullable(fieldNode.get(DESCRIPTION)).map(JsonNode::asText).orElse(null);
                    field.setComment(description);
                    field.setRef(Optional.ofNullable(fieldNode.get(VAR_REF)).map(JsonNode::asText).orElse(null));
                    JsonNode itemNode = fieldNode.get(ITEMS);
                    Optional.ofNullable(itemNode).ifPresent(i -> {
                        if (i.get(TYPE) != null) {
                            field.setItemType(i.get(TYPE).asText());
                        }
                        if (i.get(VAR_REF) != null) {
                            field.setRef(i.get(VAR_REF).asText());
                        }
                    });
                    fieldMap.put(filedName, field);
                }
                map.put(className, fieldMap);
            }
        }
        return map;
    }

    /**
     * ??????????????????????????????
     *
     * @param name              ?????????
     * @param pathNode          ????????????
     * @param targetControllers ??????Controllers
     * @param operationId       ??????ID
     * @param dtoMap            ??????map
     * @param basePath          ????????????
     * @return ??????????????????
     */
    private ControllerDTO queryPathDetailByOptions(String name, JsonNode pathNode, List<ControllerDTO> targetControllers,
                                                   String operationId, Map<String, String> dtoMap, String basePath) {
        String serviceName = this.getRouteName(name);
        Iterator<String> urlIterator = pathNode.fieldNames();
        while (urlIterator.hasNext()) {
            String url = urlIterator.next();
            JsonNode methodNode = pathNode.get(url);
            Iterator<String> methodIterator = methodNode.fieldNames();
            while (methodIterator.hasNext()) {
                String method = methodIterator.next();
                JsonNode pathDetailNode = methodNode.get(method);
                String pathOperationId = pathDetailNode.get(OPERATION_ID).asText();
                if (operationId.equals(pathOperationId)) {
                    this.processPathDetail(serviceName, targetControllers, dtoMap, url, methodNode, method, basePath);
                }
            }
        }
        return targetControllers.get(0);
    }

    /**
     * ??????????????????
     *
     * @param serviceName ?????????
     * @param controllers Controller??????s
     * @param dtoMap      dtomap
     * @param url         url
     * @param methodNode  ????????????
     * @param method      ??????
     * @param basePath    ????????????
     */
    private void processPathDetail(String serviceName, List<ControllerDTO> controllers, Map<String, String> dtoMap,
                                   String url, JsonNode methodNode, String method, String basePath) {
        PathDTO path = new PathDTO();
        path.setBasePath(basePath);
        path.setUrl(url);
        path.setMethod(method);
        JsonNode jsonNode = methodNode.findValue(method);
        JsonNode tagNode = jsonNode.get(TAGS);

        path.setInnerInterface(false);
        this.setCodeOfPathIfExists(serviceName, path, jsonNode.get(DESCRIPTION), tagNode);

        for (int i = 0; i < tagNode.size(); i++) {
            String tag = tagNode.get(i).asText();
            controllers.forEach(c -> {
                List<PathDTO> paths = c.getPaths();
                if (tag.equals(c.getName())) {
                    path.setRefController(c.getName());
                    paths.add(path);
                }
            });
        }
        path.setRemark(Optional.ofNullable(jsonNode.get(SUMMARY)).map(JsonNode::asText).orElse(null));
        path.setDescription(Optional.ofNullable(jsonNode.get(DESCRIPTION)).map(JsonNode::asText).orElse(null));
        path.setOperationId(Optional.ofNullable(jsonNode.get(OPERATION_ID)).map(JsonNode::asText).orElse(null));
        this.processConsumes(path, jsonNode);
        this.processProduces(path, jsonNode);
        this.processResponses(path, jsonNode, dtoMap);
        this.processParameters(path, jsonNode, dtoMap);
    }

    /**
     * ????????????????????????
     *
     * @param path           ????????????
     * @param jsonNode       json??????
     * @param controllerMaps Controller???Maps
     */
    private void processResponses(PathDTO path, JsonNode jsonNode, Map<String, String> controllerMaps) {
        JsonNode responseNode = jsonNode.get(RESPONSES);
        List<ResponseDTO> responses = new ArrayList<>();
        Iterator<String> responseIterator = responseNode.fieldNames();
        while (responseIterator.hasNext()) {
            String status = responseIterator.next();
            JsonNode node = responseNode.get(status);
            ResponseDTO response = new ResponseDTO();
            response.setHttpStatus(status);
            response.setDescription(node.get(DESCRIPTION).asText());
            JsonNode schemaNode = node.get(SCHEMA);
            if (schemaNode != null) {
                JsonNode refNode = schemaNode.get(VAR_REF);
                if (refNode != null) {
                    for (Map.Entry<String, String> entry : controllerMaps.entrySet()) {
                        String className = this.subString4ClassName(refNode.asText());
                        if (className.equals(entry.getKey())) {
                            response.setBody(entry.getValue());
                        }
                    }
                } else {
                    String type = Optional.ofNullable(schemaNode.get(TYPE)).map(JsonNode::asText).orElse(null);
                    String ref = Optional.ofNullable(schemaNode.get(ITEMS))
                            .flatMap(itemNode -> Optional.ofNullable(itemNode.get(VAR_REF))
                                    .map(JsonNode::asText))
                            .orElse(null);
                    if (ref != null) {
                        response.setBody(this.processRef(controllerMaps, type, ref).toString());
                    } else {
                        if (OBJECT.equals(type)) {
                            response.setBody("{}");
                        } else {
                            response.setBody(type);
                        }
                    }
                }
            }
            responses.add(response);
        }
        path.setResponses(responses);
    }

    /**
     * set the code field of the instance of {@link PathDTO} if the extraDataNode parameter
     * is not null
     *
     * @param serviceName   the name of the service
     * @param path          the dto
     * @param extraDataNode the extra data node
     * @param tagNode       the tag node
     */
    private void setCodeOfPathIfExists(String serviceName, PathDTO path, JsonNode extraDataNode, JsonNode tagNode) {
        if (extraDataNode != null) {
            try {
                SwaggerExtraData extraData;
                String resourceCode = this.processResourceCode(tagNode);
                extraData = new ObjectMapper().readValue(extraDataNode.asText(), SwaggerExtraData.class);
                PermissionData permission = extraData.getPermission();
                String action = permission.getAction();
                path.setInnerInterface(permission.isPermissionWithin());
                path.setCode(String.format("%s.%s.%s", serviceName, resourceCode, action));
            } catch (IOException e) {
                LOGGER.info("extraData read failed.", e);
            }
        }
    }

    /**
     * ?????????????????????????????????Controller
     *
     * @param tags tags??????
     * @return resourceCode
     */
    private String processResourceCode(JsonNode tags) {
        String resourceCode = null;
        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i).asText();
            // ??????choerodon-eureka????????????-endpoint?????????tag???
            if (tag.endsWith(SUFFIX_CONTROLLER)) {
                resourceCode = tag.substring(0, tag.length() - SUFFIX_CONTROLLER.length());
            } else if (tag.endsWith(SUFFIX_ENDPOINT)) {
                resourceCode = tag.substring(0, tag.length() - SUFFIX_ENDPOINT.length());
            } else {
                resourceCode = tag.replace(" ", "-").replace("(", "-").replace(")", "").replaceAll("-+", "-")
                        .toLowerCase();
            }
        }

        return resourceCode;
    }

    /**
     * ??????Consumers
     *
     * @param path     path
     * @param jsonNode jsonNode
     */
    private void processConsumes(PathDTO path, JsonNode jsonNode) {
        JsonNode consumeNode = jsonNode.get(CONSUMES);
        List<String> consumes = new ArrayList<>();
        for (int i = 0; i < consumeNode.size(); i++) {
            consumes.add(consumeNode.get(i).asText());
        }
        path.setConsumes(consumes);
    }

    /**
     * ??????Produces
     *
     * @param path     path
     * @param jsonNode jsonNode
     */
    private void processProduces(PathDTO path, JsonNode jsonNode) {
        JsonNode produceNode = jsonNode.get(PRODUCES);
        List<String> produces = new ArrayList<>();
        for (int i = 0; i < produceNode.size(); i++) {
            produces.add(produceNode.get(i).asText());
        }
        path.setProduces(produces);
    }

    /**
     * ????????????
     *
     * @param path           ????????????
     * @param jsonNode       jsonNode
     * @param controllerMaps ControllerMaps
     */
    private void processParameters(PathDTO path, JsonNode jsonNode, Map<String, String> controllerMaps) {
        JsonNode parameterNode = jsonNode.get(PARAMETERS);
        List<ParameterDTO> parameters = new ArrayList<>();
        if (parameterNode != null) {
            for (int i = 0; i < parameterNode.size(); i++) {
                try {
                    ParameterDTO parameter = this.objectMapper.treeToValue(parameterNode.get(i), ParameterDTO.class);
                    SchemaDTO schema = parameter.getSchema();
                    if (BODY.equals(parameter.getIn()) && schema != null) {
                        String ref = schema.getRef();
                        if (ref != null) {
                            for (Map.Entry<String, String> entry : controllerMaps.entrySet()) {
                                String className = this.subString4ClassName(ref);
                                if (className.equals(entry.getKey())) {
                                    String body = entry.getValue();
                                    parameter.setBody(body);
                                }
                            }
                        } else {
                            String type = schema.getType();
                            String itemRef = Optional.ofNullable(schema.getItems()).map(m -> m.get(VAR_REF)).orElse(null);
                            if (itemRef != null) {
                                parameter.setBody(this.processRef(controllerMaps, type, itemRef).toString());
                            } else {
                                if (!OBJECT.equals(type)) {
                                    parameter.setBody(type);
                                } else {
                                    Map<String, String> map = schema.getAdditionalProperties();
                                    if (map != null && ARRAY.equals(map.get(TYPE))) {
                                        parameter.setBody("[{}]");
                                    } else {
                                        parameter.setBody("{}");
                                    }
                                }
                            }
                        }
                    }
                    parameters.add(parameter);
                } catch (JsonProcessingException e) {
                    LOGGER.info("jsonNode to parameterDTO failed, exception: {}", e.getMessage());
                }
            }
        }
        path.setParameters(parameters);
    }

    /**
     * ????????????
     *
     * @param controllerMaps controllerMaps
     * @param type           type
     * @param itemRef        itemRef
     * @return ????????????
     */
    private StringBuilder processRef(Map<String, String> controllerMaps, String type, String itemRef) {
        String body = "";
        for (Map.Entry<String, String> entry : controllerMaps.entrySet()) {
            String className = this.subString4ClassName(itemRef);
            if (className.equals(entry.getKey())) {
                body = entry.getValue();
            }
        }
        //???array???????????????????????????????????????\n//\\S+\n?????????
        return this.arrayTypeAppendBrackets(type, body);
    }

    /**
     * ???????????????????????????
     *
     * @param type ??????
     * @param body ?????????
     * @return ????????????
     */
    private StringBuilder arrayTypeAppendBrackets(String type, String body) {
        StringBuilder sb = new StringBuilder();
        if (ARRAY.equals(type)) {
            sb.append("[\n");
            sb.append(body);
            sb.append("\n]");
        } else {
            sb.append(body);
        }
        return sb;
    }

    /**
     * ???????????????
     *
     * @param parent ?????????
     * @return ???????????????
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getChildren(Map<String, Object> parent) {
        return (List<Map<String, Object>>) parent.get(CHILDREN);
    }
}
