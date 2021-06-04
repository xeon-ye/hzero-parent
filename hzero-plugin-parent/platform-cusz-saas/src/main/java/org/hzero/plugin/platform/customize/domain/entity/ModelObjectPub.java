package org.hzero.plugin.platform.customize.domain.entity;

import javax.persistence.Table;

import io.choerodon.mybatis.annotation.ModifyAudit;
import io.swagger.annotations.ApiModel;

/**
 * description
 *
 * @author txcaojiamen 2020/01/07 13:48
 */
@ApiModel("模型对象发布")
//@VersionAudit
@ModifyAudit
@Table(name = "hmde_model_object_pub")
public class ModelObjectPub extends BaseModelObject {


}
