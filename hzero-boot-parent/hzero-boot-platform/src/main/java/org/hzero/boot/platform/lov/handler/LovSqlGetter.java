package org.hzero.boot.platform.lov.handler;


/**
 * SQL语句获取器
 *
 * @author gaokuo.dai@hand-china.com 2018年6月26日下午8:57:54
 */
public interface LovSqlGetter {

    /**
     * 从平台服务中获取客制化SQL语句
     *
     * @param lovCode  值集代码
     * @param tenantId 租户ID
     * @return 客制化SQL语句
     */
    String getCustomSql(String lovCode, Long tenantId);


    /**
     * 从平台服务中获取反查SQL语句
     *
     * @param lovCode  值集代码
     * @param tenantId 租户ID
     * @return 反查SQL语句
     */
    String getTranslationSql(String lovCode, Long tenantId);
}
