package org.hzero.route.constant;

/**
 * 通用变量
 * 
 * @author bojiangzhou 2018/09/28
 */
public class RouteConstants {
    /**
     * 实例 metadata 或者 label
     */
    public static final String META_NODE_GROUP_RULE = "HZERO_NODE_RULE";

    /**
     * HZero Governance
     */
    public interface Admin {
        String CODE = "hadm";
        Integer REDIS_DB = 1;
    }

    /**
     * url映射-常量
     */
    public interface UrlMapping {
        String HADM_UM = "hadm:um";
        String ORGANIZATIONID = "{organizationId}";
        String V1 = "/v1";
        String URL = "url";
        String SERVICE_STATUS = "serviceStatus";
        String Y = "Y";
        String N = "N";
    }
}
