package org.hzero.plugin.platform.customize.api.controller.v1;

import java.util.List;
import java.util.Map;

import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.util.Results;
import org.hzero.plugin.platform.customize.api.dto.*;
import org.hzero.plugin.platform.customize.app.service.CustomizeConfigService;
import org.hzero.plugin.platform.customize.app.service.CustomizeModelFieldService;
import org.hzero.plugin.platform.customize.app.service.CustomizeModelService;
import org.hzero.plugin.platform.customize.app.service.CustomizeUnitService;
import org.hzero.plugin.platform.customize.config.CuszPlatformSwaggerApiConfig;
import org.hzero.plugin.platform.customize.domain.entity.*;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @author xiangyu.qi01@hand-china.com on 2019-12-16.
 */
@Api(tags = CuszPlatformSwaggerApiConfig.CUSZ_UNIT_CONFIG_SITE)
@RestController("customizeConfigSiteController.v1")
@RequestMapping("/v1/unit-config")
public class CustomizeConfigSiteController {

    @Autowired
    private CustomizeConfigService unitConfigFieldService;

    @GetMapping("/unit/related")
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("获取单元关联的单元及字段")
    public ResponseEntity<List<UnitRelatedDTO>> relatedUnit(@Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT) @RequestParam Long unitId) {
        return Results.success(unitConfigFieldService.selectWithWdgByUnitId(unitId, -1L));
    }

}
