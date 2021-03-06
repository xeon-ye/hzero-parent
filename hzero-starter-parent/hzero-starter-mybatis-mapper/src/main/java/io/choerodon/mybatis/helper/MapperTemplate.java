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
import io.choerodon.mybatis.code.DbType;
import io.choerodon.mybatis.constant.CommonMapperConfigConstant;
import io.choerodon.mybatis.constant.SupportedKeyGenerator;
import io.choerodon.mybatis.domain.EntityColumn;
import io.choerodon.mybatis.domain.EntityTable;
import io.choerodon.mybatis.helper.snowflake.SnowflakeKeyGenerator;
import io.choerodon.mybatis.util.StringUtil;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.*;


/**
 * ??????Mapper????????????????????????Mapper?????????????????????
 *
 * @author liuzh
 */
public abstract class MapperTemplate {

    private static final String DYNAMIC_SQL = "dynamicSql";

    private static final XMLLanguageDriver languageDriver = new XMLLanguageDriver();
    protected Map<String, Method> methodMap = new HashMap<>();
    protected Map<String, Class<?>> entityClassMap = new HashMap<>();
    protected Class<?> mapperClass;
    protected MapperHelper mapperHelper;

    private static final Logger logger = LoggerFactory.getLogger(MapperTemplate.class);

    public MapperTemplate() {
    }

    public MapperTemplate(Class<?> mapperClass, MapperHelper mapperHelper) {
        this.mapperClass = mapperClass;
        this.mapperHelper = mapperHelper;
    }

    /**
     * ??????msId???????????????
     *
     * @param msId msId
     * @return Class
     */
    public static Class<?> getMapperClass(String msId) {
        try {
            return Class.forName(getMapperClassName(msId));
        } catch (ClassNotFoundException e) {
            throw new MapperException("??????MappedStatement???id=" + msId + ",?????????MappedStatement?????????!");
        }
    }

    /**
     * ??????mapper?????????
     *
     * @param msId msId
     * @return String
     */
    public static String getMapperClassName(String msId) {
        if (!msId.contains(".")) {
            throw new MapperException("??????MappedStatement???id=" + msId + ",?????????MappedStatement?????????!");
        }
        return msId.substring(0, msId.lastIndexOf('.'));
    }

    /**
     * ????????????????????????
     *
     * @param ms MappedStatement
     * @return String
     */
    public static String getMethodName(MappedStatement ms) {
        return getMethodName(ms.getId());
    }

    /**
     * ????????????????????????
     *
     * @param msId msId
     * @return String
     */
    public static String getMethodName(String msId) {
        return msId.substring(msId.lastIndexOf('.') + 1);
    }

    /**
     * ??????????????????????????????ProviderSqlSource
     *
     * @param record record
     * @return String
     */
    public String dynamicSql(Object record) {
        return DYNAMIC_SQL;
    }

    /**
     * ??????????????????
     *
     * @param methodName methodName
     * @param method     method
     */
    public void addMethodMap(String methodName, Method method) {
        methodMap.put(methodName, method);
    }

    public String getUuid() {
        return mapperHelper.getConfig().getUuid();
    }

    public String getIdentity() {
        return mapperHelper.getConfig().getIdentity();
    }

    public boolean isBefore() {
        return mapperHelper.getConfig().isBefore();
    }

    public boolean isNotEmpty() {
        return mapperHelper.getConfig().isNotEmpty();
    }

    public boolean isCheckExampleEntityClass() {
        return mapperHelper.getConfig().isCheckExampleEntityClass();
    }

    /**
     * ???????????????????????????
     *
     * @param msId msId
     * @return boolean
     */
    public boolean supportMethod(String msId) {
        Class<?> newMapperClass = getMapperClass(msId);
        if (newMapperClass != null && this.mapperClass.isAssignableFrom(newMapperClass)) {
            String methodName = getMethodName(msId);
            return methodMap.get(methodName) != null;
        }
        return false;
    }

    /**
     * ????????????????????? - ?????????typeHandler???select????????????????????????resultMap
     *
     * @param ms          MappedStatement
     * @param entityClass entityClass
     */
    protected void setResultType(MappedStatement ms, Class<?> entityClass) {
        EntityTable entityTable = EntityHelper.getTableByEntity(entityClass);
        List<ResultMap> resultMaps = new ArrayList<>();
        resultMaps.add(entityTable.getResultMap(ms.getConfiguration()));
        MetaObject metaObject = SystemMetaObject.forObject(ms);
        metaObject.setValue("resultMaps", Collections.unmodifiableList(resultMaps));
    }

    /**
     * ????????????SqlSource
     *
     * @param ms        MappedStatement
     * @param sqlSource sqlSource
     */
    protected void setSqlSource(MappedStatement ms, SqlSource sqlSource) {
        MetaObject msObject = SystemMetaObject.forObject(ms);
        msObject.setValue("sqlSource", sqlSource);
    }

    /**
     * ????????????SqlSource
     *
     * @param ms MappedStatement
     */
    public void setSqlSource(MappedStatement ms) {
        if (this.mapperClass == getMapperClass(ms.getId())) {
            throw new MapperException("??????????????????????????????Mapper????????????" + this.mapperClass);
        }
        Method method = methodMap.get(getMethodName(ms));
        try {
            //????????????????????????ms?????????????????????
            if (method.getReturnType() == Void.TYPE) {
                method.invoke(this, ms);
            } else if (SqlNode.class.isAssignableFrom(method.getReturnType())) {
                //??????????????????SqlNode
                SqlNode sqlNode = (SqlNode) method.invoke(this, ms);
                DynamicSqlSource dynamicSqlSource = new DynamicSqlSource(ms.getConfiguration(), sqlNode);
                setSqlSource(ms, dynamicSqlSource);
            } else if (String.class.equals(method.getReturnType())) {
                //??????????????????xml?????????sql?????????
                String xmlSql = (String) method.invoke(this, ms);
                SqlSource sqlSource = createSqlSource(ms, xmlSql);
                //???????????????SqlSource
                setSqlSource(ms, sqlSource);
            } else {
                throw new MapperException("?????????Mapper????????????????????????,????????????????????????void,SqlNode,String??????!");
            }
            //cache
            checkCache(ms);
        } catch (IllegalAccessException e) {
            throw new MapperException(e);
        } catch (InvocationTargetException e) {
            throw new MapperException(e.getTargetException() != null ? e.getTargetException() : e);
        }
    }

    /**
     * ???????????????????????????
     *
     * @param ms MappedStatement
     */
    private void checkCache(MappedStatement ms) {
        if (ms.getCache() == null) {
            String nameSpace = ms.getId().substring(0, ms.getId().lastIndexOf('.'));
            Cache cache;
            try {
                //?????????????????????????????????
                cache = ms.getConfiguration().getCache(nameSpace);
            } catch (IllegalArgumentException e) {
                return;
            }
            if (cache != null) {
                MetaObject metaObject = SystemMetaObject.forObject(ms);
                metaObject.setValue("cache", cache);
            }
        }
    }


    /**
     * ??????xmlSql??????sqlSource
     *
     * @param ms     MappedStatement
     * @param xmlSql xmlSql
     * @return SqlSource
     */
    public SqlSource createSqlSource(MappedStatement ms, String xmlSql) {
        return languageDriver.createSqlSource(ms.getConfiguration(), "<script>\n\t" + xmlSql + "</script>", null);
    }

    /**
     * ????????????????????? - ????????????
     *
     * @param ms MappedStatement
     * @return Class
     */
    public Class<?> getEntityClass(MappedStatement ms) {
        String msId = ms.getId();
        if (entityClassMap.containsKey(msId)) {
            return entityClassMap.get(msId);
        } else {
            Class<?> newMapperClass = getMapperClass(msId);
            Type[] types = newMapperClass.getGenericInterfaces();
            for (Type type : types) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType t = (ParameterizedType) type;
                    if (t.getRawType() == this.mapperClass
                            || this.mapperClass.isAssignableFrom((Class<?>) t.getRawType())) {
                        Class<?> returnType = (Class<?>) t.getActualTypeArguments()[0];
                        //?????????????????????????????????????????????????????????
                        EntityHelper.initEntityNameMap(returnType, getMapperClassName(ms.getId()),
                                mapperHelper.getConfig());
                        entityClassMap.put(msId, returnType);
                        return returnType;
                    }
                }
            }
        }
        throw new MapperException("????????????Mapper<T>????????????:" + msId);
    }

    /**
     * ?????????????????????????????????
     *
     * @param column EntityColumn
     * @return String
     */
    protected String getSeqNextVal(EntityColumn column) {
        return MessageFormat.format(mapperHelper.getConfig().getSeqFormat(),
                column.getSequenceName(), column.getColumn(), column.getProperty(), column.getTable().getName());
    }

    /**
     * ????????????????????????
     *
     * @param entityClass entityClass
     * @return String
     */
    protected String tableName(Class<?> entityClass) {
        EntityTable entityTable = EntityHelper.getTableByEntity(entityClass);
        String prefix = entityTable.getPrefix();
        if (StringUtil.isEmpty(prefix)) {
            //??????????????????
            prefix = mapperHelper.getConfig().getPrefix();
        }
        if (StringUtil.isNotEmpty(prefix)) {
            return prefix + "." + entityTable.getName();
        }
        return entityTable.getName();
    }

    /**
     * ??????SelectKey??????
     *
     * @param ms     MappedStatement
     * @param column EntityColumn
     */
    protected void newSelectKeyMappedStatement(MappedStatement ms, EntityColumn column) {
        newSelectKeyMappedStatement(null, ms, column);
    }

    /**
     * ??????SelectKey??????
     *
     * @param entityName entityName
     * @param ms         MappedStatement
     * @param column     EntityColumn
     */
    @SuppressWarnings("deprecation")
    protected void newSelectKeyMappedStatement(String entityName, MappedStatement ms, EntityColumn column) {
        String keyId = ms.getId() + SelectKeyGenerator.SELECT_KEY_SUFFIX;
        if (ms.getConfiguration().hasKeyGenerator(keyId)) {
            return;
        }
        Class<?> entityClass = getEntityClass(ms);
        //defaults
        Configuration configuration = ms.getConfiguration();
        KeyGenerator keyGenerator;
        Boolean executeBefore = isBefore();
        String identity = (column.getGenerator() == null || "".equals(column.getGenerator())) ? getIdentity() : column.getGenerator();
        if (SupportedKeyGenerator.snowflake.name().equals(identity) && column.isSnowflakeEnable()) {
            keyGenerator = new SnowflakeKeyGenerator();
        } else {
            // ?????????????????????ID????????????ID?????????????????????????????? keyGenerator
            if (SupportedKeyGenerator.snowflake.name().equals(identity) && mapperHelper.getConfig().getDbType() != null) {
                identity = mapperHelper.getConfig().getDbType().getIdentity();
                executeBefore = mapperHelper.getConfig().getDbType().isSupportSequence();
            }
            if (CommonMapperConfigConstant.IDENTITY_JDBC.equalsIgnoreCase(identity)) {
                keyGenerator = new Jdbc3KeyGenerator();
            } else {
                keyGenerator = processKeyGeneratorWithSequence(ms, column, keyId, entityClass, configuration, executeBefore, identity);
            }
        }

        //keyGenerator
        try {
            MetaObject msObject = SystemMetaObject.forObject(ms);
            msObject.setValue("keyGenerator", keyGenerator);
            String[] properties = column.getTable().getKeyProperties();
            if (entityName != null) {
                properties = Arrays.stream(properties).map(prop -> entityName + "." + prop).toArray(String[]::new);
            }
            msObject.setValue("keyProperties", properties);
            msObject.setValue("keyColumns", column.getTable().getKeyColumns());
        } catch (Exception e) {
            logger.debug("exception:" + e);
        }
    }

    private KeyGenerator processKeyGeneratorWithSequence(MappedStatement ms, EntityColumn column, String keyId, Class<?> entityClass, Configuration configuration, Boolean executeBefore, String identity) {
        KeyGenerator keyGenerator;
        if (CommonMapperConfigConstant.IDENTITY_SEQUENCE.equalsIgnoreCase(identity)) {
            //  sql for selectKey
            DbType DdType = Optional.ofNullable(mapperHelper.getConfig().getDbType()).orElse(DbType.ORACLE);
            switch (DdType) {
                case HANA:
                    identity = "SELECT " + getSeqNextVal(column) + " FROM DUMMY";
                    break;
                case POSTGRES:
                    identity = "SELECT nextval('" + column.getTable().getName() + "_s')";
                    break;
                default:
                    identity = "SELECT " + getSeqNextVal(column) + " FROM DUAL";
                    break;
            }
        }
        SqlSource sqlSource = new RawSqlSource(configuration, identity, entityClass);

        MappedStatement.Builder statementBuilder =
                new MappedStatement.Builder(configuration, keyId, sqlSource, SqlCommandType.SELECT);
        statementBuilder.resource(ms.getResource());
        statementBuilder.fetchSize(null);
        statementBuilder.statementType(StatementType.STATEMENT);
        statementBuilder.keyGenerator(new NoKeyGenerator());
        statementBuilder.keyProperty(column.getProperty());
        statementBuilder.keyColumn(null);
        statementBuilder.databaseId(null);
        statementBuilder.lang(configuration.getDefaultScriptingLanguageInstance());
        statementBuilder.resultOrdered(false);
        statementBuilder.resultSets(null);
        statementBuilder.timeout(configuration.getDefaultStatementTimeout());

        List<ParameterMapping> parameterMappings = new ArrayList<>();
        ParameterMap.Builder inlineParameterMapBuilder = new ParameterMap.Builder(
                configuration,
                statementBuilder.id() + "-Inline",
                entityClass,
                parameterMappings);
        statementBuilder.parameterMap(inlineParameterMapBuilder.build());

        List<ResultMap> resultMaps = new ArrayList<>();
        ResultMap.Builder inlineResultMapBuilder = new ResultMap.Builder(
                configuration,
                statementBuilder.id() + "-Inline",
                column.getJavaType(),
                new ArrayList<ResultMapping>(),
                null);
        resultMaps.add(inlineResultMapBuilder.build());
        statementBuilder.resultMaps(resultMaps);
        statementBuilder.resultSetType(null);

        statementBuilder.flushCacheRequired(false);
        statementBuilder.useCache(false);
        statementBuilder.cache(null);

        MappedStatement statement = statementBuilder.build();
        try {
            configuration.addMappedStatement(statement);
        } catch (Exception e) {
            //ignore
        }
        MappedStatement keyStatement = configuration.getMappedStatement(keyId, false);
        keyGenerator = new SelectKeyGenerator(keyStatement, executeBefore);
        try {
            configuration.addKeyGenerator(keyId, keyGenerator);
        } catch (Exception e) {
            //ignore
        }
        return keyGenerator;
    }

    public DbType getDbType() {
        return this.mapperHelper.getConfig().getDbType();
    }

}