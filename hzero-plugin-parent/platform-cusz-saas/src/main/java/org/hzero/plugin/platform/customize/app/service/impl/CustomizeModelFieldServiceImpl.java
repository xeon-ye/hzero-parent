package org.hzero.plugin.platform.customize.app.service.impl;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.boot.customize.dto.ColumnMetaData;
import org.hzero.boot.customize.dto.TableMetaData;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.hzero.boot.platform.lov.dto.LovValueDTO;
import org.hzero.core.base.BaseConstants;
import org.hzero.plugin.platform.customize.api.dto.FieldCommonSearchDTO;
import org.hzero.plugin.platform.customize.app.service.CustomizeModelFieldService;
import org.hzero.plugin.platform.customize.domain.entity.ModelField;
import org.hzero.plugin.platform.customize.domain.entity.ModelFieldPub;
import org.hzero.plugin.platform.customize.domain.entity.ModelFieldRule;
import org.hzero.plugin.platform.customize.domain.entity.ModelFieldWidget;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelFieldRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelFieldRuleRepository;
import org.hzero.plugin.platform.customize.domain.repository.CustomizeModelFieldWidgetRepository;
import org.hzero.plugin.platform.customize.infra.constant.CustomizeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author : peng.yu01@hand-china.com 2019/12/12 13:38
 */
@Service
public class CustomizeModelFieldServiceImpl implements CustomizeModelFieldService {

    @Autowired
    private CustomizeModelFieldWidgetRepository fieldWidgetRepository;
    @Autowired
    private CustomizeModelFieldRepository customizeModelFieldRepository;
    @Autowired
    private CustomizeModelFieldWidgetRepository customizeModelFieldWidgetRepository;
    @Autowired
    private CustomizeModelFieldRuleRepository customizeModelFieldRuleRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private LovAdapter lovAdapter;


    @Override
    public List<ModelField> selectFieldByModelId(FieldCommonSearchDTO searchDTO, PageRequest pageRequest) {
        Assert.notNull(searchDTO.getModelId(), "error.modelId.null.");
        if (pageRequest == null) {
            return translationLov(customizeModelFieldRepository.selectFieldWithWdg(searchDTO));
        }
        return PageHelper.doPageAndSort(pageRequest, () -> translationLov(customizeModelFieldRepository.selectFieldWithWdg(searchDTO)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelField updateFieldById(ModelField field) {
        Assert.notNull(field.getFieldName(), "error.fieldName.null.");
        ModelFieldPub modelFieldPub = new ModelFieldPub();
        modelFieldPub.setId(field.getFieldId());
        modelFieldPub.setDisplayName(field.getFieldName());
        modelFieldPub.setFieldType(field.getFieldCategory());
        customizeModelFieldRepository.updateOptional(ModelField.convertFromModelField(field), ModelFieldPub.FIELD_DISPLAY_NAME, ModelFieldPub.FIELD_FIELD_TYPE);
        //修改组件信息
        this.saveWidget(field);
        //缓存,包含组件
        ModelField cacheField = ModelField.convertFromModelFieldPub(customizeModelFieldRepository.selectByPrimaryKey(field.getFieldId()));
        ModelFieldWidget widget = customizeModelFieldWidgetRepository.selectOne(new ModelFieldWidget(cacheField.getFieldId()));
        cacheField.setWdg(widget);
        customizeModelFieldRepository.cacheModelField(cacheField);
        return field;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelField createField(ModelField modelField) {
        Assert.notNull(modelField.getFieldName(), "ModelField must set fieldName.");
        Assert.notNull(modelField.getFieldCode(), "ModelField must set fieldCode.");
        modelField.setFieldCategory(CustomizeConstants.FieldCategory.VIRTUAL_FIELD);
        modelField.setTenantId(DetailsHelper.getUserDetails().getTenantId());
        modelField.setDefaultValue(" ");
        modelField.setFieldType("string");
        modelField.setNotNull(0);
        int insert = customizeModelFieldRepository.insertSelective(ModelField.convertFromModelField(modelField));
        if (insert != 1) {
            throw new CommonException("error.create.modelField.");
        }
        customizeModelFieldRepository.cacheModelField(modelField);
        return modelField;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertFieldFromDb(List<ModelField> modelFields, Long modelId) {
        modelFields.forEach(field -> {
            field.setModelId(modelId);
            ModelFieldPub fieldPub = ModelField.convertFromModelField(field);
            customizeModelFieldRepository.insertSelective(fieldPub);
            field.setFieldId(fieldPub.getId());

        });
        customizeModelFieldRepository.cacheModelFieldList(modelFields);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ModelField> syncField(String serviceName, String tableName, Long modelId) {
        Map<String, ModelField> modelFieldMap = new HashMap<>(6);
        try {
            //获取当前模型的表字段
            String url = "http://" + serviceName + "/v1/customize/model/tableMetadata?tableName=" + tableName;
            ResponseEntity<TableMetaData> responseEntity = restTemplate.getForEntity(url, TableMetaData.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new CommonException("hpfm.cusz.error.sync.model");
            }
            for (ColumnMetaData column : responseEntity.getBody().getAllColumns()) {
                ModelField tableField = new ModelField(column);
                tableField.setModelId(modelId);
                modelFieldMap.put(tableField.getFieldCode(), tableField);
            }
        } catch (Exception e) {
            throw new CommonException("error.remote.tableMetadata.The exception : " + e.getMessage());
        }
        //获取数据库中的ModelField
        ModelFieldPub tempField = new ModelFieldPub();
        tempField.setModelObjectId(modelId);
        List<ModelFieldPub> dbFields = customizeModelFieldRepository.select(tempField);

        //如果数据库存在相同数据则将其从modelFieldMap中移除，之后其中只存放需要插入数据库中的数据（不考虑数据库中修改的情况）
        dbFields.forEach(dbField -> {
            //截取字段长度
            if (!StringUtils.isEmpty(dbField.getDisplayName())) {
                dbField.setDisplayName(org.apache.commons.lang3.StringUtils.substring(dbField.getDisplayName(), 0, 100));
            }
            //数据库存在则更新
            if (modelFieldMap.containsKey(dbField.getFieldName())) {
//                ModelField updateField = modelFieldMap.get(dbField.getFieldName());
//                updateField.setFieldId(dbField.getId());
//                customizeModelFieldRepository.updateOptional(ModelField.convertFromModelField(updateField), ModelFieldPub.FIELD_FIELD_CATEGORY, ModelField.FIELD_FIELD_NAME);
//                customizeModelFieldRepository.cacheModelField(updateField);
                modelFieldMap.remove(dbField.getFieldName());
            }
        });

        if (modelFieldMap.size() > 0) {
            modelFieldMap.values().forEach(insertField -> {
                ModelFieldPub fieldPub = ModelField.convertFromModelField(insertField);
                customizeModelFieldRepository.insertSelective(fieldPub);
                //回写主键
                insertField.setFieldId(fieldPub.getId());
                customizeModelFieldRepository.cacheModelField(insertField);
            });
        }
        return translationLov(customizeModelFieldRepository.selectFieldWithWdg(new FieldCommonSearchDTO(modelId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteField(Long fieldId) {

        ModelField modelField = ModelField.convertFromModelFieldPub(customizeModelFieldRepository.selectByPrimaryKey(fieldId));

        customizeModelFieldRepository.deleteByPrimaryKey(fieldId);

        //删除相关组件
        ModelFieldWidget widget = new ModelFieldWidget();
        widget.setFieldId(fieldId);
        customizeModelFieldWidgetRepository.delete(widget);

        //删除规则
        ModelFieldRule rule = new ModelFieldRule();
        rule.setFieldId(fieldId);
        customizeModelFieldRuleRepository.delete(rule);

        //删除缓存
        customizeModelFieldRepository.delCaChe(modelField);
    }

    @Override
    public Page<ModelField> selectUnConfigFieldByModelId(FieldCommonSearchDTO searchDTO, Long tenantId, Long unitId, PageRequest pageRequest) {
        Assert.notNull(searchDTO.getModelId(), "error.modelId.null.");
        return PageHelper.doPageAndSort(pageRequest, () -> translationLov(customizeModelFieldRepository.selectUnConfigFieldByModelId(searchDTO, tenantId, unitId)));
    }

    @Override
    public List<ModelField> translationLov(List<ModelField> modelFields) {
        Map<String, LovValueDTO> fieldCategoryMap = lovAdapter.queryLovValue(CustomizeConstants.LovCode.FIELD_CATEGORY_LOV_CODE, BaseConstants.DEFAULT_TENANT_ID).stream().collect(Collectors.toMap(LovValueDTO::getValue, Function.identity(), (key1, key2) -> key2));
        Map<String, LovValueDTO> fieldTypeMap = lovAdapter.queryLovValue(CustomizeConstants.LovCode.FIELD_TYPE_LOV_CODE, BaseConstants.DEFAULT_TENANT_ID).stream().collect(Collectors.toMap(LovValueDTO::getValue, Function.identity(), (key1, key2) -> key2));
        Map<String, LovValueDTO> fieldWidgetTypeMap = lovAdapter.queryLovValue(CustomizeConstants.LovCode.FIELD_WIDGET_LOV_CODE, BaseConstants.DEFAULT_TENANT_ID).stream().collect(Collectors.toMap(LovValueDTO::getValue, Function.identity(), (key1, key2) -> key2));
        for (ModelField modelField : modelFields) {
            if (fieldCategoryMap.containsKey(modelField.getFieldCategory())) {
                LovValueDTO valueDTO = fieldCategoryMap.get(modelField.getFieldCategory());
                modelField.set_innerMap("fieldCategoryMeaning", valueDTO.getMeaning());
            }
            if (fieldTypeMap.containsKey(modelField.getFieldType())) {
                LovValueDTO valueDTO = fieldTypeMap.get(modelField.getFieldType());
                modelField.set_innerMap("fieldTypeMeaning", valueDTO.getMeaning());
            }
            if (modelField.getWdg() != null) {
                String widget = modelField.getWdg().getFieldWidget();
                if (fieldWidgetTypeMap.containsKey(widget)) {
                    LovValueDTO valueDTO = fieldWidgetTypeMap.get(widget);
                    modelField.set_innerMap("fieldWidgetMeaning", valueDTO.getMeaning());
                }
            }
        }
        return modelFields;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWidget(ModelField field) {
        ModelFieldWidget fieldWidget = field.getWdg();
        //删除组件
        if (fieldWidget == null || StringUtils.isEmpty(fieldWidget.getFieldWidget())) {
            fieldWidget.setFieldId(field.getFieldId());
            fieldWidgetRepository.delete(fieldWidget);
            return;
        }
        //更新组件
        if (fieldWidget.getId() == null) {
            fieldWidgetRepository.insertSelective(fieldWidget);
        } else if (fieldWidget.getId() != null) {
            fieldWidgetRepository.updateOptional(fieldWidget, ModelFieldWidget.UPDATE_FIELD_COMMON_LIST);
        }
    }
}
