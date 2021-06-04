package org.hzero.plugin.platform.customize.domain.entity;

import javax.persistence.Table;

import io.choerodon.mybatis.annotation.ModifyAudit;
import io.swagger.annotations.ApiModel;

/**
 * description
 *
 * @author txcaojiamen 2020/01/07 15:28
 */
@ApiModel("模型关系关联字段发布")
//@VersionAudit
@ModifyAudit
@Table(name = "hmde_mod_rel_field_pub")
public class ModelRelationFieldPub extends BaseModelRelationField {

}
