package org.hzero.plugin.platform.customize.infra.repository.impl;

import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.hzero.boot.customize.dto.ModelMetaData;
import org.hzero.core.redis.RedisHelper;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.plugin.platform.customize.api.dto.MenuDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitConfigDTO;
import org.hzero.plugin.platform.customize.api.dto.UnitRelatedDTO;
import org.hzero.plugin.platform.customize.domain.entity.Unit;
import org.hzero.plugin.platform.customize.domain.entity.UnitField;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelFieldRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeUnitRepository;
import org.hzero.plugin.platform.customize.infra.common.ModelLocalCache;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeUnitMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 个性化配置Repository
 *
 * @author : xiangyu.qi01@hand-china.com 2019/12/13
 */
@Component
public class CustomizeUnitRepositoryImpl extends BaseRepositoryImpl<Unit> implements CustomizeUnitRepository {

    private final Logger logger = LoggerFactory.getLogger(CustomizeUnitRepositoryImpl.class);

    public static final String UNIT_SELECT_ALL_SQL_ID = "org.hzero.plugin.platform.customize.infra.mapper.CustomizeUnitMapper.selectAll";

    @Autowired
    private CustomizeUnitMapper unitMapper;
    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private CustomizeModelRepository modelRepository;
    @Autowired
    private CustomizeModelFieldRepository modelFieldRepository;
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Override
    public List<MenuDTO> selectMenuByRole() {
        CustomUserDetails self = DetailsHelper.getUserDetails();
        return unitMapper.selectMenuByRoleId(self.roleMergeIds());
    }

    @Override
    public List<MenuDTO> selectAllMenu() {
        return unitMapper.selectAllMenus();
    }

    @Override
    public Unit selectUnitWithConfigById(Long tenantId, Long unitId) {
        return unitMapper.selectUnitWithConfigById(tenantId, unitId);
    }

    @Override
    public List<Unit> selectByOption(Unit unit) {
        List<Unit> units = unitMapper.selectByOptions(unit);
        if (CollectionUtils.isEmpty(units)) {
            return null;
        }
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        units.forEach(t -> t.setModelName(modelLocalCache.getModel(t.getModelId()).getModelName()));
        return units;
    }

    @Override
    public Unit selectById(Long unitId) {
        Unit unit = unitMapper.selectById(unitId);
        if (unit == null) {
            return null;
        }
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        ModelMetaData modelMetaData = modelLocalCache.getModel(unit.getModelId());
        unit.setModelName(modelMetaData.getModelName());
        if (StringUtils.isNotEmpty(unit.getConRelatedUnit())) {
            unit.setConRelatedUnits(Arrays.asList(unit.getConRelatedUnit().split(",")));
        }
        return unit;
    }

    @Override
    public UnitConfigDTO selectUnitConfigByCode(String unitCode, Long tenantId) {
        return unitMapper.selectUnitConfigByCode(unitCode, tenantId);
    }

    @Override
    public void cacheUnit(Unit unit) {
        Assert.notNull(unit.getUnitCode(), "unitCode can not be null !");
        redisHelper.hshPut(Unit.cacheKey(), unit.getUnitCode(), redisHelper.toJson(unit.cacheConvert()));
    }

    @Override
    public Unit getUnitCache(String unitCode) {
        Assert.notNull(unitCode, "unitCode can not be null !");
        String cache = redisHelper.hshGet(Unit.cacheKey(), unitCode);
        if (StringUtils.isNotEmpty(cache)) {
            return redisHelper.fromJson(cache, Unit.class);
        }
        return new Unit();
    }

    @Override
    public void deleteUnitCache(String unitCode) {
        Assert.notNull(unitCode, "unitCode can not be null !");
        redisHelper.hshDelete(Unit.cacheKey(), unitCode);
    }

    @Override
    public void initUnitCache() {
        logger.info("Cache Unit : start");
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            Map<String, String> unitCache = new HashMap(1000);
            sqlSession.select(UNIT_SELECT_ALL_SQL_ID, (resultContext) -> {
                Unit unit = (Unit) resultContext.getResultObject();
                unitCache.put(unit.getUnitCode(), redisHelper.toJson(unit.cacheConvert()));
            });

            //先删除在插入
            unitCache.forEach((k, v) -> redisHelper.delKey(Unit.cacheKey()));
            redisHelper.hshPutAll(Unit.cacheKey(), unitCache);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("Cache Unit : end");
    }

    @Override
    public List<UnitRelatedDTO> selectRelatedUnit(Collection<String> unitCodes) {
        return unitMapper.selectRelatedUnit(unitCodes);
    }

    @Override
    public Unit selectUnitAndModelTable(Long unitId) {
        return unitMapper.selectUnitAndModelTable(unitId);
    }
}
