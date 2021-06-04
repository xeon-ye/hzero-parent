package org.hzero.boot.imported.infra.redis;

import org.apache.commons.lang3.StringUtils;
import org.hzero.common.HZeroService;
import org.hzero.core.redis.RedisHelper;

/**
 * 缓存用于标识模板是否被更新
 * bei更新的模板需要feign查询同步到本地
 *
 * @author shuangfei.zhu@hand-china.com 2019/05/08 19:32
 */
public class TemplateRedis {

    private TemplateRedis() {
    }

    /**
     * 生成redis存储key
     */
    private static String getCacheKey(Long tenantId) {
        return HZeroService.Import.CODE + ":template:" + tenantId;
    }

    /**
     * 刷新缓存
     */
    public static void refreshCache(RedisHelper redisHelper, Long tenantId, String templateCode) {
        clearCache(redisHelper, tenantId, templateCode);
        redisHelper.hshPut(getCacheKey(tenantId), templateCode, StringUtils.EMPTY);
    }

    /**
     * 查询缓存是否存在
     */
    public static boolean hasCache(RedisHelper redisHelper, Long tenantId, String templateCode) {
        return redisHelper.hshHasKey(getCacheKey(tenantId), templateCode);
    }

    /**
     * 清除缓存
     */
    public static void clearCache(RedisHelper redisHelper, Long tenantId, String templateCode) {
        redisHelper.hshDelete(getCacheKey(tenantId), templateCode);
    }
}
