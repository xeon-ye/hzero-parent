package org.hzero.boot.platform.ds;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.platform.ds.constant.DsConstants;
import org.hzero.boot.platform.ds.feign.DsRemoteService;
import org.hzero.boot.platform.ds.vo.DatasourceVO;
import org.hzero.common.HZeroService;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.redis.RedisHelper;
import org.hzero.mybatis.helper.DataSecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 数据源获取
 *
 * @author xianzhi.chen@hand-china.com 2019年1月21日下午7:17:24
 */
@Component
public class DatasourceHelper {

    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private DsRemoteService dsRemoteService;
    @Autowired
    private ObjectMapper objectMapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceHelper.class);

    /**
     * 获取数据源 ,平台级
     *
     * @param dsPurposeCode  数据源用途编码
     * @param datasourceCode 数据源编码
     * @return Datasource 数据源
     */
    public DatasourceVO getDatasource(String dsPurposeCode, String datasourceCode) {
        return getDatasource(redisHelper, dsPurposeCode, BaseConstants.DEFAULT_TENANT_ID, datasourceCode, null);
    }

    /**
     * 获取数据源 ,平台级
     *
     * @param dsPurposeCode  数据源用途编码
     * @param datasourceCode 数据源编码
     * @param serviceName    服务名称
     * @return Datasource 数据源
     */
    public DatasourceVO getDatasource(String dsPurposeCode, String datasourceCode, String serviceName) {
        return getDatasource(redisHelper, dsPurposeCode, BaseConstants.DEFAULT_TENANT_ID, datasourceCode, serviceName);
    }

    /**
     * 租户数据源是否存在
     */
    public boolean exists(String dsPurposeCode, Long tenantId, String datasourceCode) {
        DatasourceVO datasource = getDatasource(redisHelper, dsPurposeCode, tenantId, datasourceCode, null);
        return datasource != null;
    }

    /**
     * 租户数据源是否存在
     */
    public boolean exists(String dsPurposeCode, Long tenantId, String datasourceCode, String serviceName) {
        DatasourceVO datasource = getDatasource(redisHelper, dsPurposeCode, tenantId, datasourceCode, serviceName);
        return datasource != null;
    }

    /**
     * 获取数据源，租户获取不到，获取平台的
     *
     * @param dsPurposeCode  数据源用途编码
     * @param tenantId       租户ID
     * @param datasourceCode 数据源编码
     * @return Datasource 数据源
     */
    public DatasourceVO getDatasource(String dsPurposeCode, Long tenantId, String datasourceCode) {
        // 先查租户
        DatasourceVO datasource = getDatasource(redisHelper, dsPurposeCode, tenantId, datasourceCode, null);
        if (datasource == null && !Objects.equals(tenantId, BaseConstants.DEFAULT_TENANT_ID)) {
            datasource = getDatasource(redisHelper, dsPurposeCode, BaseConstants.DEFAULT_TENANT_ID, datasourceCode,
                    null);
        }
        return datasource;
    }

    /**
     * 获取数据源，租户获取不到，获取平台的
     *
     * @param dsPurposeCode  数据源用途编码
     * @param tenantId       租户ID
     * @param datasourceCode 数据源编码
     * @return Datasource 数据源
     */
    public DatasourceVO getDatasource(String dsPurposeCode, Long tenantId, String datasourceCode, String serviceName) {
        // 先查租户
        DatasourceVO datasource = getDatasource(redisHelper, dsPurposeCode, tenantId, datasourceCode, serviceName);
        if (datasource == null && !Objects.equals(tenantId, BaseConstants.DEFAULT_TENANT_ID)) {
            datasource = getDatasource(redisHelper, dsPurposeCode, BaseConstants.DEFAULT_TENANT_ID, datasourceCode,
                    serviceName);
        }
        return datasource;
    }

    /**
     * 获取数据源信息
     *
     * @param redisHelper    redisHelper
     * @param dsPurposeCode  数据源用途
     * @param tenantId       租户Id
     * @param datasourceCode 数据源编码
     * @param serviceName    数据分发服务名
     * @return 数据源信息
     */
    private DatasourceVO getDatasource(RedisHelper redisHelper, String dsPurposeCode, Long tenantId,
                                       String datasourceCode, String serviceName) {
        // 查询缓存
        String dsKey = generateCacheKey(dsPurposeCode, tenantId, datasourceCode, serviceName);
        String ds;
        try {
            redisHelper.setCurrentDatabase(HZeroService.Platform.REDIS_DB);
            ds = redisHelper.strGet(dsKey);
        } finally {
            redisHelper.clearCurrentDatabase();
        }
        if (StringUtils.isNotBlank(ds)) {
            if (Objects.equals(ds, DsConstants.DsPurpose.ERROR)) {
                return null;
            } else {
                DatasourceVO datasource = redisHelper.fromJson(ds, DatasourceVO.class);
                try {
                    String decryptPwd = DataSecurityHelper.decrypt(datasource.getPasswordEncrypted());
                    datasource.setPasswordEncrypted(decryptPwd);
                } catch (Exception ex) {
                    // 若数据库中的数据未加密，说明参数中密码是明文，直接获取即可
                    LOGGER.info("datasource password decrypt failed!");
                }
                // 最终保证datasource中的password参数是明文
                return this.addMapParams(datasource);
            }
        }
        // feign调用
        String result = dsRemoteService.getByUnique(tenantId, datasourceCode, dsPurposeCode).getBody();
        if (StringUtils.isNotBlank(result)) {
            try {
                DatasourceVO datasource = objectMapper.readValue(result, DatasourceVO.class);
                try {
                    String decryptPwd = DataSecurityHelper.decrypt(datasource.getPasswordEncrypted());
                    datasource.setPasswordEncrypted(decryptPwd);
                } catch (Exception ex) {
                    // 若数据库中的数据未加密，说明参数中密码是明文，直接获取即可
                    LOGGER.info("datasource password decrypt failed!");
                }
                return this.addMapParams(datasource);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取数据源KEY
     *
     * @param dsPurposeCode  数据域用途
     * @param datasourceCode 数据源编码
     * @return 数据源KEY
     */
    private String generateCacheKey(String dsPurposeCode, Long tenantId, String datasourceCode, String serviceName) {
        if (DsConstants.DsPurpose.DT.equals(dsPurposeCode)) {
            // 数据分发类型，服务名称不能为空
            Assert.notNull(serviceName, DsConstants.ErrorCode.DATASOURCE_SERVICE_NAME_NOT_NULL);
            return StringUtils.join(HZeroService.Platform.CODE, DsConstants.DATASOURCE, dsPurposeCode,
                    BaseConstants.Symbol.COLON, tenantId, BaseConstants.Symbol.COLON, datasourceCode,
                    BaseConstants.Symbol.COLON, serviceName);
        } else {
            return StringUtils.join(HZeroService.Platform.CODE, DsConstants.DATASOURCE, dsPurposeCode,
                    BaseConstants.Symbol.COLON, tenantId, BaseConstants.Symbol.COLON, datasourceCode);
        }
    }

    /**
     * String转Map
     *
     * @param convertString 需要转换的字符串
     * @return 转换后生成的Map参数
     */
    private Map<String, Object> convertString2Map(String convertString) {
        // 转换为Map
        GsonJsonParser gsonJsonParser = new GsonJsonParser();
        return gsonJsonParser.parseMap(convertString);
    }

    /**
     * 补全Map字段参数
     *
     * @param datasource 数据源
     * @return 完整参数实体
     */
    private DatasourceVO addMapParams(DatasourceVO datasource) {
        // 将扩展字段的String转换为Map
        if (StringUtils.isNotBlank(datasource.getExtConfig())) {
            Map<String, Object> extConfigMap = this.convertString2Map(datasource.getExtConfig());
            datasource.setExtConfigMap(extConfigMap);
        }
        if (StringUtils.isNotBlank(datasource.getOptions())) {
            Map<String, Object> optionsMap = this.convertString2Map(datasource.getOptions());
            datasource.setOptionsMap(optionsMap);
        }
        return datasource;
    }
}
