package org.hzero.plugin.platform.customize.domain.entity;

import javax.persistence.Table;

import io.choerodon.mybatis.annotation.ModifyAudit;
import io.swagger.annotations.ApiModel;

/**
 * description
 *
 * @author txcaojiamen 2020/01/07 14:24
 */
@ApiModel("模型字段发布")
//@VersionAudit
@ModifyAudit
@Table(name = "hmde_model_field_pub")
public class ModelFieldPub extends BaseModelField {

}
