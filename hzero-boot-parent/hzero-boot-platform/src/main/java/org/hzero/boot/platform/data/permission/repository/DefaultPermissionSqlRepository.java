package org.hzero.boot.platform.data.permission.repository;

import io.choerodon.core.oauth.DetailsHelper;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Table;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hzero.boot.platform.data.permission.helper.PermissionDataHelper;
import org.hzero.boot.platform.data.permission.util.KeyUtils;
import org.hzero.boot.platform.data.permission.util.StringUtils;
import org.hzero.boot.platform.data.permission.vo.PermissionRangeExclVO;
import org.hzero.boot.platform.data.permission.vo.PermissionRangeVO;
import org.hzero.common.HZeroService;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.redis.RedisHelper;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

/**
 * <p>
 * 默认数据权限资源库实现
 * </p>
 *
 * @author yunxiang.zhou01@hand-china.com 2018/07/31 11:31
 */
public class DefaultPermissionSqlRepository implements PermissionSqlRepository {

    private final RedisHelper redisHelper;
    private static final ThreadLocal<Map<String, PermissionRangeVO>> permissionCache = new ThreadLocal<>();

    /**
     * 动态租户前缀标识
     */
    private static final String DYNAMIC_PREFIX = "#{tenantId}";

    @Value("${spring.application.name:hzero-platform}")
    private String serviceName;

    public DefaultPermissionSqlRepository(RedisHelper redisHelper) {
        super();
        this.redisHelper = redisHelper;
    }

    @Override
    public Map<Table, PermissionRangeVO> getPermissionRangeVOMap(Collection<Table> tableCollection, String serviceName,
                                                                 String sqlId, Long tenantId) {
        if (CollectionUtils.isNotEmpty(tableCollection)) {
            redisHelper.setCurrentDatabase(HZeroService.Platform.REDIS_DB);
            Map<Table, PermissionRangeVO> tableListMap = new HashMap<>();
            tableCollection.forEach(table -> {
                PermissionRangeVO rangeVO = getPermissionRange(serviceName, table, sqlId, tenantId);
                if (rangeVO != null) {
                    tableListMap.put(table, rangeVO);
                }
            });
            redisHelper.clearCurrentDatabase();
            return tableListMap;
        }
        return null;
    }

    @Override
    public PermissionRangeVO getPermissionRange(String serviceName, Table table, String sqlId, Long tenantId) {
        Map<String, PermissionRangeVO> localCache = permissionCache.get();
        if (localCache == null) {
            localCache = new HashMap<>(1);
            permissionCache.set(localCache);
        }
        String localCacheKey = table.getName() + "&" + sqlId + "&" + tenantId;
        if (localCache.containsKey(localCacheKey)) {
            return localCache.get(localCacheKey);
        }

        String cacheKey = KeyUtils.generateCacheKey(table.getName());
        redisHelper.setCurrentDatabase(HZeroService.Platform.REDIS_DB);
        Map<String, String> permissionRangeVOMap = redisHelper.hshGetAll(cacheKey);
        redisHelper.clearCurrentDatabase();

        if (MapUtils.isNotEmpty(permissionRangeVOMap)) {
            PermissionRangeVO permissionRangeVO =
                    getPermissionRangeVO(tenantId, serviceName, sqlId, permissionRangeVOMap);
            if (permissionRangeVO == null && !BaseConstants.DEFAULT_TENANT_ID.equals(tenantId)) {
                // 如果当前租户下该表的数据权限规则不存在，则去平台级的规则中找
                permissionRangeVO = getPermissionRangeVO(BaseConstants.DEFAULT_TENANT_ID, serviceName, sqlId,
                        permissionRangeVOMap);
            }
            // 处理自定义规则
            handlePermissionRangeVO(permissionRangeVO, table, sqlId);
            // 处理动态表前缀
            handleDynamicDbPrefix(permissionRangeVO);
            // 处理表别名
            handleTableAlias(table, permissionRangeVO);
            localCache.put(localCacheKey, permissionRangeVO);
            return permissionRangeVO;
        }
        localCache.put(localCacheKey, null);
        return null;
    }

    @Override
    public void resetCache() {
        permissionCache.remove();
    }

    /**
     * 处理表别名
     *
     * @param table
     * @param permissionRangeVO
     */
    private void handleTableAlias(Table table, PermissionRangeVO permissionRangeVO) {
        if (permissionRangeVO != null && StringUtils.isNotEmpty(permissionRangeVO.getDbPrefix()) && CollectionUtils.isNotEmpty(permissionRangeVO.getSqlList()) && table.getAlias() == null) {
            Alias alias = new Alias(table.getName());
            table.setAlias(alias);
        }
    }

    /**
     * 处理表前缀，如果存在动态表前缀标记，则根据维护信息进行维护替换
     *
     * @param permissionRangeVO
     */
    private void handleDynamicDbPrefix(PermissionRangeVO permissionRangeVO) {
        if (permissionRangeVO == null) {
            return;
        }
        String value = null;
        if (DYNAMIC_PREFIX.equals(permissionRangeVO.getDbPrefix())) {
            String key = KeyUtils.generatePrefixCacheKey(DetailsHelper.getUserDetails().getTenantId(), serviceName);
            value = redisHelper.strGet(key);
        } else if (StringUtils.isNotEmpty(permissionRangeVO.getDbPrefix())) {
            List<String> dynamicDbPrefixList = StringUtils.getFieldList(permissionRangeVO.getDbPrefix());
            if (CollectionUtils.isNotEmpty(dynamicDbPrefixList)) {
                String key = KeyUtils.generatePrefixCacheKey(DetailsHelper.getUserDetails().getTenantId(), StringUtils.getField(dynamicDbPrefixList.get(0)));
                value = redisHelper.strGet(key);
            }
        }
        if (value != null) {
            permissionRangeVO.setDbPrefix(value);
        }
    }

    /**
     * 处理多个数据权限组合信息，返回应用层级最小的数据权限信息，判断应用层级大小的规则如下：
     * tenantId > tenantId + serviceName > tenantId + sqlI > tenantId + serviceName + sqlId
     *
     * @param tenantId             tenantId
     * @param serviceName          serviceName
     * @param sqlId                sqlId
     * @param permissionRangeVOMap permissionRangeVOMap
     * @return 数据权限信息
     */
    private PermissionRangeVO getPermissionRangeVO(Long tenantId, String serviceName, String sqlId,
                                                   Map<String, String> permissionRangeVOMap) {
        sqlId = StringUtils.handleCountSqlId(sqlId);
        String mapKey = KeyUtils.generateMapKey(tenantId, null, null);
        String mapKeyWithServiceName = KeyUtils.generateMapKey(tenantId, serviceName, null);
        String mapKeyWithSqlId = KeyUtils.generateMapKey(tenantId, null, sqlId);
        String mapKeyWithServiceNameAndSqlId = KeyUtils.generateMapKey(tenantId, serviceName, sqlId);

        PermissionRangeVO tenant = this.redisHelper
                .fromJson(permissionRangeVOMap.get(mapKey), PermissionRangeVO.class);
        PermissionRangeVO tenantService = this.redisHelper
                .fromJson(permissionRangeVOMap.get(mapKeyWithServiceName), PermissionRangeVO.class);
        PermissionRangeVO tenantSqlId = this.redisHelper
                .fromJson(permissionRangeVOMap.get(mapKeyWithSqlId), PermissionRangeVO.class);
        PermissionRangeVO tenantServiceSqlId = this.redisHelper
                .fromJson(permissionRangeVOMap.get(mapKeyWithServiceNameAndSqlId), PermissionRangeVO.class);
        return bestMatch(tenantId, serviceName, sqlId, tenantServiceSqlId, tenantSqlId, tenantService, tenant);
    }

    /**
     * 判断处理数据屏蔽范围中的值，如果存在自定义规则标识，则选择用户自定义的sql，否则选择配置的sql
     *
     * @param rangeVO 范围vo
     * @param table   表
     */
    private void handlePermissionRangeVO(PermissionRangeVO rangeVO, Table table, String sqlId) {
        if (rangeVO == null) {
            return;
        }
        if (BaseConstants.Flag.YES.equals(rangeVO.getCustomRuleFlag())) {
            String sql = PermissionDataHelper.getTableSql(table.getName());
            // 如果不是分页的count语句则进行消除
            if (!StringUtils.isCountSql(sqlId)) {
                PermissionDataHelper.removeTableSql(table.getName());
            }
            rangeVO.setSqlList(new ArrayList<>());
            rangeVO.getSqlList().add(sql);
        }
    }

    /**
     * 处理分析多个数据权限信息
     *
     * @param ranges ranges
     * @return 最后一个权限信息
     */
    private PermissionRangeVO bestMatch(Long tenantId, String serviceName, String sqlId, PermissionRangeVO... ranges) {
        for (PermissionRangeVO range : ranges) {
            if (range != null && isNotEmpty(range)) {
                boolean exclude = false;
                if (!CollectionUtils.isEmpty(range.getRangeExclList())) {
                    for (PermissionRangeExclVO excl : range.getRangeExclList()) {
                        exclude |= (
                                (excl.getTenantId() != null && Objects.equals(excl.getTenantId(), tenantId))
                                        || (StringUtils.isNotEmpty(excl.getServiceName()) && Objects.equals(excl.getServiceName(), serviceName))
                                        || (StringUtils.isNotEmpty(excl.getSqlId()) && Objects.equals(excl.getSqlId(), sqlId))
                        );
                        if (exclude) {
                            break;
                        }
                    }
                }
                if (!exclude) {
                    return range;
                }
            }
        }
        return null;
    }

    private boolean isNotEmpty(PermissionRangeVO range) {
        return BaseConstants.Flag.YES.equals(range.getCustomRuleFlag())
                || StringUtils.isNotEmpty(range.getDbPrefix())
                || CollectionUtils.isNotEmpty(range.getSqlList())
                || CollectionUtils.isNotEmpty(range.getRangeExclList());
    }
}
