package org.hzero.report.infra.meta.form;

import java.util.List;

/**
 * 复选框列表
 *
 * @author xianzhi.chen@hand-china.com 2018年11月30日下午1:20:55
 */
public class CheckBoxList extends FormElement {
    private List<CheckBox> value;

    public CheckBoxList(String name, String meaning, List<CheckBox> value) {
        super(name, meaning);
        this.type = "checkboxlist";
        this.value = value;
    }

    public List<CheckBox> getValue() {
        return this.value;
    }
}
