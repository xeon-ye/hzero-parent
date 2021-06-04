package org.hzero.plugin.platform.customize.api.controller.v1;

import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hzero.boot.customize.dto.TableMetaData;
import org.hzero.core.util.Results;
import org.hzero.plugin.platform.customize.app.service.CustomizeModelService;
import org.hzero.plugin.platform.customize.config.CuszPlatformSwaggerApiConfig;
import org.hzero.plugin.platform.customize.domain.entity.Model;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;
import org.hzero.plugin.platform.customize.domain.entity.ModelFieldPub;
import org.hzero.plugin.platform.customize.domain.entity.ModelObjectPub;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelFieldRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelRepository;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 10:59
 */
@RestController("customizeModelSiteController.v1")
@RequestMapping("/v1/customize/model")
@Api(tags = {CuszPlatformSwaggerApiConfig.CUSZ_MODEL_SITE})
public class CustomizeModelSiteController {

    @Autowired
    private CustomizeModelService customizeModelService;
    @Autowired
    private CustomizeModelRepository customizeModelRepository;
    @Autowired
    private CustomizeModelFieldRepository customizeModelFieldRepository;

    @GetMapping("/list")
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("查询模型列表-分页")
    public ResponseEntity<Page<Model>> list(@Encrypt Model model,
                                            PageRequest pageRequest) {
        return Results.success(PageHelper.doPageAndSort(pageRequest, () -> customizeModelRepository.selectByCondition(model)));
    }

    @GetMapping("/detail")
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("查询模型详情")
    public ResponseEntity<Model> detail(@Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL) @RequestParam Long modelId) {
        return Results.success(Model.convertFromModelObject(customizeModelRepository.selectByPrimaryKey(modelId)));
    }

    @PostMapping("/create")
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("创建模型")
    public ResponseEntity<Model> create(@RequestBody Model model) {
        return Results.success(customizeModelService.createModel(model));
    }

    @PostMapping("/modify")
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("修改模型")
    public ResponseEntity<Model> modify(@RequestBody Model model) {
        customizeModelService.updateByOptions(model);
        return Results.success(model);
    }


    /**
     * 临时删除接口，开发用
     *
     * @param modelId
     * @return
     */
    @DeleteMapping("/delete")
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("删除模型")
    public ResponseEntity delete(@Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL) Long modelId) {
        Assert.notNull(modelId, "modelId can not be null!");
        customizeModelRepository.deleteByPrimaryKey(modelId);
        ModelObjectPub model = new ModelObjectPub();
        model.setId(modelId);
        customizeModelRepository.delCache(Model.convertFromModelObject(model));
        //删除模型字段
        ModelFieldPub modelField = new ModelFieldPub();
        modelField.setModelObjectId(modelId);
        List<ModelFieldPub> fieldPubs = customizeModelFieldRepository.select(modelField);
        fieldPubs.forEach(t -> {
            customizeModelFieldRepository.deleteByPrimaryKey(t.getId());
        });
        ModelField modelField1 = new ModelField();
        modelField1.setModelId(modelId);
        customizeModelFieldRepository.delCacheList(modelField1);
        return Results.success();
    }

    @GetMapping("/list/tables")
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("获取当前服务所有表名")
    public ResponseEntity<List<TableMetaData>> listTables(@RequestParam String serviceName) {
        return Results.success(customizeModelService.getTables(serviceName));
    }

    @GetMapping("/list/unit")
    @Permission(level = ResourceLevel.SITE)
    @ApiOperation("查询指定单元关联的模型列表")
    public ResponseEntity<List<Model>> modelList(@Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT) @RequestParam Long unitId,
                                                 @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL) @RequestParam Long modelId) {
        return Results.success(customizeModelService.selectAssociatedModels(unitId, modelId));
    }

}
