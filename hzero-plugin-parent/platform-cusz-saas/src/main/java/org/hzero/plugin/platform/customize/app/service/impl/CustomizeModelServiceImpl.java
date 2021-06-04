package org.hzero.plugin.platform.customize.app.service.impl;

import io.choerodon.core.exception.CommonException;
import io.choerodon.mybatis.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.hzero.boot.customize.constant.ModelConstant;
import org.hzero.boot.customize.dto.ColumnMetaData;
import org.hzero.boot.customize.dto.ModelMetaData;
import org.hzero.boot.customize.dto.TableMetaData;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.hzero.boot.platform.lov.dto.LovValueDTO;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.util.UUIDUtils;
import org.hzero.plugin.platform.customize.app.service.CustomizeModelFieldService;
import org.hzero.plugin.platform.customize.app.service.CustomizeModelService;
import org.hzero.plugin.platform.customize.domain.entity.*;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelFieldRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelRelationRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelRepository;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 11:12
 */
@Service
public class CustomizeModelServiceImpl implements CustomizeModelService {

    @Autowired
    private CustomizeModelRepository customizeModelRepository;
    @Autowired
    private CustomizeModelFieldRepository customizeModelFieldRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private CustomizeModelFieldService customizeModelFieldService;
    @Autowired
    private CustomizeModelRelationRepository customizeModelRelationRepository;
    @Autowired
    private CustomizeModelRelationRepository relationRepository;
    @Autowired
    private LovAdapter lovAdapter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Model createModel(Model model) {
        checkParam(model);
        model.setModelCode(model.getModelCode().toUpperCase());
        try {
            List<ModelField> insertFields = new ArrayList<>();
            String primaryKey = "";
            model.setModelCode(model.getModelCode().toUpperCase());
            //已存在
            ModelObjectPub modelObjectPub = new ModelObjectPub();
            modelObjectPub.setCode(model.getModelCode());
            if (customizeModelRepository.selectOne(modelObjectPub) != null) {
                throw new CommonException("hpfm.cusz.error.repeat.model");
            }
            ModelObjectPub modelTable = new ModelObjectPub();
            modelTable.setRefTableName(model.getModelTable());
            modelTable.setAppId(-1L);
            if (customizeModelRepository.selectOne(modelTable) != null) {
                throw new CommonException("hpfm.cusz.error.repeat.table");
            }

            //获取当前模型的表字段  todo
            String url = "http://" + model.getServiceName() + ModelConstant.TABLE_INFO_PATH +
                    "?tableName=" + model.getModelTable();
            ResponseEntity<TableMetaData> responseEntity = restTemplate.getForEntity(url, TableMetaData.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new CommonException("hpfm.cusz.error.sync.model");
            }

            //转换数据
            for (ColumnMetaData column : responseEntity.getBody().getAllColumns()) {
                ModelField field = new ModelField(column);
                field.setTenantId(0L);
                field.setFieldType(column.getColumnType());
                insertFields.add(field);
                if (field.isFieldPrimaryKey()) {
                    //暂时只考虑一个主键的情况
                    primaryKey = field.getFieldCode();
                }
            }

            model.setPrimaryKey(primaryKey);
            modelObjectPub = Model.convertFromModel(model);
            modelObjectPub.setTenantId(0L);
            customizeModelRepository.insertSelective(modelObjectPub);
            model.setModelId(modelObjectPub.getId());
            customizeModelRepository.cacheModel(model);
            insertFields.forEach(t -> {
                        t.setFieldName(StringUtils.substring(t.getFieldName(), 0, 100));
                        t.setModelId(model.getModelId());
                    }
            );

            // 初始化ModelField
            customizeModelFieldService.insertFieldFromDb(insertFields, model.getModelId());
        } catch (Exception e) {
            throw new CommonException("hpfm.cusz.error.sync.model");
        }
        return model;
    }

    @Override
    public List<TableMetaData> getTables(String serviceName) {
        try {
            //todo
            String url = "http://" + serviceName + ModelConstant.TABLE_LIST_PATH;
            ResponseEntity<List<TableMetaData>> result = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<TableMetaData>>() {
            });
            if (!result.getStatusCode().is2xxSuccessful()) {
                throw new CommonException("hpfm.cusz.error.load.model");
            }
            return result.getBody();
        } catch (Exception e) {
            throw new CommonException("hpfm.cusz.error.load.model");
        }
    }

    @Override
    public List<Model> selectAssociatedModels(Long unitId, Long modelId) {
        Set<Long> modelIdSet = new HashSet<>();
        List<ModelRelation> allRelations = customizeModelRelationRepository.selectAllRelations();
        Collection<ModelRelation> relations = getRelations(allRelations, modelId);
        relations.forEach(relation -> {
            modelIdSet.add(relation.getMasterModelId());
            modelIdSet.add(relation.getSlaveModelId());
        });
        modelIdSet.add(modelId);
        List<ModelObjectPub> objectPubs = customizeModelRepository.selectByIds(StringUtil.join(modelIdSet, ","));
        List<Model> result = new LinkedList<>();
        //类型转换，同时排序，将主模型放在第一个
        objectPubs.forEach(modelPub -> {
            Model model = Model.convertFromModelObject(modelPub);
            if (modelId.equals(model.getModelId())) {
                result.add(0, model);
            } else {
                result.add(model);
            }
        });
        return result;
    }

    protected List<ModelRelation> getRelations(List<ModelRelation> source, Long modelId) {
        List<ModelRelation> masterRelations = new ArrayList<>(16);
        Map<Long, List<ModelRelation>> one2oneMaps = new HashMap<>(64);
        Map<Long, List<ModelRelation>> manyToOne = new HashMap<>(64);
        source.forEach(relation -> {
            Long masterModelId = relation.getMasterModelId();
            Long slaveModelId = relation.getSlaveModelId();
            if (modelId.equals(masterModelId) || modelId.equals(slaveModelId)) {
                //单元关联主模型 判断一对一，多对一
                if (CustomizeConstants.ModelRelationType.ONE_TO_ONE.equalsIgnoreCase(relation.getRelation())) {
                    masterRelations.add(relation);
                } else {
                    //多对一
                    if (slaveModelId.equals(modelId)) {
                        masterRelations.add(relation);
                    }
                }
                return;
            }
            //缓存一对一关系
            if (CustomizeConstants.ModelRelationType.ONE_TO_ONE.equalsIgnoreCase(relation.getRelation())) {
                one2oneMaps.computeIfAbsent(relation.getMasterModelId(), key -> new ArrayList<>()).add(relation);
                one2oneMaps.computeIfAbsent(relation.getSlaveModelId(), key -> new ArrayList<>()).add(relation);
            }
            //缓存多对一关系
            if (CustomizeConstants.ModelRelationType.ONE_TO_MANY.equalsIgnoreCase(relation.getRelation())) {
                manyToOne.computeIfAbsent(relation.getSlaveModelId(), key -> new ArrayList<>()).add(relation);
            }
        });
        List<ModelRelation> relations = new ArrayList<>(24);
        relations.addAll(masterRelations);
        //递归处理一对一关系
        Set<Long> processedSet = new HashSet<>(12);
        processedSet.add(modelId);
        //递归处理多对一关系
        Set<Long> processedManySet = new HashSet<>(12);
        processedManySet.add(modelId);
        for (ModelRelation modelRelation : masterRelations) {
            Long id = modelId.equals(modelRelation.getMasterModelId()) ? modelRelation.getSlaveModelId() : modelRelation.getMasterModelId();
            processOne2One(id, processedSet, one2oneMaps, relations);
            processMany2One(id, processedManySet, manyToOne, relations);
        }
        return relations;
    }

    private void processOne2One(Long modelId, Set<Long> processedSet, Map<Long, List<ModelRelation>> one2oneMaps, List<ModelRelation> relations) {
        //当前模型C 如果已经处理过1对1关系，跳过
        if (processedSet.contains(modelId)) {
            return;
        }
        //处理模型C 1对一1关系
        processedSet.add(modelId);
        if (!one2oneMaps.containsKey(modelId)) {
            return;
        }
        List<ModelRelation> relationList = one2oneMaps.get(modelId);
        for (ModelRelation modelRelation : relationList) {
            relations.add(modelRelation);
            Long id = modelId.equals(modelRelation.getMasterModelId()) ? modelRelation.getSlaveModelId() : modelRelation.getMasterModelId();
            processOne2One(id, processedSet, one2oneMaps, relations);
        }
    }

    private void processMany2One(Long modelId, Set<Long> processedSet, Map<Long, List<ModelRelation>> manyToOneMaps, List<ModelRelation> relations) {
        //当前模型C 如果已经处理过多对一关系，跳过
        if (processedSet.contains(modelId)) {
            return;
        }
        //处理模型C 1对一1关系
        processedSet.add(modelId);
        if (!manyToOneMaps.containsKey(modelId)) {
            return;
        }
        List<ModelRelation> relationList = manyToOneMaps.get(modelId);
        for (ModelRelation modelRelation : relationList) {
            relations.add(modelRelation);
            Long id = modelId.equals(modelRelation.getMasterModelId()) ? modelRelation.getSlaveModelId() : modelRelation.getMasterModelId();
            processMany2One(id, processedSet, manyToOneMaps, relations);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Model updateByOptions(Model model) {
        Assert.notNull(model.getModelId(), "error.modelId.null");
        customizeModelRepository.updateOptional(Model.convertFromModel(model), ModelObjectPub.FIELD_NAME);
        customizeModelRepository.cacheModel(Model.convertFromModelObject(customizeModelRepository.selectByPrimaryKey(model.getModelId())));
        return model;
    }

    private void checkParam(Model model) {
        Assert.notNull(model.getServiceName(), "error.serviceName.null.");
        Assert.notNull(model.getModelTable(), "error.modelTable.null.");
        Assert.notNull(model.getModelCode(), "error.modelCode.null");
        Assert.notNull(model.getModelName(), "error.modelName.null");
    }

    @Override
    public List<ModelRelation> selectAssociateRelation(Long modelId) {
        List<ModelRelation> results = relationRepository.selectRelations(modelId);
        List<LovValueDTO> valueDTOS = lovAdapter.queryLovValue(CustomizeConstants.LovCode.MODEL_RELATION_LOV_CODE, BaseConstants.DEFAULT_TENANT_ID);
        //若当前模型为从模型并且模型关系为一对多，重新设置模型关系
        for (ModelRelation result : results) {
            if (modelId.equals(result.getSlaveModelId())) {
                if (CustomizeConstants.ModelRelationType.ONE_TO_MANY.equalsIgnoreCase(result.getRelation())) {
                    result.resetRelation();
                    result.setRelation(CustomizeConstants.ModelRelationType.MANY_TO_ONE);
                } else if (CustomizeConstants.ModelRelationType.ONE_TO_ONE.equalsIgnoreCase(result.getRelation())) {
                    result.resetRelation();
                }
            }
            //翻译模型关系
            translationRelation(valueDTOS, result);
        }
        return results;
    }

    private void translationRelation(List<LovValueDTO> valueDTOS, ModelRelation relation) {
        valueDTOS.forEach(value -> {
            if (value.getValue().equalsIgnoreCase(relation.getRelation())) {
                relation.set_innerMap("fieldNameMeaning", value.getMeaning());
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelRelation createRelation(ModelRelation relation) {
        Assert.notNull(relation.getMasterModelId(), "master modelId can not be null!");
        Assert.notNull(relation.getSlaveModelId(), "slave modelId can not be null !");
        ModelMetaData masterModel = customizeModelRepository.getModelCache(relation.getMasterModelId());
        ModelMetaData slaveModel = customizeModelRepository.getModelCache(relation.getSlaveModelId());
        ModelRelationPub relationHeader = new ModelRelationPub();
        relationHeader.setCode(UUIDUtils.generateUUID());
        relationHeader.setMasterModelObjectCode(masterModel.getModelCode());
        relationHeader.setRelationModelObjectCode(slaveModel.getModelCode());
        relationHeader.setRelationType(relation.getRelation());
        relationHeader.setName(UUIDUtils.generateUUID());

        //TODO 优化，考虑走缓存
        ModelFieldPub masterModelFieldPub = customizeModelFieldRepository.selectByPrimaryKey(relation.getMasterFieldId());
        ModelFieldPub slaveModelFieldPub = customizeModelFieldRepository.selectByPrimaryKey(relation.getSlaveFieldId());

        ModelRelationFieldPub relationFieldPub = new ModelRelationFieldPub();
        relationFieldPub.setMasterModelFieldCode(masterModelFieldPub.getCode());
        relationFieldPub.setRelationModelFieldCode(slaveModelFieldPub.getCode());

        List<BaseModelRelationField> relations = new ArrayList<>(1);
        relations.add(relationFieldPub);
        relationHeader.setModelRelationFields(relations);

        relationHeader.setName(UUIDUtils.generateUUID());
        relationHeader.setTenantId(0L);
        relationRepository.insertRelation(relationHeader);
        relationRepository.cacheRelation(relation);
        return relation;
    }
}
