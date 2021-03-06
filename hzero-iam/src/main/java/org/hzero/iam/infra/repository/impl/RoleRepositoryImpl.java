package org.hzero.iam.infra.repository.impl;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

import org.hzero.boot.platform.lov.annotation.ProcessLovValue;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.cache.ProcessCacheValue;
import org.hzero.core.util.AsyncTask;
import org.hzero.core.util.CommonExecutor;
import org.hzero.iam.api.dto.*;
import org.hzero.iam.domain.entity.Menu;
import org.hzero.iam.domain.entity.Role;
import org.hzero.iam.domain.entity.RolePermission;
import org.hzero.iam.domain.entity.User;
import org.hzero.iam.domain.repository.HiamProfileRepository;
import org.hzero.iam.domain.repository.MenuRepository;
import org.hzero.iam.domain.repository.RoleRepository;
import org.hzero.iam.domain.service.RootUserService;
import org.hzero.iam.domain.vo.ProfileVO;
import org.hzero.iam.domain.vo.RoleVO;
import org.hzero.iam.infra.common.utils.HiamMenuUtils;
import org.hzero.iam.infra.common.utils.HiamRoleUtils;
import org.hzero.iam.infra.common.utils.UserUtils;
import org.hzero.iam.infra.constant.Constants;
import org.hzero.iam.infra.constant.HiamMemberType;
import org.hzero.iam.infra.mapper.RoleMapper;
import org.hzero.iam.infra.mapper.RolePermissionMapper;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.mybatis.common.Criteria;
import org.hzero.mybatis.domian.Condition;
import org.hzero.mybatis.helper.SecurityTokenHelper;
import org.hzero.mybatis.util.Sqls;

/**
 * ?????????????????????
 *
 * @author jiangzhou.bo@hand-china.com 2018/06/20 11:32
 */
@Component
public class RoleRepositoryImpl extends BaseRepositoryImpl<Role> implements RoleRepository {
    public static final Logger LOGGER = LoggerFactory.getLogger(RoleRepositoryImpl.class);

    private static final RoleVO EMPTY_ROLE = new RoleVO();

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private HiamProfileRepository profileRepository;
    @Autowired
    @Qualifier("IamCommonAsyncTaskExecutor")
    private ThreadPoolExecutor executor;

    @ProcessLovValue
    @ProcessCacheValue
    @Override
    public Page<RoleVO> selectSelfManageableRoles(RoleVO params, PageRequest pageRequest) {
        CustomUserDetails self = UserUtils.getUserDetails();

        params = Optional.ofNullable(params).orElse(new RoleVO());
        params.setUserId(self.getUserId());
        params.setUserTenantId(self.getTenantId());
        params.setUserOrganizationId(self.getOrganizationId());

        RoleVO finalParams = params;
        Page<RoleVO> roleVos = PageHelper.doPage(pageRequest, () -> roleMapper.selectUserManageableRoles(finalParams));
        CustomUserDetails userDetails = UserUtils.getUserDetails();

        // ????????????????????????????????????
        setupInheritable(roleVos.getContent(), userDetails.getTenantId());

        return roleVos;
    }

    private void setupInheritable(List<RoleVO> roleVos, Long tenantId) {
        List<AsyncTask<Boolean>> tasks = roleVos.stream().map(roleVo -> (AsyncTask<Boolean>) () -> {
            List<ProfileVO> profileVos = profileRepository.queryProfileVO(tenantId,
                    Constants.Config.CONFIG_CODE_ROLE_DISABLE_INHERIT);
            roleVo.setInheritable(Constants.DisplayStatus.ENABLE);
            for (ProfileVO profileVo : profileVos) {
                // ??????path???null?????????????????????
                if (inheritDisabled(roleVo, profileVo) || createDisabled(roleVo, profileVo)) {
                    // ??????????????????????????????
                    roleVo.setInheritable(Constants.DisplayStatus.DISABLED);
                    break;
                }
            }
            return Boolean.TRUE;
        }).collect(Collectors.toList());

        CommonExecutor.batchExecuteAsync(tasks, executor, "SetupInheritRole");
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param roleVo    ??????
     * @param profileVo ????????????
     * @return true:??????
     */
    private boolean inheritDisabled(RoleVO roleVo, ProfileVO profileVo) {
        return profileVo.getInheritLevelPath() != null && roleVo.getInheritLevelPath() != null
                && roleVo.getInheritLevelPath().contains(profileVo.getInheritLevelPath());
    }

    /**
     * h_level_path????????????????????????????????????
     *
     * @param roleVO    ??????
     * @param profileVO ????????????
     * @return true:??????
     */
    private boolean createDisabled(RoleVO roleVO, ProfileVO profileVO) {
        return profileVO.getLevelPath() != null && roleVO.getLevelPath() != null
                && roleVO.getLevelPath().contains(profileVO.getLevelPath());
    }


    @Override
    public List<RoleVO> selectSelfAllManageableRoles(RoleVO params) {
        CustomUserDetails self = UserUtils.getUserDetails();

        params = Optional.ofNullable(params).orElse(new RoleVO());
        params.setUserId(self.getUserId());
        params.setUserTenantId(self.getTenantId());
        params.setUserOrganizationId(self.getOrganizationId());
        params.setSelectAssignedRoleFlag(true);

        SecurityTokenHelper.close();
        List<RoleVO> roles = roleMapper.selectUserManageableRoles(params);
        SecurityTokenHelper.clear();
        return roles;
    }

    @Override
    public List<RoleVO> selectUserManageableRoles(RoleVO params, User user) {
        params = Optional.ofNullable(params).orElse(new RoleVO());
        params.setUserId(user.getId());
        params.setUserTenantId(user.getOrganizationId());
        params.setUserOrganizationId(user.getOrganizationId());
        params.setSelectAssignedRoleFlag(true);

        SecurityTokenHelper.close();
        List<RoleVO> roles = roleMapper.selectUserManageableRoles(params);
        SecurityTokenHelper.clear();
        return roles;
    }

    @Override
    public Page<RoleVO> selectSelfAssignableRoles(RoleVO params, PageRequest pageRequest) {
        CustomUserDetails self = UserUtils.getUserDetails();

        params = Optional.ofNullable(params).orElse(new RoleVO());
        params.setUserId(self.getUserId());
        params.setUserTenantId(self.getTenantId());
        params.setUserOrganizationId(self.getOrganizationId());
        params.setEnabled(true);
        // ??????????????????????????????
        if (CollectionUtils.isNotEmpty(params.getExcludeUserIds())) {
            Long excludeUserId = params.getExcludeUserIds().get(0);
            params.setExcludeUserId(excludeUserId);
        }
        params.setSelectAssignedRoleFlag(true);

        RoleVO finalParams = params;
        return PageHelper.doPage(pageRequest, () -> roleMapper.selectUserManageableRoles(finalParams));
    }

    @Override
    @ProcessLovValue
    public RoleVO selectRoleDetails(Long roleId) {
        return roleMapper.selectRoleDetails(roleId);
    }

    @Override
    public List<RoleVO> selectUserAdminRoles(RoleVO params, PageRequest pageRequest) {
        return PageHelper.doPage(pageRequest, () -> selectUserAdminRoles(params));
    }

    @Override
    @ProcessLovValue
    public Page<RoleVO> selectSelfAssignedRoles(RoleVO params, PageRequest pageRequest) {
        CustomUserDetails self = UserUtils.getUserDetails();

        params = Optional.ofNullable(params).orElse(new RoleVO());
        params.setUserId(self.getUserId());
        params.setUserTenantId(self.getTenantId());
        params.setUserOrganizationId(self.getOrganizationId());

        RoleVO finalParams = params;
        return PageHelper.doPage(pageRequest, () -> roleMapper.selectUserAssignedRoles(finalParams));
    }

    @Override
    public List<RoleVO> selectUserAdminRoles(RoleVO params) {
        if (params.getUserId() == null) {
            CustomUserDetails self = UserUtils.getUserDetails();
            params.setUserId(self.getUserId());
        }

        return roleMapper.selectUserAdminRoles(params);
    }

    @Override
    public List<RoleVO> selectSelfCurrentTenantRoles(@Nullable Boolean notMerge) {
        CustomUserDetails self = UserUtils.getUserDetails();

        List<RoleVO> selfRoles = this.selectCurrentTenantMemberRoles(self, false);

        if (notMerge != null && notMerge) {
            return selfRoles;
        }

        List<RoleVO> returnRoles = new ArrayList<>(selfRoles.size());

        if (self.isRoleMergeFlag()) {
            Map<String, List<RoleVO>> map = selfRoles.stream().collect(Collectors.groupingBy(RoleVO::getLevel));
            // ????????????????????????????????? ????????????????????????????????????????????????????????????????????????????????????
            if (map.size() > 1) {
                map.forEach((level, roles) -> {
                    RoleVO role = roles.get(0);
                    role.setName(RoleVO.obtainRoleName(role.getLevel(), role.getName(), self.getLanguage()));
                    returnRoles.add(role);
                });
            }
        } else {
            returnRoles.addAll(selfRoles);
        }

        return returnRoles;
    }

    @Override
    public RoleVO selectCurrentRole() {
        CustomUserDetails details = UserUtils.getUserDetails();
        if (details.getRoleId() == null) {
            return EMPTY_ROLE;
        }

        List<RoleVO> roles = this.selectCurrentTenantMemberRoles(details, true);
        return roles.stream().filter(r -> Objects.equals(r.getId(), details.getRoleId())).findFirst().orElse(EMPTY_ROLE);
    }

    @Override
    @ProcessLovValue
    public Page<RoleVO> selectSimpleRolesWithTenant(RoleVO params, PageRequest pageRequest) {
        return PageHelper.doPage(pageRequest, () -> roleMapper.selectSimpleRoles(params));
    }

    @Override
    public List<Role> selectSimpleRolesWithTenant(RoleVO params) {
        return roleMapper.selectSimpleRoles(params);
    }

    @Override
    public List<Menu> selectRolePermissionSetTree(Long roleId, PermissionSetSearchDTO permissionSetParam) {
        Role role = selectRoleSimpleById(roleId);
        Assert.notNull(role, "role is invalid");

        // ?????????????????????ID
        permissionSetParam.setAllocateRoleId(roleId);

        // ????????????????????????????????????
        Long currentRoleId = role.getParentRoleId();

        // ???????????????
        List<Menu> menuList = menuRepository.selectRolePermissionSet(currentRoleId, roleId, permissionSetParam);

        // ??????????????????
        return HiamMenuUtils.formatMenuListToTree(menuList, Boolean.TRUE);
    }

    @Override
    public List<Role> selectParentRoles(Long roleId) {
        return roleMapper.selectAllParentRoles(roleId);
    }

    @Override
    public List<Role> selectAllSubRoles(Long roleId) {
        return roleMapper.selectAllSubRoles(roleId);
    }

    @Override
    public Role selectRoleSimpleById(Long roleId) {
        return selectOneOptional(new Role().setId(roleId),
                new Criteria().select(Role.FIELD_ID, Role.FIELD_CODE, Role.FIELD_NAME, Role.FIELD_LEVEL, Role.FIELD_PARENT_ROLE_ID,
                        Role.FIELD_IS_ENABLED, Role.FIELD_TENANT_ID, Role.FIELD_BUILD_IN)
                        .where(Role.FIELD_ID));
    }

    @Override
    public Role selectRoleSimpleByCode(String roleCode) {
        List<Role> roles = selectOptional(new Role().setCode(roleCode),
                new Criteria().select(Role.FIELD_ID, Role.FIELD_CODE, Role.FIELD_NAME, Role.FIELD_TENANT_ID, Role.FIELD_LEVEL)
                        .where(Role.FIELD_CODE));
        if (CollectionUtils.isEmpty(roles)) {
            return null;
        }
        if (roles.size() > 1) {
            throw new CommonException("hiam.warn.overOneRoleByCode");
        }
        return roles.get(0);
    }

    @Override
    public Role selectOneRoleByCode(String roleCode) {
        List<Role> list = select(Role.FIELD_CODE, roleCode);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        if (list.size() > 1) {
            throw new CommonException("hiam.warn.overOneRoleByCode");
        }
        return list.get(0);
    }

    @Override
    public Role selectRoleSimpleByLevelPath(String levelPath) {
        return selectOneOptional(new Role().setLevelPath(levelPath),
                new Criteria().select(Role.FIELD_ID, Role.FIELD_CODE, Role.FIELD_TENANT_ID, Role.FIELD_IS_ENABLED, Role.FIELD_LEVEL)
                        .where(Role.FIELD_LEVEL_PATH));
    }

    @Override
    public List<Role> selectBuiltInRoles(boolean includeSuperAdmin) {
        if (includeSuperAdmin) {
            return selectByCondition(Condition.builder(Role.class)
                    .andWhere(Sqls.custom().andEqualTo(Role.FIELD_BUILD_IN, BaseConstants.Flag.YES)).build());
        } else {
            return selectByCondition(Condition.builder(Role.class)
                    .andWhere(Sqls.custom().andEqualTo(Role.FIELD_BUILD_IN, BaseConstants.Flag.YES)
                            .andNotEqualTo(Role.FIELD_PARENT_ROLE_ID, Role.ROOT_ID))
                    .build());
        }
    }

    @Override
    @ProcessLovValue
    public Page<RoleVO> selectMemberRoles(Long memberId, HiamMemberType memberType, MemberRoleSearchDTO memberRoleSearchDTO, PageRequest pageRequest) {
        Page<RoleVO> page = PageHelper.doPage(pageRequest, () -> listMemberRoles(memberId, memberType, memberRoleSearchDTO));

        List<RoleVO> selfAllRoles = selectSelfAllManageableRoles(null);
        Set<Long> allRoleIds = selfAllRoles.stream().map(RoleVO::getId).collect(Collectors.toSet());

        for (RoleVO role : page.getContent()) {
            if (allRoleIds.contains(role.getId())) {
                role.setManageableFlag(BaseConstants.Flag.YES);
            }
        }

        return page;
    }

    @Override
    public List<RoleVO> listMemberRoles(Long memberId, HiamMemberType memberType, MemberRoleSearchDTO memberRoleSearchDTO) {
        RoleVO params = new RoleVO();
        params.setMemberId(memberId);
        params.setMemberType(memberType.value());
        params.setName(memberRoleSearchDTO.getRoleName());
        params.setTenantId(memberRoleSearchDTO.getTenantId());

        return roleMapper.selectMemberRoles(params);
    }

    @Override
    public List<Role> selectInheritSubRoleTreeWithPermissionSets(Long inheritRoleId, Set<Long> permissionSetIds, String type) {
        if (permissionSetIds.size() == 0 || inheritRoleId == null) {
            return Collections.emptyList();
        }

        // ?????????????????????????????????????????????????????????????????????????????????????????????

        // ?????????????????????????????????
        List<Role> inheritedRoles = roleMapper.selectAllInheritedRole(inheritRoleId);

        batchSelectRolePermission(inheritedRoles, permissionSetIds, type);

        return HiamRoleUtils.formatRoleListToTree(inheritedRoles, true);
    }

    @Override
    public List<Role> selectCreatedSubRoleTreeWithPermissionSets(Long parentRoleId, Set<Long> permissionSetIds, String type) {
        if (permissionSetIds.size() == 0 || parentRoleId == null) {
            return Collections.emptyList();
        }

        // ?????????????????????????????????????????????????????????????????????????????????????????????

        // ?????????????????????????????????
        List<Role> createdRoles = roleMapper.selectAllCreatedRole(parentRoleId);

        batchSelectRolePermission(createdRoles, permissionSetIds, type);

        return HiamRoleUtils.formatRoleListToTree(createdRoles, false);
    }

    private void batchSelectRolePermission(List<Role> roles, Set<Long> permissionSetIds, String type) {
        // ??????????????????3000????????????????????????????????? perMaxRole ?????????
        int perMaxRole = 5000 / permissionSetIds.size();

        Stream<RolePermission> rolePermissionStream;

        if (roles.size() <= perMaxRole) {
            List<RolePermission> resultList = rolePermissionMapper.selectRolePermissionSets(buildQueryParam(roles, permissionSetIds, type));
            rolePermissionStream = resultList.stream();
        } else {
            // ???????????????????????????
            List<List<Role>> partList = Lists.partition(roles, perMaxRole);
            // ????????????????????????
            List<AsyncTask<List<RolePermission>>> tasks = partList.stream()
                    .map(subRoleList -> (AsyncTask<List<RolePermission>>) () -> {
                        return rolePermissionMapper.selectRolePermissionSets(buildQueryParam(subRoleList, permissionSetIds, type));
                    })
                    .collect(Collectors.toList());
            // ????????????????????????
            List<List<RolePermission>> resultList = CommonExecutor.batchExecuteAsync(tasks, executor, "BatchSelectRolePermission");
            // ?????????
            rolePermissionStream = resultList.stream().flatMap(List::stream);
        }
        // ?????????????????????????????????
        Map<Long, Role> mapRole = roles.stream().collect(Collectors.toMap(Role::getId, Function.identity()));

        Map<Long, List<RolePermission>> mapRps = rolePermissionStream.parallel().collect(Collectors.groupingBy(RolePermission::getRoleId, Collectors.toList()));

        mapRole.forEach((id, role) -> {
            role.setPermissionSets(Optional.ofNullable(mapRps.get(id)).orElse(new ArrayList<>()));
        });
    }

    private RolePermission buildQueryParam(List<Role> roles, Set<Long> permissionSetIds, String type) {
        RolePermission params = new RolePermission();
        params.setPermissionSetIds(permissionSetIds);
        params.setRoleIds(roles.stream().map(Role::getId).collect(Collectors.toSet()));
        params.setType(type);
        return params;
    }

    @Override
    public RoleVO selectAdminRole(Long roleId) {
        CustomUserDetails self = UserUtils.getUserDetails();
        RoleVO params = new RoleVO();
        params.setId(roleId);
        params.setUserId(self.getUserId());
        params.setUserTenantId(self.getTenantId());
        params.setUserOrganizationId(self.getOrganizationId());
        return roleMapper.selectAdminRole(params);
    }

    @Override
    public List<RolePermission> selectRolePermissions(RolePermission params) {
        return rolePermissionMapper.selectRolePermissionSets(params);
    }

    @Override
    public List<Role> listTenantAdmin(Long tenantId) {
        return roleMapper.selectTenantAdmin(tenantId);
    }

    @ProcessLovValue
    @ProcessCacheValue
    @Override
    public Page<RoleDTO> selectUserManageableRoleTree(PageRequest pageRequest, RoleVO params) {
        CustomUserDetails self = UserUtils.getUserDetails();
        params = Optional.ofNullable(params).orElse(new RoleVO());
        params.setUserId(self.getUserId());
        params.setUserTenantId(self.getTenantId());
        params.setUserOrganizationId(self.getOrganizationId());

        // null ??????????????????list,????????????
        if (Objects.isNull(params.getParentRoleId())) {
            params.setQueryRootNodeFlag(1);
            params.setParentRoleId(null);
        }

        // ????????????
        RoleVO finalParams = params;
        Page<RoleVO> page = PageHelper.doPageAndSort(pageRequest,
                () -> roleMapper.selectUserManageableRoleTree(finalParams));

        // ?????????????????????,?????????????????????????????????????????????????????????for???????????????????????????????????????????????????
        List<RoleDTO> roleDTOList = page.getContent().stream().map(item -> {
            RoleDTO roleDTO = new RoleDTO();
            BeanUtils.copyProperties(item, roleDTO);
            if (roleDTO.getChildrenNum() != null && roleDTO.getChildrenNum() > 0) {
                // ???????????????
                roleDTO.addChildren(Collections.emptyList());
            }
            return roleDTO;
        }).collect(Collectors.toList());

        // ??????????????????
        Page<RoleDTO> roleDTOPage = new Page<>();
        BeanUtils.copyProperties(page, roleDTOPage);
        roleDTOPage.setContent(roleDTOList);
        return roleDTOPage;
    }

    @Override
    public List<Role> selectUserRole(Long tenantId, Long userId) {
        return roleMapper.selectUserRole(tenantId, userId);
    }

    @ProcessLovValue
    @ProcessCacheValue
    @Override
    public List<RoleVO> selectUserManageableRoleTree(RoleVO params) {
        return roleMapper.selectUserManageableRoleTree(params);
    }

    @Override
    public boolean checkPermission(RolePermissionCheckDTO rolePermissionCheckDTO) {
        Assert.notNull(rolePermissionCheckDTO, "parameter cannot be null.");
        rolePermissionCheckDTO.validate();
        return rolePermissionMapper.checkPermission(rolePermissionCheckDTO) > 0;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param roleId ??????ID
     * @return ????????????
     */
    @Override
    public List<Role> selectAllSubRolesIncludeSelf(Long roleId) {

        List<Role> roles = roleMapper.selectAllSubRolesIncludeSelf(roleId);
        return HiamRoleUtils.formatRoleListToTree(roles, false);
    }

    @Override
    public Page<RoleVO> selectSecGrpAssignableRole(Long secGrpId, Long roleId, RoleSecGrpDTO queryDTO, PageRequest pageRequest) {
        // ????????????????????????????????????
        return PageHelper.doPage(pageRequest, () -> this.roleMapper.selectSecGrpAssignableRole(secGrpId, roleId, queryDTO));
    }

    @Override
    public Page<RoleVO> selectByRoleIds(List<Long> roleIds, PageRequest pageRequest) {
        return PageHelper.doPage(pageRequest, () -> roleMapper.selectByRoleIds(roleIds));
    }

    @Override
    public List<Role> selectBuiltInTemplateRole(String roleLabel) {
        return roleMapper.selectBuiltInTemplateRole(roleLabel);
    }

    @Override
    public Map<String, String> selectTplRoleNameById(Long roleId) {
        List<Map<String, String>> langs = roleMapper.selectTplRoleNameById(roleId);
        return langs.stream()
                .filter(m -> StringUtils.isNoneBlank(m.get("lang"), m.get("name")))
                .collect(Collectors.toMap(m -> m.get("lang"), m -> m.get("name")));
    }

    @Override
    public Long countSubRole(Long parentRoleId, Long roleId) {
        return roleMapper.countSubRole(parentRoleId, roleId);
    }

    @Override
    public void batchUpdateEnableFlag(Long roleId, Integer enableFlag, boolean updateSubRole) {
        roleMapper.batchUpdateEnableFlagBySql(roleId, enableFlag, updateSubRole);
    }

    @Override
    public List<RoleVO> selectSubAssignedRoles(Long roleId, Long userId) {
        return roleMapper.selectSubAssignedRoles(roleId, userId);
    }

    @Override
    public List<Role> selectRoleByLabel(Long tenantId, @Nonnull Set<String> roleLabels, String assignType) {
        if (CollectionUtils.isEmpty(roleLabels)) {
            return Collections.emptyList();
        }
        return roleMapper.selectRoleByLabel(tenantId, roleLabels, assignType);
    }

    @Override
    public boolean isTopAdminRole(Long userId, Long roleId) {
        List<Long> topRoleIds = roleMapper.queryTopAdminRoleId(userId, roleId);
        return topRoleIds != null && topRoleIds.contains(roleId);
    }

    private List<RoleVO> selectCurrentTenantMemberRoles(CustomUserDetails self, boolean onlyCurrentRole) {
        RoleVO params = new RoleVO();
        params.setMemberId(self.getUserId());
        params.setMemberType(HiamMemberType.USER.value());
        params.setTenantId(self.getTenantId());
        params.setCheckMemberRoleExpire(true);
        params.setQueryAdminFlag(true);

        if (onlyCurrentRole) {
            params.setId(self.getRoleId());
        }

        List<RoleVO> roles = roleMapper.selectMemberRoles(params);

        if (RootUserService.isRootUser()) {
            List<RoleVO> rootRoles = roleMapper.selectRootMemberRoles(params);
            Set<Long> ids = roles.stream().map(RoleVO::getId).collect(Collectors.toSet());
            rootRoles = rootRoles.stream().filter(r -> !ids.contains(r.getId())).collect(Collectors.toList());
            roles.addAll(rootRoles);
        }
        return roles;
    }
}
