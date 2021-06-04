package org.hzero.report.infra.meta.form;

/**
 * 复选框
 *
 * @author xianzhi.chen@hand-china.com 2018年11月30日下午1:21:12
 */
public class CheckBox extends FormElement {

    private String value;
    private boolean checked;

    public CheckBox(String name, String meaning, String value) {
        super(name, meaning);
        this.type = "Checkbox";
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public boolean isChecked() {
        return this.checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
