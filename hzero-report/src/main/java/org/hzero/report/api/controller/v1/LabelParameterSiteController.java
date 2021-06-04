package org.hzero.report.api.controller.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hzero.core.base.BaseController;
import org.hzero.core.util.Results;
import org.hzero.mybatis.helper.SecurityTokenHelper;
import org.hzero.report.config.ReportSwaggerApiConfig;
import org.hzero.report.domain.entity.LabelParameter;
import org.hzero.report.domain.repository.LabelParameterRepository;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.swagger.annotation.Permission;

/**
 * 标签参数 管理 API
 *
 * @author fanghan.liu@hand-china.com 2019-11-27 10:35:39
 */
@Api(tags = ReportSwaggerApiConfig.LABEL_PARAMETER_SITE)
@RestController("labelParameterSiteController.v1")
@RequestMapping("/v1/label-parameters")
public class LabelParameterSiteController extends BaseController {

    @Autowired
    private LabelParameterRepository labelParameterRepository;

    @ApiOperation(value = "删除标签参数")
    @Permission(level = ResourceLevel.SITE)
    @DeleteMapping
    public ResponseEntity remove(Long tenantId, @Encrypt @RequestBody LabelParameter labelParameter) {
        labelParameter.setTenantId(tenantId);
        SecurityTokenHelper.validToken(labelParameter);
        labelParameterRepository.deleteByPrimaryKey(labelParameter.getLabelParameterId());
        return Results.success();
    }

}
