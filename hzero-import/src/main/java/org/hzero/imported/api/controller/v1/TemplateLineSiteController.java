package org.hzero.imported.api.controller.v1;

import org.hzero.boot.platform.lov.annotation.ProcessLovValue;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.base.BaseController;
import org.hzero.core.util.Results;
import org.hzero.imported.app.service.TemplateLineService;
import org.hzero.imported.config.ImportSwaggerApiConfig;
import org.hzero.imported.domain.entity.TemplateLine;
import org.hzero.mybatis.helper.SecurityTokenHelper;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.swagger.annotation.CustomPageRequest;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.*;


/**
 * 模板行管理接口
 *
 * @author shuangfei.zhu@hand-china.com 2018-12-7 16:42:53
 */
@Api(tags = ImportSwaggerApiConfig.TEMPLATE_LINE_SITE)
@RestController("templateLineSiteController.v1")
@RequestMapping(value = "/v1/template-lines")
public class TemplateLineSiteController extends BaseController {

    private final TemplateLineService lineService;

    @Autowired
    public TemplateLineSiteController(TemplateLineService lineService) {
        this.lineService = lineService;
    }

    @ApiOperation(value = "通过目标id查询模板行信息")
    @GetMapping
    @Permission(level = ResourceLevel.SITE)
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    @CustomPageRequest
    @ApiImplicitParams({
            @ApiImplicitParam(name = "columnCode", value = "列编码", paramType = "query"),
            @ApiImplicitParam(name = "columnName", value = "列名称", paramType = "query")
    })
    public ResponseEntity<Page<TemplateLine>> pageTemplateLinesByHeaderId(@RequestParam @ApiParam(value = "模板目标id", required = true) @Encrypt Long targetId, String columnCode, String columnName,
                                                                          @ApiIgnore @SortDefault(TemplateLine.FIELD_COLUMN_INDEX) PageRequest pageRequest) {
        return Results.success(lineService.pageTemplateLine(targetId, columnCode, columnName, pageRequest));
    }

    @ApiOperation(value = "通过行id查询模板行信息")
    @GetMapping(value = "/{lineId}")
    @Permission(level = ResourceLevel.SITE)
    public ResponseEntity<TemplateLine> detailTemplateLine(@PathVariable @ApiParam(value = "模板行id", required = true) @Encrypt Long lineId) {
        return Results.success(lineService.detailTemplateLine(lineId, null));
    }

    @ApiOperation(value = "创建模板行信息")
    @PostMapping
    @Permission(level = ResourceLevel.SITE)
    public ResponseEntity<TemplateLine> createTemplateLine(@RequestBody @Encrypt TemplateLine templateLine) {
        validObject(templateLine);
        return Results.success(lineService.createTemplateLine(templateLine));
    }

    @ApiOperation(value = "更新模板行信息")
    @PutMapping
    @Permission(level = ResourceLevel.SITE)
    public ResponseEntity<TemplateLine> updateTemplateLine(@RequestBody @Encrypt TemplateLine templateLine) {
        SecurityTokenHelper.validToken(templateLine);
        validObject(templateLine);
        return Results.success(lineService.updateTemplateLine(templateLine));
    }

    @ApiOperation(value = "通过模板行id进行删除")
    @DeleteMapping
    @Permission(level = ResourceLevel.SITE)
    public ResponseEntity<Void> deleteTemplateLine(@RequestBody @Encrypt TemplateLine templateLine) {
        SecurityTokenHelper.validToken(templateLine);
        lineService.deleteTemplateLine(templateLine.getId());
        return Results.success();
    }
}
