package org.hzero.plugin.platform.customize.api.controller.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.collections4.CollectionUtils;
import org.hzero.boot.customize.service.CustomizeClient;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.hzero.boot.platform.lov.dto.LovValueDTO;
import org.hzero.boot.platform.lov.dto.LovViewDTO;
import org.hzero.core.util.Results;
import org.hzero.plugin.platform.customize.api.dto.UnitConfigDTO;
import org.hzero.plugin.platform.customize.app.service.CustomizeCommonService;
import org.hzero.plugin.platform.customize.config.CuszPlatformSwaggerApiConfig;
import org.hzero.plugin.platform.customize.infra.init.CustomizeRedisValueInit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @author xiangyu.qi01@hand-china.com on 2019-12-16.
 */
@Api(tags = CuszPlatformSwaggerApiConfig.CUSZ_COMMON)
@RestController("customizeCommonController.v1")
@RequestMapping("/v1/{organizationId}")
public class CustomizeCommonController {

    @Autowired
    private CustomizeCommonService customizeCommonService;
    @Autowired
    private CustomizeRedisValueInit redisValueInit;
    @Autowired
    private LovAdapter lovAdapter;
    @Autowired
    private CustomizeClient customizeClient;

    @ApiOperation(value = "根据个性化单元编码获得单元配置详情", notes = "获得个性化单元配置详情")
    @GetMapping("/ui-customize")
    @Permission(permissionLogin = true, level = ResourceLevel.ORGANIZATION)
    public ResponseEntity<Map<String, UnitConfigDTO>> uiCustomize(@ApiParam(value = "租户ID", required = true) @PathVariable("organizationId") Long tenantId,
                                                                  @RequestParam String[] unitCode) {
        tenantId = customizeClient.getTenantId();
        return Results.success(customizeCommonService.getUiCustomize(tenantId, unitCode));
    }

    @ApiOperation(value = "刷新平台级模型和单元的缓存", notes = "刷新平台级模型和单元的缓存")
    @GetMapping("/ui-customize/refreshCache")
    @Permission(permissionLogin = true, level = ResourceLevel.ORGANIZATION)
    public ResponseEntity<Map<String, UnitConfigDTO>> refreshCache() {
        try {
            redisValueInit.afterPropertiesSet();
        } catch (Exception e) {
            return Results.error();
        }
        return Results.success();
    }

    @ApiOperation(value = "个性化值集翻译", notes = "刷新平台级模型和单元的缓存")
    @PostMapping("/ui-customize/translateLov")
    @Permission(permissionLogin = true, level = ResourceLevel.ORGANIZATION)
    public ResponseEntity<Map<String, Object>> translateLov(@PathVariable("organizationId") Long tenantId, String lovViewCode, @RequestBody List<String> ids) {
        if (StringUtils.isEmpty(lovViewCode) && CollectionUtils.isEmpty(ids)) {
            return Results.success();
        }
        List<LovValueDTO> result;
        LovViewDTO lovViewDTO = lovAdapter.queryLovViewInfo(lovViewCode, tenantId);
        if (lovViewDTO == null) {
            return Results.success();
        }
        String lovCode = lovViewDTO.getLovCode();
        try {
            result = lovAdapter.queryLovValue(lovCode, tenantId, ids.stream().map(t-> "'"+t+"'").collect(Collectors.toList()));
        } catch (Exception e) {
            //ignore
            result = new ArrayList<>();
        }
        //多选组件，翻译多个值
        Map<String, Object> translateMap = result.stream().filter(t -> !StringUtils.isEmpty(t.getValue())).collect(Collectors.toMap(LovValueDTO::getValue, LovValueDTO::getMetadata));
        Map<String, Object> translateResult = new HashMap<>(ids.size());
        ids.forEach(t -> translateResult.put(t, translateMap.getOrDefault(t, t)));
        return Results.success(translateResult);
    }


}
