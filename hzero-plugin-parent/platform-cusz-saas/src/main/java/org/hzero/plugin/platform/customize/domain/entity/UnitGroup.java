package org.hzero.plugin.platform.customize.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import io.swagger.annotations.ApiModelProperty;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.hzero.starter.keyencrypt.core.Encrypt;

import javax.persistence.*;
import java.util.List;

/**
 * @author xiangyu.qi01@hand-china.com on 2020-01-07.
 */
@Table(name = "hpfm_cusz_unit_group")
/*@VersionAudit*/
@ModifyAudit
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UnitGroup extends AuditDomain {

    public static final String FIELD_ID = "unitGroupId";
    public static final String FIELD_GROUP_CODE = "groupCode";
    public static final String FIELD_GROUP_NAME = "groupName";
    public static final String FIELD_MENU_CODE = "menuCode";

    @Id
    @GeneratedValue
    @Encrypt(CustomizeConstants.EncryptKey.ENCRYPT_KEY_UNIT_GROUP)
    private Long unitGroupId;

    @ApiModelProperty("单元组编码")
    private String groupCode;

    @ApiModelProperty("单元组名称")
    private String groupName;

    /**
     * 单元所属菜单编码
     */
    @Column(name = "group_menu_code")
    @ApiModelProperty("菜单编码")
    private String menuCode;

    @Transient
    private List<Unit> units;

    public Long getUnitGroupId() {
        return unitGroupId;
    }

    public void setUnitGroupId(Long unitGroupId) {
        this.unitGroupId = unitGroupId;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public void setUnits(List<Unit> units) {
        this.units = units;
    }
}
