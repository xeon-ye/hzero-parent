package org.hzero.plugin.platform.customize.api.controller.v1;

import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.hzero.core.util.Results;
import org.hzero.plugin.platform.customize.api.dto.ConfigFieldLovDTO;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.api.dto.MenuDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitCompositeDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitRelatedDTO;
import org.hzero.plugin.platform.customize.api.dto.UserConfigDTO;
import org.hzero.plugin.platform.customize.app.service.CustomizeConfigService;
import org.hzero.plugin.platform.customize.app.service.CustomizeModelFieldService;
import org.hzero.plugin.platform.customize.app.service.CustomizeModelService;
import org.hzero.plugin.platform.customize.app.service.CustomizeUnitService;
import org.hzero.plugin.platform.customize.config.CuszPlatformSwaggerApiConfig;
import org.hzero.plugin.platform.customize.domain.entity.Config;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.entity.ConfigFieldMap;
import org.hzero.plugin.platform.customize.domain.entity.FieldConditionHeader;
import org.hzero.plugin.platform.customize.domain.entity.Model;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;
import org.hzero.plugin.platform.customize.domain.entity.UnitGroup;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author xiangyu.qi01@hand-china.com on 2019-12-16.
 */
@Api(tags = CuszPlatformSwaggerApiConfig.CUSZ_UNIT_CONFIG)
@RestController("customizeConfigController.v1")
@RequestMapping("/v1/{organizationId}/unit-config")
public class CustomizeConfigController {

    @Autowired
    private CustomizeConfigService unitConfigFieldService;
    @Autowired
    private CustomizeUnitService customizeUnitService;
    @Autowired
    private CustomizeUnitService unitService;
    @Autowired
    private CustomizeModelFieldService customizeModelFieldService;
    @Autowired
    private CustomizeModelService customizeModelService;

    @GetMapping("/menu-tree")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("???????????????-?????????????????????")
    public ResponseEntity<List<MenuDTO>> menuTrees(@PathVariable(name = "organizationId") Long tenantId) {
        return Results.success(customizeUnitService.selectMenuTree());
    }

    @GetMapping("/groupUnits")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("???????????????-???????????????????????????????????????")
    public ResponseEntity<List<UnitGroup>> unitLists(@PathVariable(name = "organizationId") Long tenantId, @RequestParam String menuCode) {
        return Results.success(unitService.selectGroupAndUnits(menuCode));
    }

    @GetMapping("/details")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("???????????????-??????????????????????????????????????????")
    public ResponseEntity<UnitCompositeDTO> unitConfig(@PathVariable(name = "organizationId") Long tenantId,
                                                       @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT)
                                                       @RequestParam Long unitId) {
        return Results.success(unitConfigFieldService.selectConfigDetails(tenantId, unitId));
    }

    @ApiOperation("?????????????????????")
    @PostMapping("/save-header")
    @Permission(level = ResourceLevel.ORGANIZATION)
    public ResponseEntity saveConfigHeader(@PathVariable("organizationId") Long tenantId, @RequestBody Config config) {
        config.setTenantId(tenantId);
        unitConfigFieldService.saveUnitConfig(config);
        return Results.success();
    }


    @ApiOperation("?????????????????????")
    @PostMapping("/save")
    @Permission(level = ResourceLevel.ORGANIZATION)
    public ResponseEntity saveConfig(@PathVariable("organizationId") Long tenantId, @RequestBody ConfigField configField) {
        configField.setTenantId(tenantId);
        unitConfigFieldService.saveUnitConfigFieldAndWdg(configField);
        return Results.success();
    }

    @ApiOperation("??????????????????")
    @DeleteMapping("/delete")
    @Permission(level = ResourceLevel.ORGANIZATION)
    public ResponseEntity<ConfigField> deleteConfigField(@ApiParam(value = "??????ID", required = true) @PathVariable("organizationId") Long tenantId,
                                                         @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_CONFIG_FIELD)
                                                         @RequestParam Long configFieldId) {
        ConfigField configField = new ConfigField();
        configField.setConfigFieldId(configFieldId);
        configField.setTenantId(tenantId);
        unitConfigFieldService.deleteConfigFieldById(configField);
        return Results.success();
    }

    @ApiOperation("?????????????????????????????????")
    @GetMapping("/lov/config-field")
    @Permission(level = ResourceLevel.ORGANIZATION)
    public ResponseEntity<List<ConfigFieldLovDTO>> lovConfigFields(@ApiParam(value = "??????ID", required = true) @PathVariable("organizationId") Long tenantId,
                                                                   @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT)
                                                                   @RequestParam Long unitId,
                                                                   @Encrypt FieldCommonSearchDTO searchDTO, PageRequest pageRequest) {
        //todo ??????
        return Results.success(unitConfigFieldService.selectSimpleConfigField(tenantId, unitId, searchDTO, pageRequest));
    }

    @ApiOperation("??????????????????lov??????????????????")
    @GetMapping("/mapping/details")
    @Permission(level = ResourceLevel.ORGANIZATION)
    public ResponseEntity<List<ConfigFieldMap>> details(@ApiParam(value = "??????ID", required = true) @PathVariable("organizationId") Long tenantId,
                                                        @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_CONFIG_FIELD)
                                                        @RequestParam(required = false) Long configFieldId) {
        return Results.success(unitConfigFieldService.selectFieldMapping(configFieldId));
    }

    @GetMapping("/field/list/not-config")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("???????????????-????????????????????????????????????????????? - HPFM.CUST.MODEL_FIELD.NOT_CONFIG")
    public ResponseEntity<Page<ModelField>> list(@Encrypt FieldCommonSearchDTO searchDTO, @PathVariable("organizationId") Long tenantId,
                                                 @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT)
                                                 @RequestParam Long unitId, PageRequest pageRequest) {
        return Results.success(customizeModelFieldService.selectUnConfigFieldByModelId(searchDTO, tenantId, unitId, pageRequest));
    }

    @GetMapping("/list/model")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("???????????????????????????????????????")
    public ResponseEntity<List<Model>> modelListOrg(@Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT) @RequestParam Long unitId,
                                                    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_MODEL) @RequestParam Long modelId) {
        return Results.success(customizeModelService.selectAssociatedModels(unitId, modelId));
    }

    @GetMapping("/list/model-field")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("????????????????????????-??????-lov")
    public ResponseEntity<Page<ModelField>> listLov(@Encrypt FieldCommonSearchDTO searchDTO, PageRequest pageRequest) {
        return Results.success((Page<ModelField>) customizeModelFieldService.selectFieldByModelId(searchDTO, pageRequest));
    }

    @GetMapping("/user-ui")
    @Permission(level = ResourceLevel.ORGANIZATION, permissionLogin = true)
    @ApiOperation("?????????-????????????????????????????????????????????????")
    public ResponseEntity<Map<String, UserConfigDTO>> userTable(@PathVariable("organizationId") Long tenantId,
                                                                @RequestParam("unitCode") String unitCodes) {
        return Results.success(unitConfigFieldService.userConfigByUnitCodes(tenantId, StringUtils.commaDelimitedListToStringArray(unitCodes)));
    }

    @PostMapping("/user-ui")
    @Permission(level = ResourceLevel.ORGANIZATION, permissionLogin = true)
    @ApiOperation("?????????-????????????????????????????????????????????????")
    public ResponseEntity saveUserTable(@PathVariable("organizationId") Long tenantId,
                                        @RequestBody UserConfigDTO userConfigDTO) {

        return Results.success(unitConfigFieldService.saveUserConfigFields(tenantId, userConfigDTO));
    }

    @DeleteMapping("/user-ui")
    @Permission(level = ResourceLevel.ORGANIZATION, permissionLogin = true)
    @ApiOperation("?????????-????????????????????????????????????????????????")
    public ResponseEntity deleteUserTable(@PathVariable("organizationId") Long tenantId,
                                          @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT)
                                          @RequestParam Long unitId) {

        return Results.success(unitConfigFieldService.deleteUserConfigField(tenantId, unitId));
    }

    @GetMapping("/condition")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("?????????????????????????????????")
    public ResponseEntity<List<FieldConditionHeader>> condList(@PathVariable(name = "organizationId") Long tenantId,
                                                               @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT) @RequestParam Long unitId,
                                                               @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_CONFIG_FIELD) @RequestParam Long configFieldId) {
        FieldConditionHeader search = new FieldConditionHeader();
        search.setTenantId(tenantId);
        search.setConfigFieldId(configFieldId);
        return Results.success(unitConfigFieldService.selectCondHeaderByFieldId(search, unitId, false));
    }

    @GetMapping("/condition-valid")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("???????????????????????????????????????")
    public ResponseEntity<FieldConditionHeader> condValidList(@PathVariable(name = "organizationId") Long tenantId,
                                                              @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT) @RequestParam Long unitId,
                                                              @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_CONFIG_FIELD) @RequestParam Long configFieldId) {
        FieldConditionHeader search = new FieldConditionHeader();
        search.setTenantId(tenantId);
        search.setConfigFieldId(configFieldId);
        search.setConType(CustomizeConstants.ConditionType.VALID);
        return Results.success(unitConfigFieldService.selectConValidByFieldId(search, unitId));
    }

    @GetMapping("/unit/related")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("????????????????????????????????????")
    public ResponseEntity<List<UnitRelatedDTO>> relatedUnit(@PathVariable(name = "organizationId") Long tenantId, @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT) @RequestParam Long unitId) {
        return Results.success(unitConfigFieldService.selectWithWdgByUnitId(unitId, tenantId));
    }

}
