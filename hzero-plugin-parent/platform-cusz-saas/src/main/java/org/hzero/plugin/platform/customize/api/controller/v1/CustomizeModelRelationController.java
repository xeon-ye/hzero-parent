package org.hzero.plugin.platform.customize.api.controller.v1;

import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hzero.core.util.Results;
import org.hzero.plugin.platform.customize.app.service.CustomizeModelService;
import org.hzero.plugin.platform.customize.config.CuszPlatformSwaggerApiConfig;
import org.hzero.plugin.platform.customize.domain.entity.ModelRelation;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelRelationRepository;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2019/12/13 11:48
 */
@Api(tags = CuszPlatformSwaggerApiConfig.CUSZ_MODEL_RELATION)
@RestController("customizeModelRelationController.v1")
@RequestMapping("/v1/{organizationId}/model/relation")
public class CustomizeModelRelationController {

    @Autowired
    private CustomizeModelService customizeModelService;
    @Autowired
    private CustomizeModelRelationRepository relationRepository;

    @GetMapping("/list")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("根据模型主键获取模型关系")
    public ResponseEntity<List<ModelRelation>> list(@Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL) Long modelId) {
        return Results.success(customizeModelService.selectAssociateRelation(modelId));
    }

    @PostMapping("/create")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("新建模型关系")
    public ResponseEntity<ModelRelation> create(@RequestBody ModelRelation relation) {
        return Results.success(customizeModelService.createRelation(relation));
    }

    @DeleteMapping
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("根据主键删除模型关系")
    public ResponseEntity delete(@Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL_REL_HEADER) @RequestParam Long relationId) {
        relationRepository.deleteRelation(relationId);
        return Results.success();
    }

}
