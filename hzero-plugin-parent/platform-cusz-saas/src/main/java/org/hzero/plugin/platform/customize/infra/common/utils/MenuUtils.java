package org.hzero.plugin.platform.customize.infra.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hzero.plugin.platform.customize.api.dto.MenuDTO;

/**
 * @author : peng.yu01@hand-china.com 2019/12/18 14:04
 */
public class MenuUtils {

    public static List<MenuDTO> formatMenusToTree(List<MenuDTO> menuList) {
        if (CollectionUtils.isEmpty(menuList)) {
            return Collections.emptyList();
        } else {
            menuList.forEach((item) -> {
                formatMenuTreeInternal(item, menuList);
            });
            List<MenuDTO> rootMenuList = menuList.stream().filter((item) -> {
                return item.getParentMenu() == null && (item.getParentId() == null ||
                        item.getParentId() == 0L) && ("root".equals(item.getMenuType()) ||
                        "dir".equals(item.getMenuType())) && isDisplay(item);
            }).collect(Collectors.toList());
            return sortMenu(rootMenuList);
        }
    }

    public static boolean isDisplay(MenuDTO menu) {
        if (!"menu".equals(menu.getMenuType()) && !"link".equals(menu.getMenuType())) {
            List<MenuDTO> subMenuList = menu.getSubMenus();
            if (CollectionUtils.isEmpty(subMenuList)) {
                return false;
            } else {
                List<MenuDTO> displaySubMenuList = new ArrayList();
                subMenuList.forEach((item) -> {
                    boolean isDisplay = isDisplay(item);
                    if (isDisplay) {
                        displaySubMenuList.add(item);
                    }
                });
                if (CollectionUtils.isEmpty(displaySubMenuList)) {
                    return false;
                } else {
                    menu.setSubMenus(displaySubMenuList);
                    return true;
                }
            }
        } else {
            return true;
        }
    }

    private static void formatMenuTreeInternal(MenuDTO parentMenu, List<MenuDTO> menuList) {
        List<MenuDTO> childMenuList = menuList.stream().filter((item) -> {
            return parentMenu.getMenuId().equals(item.getParentId());
        }).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(childMenuList)) {
            parentMenu.setSubMenus(sortMenu(childMenuList));
            childMenuList.forEach((item) -> {
                item.setParentMenu(parentMenu);
                item.setParentName(parentMenu.getMenuName());
                formatMenuTreeInternal(item, menuList);
            });
        }

    }

    public static List<MenuDTO> sortMenu(List<MenuDTO> menuList) {
        menuList.sort((m1, m2) -> {
            if (m1.getMenuSort() == null) {
                return -1;
            } else if (m2.getMenuSort() != null && m1.getMenuSort() <= m2.getMenuSort()) {
                return m1.getMenuSort().equals(m2.getMenuSort()) ? 0 : -1;
            } else {
                return 1;
            }
        });
        return menuList;
    }

}
