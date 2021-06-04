package org.hzero.plugin.platform.customize.infra.repository.impl;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.hzero.core.redis.RedisHelper;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.app.service.CustomizeCommonService;
import org.hzero.plugin.platform.customize.domain.entity.Config;
import org.hzero.plugin.platform.customize.domain.entity.ConfigField;
import org.hzero.plugin.platform.customize.domain.entity.Unit;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeConfigRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeUnitRepository;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 个性化配置Repository
 *
 * @author : xiangyu.qi01@hand-china.com 2019/12/13
 */
@Component
public class CustomizeConfigRepositoryImpl extends BaseRepositoryImpl<Config> implements CustomizeConfigRepository {

    private final Logger logger = LoggerFactory.getLogger(CustomizeConfigRepositoryImpl.class);

    public static final String CUSTOMIZE_SELECT_ALL_SQL_ID = "org.hzero.plugin.platform.customize.infra.mapper.CustomizeConfigMapper.selectAllWithUnitCode";

    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private CustomizeUnitRepository unitRepository;
    @Autowired
    private SqlSessionFactory sqlSessionFactory;
    @Autowired
    private CustomizeCommonService commonService;

    @Override
    public Config selectConfigByUnitId(Long tenantId, Long unitId) {
        Config config = new Config();
        config.setTenantId(tenantId);
        config.setUnitId(unitId);
        return super.selectOne(config);
    }

    @Override
    public void cacheConfig(Config config) {
        Assert.notNull(config.getTenantId(), "tenantId can not be null!");
        Assert.notNull(config.getUnitCode(), "unitCode can not be null!");

        redisHelper.hshPut(Config.cacheKey(config.getTenantId()), config.getUnitCode(), redisHelper.toJson(config.cacheConvert()));
    }


    @Override
    public Config getConfigCache(Long tenantId, String unitCode) {
        Assert.notNull(tenantId, "tenantId can not be null!");
        Assert.notNull(unitCode, "unitId can not be null!");

        String cache = redisHelper.hshGet(Config.cacheKey(tenantId), unitCode);
        if (StringUtils.isEmpty(cache)) {
            return null;
        }
        return redisHelper.fromJson(cache, Config.class);
    }

    @Override
    public void initConfigCache() {

        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            Map<String, Map<String, String>> cacheMap = new HashMap<>(32);
            Map<Long, List<String>> configFieldKeyMap = new HashMap<>(128);
            try {
                sqlSession.select(CUSTOMIZE_SELECT_ALL_SQL_ID, (resultContext) -> {
                    Config config = (Config) resultContext.getResultObject();
                    if (config.getTenantId() == null || StringUtils.isEmpty(config.getUnitCode())) {
                        logger.warn("Cache Config : {} tenantId or unitCode is null.", config.getId());
                        return;
                    }
                    String cacheKey = Config.cacheKey(config.getTenantId());
                    Map<String, String> innerMap = cacheMap.computeIfAbsent(cacheKey, key -> new HashMap<>(128));
                    innerMap.putIfAbsent(config.getUnitCode(), redisHelper.toJson(config.cacheConvert()));
                    //初始化拦截器缓存
                    commonService.cacheCustomizeConfig(config.getTenantId(), config.getUnitId());
                    //获取租户个性化字段缓存key
                    List<String> configFieldKeyList = configFieldKeyMap.computeIfAbsent(config.getTenantId(), v -> new ArrayList<>(1000));
                    configFieldKeyList.add(ConfigField.cacheKey(config.getTenantId(), config.getUnitCode()));
                });

                for (Map.Entry<String, Map<String, String>> entry : cacheMap.entrySet()) {
                    redisHelper.hshPutAll(entry.getKey(), entry.getValue());
                }
                //删除租户级字段配置缓存
                if (!configFieldKeyMap.isEmpty()) {
                    configFieldKeyMap.values().forEach(keyList -> redisHelper.delKeys(keyList));
                }
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void deleteUserConfigCache(Config config) {
        Assert.notNull(config.getUserId(), "userId can not be null !");
        Assert.notNull(config.getTenantId(), "tenantId can not be null!");
        Assert.notNull(config.getUnitCode(), "unitCode can not be null!");
        redisHelper.hshDelete(Config.cacheKey(config.getTenantId(), config.getUserId()), config.getUnitCode());
    }

    @Override
    public Config getUserConfigCache(Long tenantId, Long userId, String unitCode) {
        Assert.notNull(userId, "userId can not be null !");
        Assert.notNull(tenantId, "tenantId can not be null!");
        Assert.notNull(unitCode, "unitCode can not be null!");
        String cacheKey = Config.cacheKey(tenantId, userId);
        String cache = redisHelper.hshGet(cacheKey, unitCode);
        if (!StringUtils.isEmpty(cache)) {
            if (CustomizeConstants.NILL.equals(cache)) {
                return null;
            } else {
                return redisHelper.fromJson(cache, Config.class);
            }
        }
        //查询数据库
        Unit unit = unitRepository.getUnitCache(unitCode);
        Config configHeader = new Config();
        configHeader.setUserId(userId);
        configHeader.setTenantId(tenantId);
        configHeader.setUnitId(unit.getId());

        //查询个性化头配置
        Config userConfigHeaderDb = this.selectOne(configHeader);
        if (userConfigHeaderDb != null) {
            Config cacheConfig = userConfigHeaderDb.cacheConvert();
            redisHelper.hshPut(cacheKey, unitCode, redisHelper.toJson(cacheConfig));
            return cacheConfig;
        } else {
            redisHelper.hshPut(cacheKey, unitCode, CustomizeConstants.NILL);
        }
        return null;
    }

    @Override
    public void delConfigFieldCacheByUnitField(UnitField unitField) {
        Config configCondition = new Config();
        configCondition.setUnitId(unitField.getUnitId());
        configCondition.setUserId(-1L);
        List<Config> configs = select(configCondition);
        if(CollectionUtils.isEmpty(configs)) {
            return;
        }
        Map<Long, List<String>> configFieldKeyMap = new HashMap<>(128);
        for(Config config : configs) {
            //初始化拦截器缓存
            commonService.cacheCustomizeConfig(config.getTenantId(), config.getUnitId());
            //获取租户个性化字段缓存key
            List<String> configFieldKeyList = configFieldKeyMap.computeIfAbsent(config.getTenantId(), v -> new ArrayList<>(1000));
            configFieldKeyList.add(ConfigField.cacheKey(config.getTenantId(), unitField.getUnitCode()));
        }
        //删除租户级字段配置缓存
        if (!configFieldKeyMap.isEmpty()) {
            configFieldKeyMap.values().forEach(keyList -> redisHelper.delKeys(keyList));
        }
    }
}
