package org.hzero.boot.platform.data.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * 数据权限配置
 * </p>
 *
 * @author qingsheng.chen 2019/3/12 星期二 10:00
 */
@ConfigurationProperties(prefix = "hzero.data.permission")
public class PermissionDataProperties {
    /**
     * 数据库所有者，配置之后，数据库前缀的屏蔽规则会在数据库前缀后拼上 .dbOwner
     */
    private String dbOwner;

    public String getDbOwner() {
        return dbOwner;
    }

    public PermissionDataProperties setDbOwner(String dbOwner) {
        this.dbOwner = dbOwner;
        return this;
    }
}
