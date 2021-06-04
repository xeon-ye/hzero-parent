package org.hzero.plugin.platform.customize.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.service.Tag;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * @author xiangyu.qi01@hand-china.com on 2019-12-16.
 */
@Configuration
public class CuszPlatformSwaggerApiConfig {

    public static final String CUSZ_COMMON_SITE = "Cusz Common API(Site Level)";
    public static final String CUSZ_COMMON = "Cusz Common API";
    public static final String CUSZ_UNIT_CONFIG = "Cusz Unit Config";
    public static final String CUSZ_UNIT_CONFIG_SITE = "Cusz Unit Config(Site Level)";
    public static final String CUSZ_UNIT_SITE = "Cusz Unit (Site Level)";
    public static final String CUSZ_UNIT = "Cusz Unit";

    public static final String CUSZ_MODEL_SITE = "Cusz Model(Site Level)";
    public static final String CUSZ_MODEL_FIELD_SITE = "Cusz Model Field(Site Level)";
    public static final String CUSZ_MODEL_RELATION_SITE = "Cusz Model Relation(Site Level)";

    public static final String CUSZ_MODEL = "Cusz Model";
    public static final String CUSZ_MODEL_FIELD = "Cusz Model Field";
    public static final String CUSZ_MODEL_RELATION = "Cusz Model Relation";

    @Autowired
    public CuszPlatformSwaggerApiConfig(Docket docket) {
        docket.tags(
                new Tag(CUSZ_MODEL_SITE, "模型管理(平台级)"),
                new Tag(CUSZ_MODEL_FIELD_SITE, "模型字段管理（平台级）"),
                new Tag(CUSZ_MODEL_RELATION_SITE, "模型关系管理（平台级）"),
                new Tag(CUSZ_UNIT_SITE, "个性化单元管理（平台级）"),
                new Tag(CUSZ_UNIT_CONFIG, "租户个性化管理"),
                new Tag(CUSZ_COMMON_SITE, "租户级个性化通用API（平台级）"),

                new Tag(CUSZ_MODEL, "模型管理"),
                new Tag(CUSZ_MODEL_FIELD, "模型字段管理"),
                new Tag(CUSZ_MODEL_RELATION, "模型关系管理）"),
                new Tag(CUSZ_UNIT, "个性化单元管理"),
                new Tag(CUSZ_COMMON, "租户级个性化通用API")
        );
    }
}
