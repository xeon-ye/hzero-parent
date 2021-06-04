package org.hzero.plugin.platform.customize.infra.repository.impl;

import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.mybatis.util.StringUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.customize.constant.ModelConstant;
import org.hzero.boot.customize.dto.CustomizeConfig;
import org.hzero.boot.customize.dto.CustomizeConfigField;
import org.hzero.boot.customize.dto.ModelFieldMetaData;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.hzero.core.helper.LanguageHelper;
import org.hzero.core.redis.RedisHelper;
import org.hzero.plugin.platform.customize.api.dto.*;
import org.hzero.plugin.platform.customize.domain.entity.*;
import org.hzero.plugin.platform.customize.domain.repository.*;
import org.hzero.plugin.platform.customize.infra.common.ConfigLocalCache;
import org.hzero.plugin.platform.customize.infra.common.ModelLocalCache;
import org.hzero.plugin.platform.customize.infra.common.UnitLocalCache;
import org.hzero.plugin.platform.customize.infra.mapper.CustomizeConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xiangyu.qi01@hand-china.com on 2020-01-03.
 */
@Component
public class CustomizeCommonRepositoryImpl implements CustomizeCommonRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomizeConfigRepositoryImpl.class);

    @Autowired
    private CustomizeConfigMapper unitConfigMapper;
    @Autowired
    private CustomizeUnitRepository unitRepository;
    @Autowired
    private CustomizeModelRepository modelRepository;
    @Autowired
    private CustomizeModelFieldRepository modelFieldRepository;
    @Autowired
    private CustomizeConfigRepository configRepository;
    @Autowired
    private CustomizeConfigFieldRepository configFieldRepository;
    @Autowired
    private CustomizeUnitFieldRepository unitFieldRepository;
    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private LovAdapter lovAdapter;

    @Value("${hzero.service.platform.customize.user_cusz_enabled:true}")
    private boolean userCustomizeEnabled;

    @Override
    public Map<String, UnitConfigDTO> unitConfigDetailCache(Long tenantId, String[] unitCodes) {
        if (ArrayUtils.isEmpty(unitCodes)) {
            return Collections.emptyMap();
        }
        UnitLocalCache unitLocalCache = new UnitLocalCache(unitFieldRepository);
        ConfigLocalCache configLocalCache = new ConfigLocalCache(configFieldRepository);
        ModelLocalCache modelLocalCache = new ModelLocalCache(modelRepository, modelFieldRepository);
        Map<String, UnitConfigDTO> result = new HashMap<>(unitCodes.length);

        for (String unitCode : unitCodes) {
            UnitConfigDTO unitConfigDTO = unitConfigDetailCache(unitLocalCache, configLocalCache, modelLocalCache, tenantId, unitCode);
            if (unitConfigDTO != null) {
                result.put(unitCode, unitConfigDTO);
            }
        }
        return result;
    }

    @Override
    public CustomizeConfig getCustomizeConfig(Long tenantId, Long unitId) {
        Unit unit = unitRepository.selectUnitAndModelTable(unitId);
        if (unit == null) {
            return null;
        }
        CustomizeConfig configVO = new CustomizeConfig();
        configVO.setUnitCode(unit.getUnitCode());
        configVO.setModelId(unit.getModelId());
        configVO.setSqlIds(unit.getSqlIds());
        configVO.setMasterTable(unit.getModelTable());
        configVO.setUnitType(unit.getUnitType());
        List<CustomizeConfigField> fields = configFieldRepository.selectConfigModelFieldsByUnitId(tenantId, unit.getId());
        UnitField condition = new UnitField();
        condition.setUnitId(unit.getId());
        List<UnitField> unitFields = unitFieldRepository.select(condition);
        Map<Long, Boolean> unitFieldsMap = Collections.emptyMap();
        if (CollectionUtils.isNotEmpty(unitFields)) {
            unitFieldsMap = unitFields.stream().filter(t -> !t.isNotTableField()).collect(Collectors.toMap(UnitField::getFieldId, v -> true));
        }
        if (CollectionUtils.isNotEmpty(fields)) {
            List<CustomizeConfigField> noStdFields = new ArrayList<>(fields.size());
            for (CustomizeConfigField configField : fields) {
                if (!unitFieldsMap.containsKey(configField.getFieldId())) {
                    noStdFields.add(configField);
                }
            }
            configVO.setFields(noStdFields);
        }
        return configVO;
    }

    @Override
    public CustomizeConfig cacheCustomizeConfig(Long tenantId, Long unitId) {
        CustomizeConfig config = this.getCustomizeConfig(tenantId, unitId);
        if (config != null && CollectionUtils.isNotEmpty(config.getFields())) {
            saveUnitConfigToRedis(tenantId, config.getUnitCode(), config);
        } else if (config != null) {
            //删除缓存
            redisHelper.hshDelete(getCacheKey(config.getUnitCode()), String.valueOf(tenantId));
        }
        return config;
    }


    protected void saveUnitConfigToRedis(Long tenantId, String unitCode, CustomizeConfig customizeConfigVO) {
        redisHelper.hshPut(getCacheKey(unitCode), String.valueOf(tenantId), redisHelper.toJson(customizeConfigVO));
    }

    protected UnitConfigDTO unitConfigDetailCache(UnitLocalCache unitLocalCache,
                                                  ConfigLocalCache configLocalCache,
                                                  ModelLocalCache modelLocalCache,
                                                  Long tenantId,
                                                  String unitCode) {
        //个性化单元配置
        Unit unit = unitRepository.getUnitCache(unitCode);
        if (unit == null || !ObjectUtils.nullSafeEquals(1, unit.getEnableFlag())) {
            return null;
        }
        Long userId = DetailsHelper.getUserDetails().getUserId();
        //个性化配置头
        Config config = configRepository.getConfigCache(tenantId, unitCode);
        if (config == null) {
            config = new Config();
        }
        config.setReadOnly(unit.getReadOnly());
        if (unit.getFormMaxCol() != null) {
            config.setMaxCol(Long.valueOf(unit.getFormMaxCol()));
        }
        //用户级配置
        //用户个性化配置
        Map<String, ConfigField> userConfigsMap = new HashMap<>(1);
        if(userCustomizeEnabled) {
            Config userConfig = configRepository.getUserConfigCache(tenantId, userId, unitCode);
            if (userConfig != null) {
                config.setPageSize(userConfig.getPageSize());
            }
            userConfigsMap = configFieldRepository.getUserConfigFieldCache(tenantId, userId, unitCode);
        }
        UnitConfigDTO unitConfigDTO = new UnitConfigDTO();
        unitConfigDTO.readFormConfig(config, unit);
        //个性化配置字段
        Map<String, ConfigField> configCache = new HashMap<>(configLocalCache.getAllConfigFields(tenantId, unitCode));
        //个性化单元字段配置
        Map<String, UnitField> unitCache = new HashMap<>(unitLocalCache.getAllUnitField(unitCode));


        if (MapUtils.isNotEmpty(unitCache)) {
            unitCache.forEach((key, value) -> {
                if (!configCache.containsKey(key)) {
                    ConfigField configField = ConfigField.readFormUnitField(value);
                    //从模型获取组件配置
                    if (!configField.isNotTableField()) {
                        ModelFieldMetaData modelFieldMetaData = modelLocalCache.getModelField(value.getModelId(), value.getFieldId());
                        if (modelFieldMetaData.getWdgMetaData() != null) {
                            configField.setWidget(ConfigFieldWidget.readFromModelWdg(modelFieldMetaData.getWdgMetaData()));
                        }
                    }
                    configCache.put(key, configField);
                } else {
                    //拷贝单元的参数配置
                    configCache.get(key).setParamList(ConfigField.readFormUnitField(value).getParamList());
                }
            });
        }
        String lang = LanguageHelper.language();
        List<UnitConfigFieldDTO> unitConfigFieldDTOS = new ArrayList<>(configCache.size());
        for (Map.Entry<String, ConfigField> entry : configCache.entrySet()) {
            String mapKey = entry.getKey();
            ConfigField configField = entry.getValue();
            //如果是文本渲染，从modelField获取组件类型
            if (configField.getWidget() == null && !configField.isNotTableField() && "TEXT".equals(configField.getRenderOptions())) {
                ConfigFieldWidget fieldWidget = new ConfigFieldWidget();
                configField.setWidget(fieldWidget);
                fieldWidget.setFieldWidget(modelLocalCache.getModelField(configField.getModelId(), configField.getFieldId()).getWdgMetaData().getFieldWidget());
            }
            //翻译字段名称多语言
            configField.convertTls(lang, unitCache.containsKey(mapKey));
            //翻译组件默认值
            if (configField.getWidget() != null) {
                configField.getWidget().translateLov(lovAdapter, tenantId);
            }
            UnitConfigFieldDTO unitConfigField = new UnitConfigFieldDTO();
            unitConfigField.readFormConfig(configField);

            if (unitCache.containsKey(mapKey)) {
                unitConfigField.setStandardField(true);
                //标准字段，别名从unit配置中读取
                unitConfigField.setFieldAlias(unitCache.get(mapKey).fieldCode());
            } else {
                ConfigFieldParam.translateParmaUnit(configField.getParamList(), tenantId, unitLocalCache, configLocalCache);
            }

            unitConfigField.setFieldCode(StringUtil.underlineToCamelhump(unitConfigField.fieldCode()));
            //值集带值映射
            if ((ModelConstant.WidgetType.LOV.equals(unitConfigField.getFieldType())
                    || ModelConstant.WidgetType.SELECT.equals(unitConfigField.getFieldType()))
                    && CollectionUtils.isNotEmpty(unitConfigField.getLovMappings())) {
                unitConfigField.getLovMappings().forEach(lovMap -> {
                    lovMap.setSourceCode(StringUtil.underlineToCamelhump(lovMap.getSourceCode()));
                    //获取targetCode
                    Long targetFieldId = lovMap.getTargetFieldId();
                    String targetCode = null;
                    if (configLocalCache.containsConfigField(tenantId, unitCode, targetFieldId)) {
                        ConfigField cnfField = configLocalCache.getConfigField(tenantId, unitCode, targetFieldId);
                        targetCode = cnfField.fieldCode();
                    } else if (unitLocalCache.containsField(unitCode, targetFieldId)) {
                        UnitField unitField = unitLocalCache.getUnitField(unitCode, targetFieldId);
                        targetCode = unitField.getFieldCode();
                    }
                    if (StringUtils.isNotEmpty(targetCode)) {
                        lovMap.setTargetCode(StringUtil.underlineToCamelhump(targetCode));
                    }
                });
            }
            //转换条件
            List<FieldConditionHeader> conditionHeaders = configField.getConditionHeaders();
            List<UnitConfigFieldConditionHeaderDTO> headerDTOS;
            if (CollectionUtils.isNotEmpty(conditionHeaders)) {
                headerDTOS = new ArrayList<>(conditionHeaders.size());
                for (FieldConditionHeader header : conditionHeaders) {
                    //覆盖别名,优先使用租户个性化配置的别名
                    List<UnitConfigFieldConditionLineDTO> conditionLineDTOS = convertCondition(header, tenantId, unitLocalCache, configLocalCache);
                    UnitConfigFieldConditionHeaderDTO headerDTO = header.cacheConvertToDTO();
                    headerDTO.setLines(conditionLineDTOS);
                    headerDTOS.add(headerDTO);
                }
                unitConfigField.setConditionHeaderDTOs(headerDTOS);
            }
            //转换高级校验配置
            FieldConditionHeader conValid = configField.getConValid();
            if (conValid != null) {
                FieldConValidDTO conValidDTO = new FieldConValidDTO();
                //覆盖别名,优先使用租户个性化配置的别名
                List<UnitConfigFieldConditionLineDTO> lineDTOS = convertCondition(conValid, tenantId, unitLocalCache, configLocalCache);
                conValidDTO.setConLineList(lineDTOS);
                if (CollectionUtils.isNotEmpty(conValid.getValids())){
                    conValidDTO.setConValidList(conValid.getValids().stream().map(valid -> valid.convertTls(lang, unitCache.containsKey(mapKey))).collect(Collectors.toList()));
                }
                unitConfigField.setConValidDTO(conValidDTO);
            }

            if (userConfigsMap.containsKey(mapKey)) {
                ConfigField higher = userConfigsMap.get(mapKey);
                //根据当前语言设置多语言字段
                higher.convertTls(lang, unitCache.containsKey(mapKey));
                //用户级覆盖租户级
                unitConfigField.overrideUserConfig(higher);
            }
            unitConfigFieldDTOS.add(unitConfigField);
        }
        unitConfigDTO.setFields(unitConfigFieldDTOS);
        unit.setUnitCode(unitCode);
        unitConfigDTO.setUnitAlias(unit.autoGenerateUnitAlias(null));
        return unitConfigDTO;
    }

    private List<UnitConfigFieldConditionLineDTO> convertCondition(FieldConditionHeader header, Long tenantId, UnitLocalCache unitLocalCache,
                                                                   ConfigLocalCache configLocalCache) {
        if (CollectionUtils.isEmpty(header.getLines())) {
            return Collections.emptyList();
        }
        return header.getLines().stream().map(line -> {
            UnitConfigFieldConditionLineDTO lineDTO = line.cacheConvertToDTO();
            //别名覆盖
            lineDTO.coverSourceFieldCode(configLocalCache.getConfigField(tenantId, line.getSourceUnitCode(), line.getSourceFieldId()),
                    unitLocalCache.getUnitField(line.getSourceUnitCode(), line.getSourceFieldId()));
            if (line.getTargetFieldId() != null) {
                lineDTO.coverTargetFieldCode(configLocalCache.getConfigField(tenantId, line.getTargetUnitCode(), line.getTargetFieldId()),
                        unitLocalCache.getUnitField(line.getTargetUnitCode(), line.getTargetFieldId()));
            }
            return lineDTO;
        }).collect(Collectors.toList());
    }

    /**
     * 生成redis存储key
     */
    private static String getCacheKey(String unitCode) {
        return org.hzero.boot.customize.constant.CustomizeConstants.INTERCEPTOR_CACHE_KEY + unitCode;
    }

}
