package org.hzero.plugin.platform.customize.infra.constant;

/**
 * @author : peng.yu01@hand-china.com 2019/12/16 19:44
 */
public interface CustomizeConstants {

    String NILL = "NILL";

    /**
     * 模型字段类别
     */
    interface FieldCategory {
        //实体字段
        String ENTITY_FIELD = "TABLE_FIELD";
        //虚拟字段
        String VIRTUAL_FIELD = "VIRTUAL_FIELD";
    }


    interface CustType {
        /**
         * 标准字段
         */
        String STD_TYPE = "STD";
        /**
         * 扩展字段
         */
        String EXT_TYPE = "EXT";
    }

    interface ModelRelationType {
        // 一对一
        String ONE_TO_ONE = "ONE_TO_ONE";
        // 一对多
        String ONE_TO_MANY = "ONE_TO_MANY";
        // 多对一
        String MANY_TO_ONE = "MANY_TO_ONE";
    }

    interface LovCode {
        //字段数据类型lov
        String FIELD_TYPE_LOV_CODE = "HPFM.CUST.FIELD_TYPE";
        //字段类型lov
        String FIELD_CATEGORY_LOV_CODE = "HPFM.CUST.FIELD_CATEGORY";
        //模型关系lov
        String MODEL_RELATION_LOV_CODE = "HPFM.CUST.MODEL_RELATION";
        //组件类型lov编码
        String FIELD_WIDGET_LOV_CODE = "HPFM.CUST.FIELD_COMPONENT";
        //单元类型lov
        String UNIT_TYPE_LOV_CODE = "HPFM.CUST.UNIT_TYPE";
    }

    interface FormType {
        String FORM = "FORM";
        String GRID = "GRID";
        String FILTER = "FILTER";
        String TABPANE = "TABPANE";
        String QUERY_FORM = "QUERYFORM";
    }

    /**
     * 条件类型
     */
    interface ConditionType {
        //必输
        String REQUIRED = "required";
        //编辑
        String EDITABLE = "editable";
        //显示
        String VISIBLE = "visible";
        //校验
        String VALID = "valid";
    }

    interface RenderOptions {
        String WIDGET = "WIDGET";
        String TEXT = "TEXT";
    }

    /**
     * 加密key
     */
    interface EncryptKey {
        String ENCRYPT_KEY_MODEL = "hmde_model_object_pub";
        String ENCRYPT_KEY_MODEL_FIELD = "hmde_model_field_pub";
        String ENCRYPT_KEY_MODEL_FIELD_WDG = "hpfm_cusz_model_field_wdg";
        String ENCRYPT_KEY_MODEL_REL_HEADER = "hmde_model_relation_pub";
        String ENCRYPT_KEY_MODEL_REL_LINE = "hmde_mod_rel_field_pub";
        String ENCRYPT_KEY_UNIT = "hpfm_cusz_unit";
        String ENCRYPT_KEY_UNIT_FIELD = "hpfm_cusz_unit_field";
        String ENCRYPT_KEY_UNIT_FIELD_PAR = "hpfm_cusz_unit_field_par";
        String ENCRYPT_KEY_UNIT_GROUP = "hpfm_cusz_unit_group";
        String ENCRYPT_KEY_CONFIG = "hpfm_cusz_config";
        String ENCRYPT_KEY_CONFIG_FIELD = "hpfm_cusz_config_field";
        String ENCRYPT_KEY_CONFIG_FIELD_MAP = "hpfm_cusz_config_field_map";
        String ENCRYPT_KEY_CONFIG_FIELD_PAR = "hpfm_cusz_config_field_par";
        String ENCRYPT_KEY_CONFIG_FIELD_WDG = "hpfm_cusz_config_field_wdg";
        String ENCRYPT_KEY_FIELD_COND_HEADER = "hpfm_cusz_field_con_header";
        String ENCRYPT_KEY_FIELD_COND_LINE = "hpfm_cusz_field_con_line";
        String ENCRYPT_KEY_FIELD_COND_VALID = "hpfm_cusz_field_con_valid";
    }


}
