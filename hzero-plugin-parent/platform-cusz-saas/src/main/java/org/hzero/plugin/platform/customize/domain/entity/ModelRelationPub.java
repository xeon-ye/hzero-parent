package org.hzero.plugin.platform.customize.domain.entity;

import javax.persistence.Table;

import io.choerodon.mybatis.annotation.ModifyAudit;
import io.swagger.annotations.ApiModel;

/**
 * description
 *
 * @author txcaojiamen 2020/01/07 15:02
 */
@ApiModel("模型关系发布")
//@VersionAudit
@ModifyAudit
@Table(name = "hmde_model_relation_pub")
public class ModelRelationPub extends BaseModelRelation {

}
