package org.hzero.plugin.platform.customize.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author : peng.yu01@hand-china.com 2019/12/17 19:32
 */
public class MenuDTO {
    private Long menuId;
    private String menuCode;
    private String menuName;
    private String quickIndex;
    private String level;
    private Long parentId;
    private String menuType;
    private Integer menuSort;
    private List<MenuDTO> subMenus;
    @JsonIgnore
    private MenuDTO parentMenu;
    private String parentName;

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public String getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getQuickIndex() {
        return quickIndex;
    }

    public void setQuickIndex(String quickIndex) {
        this.quickIndex = quickIndex;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public Integer getMenuSort() {
        return menuSort;
    }

    public void setMenuSort(Integer menuSort) {
        this.menuSort = menuSort;
    }

    public List<MenuDTO> getSubMenus() {
        return subMenus;
    }

    public void setSubMenus(List<MenuDTO> subMenus) {
        this.subMenus = subMenus;
    }

    public MenuDTO getParentMenu() {
        return parentMenu;
    }

    public void setParentMenu(MenuDTO parentMenu) {
        this.parentMenu = parentMenu;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }
}
