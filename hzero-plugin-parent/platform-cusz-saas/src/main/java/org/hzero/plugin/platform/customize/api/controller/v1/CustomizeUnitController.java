package org.hzero.plugin.platform.customize.api.controller.v1;

import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hzero.core.util.Results;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.api.dto.MenuDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitDTO;
import org.hzero.plugin.platform.customize.app.service.CustomizeUnitService;
import org.hzero.plugin.platform.customize.config.CuszPlatformSwaggerApiConfig;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;
import org.hzero.plugin.platform.customize.domain.entity.Unit;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;
import org.hzero.plugin.platform.customize.domain.entity.UnitGroup;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author xiangyu.qi01@hand-china.com on 2019-12-23.
 */
@Api(tags = CuszPlatformSwaggerApiConfig.CUSZ_UNIT)
@RestController("customizeUnitController.v1")
@RequestMapping("/v1/{organizationId}/customize/unit")
public class CustomizeUnitController {

    @Autowired
    private CustomizeUnitService customizeUnitService;

    @GetMapping("/list")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("??????????????????-??????")
    public ResponseEntity<Page<Unit>> list(@Encrypt Unit unit, PageRequest pageRequest) {
        return Results.success(customizeUnitService.selectUnitByOption(unit, pageRequest));
    }

    @GetMapping("/list/group")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("???????????????????????????")
    public ResponseEntity<List<Unit>> listGroup(@Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT_GROUP) @RequestParam Long unitGroupId) {
        return Results.success(customizeUnitService.selectByGroupId(unitGroupId));
    }

    @PostMapping
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("????????????")
    public ResponseEntity<Unit> create(@RequestBody Unit unit) {
        return Results.success(customizeUnitService.createUnit(unit));
    }

    @GetMapping("/detail")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("??????????????????????????????")
    public ResponseEntity<UnitDTO> detail(@Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT) @RequestParam Long unitId) {
        return Results.success(customizeUnitService.selectUnitById(unitId));
    }

    @PutMapping
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("????????????")
    public ResponseEntity<Unit> update(@RequestBody Unit unit) {
        return Results.success(customizeUnitService.saveUnit(unit));
    }

//    @DeleteMapping
//    @Permission(level = ResourceLevel.ORGANIZATION)
//    @ApiOperation("????????????")
//    public ResponseEntity<Unit> delete(Unit unit) {
//        return Results.success(customizeUnitService.saveUnit(unit));
//    }

    @DeleteMapping("/field")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("??????????????????")
    public ResponseEntity<Void> deleteLine(@Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT_FIELD) @RequestParam Long unitFieldId) {
        customizeUnitService.deleteUnitField(unitFieldId);
        return Results.success();
    }

    @PostMapping("/field-save")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("???????????????")
    public ResponseEntity<UnitField> modify(@RequestBody UnitField unitField) {
        return Results.success(customizeUnitService.saveUnitField(unitField));
    }

    @GetMapping("/menu-tree")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("???????????????-?????????????????????")
    public ResponseEntity<List<MenuDTO>> menuTrees() {
        return Results.success(customizeUnitService.selectAllMenuForTree());
    }

    @GetMapping("/not-config")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("?????????????????????????????????????????????")
    public ResponseEntity<Page<ModelField>> notConfigField(@Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT) @RequestParam Long unitId,
                                                           @Encrypt FieldCommonSearchDTO searchDTO, PageRequest pageRequest) {
        return Results.success(customizeUnitService.selectNotConfigField(unitId, searchDTO, pageRequest));
    }

    @GetMapping("/group")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("????????????????????????????????????")
    public ResponseEntity<List<UnitGroup>> unitGroups(@Encrypt UnitGroup unitGroup) {
        return Results.success(customizeUnitService.selectByOptions(unitGroup));
    }

    @PostMapping("/group")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("????????????????????????")
    public ResponseEntity<UnitGroup> createUnitGroups(@RequestBody UnitGroup unitGroup) {
        return Results.success(customizeUnitService.createGroup(unitGroup));
    }


    @PutMapping("/group")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("????????????????????????")
    public ResponseEntity<UnitGroup> updateUnitGroups(@RequestBody UnitGroup unitGroup) {
        return Results.success(customizeUnitService.updateGroup(unitGroup));
    }

    @PostMapping("/copy")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("?????????????????????")
    public ResponseEntity<UnitGroup> copyUnit(@RequestBody Unit unit) {
        customizeUnitService.copyUnit(unit, unit.getCopyUnitCode());
        return Results.success();
    }

    @PostMapping("/delete")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("?????????????????????")
    public ResponseEntity<UnitGroup> deleteUnit(String unitCode) {
        customizeUnitService.deleteUnit(unitCode);
        return Results.success();
    }

}
