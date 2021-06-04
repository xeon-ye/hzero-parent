package org.hzero.plugin.platform.customize.api.controller.v1;

import java.util.Map;

import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.hzero.core.util.Results;
import org.hzero.plugin.platform.customize.api.dto.UnitConfigDTO;
import org.hzero.plugin.platform.customize.app.service.CustomizeCommonService;
import org.hzero.plugin.platform.customize.config.CuszPlatformSwaggerApiConfig;
import org.hzero.plugin.platform.customize.infra.init.CustomizeRedisValueInit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author xiangyu.qi01@hand-china.com on 2019-12-16.
 */
@Api(tags = CuszPlatformSwaggerApiConfig.CUSZ_COMMON_SITE)
@RestController("customizeCommonSiteController.v1")
@RequestMapping("/v1")
public class CustomizeCommonSiteController {

    @Autowired
    private CustomizeRedisValueInit redisValueInit;

    @ApiOperation(value = "刷新平台级模型和单元的缓存", notes = "刷新平台级模型和单元的缓存")
    @GetMapping("/ui-customize/refreshCache")
    @Permission(permissionLogin = true, level = ResourceLevel.SITE)
    public ResponseEntity<Map<String, UnitConfigDTO>> refreshCache() {
        try {
            redisValueInit.afterPropertiesSet();
        } catch (Exception e) {
            return Results.error();
        }
        return Results.success();
    }


}
