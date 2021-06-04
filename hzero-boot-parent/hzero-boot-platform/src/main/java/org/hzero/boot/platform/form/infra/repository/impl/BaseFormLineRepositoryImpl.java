package org.hzero.boot.platform.form.infra.repository.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.platform.form.domain.repository.BaseFormLineRepository;
import org.hzero.boot.platform.form.domain.vo.FormLineVO;
import org.hzero.common.HZeroService;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.redis.RedisHelper;
import org.hzero.core.redis.safe.SafeRedisHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.choerodon.core.exception.CommonException;

/**
 * 基础表单配置行资源库实现
 *
 * @author xiaoyu.zhao@hand-china.com 2019/11/22 10:33
 */
@Component
public class BaseFormLineRepositoryImpl implements BaseFormLineRepository {

    /**
     * 表单配置行缓存Key
     */
    private static final String CACHE_FORM_LINE_CONFIGURATION = HZeroService.Platform.CODE + ":form-configuration:";
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseFormLineRepositoryImpl.class);

    private final RedisHelper redisHelper;

    public BaseFormLineRepositoryImpl(RedisHelper redisHelper) {
        this.redisHelper = redisHelper;
    }

    @Override
    public void saveFormLineCache(String formHeaderCode, FormLineVO formLineVO) {
        if (Objects.equals(null, formLineVO) || Objects.equals(null, formLineVO.getTenantId())
                || Objects.equals(null, formLineVO.getItemCode())) {
            LOGGER.error("Form configuration line cache failed due to invalid parameters");
            throw new CommonException(BaseConstants.ErrorCode.NOT_NULL);
        }
        String cacheKey = StringUtils.join(CACHE_FORM_LINE_CONFIGURATION, formHeaderCode, BaseConstants.Symbol.COLON,
                formLineVO.getTenantId());
        SafeRedisHelper.execute(HZeroService.Platform.REDIS_DB, redisHelper,
                () -> redisHelper.hshPut(cacheKey, formLineVO.getItemCode(), redisHelper.toJson(formLineVO)));
    }

    @Override
    public void deleteFormLineCache(String formHeaderCode, Long tenantId, String itemCode) {
        String cacheKey = StringUtils.join(CACHE_FORM_LINE_CONFIGURATION, formHeaderCode, BaseConstants.Symbol.COLON,
                tenantId);
        LOGGER.info("========>>>start deleting form line cache......");
        SafeRedisHelper.execute(HZeroService.Platform.REDIS_DB, redisHelper,
                () -> redisHelper.hshDelete(cacheKey, itemCode));
    }


    @Override
    public FormLineVO getOneFromLineCache(String formHeaderCode, Long tenantId, String itemCode) {
        String cacheKey = StringUtils.join(CACHE_FORM_LINE_CONFIGURATION, formHeaderCode, BaseConstants.Symbol.COLON,
                tenantId);
        String formLineCache = SafeRedisHelper.execute(HZeroService.Platform.REDIS_DB, redisHelper,
                () -> redisHelper.hshGet(cacheKey, itemCode));
        return redisHelper.fromJson(formLineCache, FormLineVO.class);
    }

    @Override
    public List<FormLineVO> getAllFormLineCache(String formHeaderCode, Long tenantId) {
        String cacheKey = StringUtils.join(CACHE_FORM_LINE_CONFIGURATION, formHeaderCode, BaseConstants.Symbol.COLON,
                tenantId);
        List<String> formLineCaches = SafeRedisHelper.execute(HZeroService.Platform.REDIS_DB, redisHelper,
                () -> redisHelper.hshVals(cacheKey));
        List<FormLineVO> resultList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(formLineCaches)) {
            formLineCaches.forEach(formLineCache ->
                    resultList.add(redisHelper.fromJson(formLineCache, FormLineVO.class)));
        }
        return resultList;
    }
}
