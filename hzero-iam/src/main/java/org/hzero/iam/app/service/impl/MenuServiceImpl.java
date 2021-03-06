package org.hzero.iam.app.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.Charsets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.redis.RedisHelper;
import org.hzero.iam.api.dto.MenuCopyDataDTO;
import org.hzero.iam.api.dto.MenuSearchDTO;
import org.hzero.iam.api.dto.MenuSiteExportDTO;
import org.hzero.iam.app.service.MenuService;
import org.hzero.iam.domain.entity.Menu;
import org.hzero.iam.domain.entity.MenuPermission;
import org.hzero.iam.domain.repository.MenuPermissionRepository;
import org.hzero.iam.domain.repository.MenuRepository;
import org.hzero.iam.domain.service.MenuCoreService;
import org.hzero.iam.infra.common.utils.HiamMenuUtils;
import org.hzero.iam.infra.constant.Constants;
import org.hzero.iam.infra.constant.HiamMenuType;
import org.hzero.iam.infra.constant.HiamResourceLevel;
import org.hzero.iam.infra.constant.PermissionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;

/**
 * @author bojiangzhou 2019/01/18
 * @author allen 2018/7/2
 */
@Service
public class MenuServiceImpl implements MenuService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuServiceImpl.class);

    @Autowired
    private MenuCoreService menuCoreService;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private MenuPermissionRepository menuPermissionRepository;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Menu createMenuInSite(Menu menu) {
        menu.setTenantId(Constants.SITE_TENANT_ID);
        // ?????????????????????????????????
        menu.setCustomFlag(BaseConstants.Flag.NO);
        return menuCoreService.createMenu(menu, true);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Menu createMenuInTenant(Long tenantId, Menu menu) {
        Assert.notNull(tenantId, "tenantId should not be null.");
        menu.setTenantId(tenantId);
        // ?????????????????????????????????
        menu.setLevel(HiamResourceLevel.ORGANIZATION.value());
        // ??????????????????????????????????????????
        menu.setCustomFlag(BaseConstants.Flag.YES);

        return menuCoreService.createMenu(menu, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertMenuForCopy(HiamResourceLevel queryLevel, HiamResourceLevel level, MenuCopyDataDTO menuCopyData) {
        Long targetTenantId = menuCopyData.getTargetTenantId();
        Long sourceTenantId = menuCopyData.getSourceTenantId();

        List<Long> copyMenuIds = menuCopyData.getCopyMenuIds();
        if (CollectionUtils.isEmpty(copyMenuIds)) {
            return;
        }
        //????????????????????????
        List<Menu> copyData = menuRepository.selectMenuDetail(sourceTenantId, queryLevel, level, copyMenuIds);
        if (CollectionUtils.isEmpty(copyData)) {
            return;
        }

        Menu newRootMenu = menuCopyData.getRootMenu();
        Assert.notNull(newRootMenu, BaseConstants.ErrorCode.DATA_INVALID);
        // ????????????????????????
        newRootMenu.setTenantId(targetTenantId);
        //???????????????
        List<Menu> sourceMenuTreeList = HiamMenuUtils.formatMenuListToTree(copyData, true);
        //?????????????????????????????????????????????????????????
        Assert.isTrue(sourceMenuTreeList.size() == 1, BaseConstants.ErrorCode.DATA_INVALID);
        Menu sourceRootMenu = sourceMenuTreeList.get(0);
        //????????????????????????????????????????????????menuCode??????????????????
        String oldRootMenuCode = sourceRootMenu.getCode();
        //???????????????menuCode?????????????????????
        copyData.forEach(item -> {
            if (item.getCode().startsWith(oldRootMenuCode)) {
                item.setCode(item.getCode().replace(oldRootMenuCode, newRootMenu.getCode()));
            } else {
                //??????????????????????????????????????????????????????code????????????????????????????????????????????????code???????????????????????????????????????
                //??????????????????????????????
                item.setCode(newRootMenu.getCode() + item.getCode().substring(item.getCode().lastIndexOf(Menu.MENU_CODE_SPLIT)));
            }

            item.setTenantId(targetTenantId);
        });

        //????????????????????????????????????????????????????????????????????????????????????????????????
        List<Menu> newMenuTreeList = new ArrayList<>(1);
        newRootMenu.setSubMenus(sourceRootMenu.getSubMenus());
        newRootMenu.setLevel(sourceRootMenu.getLevel());
        newRootMenu.setCustomFlag(sourceRootMenu.getCustomFlag());
        newMenuTreeList.add(newRootMenu);

        List<MenuPermission> menuPermissionList = new ArrayList<>(1024);
        Long userId = Optional.ofNullable(DetailsHelper.getUserDetails()).map(CustomUserDetails::getUserId).orElse(-1L);

        //????????????
        recursiveInsertMenuTree(newMenuTreeList, menuPermissionList, userId);

        LOGGER.debug("Copy menu_permission, size: {}", menuPermissionList.size());
        // ??????????????????
        menuPermissionRepository.batchInsertBySql(menuPermissionList);
    }

    @Override
    public void insertCustomMenu(Long tenantId, Menu menu) {
        Assert.notNull(tenantId, "tenantId should not be null.");
        menu.setTenantId(tenantId);
        // ?????????????????????????????????
        menu.setLevel(HiamResourceLevel.ORGANIZATION.value());
        // ??????????????????????????????????????????
        menu.setCustomFlag(BaseConstants.Flag.YES);
        menuCoreService.createMenu(menu, true);
    }

    @Override
    public List<MenuSiteExportDTO> exportSiteMenuData(MenuSearchDTO menuSearchDTO) {
        return menuRepository.exportSiteMenuData(menuSearchDTO);
    }

    /**
     * ?????????????????????
     *
     * @param menuTreeList ?????????
     */
    private void recursiveInsertMenuTree(List<Menu> menuTreeList, List<MenuPermission> menuPermissionList, Long userId) {
        for (Menu menuTree : menuTreeList) {
            //????????????
            menuTree.setId(null);
            List<Menu> subMenus = menuTree.getSubMenus();
            menuTree.setSubMenus(null);
            menuTree.setParentMenu(null);
            menuCoreService.createMenu(menuTree, false);
            //????????????
            List<MenuPermission> menuPermissions = menuTree.getMenuPermissions();
            if (CollectionUtils.isNotEmpty(menuPermissions)) {
                menuPermissions.forEach(item -> {
                    item.setMenuId(menuTree.getId());
                    item.setTenantId(menuTree.getTenantId());
                    item.setCreatedBy(userId);
                    item.setLastUpdatedBy(userId);
                });
                menuPermissionList.addAll(menuPermissions);
            }
            if (CollectionUtils.isNotEmpty(subMenus)) {
                //?????????????????????
                subMenus.forEach(item -> {
                    item.setParentId(menuTree.getId());
                });
                recursiveInsertMenuTree(subMenus, menuPermissionList, userId);
            }
        }
    }


    @Override
    public void checkDuplicate(Menu menu) {
        menu.initMenu();
        menuCoreService.checkMenuExists(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Menu update(Menu menu) {
        return menuCoreService.updateMenu(menu);
    }

    @Override
    public Menu updateCustomMenu(Long tenantId, Menu menu) {
        Assert.notNull(menu.getId(), "menuId must not be null when update menu.");
        Menu db = menuRepository.selectByPrimaryKey(menu.getId());
        Assert.notNull(db, "menuId must not be null when update menu.");
        if (Objects.equals(db.getCustomFlag(), BaseConstants.Flag.NO) || !Objects.equals(tenantId, db.getTenantId())) {
            throw new CommonException("error.menu.update");
        }
        return menuCoreService.updateMenu(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(Long tenantId, Long menuId) {
        menuCoreService.deleteMenuById(tenantId, menuId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void enableMenu(Long tenantId, Long menuId) {
        menuCoreService.changeEnableFlag(tenantId, menuId, BaseConstants.Flag.YES);
    }

    @Override
    public void enableCustomMenu(Long tenantId, Long menuId) {
        Menu db = menuRepository.selectByPrimaryKey(menuId);
        Assert.notNull(db, "menuId must not be null when update menu.");
        if (Objects.equals(db.getCustomFlag(), BaseConstants.Flag.NO) || !Objects.equals(tenantId, db.getTenantId())) {
            throw new CommonException("error.menu.update");
        }
        menuCoreService.changeEnableFlag(tenantId, menuId, BaseConstants.Flag.YES);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void disableMenu(Long tenantId, Long menuId) {
        menuCoreService.changeEnableFlag(tenantId, menuId, BaseConstants.Flag.NO);
    }

    @Override
    public void disableCustomMenu(Long tenantId, Long menuId) {
        Menu db = menuRepository.selectByPrimaryKey(menuId);
        Assert.notNull(db, "menuId must not be null when update menu.");
        if (Objects.equals(db.getCustomFlag(), BaseConstants.Flag.NO) || !Objects.equals(tenantId, db.getTenantId())) {
            throw new CommonException("error.menu.update");
        }
        menuCoreService.changeEnableFlag(tenantId, menuId, BaseConstants.Flag.NO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void assignPsPermissions(Long permissionSetId, PermissionType permissionType, String[] permissionCodes) {
        menuCoreService.assignPsPermissions(permissionSetId, permissionType, permissionCodes);
    }

    @Override
    public void assignPsPermissions(Long tenantId, String code, String level, PermissionType permissionType, String[] permissionCodes) {
        Menu ps = selectOne(tenantId, code, level);
        this.assignPsPermissions(ps.getId(), permissionType, permissionCodes);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void recyclePsPermissions(Long permissionSetId, String[] permissionCodes, PermissionType permissionType) {
        menuCoreService.recyclePsPermissions(permissionSetId, permissionCodes, permissionType);
    }

    @Override
    public void recyclePsPermissions(Long tenantId, String code, String level, String[] permissionCodes, PermissionType permissionType) {
        Menu ps = selectOne(tenantId, code, level);
        this.recyclePsPermissions(ps.getId(), permissionCodes, permissionType);
    }

    private Menu selectOne(Long tenantId, String code, String level) {
        Menu params = new Menu();
        params.setTenantId(tenantId);
        params.setCode(code);
        params.setLevel(level);
        // ???????????????????????????
        Menu menu = menuRepository.selectOne(params);
        if (null == menu) {
            throw new CommonException("hiam.warn.menu.notFoundByCode");
        }
        return menu;
    }

    @Override
    public Map<String, Object> fixMenuData(boolean initAll) {
        Map<String, Object> result = new HashMap<>();

        int updateMenuLevelPathCount = 0;
        for (HiamMenuType menuType : HiamMenuType.values()) {
            updateMenuLevelPathCount += menuCoreService.initLevelPath(menuType, initAll);
        }

        result.put("updateMenu[levelPath]Count", updateMenuLevelPathCount);
        return result;
    }

    /**
     * @param menuTreeList ??????????????????????????????
     * @param response     HttpServletResponse??????
     * @throws IOException
     */
    @Override
    public void handleCustomMenuExportData(Long tenantId, List<Menu> menuTreeList, HttpServletResponse response) throws IOException {
        if (CollectionUtils.isEmpty(menuTreeList)) {
            return;
        }
        menuTreeList.forEach(item -> {
            //????????????????????????????????????????????????????????????????????????
            Assert.isTrue(Objects.equals(tenantId, item.getTenantId()), BaseConstants.ErrorCode.DATA_INVALID);
        });

        response.setStatus(HttpStatus.OK.value());
        String fileName = new StringBuilder(Menu.MENU_EXPORT_FILE_PREFIX).append(System.currentTimeMillis()).append(Menu.MENU_EXPORT_FILE_TYPE).toString();

        response.setHeader("content-type", "application/octet-stream");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes(Charsets.UTF_8.displayName()), Charsets.UTF_8.displayName()));


        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(redisHelper.toJson(menuTreeList).getBytes(Charsets.UTF_8.displayName()));
        outputStream.close();
    }

    @Override
    public List<Menu> handleCustomMenuImportData(Long tenantId, MultipartFile customMenuFile) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        InputStream inputStream = customMenuFile.getInputStream();
        BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8.displayName()));
        try {
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } finally {
            bf.close();
        }

        ObjectMapper objectMapper = RedisHelper.getObjectMapper();
        List<Menu> menuList = objectMapper.readValue(stringBuilder.toString(), new TypeReference<List<Menu>>() {
        });
        if (CollectionUtils.isEmpty(menuList)) {
            return menuList;
        }
        //??????????????????????????????????????????????????????????????????
        menuList.forEach(item -> item.setSubMenus(null));
        List<Menu> menuTreeList = HiamMenuUtils.formatMenuListToTree(menuList, true);

        //???????????????
        for (Menu menuTree : menuTreeList) {
            //?????????????????????0??????????????????????????????????????????ID?????????????????????
            if (!Menu.ROOT_ID.equals(menuTree.getParentId())) {
                String parentCode = menuTree.getParentCode();
                if (StringUtils.isEmpty(parentCode)) {
                    throw new CommonException("error.parentMenuCode.null", menuTree.getCode());
                }
                Menu queryParam = new Menu();
                queryParam.setCode(parentCode);
                queryParam.setTenantId(menuTree.getParentTenantId());
                //????????????????????????????????????????????????
                queryParam.setLevel(HiamResourceLevel.ORGANIZATION.value());
                Menu parentMenu = menuRepository.selectOne(queryParam);
                if (parentMenu == null) {
                    throw new CommonException("error.parentMenu.not.exist", menuTree.getCode());
                }
                menuTree.setParentId(parentMenu.getParentId());
            }
        }

        return menuTreeList;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param menuTreeList ??????????????????????????????
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importMenuTree(Long tenantId, List<Menu> menuTreeList) {

        for (Menu menuTree : menuTreeList) {
            //???????????????????????????
            Menu menu = menuRepository.selectMenuUnique(tenantId, menuTree.getCode(), HiamResourceLevel.ORGANIZATION.value());
            if (menu == null) {
                //??????
                menuTree.setTenantId(tenantId);
                menuTree.setId(null);
                // ?????????????????????????????????
                menuTree.setLevel(HiamResourceLevel.ORGANIZATION.value());
                // ??????????????????????????????????????????
                menuTree.setCustomFlag(BaseConstants.Flag.YES);
                menuCoreService.createMenu(menuTree, false);


            } else {
                if (BaseConstants.Flag.NO.equals(menu.getCustomFlag())) {
                    //???????????????????????????????????????
                    throw new CommonException("error.menu.illegal-data", menuTree.getCode());

                }
                //????????????????????????
                menu.setName(menuTree.getName());
                menu.setQuickIndex(menuTree.getQuickIndex());
                menu.setRoute(menuTree.getRoute());
                menu.setSort(menuTree.getSort());
                menu.setIcon(menuTree.getIcon());
                menu.setDescription(menuTree.getDescription());
                menu.setVirtualFlag(menuTree.getVirtualFlag());
                menu.setEnabledFlag(menuTree.getEnabledFlag());
                menuCoreService.updateMenu(menu);

                //???????????????,????????????????????????
                menuPermissionRepository.deleteByMenuId(menu.getId());

            }
            //????????????
            List<MenuPermission> menuPermissions = menuTree.getMenuPermissions();
            if (CollectionUtils.isNotEmpty(menuPermissions)) {
                menuPermissions.forEach(item -> {
                    item.setMenuId(menuTree.getId());
                    item.setTenantId(menuTree.getTenantId());
                });
                menuPermissionRepository.batchInsert(menuPermissions);
            }

            List<Menu> subMenus = menuTree.getSubMenus();
            if (CollectionUtils.isNotEmpty(subMenus)) {
                //?????????????????????
                subMenus.forEach(item -> item.setParentId(menuTree.getId()));
                importMenuTree(tenantId, subMenus);
            }
        }
    }

}
