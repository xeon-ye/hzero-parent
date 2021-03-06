/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2016 abel533@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.choerodon.mybatis.helper;

import io.choerodon.mybatis.MapperException;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.annotation.VersionAudit;
import io.choerodon.mybatis.constant.ExpressionConstants;
import io.choerodon.mybatis.domain.AuditDomain;
import io.choerodon.mybatis.domain.EntityColumn;
import io.choerodon.mybatis.domain.EntityTable;
import io.choerodon.mybatis.domain.JoinTableSql;
import io.choerodon.mybatis.util.StringUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.jdbc.SQL;
import org.hzero.mybatis.common.Criteria;
import org.hzero.mybatis.common.query.*;
import org.hzero.mybatis.domian.IDynamicTableName;
import org.hzero.mybatis.helper.TenantLimitedHelper;
import org.hzero.mybatis.util.OGNL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.persistence.criteria.JoinType;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;

/**
 * ?????????SQL????????????
 *
 * @author liuzh
 * @since 2015-11-03 22:40
 */
public class SqlHelper {
    private static final String TENANT_ID = "tenant_id";
    private static Logger logger = LoggerFactory.getLogger(SqlHelper.class);

    public static final List<String> MODIFY_AUDIT_FIELDS = Collections.unmodifiableList(Arrays.asList("creationDate", "createdBy", "lastUpdateDate", "lastUpdatedBy"));

    public static final List<String> VERSION_AUDIT_FIELDS = Collections.singletonList("objectVersionNumber");
    private static final String LANG_BIND = "<bind name=\"lang\" value=\"@io.choerodon.mybatis.helper.LanguageHelper@language()\" />";
    private static final String AUDIT_BIND = "<bind name=\"audit\" value=\"@io.choerodon.mybatis.helper.AuditHelper@audit()\" />";
    private static final String OPTIONAL_BIND = "<bind name=\"optional\" value=\"@io.choerodon.mybatis.helper.OptionalHelper@optional()\" />";
    private static final String TENANT_BIND = "<bind name=\"__tenantLimit\" value=\"@org.hzero.mybatis.helper.TenantLimitedHelper@isOpen()\" />" +
            "<bind name=\"__tenantId\" value=\"@org.hzero.mybatis.helper.TenantLimitedHelper@tenantId()\" />" +
            "<bind name=\"__tenantIds\" value=\"@org.hzero.mybatis.helper.TenantLimitedHelper@tenantIds()\" />" +
            "<bind name=\"__equal\" value=\"@org.hzero.mybatis.helper.TenantLimitedHelper@isEqual()\" />";
    private static final String IF_TEST = "<if test=\"";
    private static final String IF = "</if>";
    private static final String AND = " AND ";
    private static final String OR = " OR ";
    private static final String LEFT_WHERE = "<where>";
    private static final String RIGHT_WHERE = "</where>";
    private static final String MULTI_LANGUAGE_JOIN = "multiLanguageJoin";
    private static final String FIELD_LANG = "lang";

    /**
     * getBindCache
     *
     * @param column column
     * @return String
     */
    public static String getBindCache(EntityColumn column) {
        return getBindCache(null, column);
    }

    public static String getWhereTenantLimit(Class<?> entityClass) {
        for (EntityColumn column : EntityHelper.getColumns(entityClass)) {
            if (TENANT_ID.equalsIgnoreCase(column.getColumn())) {
                return LEFT_WHERE + getTenantLimit(column) + RIGHT_WHERE;
            }
        }
        return "";
    }

    public static String getTenantLimit(Class<?> entityClass) {
        for (EntityColumn column : EntityHelper.getColumns(entityClass)) {
            if (TENANT_ID.equalsIgnoreCase(column.getColumn())) {
                return getTenantLimit(column);
            }
        }
        return "";
    }

    private static String getTenantLimit(EntityColumn column) {
        String columnName = column.getColumn();
        if (column.isMultiLanguage() || column.isId() && column.getTable().isMultiLanguage()) {
            columnName = "t." + columnName;
        }
        return getTenantBind() +
                "<if test=\"__tenantId != null and __tenantLimit and __equal\">AND " +
                columnName +
                " = #{__tenantId,jdbcType=BIGINT,javaType=java.lang.Long}</if><if test=\"__tenantIds != null and !__tenantIds.isEmpty() and __tenantLimit and !__equal\">AND " +
                columnName +
                " IN <foreach collection=\"__tenantIds\" separator=\",\" item=\"___tenantId\" open=\"(\" close=\")\">#{___tenantId,jdbcType=BIGINT,javaType=java.lang.Long}</foreach></if>";
    }

    private static String getTenantLimitSql(String tenantIdColumn) {
        if (TenantLimitedHelper.isOpen()) {
            if (TenantLimitedHelper.isEqual()) {
                return tenantIdColumn + " = " + TenantLimitedHelper.tenantId();
            } else {
                return tenantIdColumn + " IN (0, " + TenantLimitedHelper.tenantId() + ")";
            }
        }
        return "";
    }

    /**
     * getBindCache
     *
     * @param column     column
     * @param entityName entityName
     * @return String
     */
    public static String getBindCache(String entityName, EntityColumn column) {
        StringBuilder sql = new StringBuilder();
        sql.append("<bind name=\"");
        sql.append(column.getProperty()).append("_cache\" ");
        sql.append("value=\"");
        if (entityName != null) {
            sql.append(entityName).append(".");
        }
        sql.append(column.getProperty()).append("\"/>");
        return sql.toString();
    }

    /**
     * getBindValue
     *
     * @param value  value
     * @param column column
     * @return String
     */
    public static String getBindValue(EntityColumn column, String value) {
        StringBuilder sql = new StringBuilder();
        sql.append("<bind name=\"");
        sql.append(column.getProperty()).append("_bind\" ");
        sql.append("value='").append(value).append("'/>");
        return sql.toString();
    }

    public static String getLangBind() {
        return LANG_BIND;
    }

    public static String getTenantBind() {
        return TENANT_BIND;
    }

    public static String getAuditBind() {
        return AUDIT_BIND;
    }

    public static String getOptionalBind() {
        return OPTIONAL_BIND;
    }

    /**
     * getIfCacheNotNull
     *
     * @param contents contents
     * @param column   column
     * @return String
     */
    public static String getIfCacheNotNull(EntityColumn column, String contents) {
        StringBuilder sql = new StringBuilder();
        sql.append(IF_TEST).append(column.getProperty()).append("_cache != null\">");
        sql.append(contents);
        sql.append(IF);
        return sql.toString();
    }

    /**
     * ??????_cache == null
     *
     * @param contents contents
     * @param column   column
     * @return String
     */
    public static String getIfCacheIsNull(EntityColumn column, String contents) {
        StringBuilder sql = new StringBuilder();
        sql.append(IF_TEST).append(column.getProperty()).append("_cache == null\">");
        sql.append(contents);
        sql.append(IF);
        return sql.toString();
    }

    /**
     * ????????????!=null???????????????
     *
     * @param column   column
     * @param contents contents
     * @param empty    empty
     * @return String
     */
    public static String getIfNotNull(EntityColumn column, String contents, boolean empty) {
        return getIfNotNull(null, column, contents, empty);
    }

    /**
     * ????????????!=null???????????????
     *
     * @param entityName entityName
     * @param column     column
     * @param contents   contents
     * @param empty      empty
     * @return String
     */
    public static String getIfNotNull(String entityName, EntityColumn column, String contents, boolean empty) {
        StringBuilder sql = new StringBuilder();
        sql.append(IF_TEST);
        if (StringUtil.isNotEmpty(entityName)) {
            sql.append(entityName).append(".");
        }
        sql.append(column.getProperty()).append(" != null");
        if (empty && column.getJavaType().equals(String.class)) {
            sql.append(AND);
            if (StringUtil.isNotEmpty(entityName)) {
                sql.append(entityName).append(".");
            }
            sql.append(column.getProperty()).append(" != '' ");
        }
        sql.append("\">");
        sql.append(contents);
        sql.append(IF);
        return sql.toString();
    }

    /**
     * ????????????==null???????????????
     *
     * @param column   column
     * @param contents contents
     * @param empty    empty
     * @return String
     */
    public static String getIfIsNull(EntityColumn column, String contents, boolean empty) {
        return getIfIsNull(null, column, contents, empty);
    }

    /**
     * ????????????==null???????????????
     *
     * @param entityName entityName
     * @param column     column
     * @param contents   contents
     * @param empty      empty
     * @return String
     */
    public static String getIfIsNull(String entityName, EntityColumn column, String contents, boolean empty) {
        StringBuilder sql = new StringBuilder();
        sql.append(IF_TEST);
        if (StringUtil.isNotEmpty(entityName)) {
            sql.append(entityName).append(".");
        }
        sql.append(column.getProperty()).append(" == null");
        if (empty && column.getJavaType().equals(String.class)) {
            sql.append(" or ");
            if (StringUtil.isNotEmpty(entityName)) {
                sql.append(entityName).append(".");
            }
            sql.append(column.getProperty()).append(" == '' ");
        }
        sql.append("\">");
        sql.append(contents);
        sql.append(IF);
        return sql.toString();
    }

    /**
     * ????????????optionals.contains()???????????????
     *
     * @param optionalsName optionalsName
     * @param column        column
     * @param contents      contents
     * @return String
     */
    public static String getIfContains(String optionalsName, EntityColumn column, String contents) {
        StringBuilder sql = new StringBuilder();
        sql.append(IF_TEST);
        sql.append(optionalsName).append(".contains(&quot;").append(column.getProperty()).append("&quot;)");
        sql.append("\">");
        sql.append(contents);
        sql.append(IF);
        return sql.toString();
    }

    /**
     * ???????????????????????????id,name,code...
     *
     * @param entityClass entityClass
     * @return String
     */
    public static String getAllColumns(Class<?> entityClass) {
        Set<EntityColumn> columnList = EntityHelper.getColumns(entityClass);
        StringBuilder sql = new StringBuilder();
        for (EntityColumn entityColumn : columnList) {
            if (EntityHelper.getTableByEntity(entityClass).isMultiLanguage()
                    && (entityColumn.isMultiLanguage() || entityColumn.isId())) {
                sql.append(entityColumn.isMultiLanguage() ? "t." : "b.");
            }
            sql.append(entityColumn.getColumn()).append(",");
        }
        return sql.substring(0, sql.length() - 1);
    }

    /**
     * select xxx,xxx...
     *
     * @param entityClass entityClass
     * @return String
     */
    public static String selectAllColumns(Class<?> entityClass) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(getAllColumns(entityClass));
        sql.append(" ");
        return sql.toString();
    }

    /**
     * select count(x)
     *
     * @param entityClass entityClass
     * @return String
     */
    public static String selectCount(Class<?> entityClass) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        Set<EntityColumn> pkColumns = EntityHelper.getPkColumns(entityClass);
        processSelectCount(entityClass, sql, pkColumns);
        return sql.toString();
    }

    private static void processSelectCount(Class<?> entityClass, StringBuilder sql, Set<EntityColumn> pkColumns) {
        if (pkColumns.size() == 1) {
            EntityTable table = EntityHelper.getTableByEntity(entityClass);
            if (table.isMultiLanguage()) {
                sql.append("COUNT(t.").append(pkColumns.iterator().next().getColumn()).append(") ");
            } else {
                sql.append("COUNT(").append(pkColumns.iterator().next().getColumn()).append(") ");
            }
        } else {
            sql.append("COUNT(*) ");
        }
    }

    /**
     * select case when count(x) more than 0 then 1 else 0 end
     *
     * @param entityClass entityClass
     * @return String
     */
    public static String selectCountExists(Class<?> entityClass) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT CASE WHEN ");
        Set<EntityColumn> pkColumns = EntityHelper.getPkColumns(entityClass);
        processSelectCount(entityClass, sql, pkColumns);
        sql.append(" > 0 THEN 1 ELSE 0 END AS result ");
        return sql.toString();
    }

    /**
     * from tableName - ????????????
     *
     * @param entityClass      entityClass
     * @param defaultTableName defaultTableName
     * @return String
     */
    public static String selectFromTableTl(Class<?> entityClass, String defaultTableName) {
        StringBuilder builder = new StringBuilder();
        builder.append(" FROM ");
        builder.append(defaultTableName);
        builder.append(" ");
        EntityTable table = EntityHelper.getTableByEntity(entityClass);
        if (table.isMultiLanguage()) {
            builder.append("b ")
                    .append("<bind name=\"lang\" value=\"@io.choerodon.mybatis.helper.LanguageHelper@language()\"/> ")
                    .append("LEFT JOIN ")
                    .append(defaultTableName)
                    .append("_tl t ")
                    .append("ON (");
            for (EntityColumn column : table.getEntityClassPkColumns()) {
                builder.append("b.").append(column.getColumn())
                        .append("=t.").append(column.getColumn())
                        .append(AND);
            }
            builder.append("t.lang=#{lang}");
            builder.append(")");
        }
        return builder.toString();
    }

    /**
     * update tableName - ????????????
     *
     * @param entityClass      entityClass
     * @param defaultTableName defaultTableName
     * @return String
     */
    public static String updateTable(Class<?> entityClass, String defaultTableName) {
        return updateTable(entityClass, defaultTableName, null);
    }

    /**
     * update tableName - ????????????
     *
     * @param entityClass      entityClass
     * @param defaultTableName ????????????
     * @param entityName       ??????
     * @return String
     */
    public static String updateTable(Class<?> entityClass, String defaultTableName, String entityName) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(defaultTableName);
        sql.append(" ");
        return sql.toString();
    }

    /**
     * delete tableName - ????????????
     *
     * @param entityClass      entityClass
     * @param defaultTableName defaultTableName
     * @return String
     */
    public static String deleteFromTable(Class<?> entityClass, String defaultTableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ");
        sql.append(defaultTableName);
        sql.append(" ");
        return sql.toString();
    }

    /**
     * insert into tableName - ????????????
     *
     * @param entityClass      entityClass
     * @param defaultTableName defaultTableName
     * @return String
     */
    public static String insertIntoTable(Class<?> entityClass, String defaultTableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        sql.append(defaultTableName);
        sql.append(" ");
        return sql.toString();
    }

    /**
     * insert table()???
     *
     * @param entityClass entityClass
     * @param skipId      ?????????????????????id??????
     * @param notNull     ????????????!=null
     * @param notEmpty    ????????????String??????!=''
     * @return String
     */
    public static String insertColumns(Class<?> entityClass, boolean skipId, boolean notNull, boolean notEmpty) {
        return insertColumns(null, entityClass, skipId, notNull, notEmpty);
    }

    /**
     * insert table()???
     *
     * @param entityName  entityName
     * @param entityClass entityClass
     * @param skipId      ?????????????????????id??????
     * @param notNull     ????????????!=null
     * @param notEmpty    ????????????String??????!=''
     * @return String
     */
    public static String insertColumns(String entityName, Class<?> entityClass, boolean skipId, boolean notNull, boolean notEmpty) {
        StringBuilder sql = new StringBuilder();
        sql.append("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
        //???????????????
        Set<EntityColumn> columnList = EntityHelper.getColumns(entityClass);
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        for (EntityColumn column : columnList) {
            if (doContinue(skipId, column)) {
                continue;
            }
            if (notNull) {
                sql.append(SqlHelper.getIfNotNull(entityName, column, column.getColumn() + ",", notEmpty));
            } else if (column.isIdentity()) {
                sql.append(SqlHelper.getIfCacheNotNull(column, column.getColumn() + ","));
            } else {
                sql.append(column.getColumn()).append(",");
            }
        }
        sql.append("</trim>");
        return sql.toString();
    }

    private static boolean doContinue(boolean skipId, EntityColumn column) {
        if (!column.isInsertable()) {
            return true;
        }
        return skipId && column.isId();
    }

    /**
     * insert-values()???
     *
     * @param entityClass entityClass
     * @param skipId      ?????????????????????id??????
     * @param notNull     ????????????!=null
     * @param notEmpty    ????????????String??????!=''
     * @return String
     */
    public static String insertValuesColumns(Class<?> entityClass, boolean skipId, boolean notNull, boolean notEmpty) {
        StringBuilder sql = new StringBuilder();
        sql.append("<trim prefix=\"VALUES (\" suffix=\")\" suffixOverrides=\",\">");
        //???????????????
        Set<EntityColumn> columnList = EntityHelper.getColumns(entityClass);
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        for (EntityColumn column : columnList) {
            if (doContinue(skipId, column)) {
                continue;
            }
            if (notNull) {
                sql.append(SqlHelper.getIfNotNull(column, column.getColumnHolder() + ",", notEmpty));
            } else {
                sql.append(column.getColumnHolder() + ",");
            }
        }
        sql.append("</trim>");
        return sql.toString();
    }

    /**
     * update set???
     *
     * @param entityClass entityClass
     * @param entityName  ???????????????
     * @param notNull     ????????????!=null
     * @param notEmpty    ????????????String??????!=''
     * @return String
     */
    public static String updateSetColumns(Class<?> entityClass, String entityName, boolean notNull, boolean notEmpty) {
        StringBuilder sql = new StringBuilder();
        sql.append("<set>");
        //???????????????
        Set<EntityColumn> columnList = EntityHelper.getColumns(entityClass);
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        for (EntityColumn column : columnList) {
            if (!column.isId() && column.isUpdatable()) {
                if (notNull) {
                    sql.append(SqlHelper.getIfNotNull(entityName, column,
                            column.getColumnEqualsHolder(entityName) + ",", notEmpty));
                } else {
                    sql.append(column.getColumnEqualsHolder(entityName) + ",");
                }
            }
        }
        sql.append("</set>");
        return sql.toString();
    }

    /**
     * update set???
     *
     * @param entityClass entityClass
     * @param entityName  ???????????????
     * @param notNull     ????????????!=null
     * @param notEmpty    ????????????String??????!=''
     * @return String String
     * @throws Exception Exception
     */
    public static String updateSetColumnsAndVersion(Class<?> entityClass,
                                                    String entityName,
                                                    boolean notNull,
                                                    boolean notEmpty) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("<set>");
        //???????????????
        Set<EntityColumn> columnSet = EntityHelper.getColumns(entityClass);
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        boolean modifyAudit = entityClass.isAnnotationPresent(ModifyAudit.class);
        boolean versionAudit = entityClass.isAnnotationPresent(VersionAudit.class);
        for (EntityColumn column : columnSet) {
            String columnName = column.getProperty();
            //???????????????????????????????????????
            if (fillSelfMaintainFileds(sql, modifyAudit, versionAudit, column, columnName)) {
                continue;
            }
            if (!column.isId() && column.isUpdatable()) {
                if (notNull) {
                    sql.append(SqlHelper.getIfNotNull(entityName, column,
                            column.getColumnEqualsHolder(entityName) + ",", notEmpty));
                } else {
                    sql.append(column.getColumnEqualsHolder(entityName) + ",");
                }
            }
        }
        sql.append("</set>");
        return sql.toString();
    }

    private static boolean fillSelfMaintainFileds(StringBuilder sql, boolean modifyAudit,
                                                  boolean versionAudit, EntityColumn column,
                                                  String columnName) {
        if (modifyAudit && SqlHelper.MODIFY_AUDIT_FIELDS.contains(column.getProperty())) {
            if ("lastUpdateDate".equals(columnName)) {
                sql.append("LAST_UPDATE_DATE = #{audit.now,jdbcType=TIMESTAMP},");
            } else if ("lastUpdatedBy".equals(columnName)) {
                sql.append("LAST_UPDATED_BY = #{audit.user,jdbcType=BIGINT},");
            }
            return true;
        }
        if (versionAudit && SqlHelper.VERSION_AUDIT_FIELDS.contains(column.getProperty())) {
            if ("objectVersionNumber".equals(columnName)) {
                sql.append("OBJECT_VERSION_NUMBER = OBJECT_VERSION_NUMBER+1,");
            } else {
                throw new MapperException("?????????Version???" + columnName);
            }
            return true;
        }
        return false;
    }

    /**
     * where????????????
     *
     * @param entityClass entityClass
     * @return String
     */
    public static String wherePkColumnsTl(Class<?> entityClass) {
        StringBuilder sql = new StringBuilder();
        sql.append(LEFT_WHERE);
        //???????????????
        Set<EntityColumn> columnList = EntityHelper.getPkColumns(entityClass);
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        for (EntityColumn column : columnList) {
            sql.append(AND).append(column.getColumnEqualsHolderTl(null));
        }
        for (EntityColumn column : EntityHelper.getColumns(entityClass)) {
            if (TENANT_ID.equalsIgnoreCase(column.getColumn())) {
                sql.append(getTenantLimit(column));
            }
        }
        sql.append(RIGHT_WHERE);
        return sql.toString();
    }


    /**
     * ??????where????????????
     *
     * @param entityClass entityClass
     * @return String
     */
    public static String wherePkColumns(Class<?> entityClass) {
        StringBuilder sql = new StringBuilder();
        sql.append(LEFT_WHERE);
        //???????????????
        Set<EntityColumn> columnList = EntityHelper.getPkColumns(entityClass);
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        for (EntityColumn column : columnList) {
            sql.append(AND + column.getColumnEqualsHolder(null));
        }
        sql.append(RIGHT_WHERE);
        return sql.toString();
    }

    /**
     * ??????where???OBJECT_VERSION_NUMBER
     *
     * @param entityClass entityClass
     * @return String
     */
    public static String wherePrimaryAndVersion(Class<?> entityClass) {
        boolean versionAudit = entityClass.isAnnotationPresent(VersionAudit.class);
        StringBuilder sql = new StringBuilder();
        sql.append(LEFT_WHERE);
        // ???????????????
        Set<EntityColumn> columnList = EntityHelper.getPkColumns(entityClass);
        // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        for (EntityColumn column : columnList) {
            sql.append(AND).append(column.getColumnEqualsHolder(null));
        }
        if (versionAudit) {
            sql.append(" AND OBJECT_VERSION_NUMBER = #{objectVersionNumber}");
        }
        sql.append(RIGHT_WHERE);
        return sql.toString();
    }

    /**
     * where????????????????????????????????????!=null
     *
     * @param entityClass entityClass
     * @param empty       empty
     * @return String
     */
    public static String whereAllIfColumns(Class<?> entityClass, boolean empty) {
        StringBuilder sql = new StringBuilder();
        sql.append(LEFT_WHERE);
        //???????????????
        Set<EntityColumn> columnList = EntityHelper.getColumns(entityClass);
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        for (EntityColumn column : columnList) {
            sql.append(getIfNotNull(column, AND + column.getColumnEqualsHolder(null), empty));
        }
        sql.append(RIGHT_WHERE);
        return sql.toString();
    }

    /**
     * ??????????????????sql
     *
     * @param entityClass entityClass
     * @param empty       empty
     * @return String
     */
    public static String whereAllIfColumnsTl(Class<?> entityClass, boolean empty) {
        StringBuilder sql = new StringBuilder();
        sql.append(LEFT_WHERE);
        //???????????????
        Set<EntityColumn> columnList = EntityHelper.getColumns(entityClass);
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        for (EntityColumn column : columnList) {
            sql.append(getIfNotNull(column, AND + column.getColumnEqualsHolderTl(null), empty));
            if (TENANT_ID.equalsIgnoreCase(column.getColumn())) {
                sql.append(getTenantLimit(column));
            }
        }
        sql.append(RIGHT_WHERE);
        return sql.toString();
    }

    /**
     * ???????????????orderBy????????????????????????
     *
     * @param entityClass entityClass
     * @return String
     */
    public static String orderByDefault(Class<?> entityClass) {
        StringBuilder sql = new StringBuilder();
        String orderByClause = EntityHelper.getOrderByClause(entityClass);
        if (orderByClause.length() > 0) {
            sql.append(" ORDER BY ");
            sql.append(orderByClause);
        }
        return sql.toString();
    }


    /**
     * condition????????????orderBy????????????????????????orderBy
     *
     * @param entityClass entityClass
     * @return String
     */
    public static String conditionOrderBy(Class<?> entityClass) {
        StringBuilder sql = new StringBuilder();
        sql.append("<if test=\"orderByClause != null\">");
        sql.append("order by ${orderByClause}");
        sql.append(IF);
        String orderByClause = EntityHelper.getOrderByClause(entityClass);
        if (orderByClause.length() > 0) {
            sql.append("<if test=\"orderByClause == null\">");
            sql.append("ORDER BY ").append(orderByClause);
            sql.append(IF);
        }
        return sql.toString();
    }

    /**
     * Example????????????where???????????????????????????Example?????????
     *
     * @return String
     */
    public static String exampleWhereClause() {
        return "<if test=\"_parameter != null and oredCriteria != null and oredCriteria.size() > 0\">"
                + "<where>\n"
                + "<trim prefix=\"(\" prefixOverrides=\"and|or\" suffix=\")\">\n"
                + "  <foreach collection=\"oredCriteria\" item=\"criteria\">\n"
                + "    <if test=\"criteria.valid\">\n"
                + "    ${criteria.andOr}"
                + "      <trim prefix=\"(\" prefixOverrides=\"and|or\" suffix=\")\">\n"
                + "        <foreach collection=\"criteria.criteria\" item=\"criterion\">\n"
                + "          <choose>\n"
                + "            <when test=\"criterion.noValue\">\n"
                + "              ${criterion.andOr} ${criterion.condition}\n"
                + "            </when>\n"
                + "            <when test=\"criterion.singleValue\">\n"
                + "              ${criterion.andOr} ${criterion.condition} #{criterion.value}\n"
                + "            </when>\n"
                + "            <when test=\"criterion.betweenValue\">\n"
                + "              ${criterion.andOr} (${criterion.condition} #{criterion.value} and #{criterion.secondValue})\n"
                + "            </when>\n"
                + "            <when test=\"criterion.listValue\">\n"
                + "              ${criterion.andOr} ${criterion.condition}\n"
                + "              <foreach close=\")\" collection=\"criterion.value\" "
                + "                       item=\"listItem\" open=\"(\" separator=\",\">\n"
                + "                #{listItem}\n"
                + "              </foreach>\n"
                + "            </when>\n"
                + "          </choose>\n"
                + "        </foreach>\n"
                + "      </trim>\n"
                + "    </if>\n"
                + "  </foreach>\n"
                + "</trim>"
                + "</where>"
                + "</if>";
    }

    /**
     * Condition-Update??????where?????????????????????????????????Example???@Param("example")?????????
     *
     * @return String
     */
    public static String updateByExampleWhereClause() {
        return "<where>\n"
                + "<trim prefix=\"(\" prefixOverrides=\"and|or\" suffix=\")\">\n"
                + "  <foreach collection=\"example.oredCriteria\" item=\"criteria\">\n"
                + "    <if test=\"criteria.valid\">\n"
                + "    ${criteria.andOr}"
                + "      <trim prefix=\"(\" prefixOverrides=\"and\" suffix=\")\">\n"
                + "        <foreach collection=\"criteria.criteria\" item=\"criterion\">\n"
                + "          <choose>\n"
                + "            <when test=\"criterion.noValue\">\n"
                + "              ${criterion.andOr} ${criterion.condition}\n"
                + "            </when>\n"
                + "            <when test=\"criterion.singleValue\">\n"
                + "              ${criterion.andOr} ${criterion.condition} #{criterion.value}\n"
                + "            </when>\n"
                + "            <when test=\"criterion.betweenValue\">\n"
                + "              ${criterion.andOr} (${criterion.condition} #{criterion.value} and #{criterion.secondValue})\n"
                + "            </when>\n"
                + "            <when test=\"criterion.listValue\">\n"
                + "              ${criterion.andOr} ${criterion.condition}\n"
                + "              <foreach close=\")\" collection=\"criterion.value\" "
                + "                       item=\"listItem\" open=\"(\" separator=\",\">\n"
                + "                #{listItem}\n"
                + "              </foreach>\n"
                + "            </when>\n"
                + "          </choose>\n"
                + "        </foreach>\n"
                + "      </trim>\n"
                + "    </if>\n"
                + "  </foreach>\n"
                + "</trim>"
                + "</where>";
    }

    /**
     * HZero????????????
     */

    /**
     * ???????????? - ??????????????????
     *
     * @param entityClass
     * @param tableName
     * @return
     */
    public static String getDynamicTableName(Class<?> entityClass, String tableName) {
        if (IDynamicTableName.class.isAssignableFrom(entityClass)) {
            StringBuilder sql = new StringBuilder();
            sql.append("<choose>");
            sql.append("<when test=\"@tk.mybatis.mapper.util.OGNL@isDynamicParameter(_parameter) and dynamicTableName != null and dynamicTableName != ''\">");
            sql.append("${dynamicTableName}\n");
            sql.append("</when>");
            //??????????????????????????????????????????
            sql.append("<otherwise>");
            sql.append(tableName);
            sql.append("</otherwise>");
            sql.append("</choose>");
            return sql.toString();
        } else {
            return tableName;
        }
    }

    /**
     * condition ?????? for update
     *
     * @return
     */
    public static String conditionCheck(Class<?> entityClass) {
        StringBuilder sql = new StringBuilder();
        sql.append("<bind name=\"checkConditionEntityClass\" value=\"@org.hzero.mybatis.util.OGNL@checkConditionEntityClass(_parameter, '");
        sql.append(entityClass.getCanonicalName());
        sql.append("')\"/>");
        return sql.toString();
    }

    /**
     * from tableName - ????????????
     *
     * @param entityClass
     * @param defaultTableName
     * @return
     */
    public static String fromTable(Class<?> entityClass, String defaultTableName) {
        StringBuilder sql = new StringBuilder();
        sql.append(" FROM ");
        sql.append(getDynamicTableName(entityClass, defaultTableName));
        sql.append(" ");
        return sql.toString();
    }

    /**
     * condition ?????? for update
     *
     * @return
     */
    public static String conditionForUpdate() {
        StringBuilder sql = new StringBuilder();
        sql.append("<if test=\"@org.hzero.mybatis.util.OGNL@hasForUpdate(_parameter)\">");
        sql.append("FOR UPDATE");
        sql.append("</if>");
        return sql.toString();
    }

    /**
     * condition????????????????????????
     *
     * @return
     */
    public static String conditionSelectColumns(Class<?> entityClass) {
        StringBuilder sql = new StringBuilder();
        sql.append("<choose>");
        sql.append("<when test=\"@org.hzero.mybatis.util.OGNL@hasSelectColumns(_parameter)\">");
        sql.append("<foreach collection=\"_parameter.selectColumns\" item=\"selectColumn\" separator=\",\">");
        sql.append("${selectColumn}");
        sql.append("</foreach>");
        sql.append("</when>");
        //??????????????????????????????????????????
        sql.append("<otherwise>");
        sql.append(getAllColumns(entityClass));
        sql.append("</otherwise>");
        sql.append("</choose>");
        return sql.toString();
    }

    /**
     * ????????????????????????SQL.
     *
     * @param recode   ??????
     * @param criteria ????????????
     * @return sql
     */
    public static String buildSelectSelectiveSql(AuditDomain recode, Criteria criteria) {
        EntityTable table = EntityHelper.getEntityTable(recode.getClass());
        List<Selection> selectFields = new ArrayList<>(50);
        List<Selection> selections = criteria.getSelectFields();
        if (selections == null || selections.isEmpty()) {
            for (EntityColumn column : table.getEntityClassColumns()) {
                if (criteria.getExcludeSelectFields() == null
                        || !criteria.getExcludeSelectFields().contains(column.getProperty())) {
                    selectFields.add(new Selection(column.getProperty()));
                }
            }
            /*
             * Transient?????????joinColumn???????????????????????????
             */
            for (EntityColumn column : table.getEntityClassTransientColumns()) {
                if (column.getJoinColumn() != null && (criteria.getExcludeSelectFields() == null
                        || !criteria.getExcludeSelectFields().contains(column.getProperty()))) {
                    selectFields.add(new Selection(column.getProperty()));
                }
            }
        } else {
            for (Selection selection : selections) {
                if (criteria.getExcludeSelectFields() == null
                        || !criteria.getExcludeSelectFields().contains(selection.getField())) {
                    selectFields.add(selection);
                }
            }
        }


        String sql = new SQL() {
            {
                // SELECT
                for (Selection selection : selectFields) {
                    String selectionSql = generateSelectionSQL(recode, selection);
                    if (StringUtils.isNotEmpty(selectionSql)) {
                        SELECT(selectionSql);
                    }
                }
                // FROM
                FROM(table.getName() + " " + table.getAlias());

                // JOIN
                for (Map.Entry<String, EntityColumn> entry : table.getJoinMapping().entrySet()) {
                    EntityColumn column = entry.getValue();
                    JoinTable jt = column.findJoinTableByName(entry.getKey());
                    List<JoinTableSql> joinTableSqlList = buildJoinSQL(recode, column, jt, selectFields);
                    if (!CollectionUtils.isEmpty(joinTableSqlList)) {
                        joinTableSqlList.forEach(joinTableSql -> {

                            JoinType joinType = joinTableSql.getJoinType();
                            switch (joinType) {
                                case LEFT:
                                    LEFT_OUTER_JOIN(joinTableSql.getContent());
                                    break;
                                case INNER:
                                    INNER_JOIN(joinTableSql.getContent());
                                    break;
                                case RIGHT:
                                    RIGHT_OUTER_JOIN(joinTableSql.getContent());
                                    break;
                            }
                        });
                    }
                }
                //WHERE
                StringBuilder whereSqlBuilder = new StringBuilder(generateWhereClauseSQL(recode, criteria));
                for (EntityColumn column : table.getEntityClassColumns()) {
                    if (TENANT_ID.equalsIgnoreCase(column.getColumn()) && TenantLimitedHelper.isOpen()) {
                        whereSqlBuilder.append(" AND ").append(getTenantLimitSql(table.getAlias() + ".TENANT_ID"));
                    }
                }
                String whereSql = whereSqlBuilder.toString();
                if (StringUtils.isNotBlank(whereSql)) {
                    WHERE(whereSql);
                }

                // ORDER BY
                List<SortField> sortFields = criteria.getSortFields();
                if (sortFields != null && !sortFields.isEmpty()) {
                    for (SortField sortField : sortFields) {
                        for (EntityColumn sortColumn : table.getSortColumns()) {
                            if (sortColumn.getProperty().equals(sortField.getField())) {
                                ORDER_BY(findColumnNameByField(recode, sortField.getField(), false) + sortField.getSortType().sql());
                                break;
                            }
                        }
                    }
                } else {
                    for (EntityColumn sortColumn : table.getSortColumns()) {
                        if (sortColumn.getOrderBy() != null) {
                            ORDER_BY(findColumnNameByField(recode, sortColumn, false) + " " + sortColumn.getOrderBy());
                        }
                    }
                }


            }
        }.usingAppender(new StringBuilder()).toString();
        return sql;
    }

    /**
     * <p>
     * ??????JOIN??????SQL.
     *     <ul>
     *         <li> ?????? mutiLanguage </li>
     *         <li> @Jointable ??????????????????????????????????????????????????????????????? </li>
     *     </ul>
     * </p>
     *
     * @param recode      recode
     * @param localColumn ??????????????????
     * @param joinTable   @JoinTable
     * @param selections
     * @return
     */
    private static List<JoinTableSql> buildJoinSQL(AuditDomain recode, EntityColumn localColumn, JoinTable joinTable, List<Selection> selections) {
        EntityTable localTable = EntityHelper.getEntityTable(recode.getClass());
        EntityTable foreignTable = EntityHelper.getEntityTable(joinTable.target());

        boolean foundJoinColumn = false;
        for (Selection selection : selections) {
            EntityColumn entityColumn = localTable.findColumnByProperty(selection.getField());
            if (entityColumn != null && entityColumn.getJoinColumn() != null && joinTable.name().equals(entityColumn.getJoinColumn().joinName())) {
                foundJoinColumn = true;
                break;
            }
        }

        if (foundJoinColumn) {
            if (MULTI_LANGUAGE_JOIN.equals(joinTable.name())) {
                return generateMultiLanguageJoinSQL(localColumn, localTable, foreignTable, joinTable);
            } else {
                return generateJoinTableJoinSQL(localColumn, localTable, foreignTable, joinTable);
            }
        }

        return null;
    }

    /**
     * ?????? MultiLanguage ??????????????? join sql
     *
     * @param localColumn
     * @param localTable
     * @param foreignTable
     * @param joinTable
     * @return
     */
    private static List<JoinTableSql> generateMultiLanguageJoinSQL(EntityColumn localColumn, EntityTable localTable, EntityTable foreignTable, JoinTable joinTable) {
        String jointTableName = getMultiLanguageTableName(foreignTable);
        String joinSql = generateForeignJoinOnSql(jointTableName, localColumn, localTable, foreignTable, joinTable);

        List<JoinTableSql> joinSqlList = new ArrayList<>();
        joinSqlList.add(new JoinTableSql(joinTable.type(), joinSql));

        return joinSqlList;
    }

    /**
     * ?????? JoinTable??????????????? Join sql
     *
     * @param localColumn
     * @param localTable
     * @param foreignTable
     * @param joinTable
     * @return
     */
    private static List<JoinTableSql> generateJoinTableJoinSQL(EntityColumn localColumn, EntityTable localTable, EntityTable foreignTable, JoinTable joinTable) {
        List<JoinTableSql> joinSqlList = new ArrayList<>();

        //generate sql for  foreignTable

        String foreignTableJoinSql = generateForeignJoinOnSql(foreignTable.getName(), localColumn, localTable, foreignTable, joinTable);
        joinSqlList.add(new JoinTableSql(joinTable.type(), foreignTableJoinSql));

        //generate sql for  foreignTLTable
        if (joinTable.joinMultiLanguageTable()) {
            String jointTableName = getMultiLanguageTableName(foreignTable);

            String foreignMultiLanguageJoinSql = generateForeignMutilanguageJoinOnSql(jointTableName, localTable, foreignTable, joinTable);


            joinSqlList.add(new JoinTableSql(joinTable.type(), foreignMultiLanguageJoinSql));
        }

        return joinSqlList;
    }

    /**
     * ?????? ?????? ??? ????????????????????? ???Join on??????
     *
     * @param joinTLTableName ???????????????????????????
     * @param localTable      ??????
     * @param foreignTable    ?????????????????????
     * @param joinTable       JoinTable
     * @return joinOnSql
     */
    private static String generateForeignMutilanguageJoinOnSql(String joinTLTableName, EntityTable localTable, EntityTable foreignTable, JoinTable joinTable) {
        StringBuilder sb = new StringBuilder();
        String joinKey = EntityHelper.buildJoinKey(joinTable);
        String joinTLKey = EntityHelper.buildJoinTLKey(joinTable);
        sb.append(joinTLTableName).append(" ").append(localTable.getAlias(joinTLKey)).append(" ON ");

        sb.append(localTable.getAlias(joinTLKey)).append(".").append(foreignTable.getKeyColumns()[0]).append(" = ");
        sb.append(localTable.getAlias(joinKey)).append(".").append(foreignTable.getKeyColumns()[0]).append(" ");

        String langExpression = "#{lang,jdbcType=VARCHAR,javaType=java.lang.String}";

        JoinOn[] joinOns = joinTable.on();
        for (JoinOn joinOn : joinOns) {
            String joinField = joinOn.joinField();
            if (FIELD_LANG.equals(joinField)) {
                langExpression = joinOn.joinExpression();
            }

        }

        sb.append(" AND ").append(localTable.getAlias(joinTLKey)).append(".").append(FIELD_LANG.toUpperCase()).append(" = ").append(lang(langExpression));

        return sb.toString();
    }

    private static String lang(String langExpression) {
        if ("__current_locale".equals(langExpression)) {
            return OGNL.language();
        }
        return langExpression;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param entityTable
     * @return
     */
    private static String getMultiLanguageTableName(EntityTable entityTable) {
        String jointTableName = entityTable.getName();

        if (jointTableName.toUpperCase().endsWith("_B")) {
            jointTableName = jointTableName.substring(0, jointTableName.length() - 2) + "_TL";
        } else {
            jointTableName = jointTableName + "_TL";
        }
        return jointTableName;
    }

    /**
     * ?????? ?????????????????????join on ??????
     *
     * @param jointTableName ??????????????????
     * @param localColumn    ????????????
     * @param localTable     ??????
     * @param foreignTable   ??????
     * @param joinTable      joinTable??????
     * @return joinOnSql
     */
    private static String generateForeignJoinOnSql(String jointTableName, EntityColumn localColumn, EntityTable localTable, EntityTable foreignTable, JoinTable joinTable) {
        StringBuilder sb = new StringBuilder();
        String joinKey = EntityHelper.buildJoinKey(joinTable);
        sb.append(jointTableName).append(" ").append(localTable.getAlias(joinKey)).append(" ON ");

        JoinOn[] joinOns = joinTable.on();
        for (int i = 0, j = joinOns.length; i < j; i++) {
            JoinOn joinOn = joinOns[i];
            String joinField = joinOn.joinField();
            if (StringUtils.isEmpty(joinField) || (!MULTI_LANGUAGE_JOIN.equals(joinTable.name()) && FIELD_LANG.equals(joinField))) {
                continue;
            }

            if (i != 0) {
                sb.append(" AND ");
            }
            EntityColumn foreignColumn = foreignTable.findColumnByProperty(joinField);
            String columnName = foreignColumn != null ? foreignColumn.getColumn() : StringUtil.camelhumpToUnderline(joinField);
            if (StringUtils.isEmpty(joinOn.joinExpression())) {
                sb.append(localTable.getAlias()).append(".").append(localColumn.getColumn()).append(" = ");
                sb.append(localTable.getAlias(joinKey)).append(".").append(columnName);
            } else {
                sb.append(localTable.getAlias(joinKey)).append(".").append(columnName);
                sb.append(" = ").append(processJoinExpression(localTable.getAlias(), joinOn.joinExpression()));
            }
        }
        return sb.toString();
    }

    private static String processJoinExpression(String tableAlias, String expression) {
        return lang(expression).replaceAll(ExpressionConstants.TABLE_ALIAS_REG, tableAlias + ".");
    }

    /**
     * ??????????????????SQL.
     *
     * @param recode
     * @param selection
     * @return SQL
     */
    private static String generateSelectionSQL(AuditDomain recode, Selection selection) {
        return findColumnNameByField(recode, selection.getField(), true);
    }

    /**
     * ???????????????????????????SQL.
     *
     * @param recode
     * @param field
     * @return
     */
    private static String findColumnNameByField(AuditDomain recode, String field, boolean withAlias) {
        EntityTable table = EntityHelper.getEntityTable(recode.getClass());
        EntityColumn entityColumn = table.findColumnByProperty(field);
        return findColumnNameByField(recode, entityColumn, withAlias);
    }

    /**
     * ???????????????????????????SQL.
     *
     * @param recode
     * @param entityColumn
     * @return
     */
    private static String findColumnNameByField(AuditDomain recode, EntityColumn entityColumn, boolean withAlias) {
        EntityTable table = EntityHelper.getEntityTable(recode.getClass());
        StringBuilder sb = new StringBuilder();
        if (entityColumn != null) {
            JoinColumn jc = entityColumn.getJoinColumn();
            if (jc != null) {
                EntityColumn joinField = table.getJoinMapping().get(jc.joinName());
                JoinTable joinTable = joinField.findJoinTableByName(jc.joinName());
                // ?????? @JoinColumn ?????? Expression ????????????
                if (jc.expression() != null && !"".equals(jc.expression())) {
                    sb.append(jc.expression()).append(" AS ").append(entityColumn.getColumn());
                } else {
                    if (joinTable != null) {
                        EntityTable joinEntityTable = EntityHelper.getEntityTable(joinTable.target());
                        EntityColumn refColumn = joinEntityTable.findColumnByProperty(jc.field());
                        // ????????????????????????????????????????????????
                        if (joinTable.joinMultiLanguageTable() && !MULTI_LANGUAGE_JOIN.equals(joinTable.name())
                                && refColumn.isMultiLanguage()) {
                            sb.append(table.getAlias(EntityHelper.buildJoinTLKey(joinTable))).append(".");
                        } else {
                            sb.append(table.getAlias(EntityHelper.buildJoinKey(joinTable))).append(".")
                                    .append(refColumn.getColumn());
                        }
                        if (withAlias) {
                            sb.append(" AS ").append(entityColumn.getColumn());
                        }
                    }
                }
            } else {
                sb.append(table.getAlias()).append(".").append(entityColumn.getColumn());
            }
        }
        return sb.toString();
    }

    /**
     * ??????JOIN??????SQL.
     *
     * @param record
     * @return SQL
     */
    private static String generateJoinSQL(AuditDomain record, EntityColumn localColumn, JoinTable joinTable, List<Selection> selections) {
        StringBuilder sb = new StringBuilder();
        EntityTable localTable = EntityHelper.getEntityTable(record.getClass());
        String joinKey = EntityHelper.buildJoinKey(joinTable);
        EntityTable foreignTable = EntityHelper.getEntityTable(joinTable.target());
        boolean foundJoinColumn = false;
        for (Selection selection : selections) {
            EntityColumn entityColumn = localTable.findColumnByProperty(selection.getField());
            if (entityColumn != null && entityColumn.getJoinColumn() != null && joinTable.name().equals(entityColumn.getJoinColumn().joinName())) {
                foundJoinColumn = true;
                break;
            }
        }
        if (foundJoinColumn) {
            String jointTableName = foreignTable.getName();
            if (joinTable.joinMultiLanguageTable()) {
                jointTableName = foreignTable.getMultiLanguageTableName();
            }
            sb.append(jointTableName).append(" ").append(localTable.getAlias(joinKey)).append(" ON ");
            JoinOn[] joinOns = joinTable.on();
            for (int i = 0, j = joinOns.length; i < j; i++) {
                JoinOn joinOn = joinOns[i];
                String joinField = joinOn.joinField();
                if (StringUtils.isEmpty(joinField)) {
                    continue;
                }
                if (i != 0) {
                    sb.append(" AND ");
                }
                EntityColumn foreignColumn = foreignTable.findColumnByProperty(joinField);
                String columnName = foreignColumn != null ? foreignColumn.getColumn() : StringUtil.camelhumpToUnderline(joinField);
                if (StringUtils.isEmpty(joinOn.joinExpression())) {
                    sb.append(localTable.getAlias()).append(".").append(localColumn.getColumn()).append(" = ");
                    sb.append(localTable.getAlias(joinKey)).append(".").append(columnName);
                } else {
                    sb.append(localTable.getAlias(joinKey)).append(".").append(columnName);
                    if ("__current_locale".equals(joinOn.joinExpression())) {
                        sb.append(" = ").append(OGNL.language());
                    } else {
                        sb.append(" = ").append(joinOn.joinExpression());
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * ??????Where???SQL.
     *
     * @param record
     * @return SQL
     */
    private static String generateWhereClauseSQL(AuditDomain record, Criteria criteria) {
        StringBuilder sb = new StringBuilder();
        List<WhereField> whereFields = criteria.getWhereFields();
        EntityTable table = EntityHelper.getEntityTable(record.getClass());

        for (EntityColumn column : table.getWhereColumns()) {
            try {
                if (BeanUtils.getProperty(record, column.getProperty()) != null) {
                    Where where = column.getWhere();
                    Comparison comparison = where.comparison();
                    String expression = where.expression();
                    boolean isWhereField = false;
                    if (whereFields != null && !whereFields.isEmpty()) {
                        for (WhereField whereField : whereFields) {
                            String f = whereField.getField();
                            if (f != null && f.equals(column.getProperty())) {
                                isWhereField = true;
                                if (whereField.getComparison() != null) {
                                    comparison = whereField.getComparison();
                                    if (whereField.getExpression() != null) {
                                        expression = whereField.getExpression();
                                    }
                                }
                                break;
                            }
                        }
                        if (!isWhereField) {
                            continue;
                        }
                    }
                    // ?????? @Where ????????? Expression ?????????
                    if (!"".equals(expression)) {
                        expression = expression.trim();
                        sb = processWhereExpression(sb, table.getAlias(), expression);
                    } else {
                        if (sb.length() > 0) {
                            sb.append(" AND ");
                        }
                        String columnName = column.getColumn();
                        JoinColumn jc = column.getJoinColumn();
                        if (jc != null) {
                            EntityColumn joinField = table.getJoinMapping().get(jc.joinName());
                            JoinTable jt = joinField.findJoinTableByName(jc.joinName());
                            EntityTable foreignTable = EntityHelper.getEntityTable(jt.target());
                            EntityColumn foreignColumn = foreignTable.findColumnByProperty(jc.field());
                            columnName = foreignColumn.getColumn();
                            sb.append(table.getAlias(EntityHelper.buildJoinKey(jt))).append(".");
                        } else {
                            sb.append(table.getAlias()).append(".");
                        }
                        sb.append(columnName).append(
                                formatComparisonSQL(comparison.sql(), column.getColumnHolder("record")));
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return sb.toString();
    }

    /**
     * ?????????SQL
     *
     * @param format
     * @param placeHolder
     * @return
     */
    private static String formatComparisonSQL(String format, String placeHolder) {
        if (format.indexOf("{0}") != -1) {
            MessageFormat mf = new MessageFormat(format);
            return mf.format(new String[]{placeHolder});
        } else {
            return format + placeHolder;
        }
    }

    public static String buildOGNL(String content, String separator) {
        return StringUtils.isEmpty(separator) ? "'${" + content + "}'" : "'${" + content + "}'" + separator;
    }

    /**
     * ???????????????????????? null ?????????
     *
     * @param parameterName ?????????
     * @param columnSet     ??????????????????
     * @return
     */
    public static String notAllNullParameterCheck(String parameterName, Set<EntityColumn> columnSet) {
        StringBuilder sql = new StringBuilder();
        sql.append("<bind name=\"notAllNullParameterCheck\" value=\"@org.hzero.mybatis.util.OGNL@notAllNullParameterCheck(");
        sql.append(parameterName).append(", '");
        StringBuilder fields = new StringBuilder();
        for (EntityColumn column : columnSet) {
            if (fields.length() > 0) {
                fields.append(",");
            }
            fields.append(column.getProperty());
        }
        sql.append(fields);
        sql.append("')\"/>");
        return sql.toString();
    }

    /**
     * ??????@Where?????????Expression????????? ????????????????????? AND
     * OBJECT_VERSION_NUMBER=#{dto.objectVersionNumber,jdbcType=DECIMAL} ?????? AND
     * OBJECT_VERSION_NUMBER=#{objectVersionNumber,jdbcType=DECIMAL}
     * <p>
     * ????????????????????? 1.?????? and or 2.??????????????????????????????????????????a 3.???????????????#{objectVersionNumber} -> #{dto.objectVersionNumber}
     *
     * @param sb         where?????????
     * @param tableAlias ?????????
     * @param expression expression?????????
     * @return
     */
    private static StringBuilder processWhereExpression(StringBuilder sb, String tableAlias, String expression) {
        if (!expression.toUpperCase().startsWith(AND) && !expression.toUpperCase().startsWith(OR)) {
            if (sb.length() > 0) {
                sb.append(" AND");
            }
        }
        expression = expression.replaceAll(ExpressionConstants.TABLE_ALIAS_REG, tableAlias + ".").replaceAll(ExpressionConstants.VARIABLE_REG, ExpressionConstants.PLACEHOLDER_DTO);
        return sb.append(" ").append(expression);
    }
}
