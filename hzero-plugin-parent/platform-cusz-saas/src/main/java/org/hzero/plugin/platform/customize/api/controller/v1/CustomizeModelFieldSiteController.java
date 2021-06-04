package org.hzero.plugin.platform.customize.api.controller.v1;

import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hzero.core.util.Results;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.app.service.CustomizeModelFieldService;
import org.hzero.plugin.platform.customize.config.CuszPlatformSwaggerApiConfig;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 13:29
 */
@Api(tags = CuszPlatformSwaggerApiConfig.CUSZ_MODEL_FIELD_SITE)
@RestController("customizeModelFieldSiteController.v1")
@RequestMapping("/v1/model/field")
public class CustomizeModelFieldSiteController {

    @Autowired
    private CustomizeModelFieldService customizeModelFieldService;

    @GetMapping("/list")
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("查询模型字段列表")
    public ResponseEntity<List<ModelField>> list(@Encrypt FieldCommonSearchDTO searchDTO) {
        return Results.success(customizeModelFieldService.selectFieldByModelId(searchDTO, null));
    }

    @PostMapping("/modify")
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("修改模型字段")
    public ResponseEntity<ModelField> modify(@RequestBody ModelField field) {
        return Results.success(customizeModelFieldService.updateFieldById(field));
    }

//    @PostMapping("/create")
//    @Permission(level = ResourceLevel.SITE)
//    @ApiOperation("创建模型字段")
//    public ResponseEntity<ModelField> create(@RequestBody ModelField modelField) {
//        customizeModelFieldService.createField(modelField);
//        return Results.success(modelField);
//    }

    @GetMapping("/refresh")
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("刷新模型字段")
    public ResponseEntity<List<ModelField>> refresh(@RequestParam String serviceName, @RequestParam String tableName,
                                                    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL) @RequestParam Long modelId) {
        return Results.success(customizeModelFieldService.syncField(serviceName, tableName, modelId));
    }

//    @DeleteMapping
//    @Permission(level = ResourceLevel.SITE)
//    @ApiOperation("根据主键删除")
//    public ResponseEntity delete(@RequestParam Long fieldId) {
//        customizeModelFieldService.deleteField(fieldId);
//        return Results.success();
//    }
}


