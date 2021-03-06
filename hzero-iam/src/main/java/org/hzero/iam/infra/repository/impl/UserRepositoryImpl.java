package org.hzero.iam.infra.repository.impl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.internal.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

import org.hzero.boot.oauth.domain.repository.BaseUserRepository;
import org.hzero.boot.platform.lov.annotation.ProcessLovValue;
import org.hzero.common.HZeroCacheKey;
import org.hzero.common.HZeroConstant;
import org.hzero.common.HZeroService;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.redis.RedisHelper;
import org.hzero.core.redis.safe.SafeRedisHelper;
import org.hzero.core.user.UserType;
import org.hzero.core.util.AsyncTask;
import org.hzero.core.util.CommonExecutor;
import org.hzero.export.vo.ExportParam;
import org.hzero.iam.api.dto.MemberRoleSearchDTO;
import org.hzero.iam.api.dto.UserEmployeeAssignDTO;
import org.hzero.iam.api.dto.UserExportDTO;
import org.hzero.iam.domain.entity.Tenant;
import org.hzero.iam.domain.entity.User;
import org.hzero.iam.domain.entity.UserConfig;
import org.hzero.iam.domain.entity.UserInfo;
import org.hzero.iam.domain.repository.UserRepository;
import org.hzero.iam.domain.vo.CompanyVO;
import org.hzero.iam.domain.vo.RoleVO;
import org.hzero.iam.domain.vo.UserCacheVO;
import org.hzero.iam.domain.vo.UserVO;
import org.hzero.iam.infra.common.utils.UserUtils;
import org.hzero.iam.infra.constant.Constants;
import org.hzero.iam.infra.mapper.UserConfigMapper;
import org.hzero.iam.infra.mapper.UserInfoMapper;
import org.hzero.iam.infra.mapper.UserMapper;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.hzero.mybatis.common.Criteria;
import org.hzero.mybatis.domian.Condition;
import org.hzero.mybatis.helper.SecurityTokenHelper;
import org.hzero.mybatis.util.Sqls;

/**
 * @author bojiangzhou ????????????
 * @author allen 2018/6/26
 */
@Repository
public class UserRepositoryImpl extends BaseRepositoryImpl<User> implements UserRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRepositoryImpl.class);


    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserConfigMapper userConfigMapper;
    @Autowired
    private RedisHelper redisHelper;
    @Autowired
    private BaseUserRepository baseUserRepository;
    @Autowired
    @Qualifier("IamCommonAsyncTaskExecutor")
    private ThreadPoolExecutor commonExecutor;

    @Autowired(required = false)
    private List<UserSelfWrapper> userSelfWrappers;

    /**
     * ???????????????????????????????????????
     */
    @Value("${hzero.recent-access-tenant.max-count:20}")
    private int maxRecentAccessTenantCount;
    /**
     * ???????????????????????????
     */
    @Value("${hzero.recent-access-tenant.days:7}")
    private int daysRecentAccessTenant;

    @Override
    @ProcessLovValue
    public Page<UserVO> selectSimpleUsers(UserVO params, PageRequest pageRequest) {
        return PageHelper.doPageAndSort(pageRequest, () -> userMapper.selectSimpleUsers(params));
    }

    @Override
    @ProcessLovValue
    public Page<UserVO> selectAllocateUsers(UserVO params, PageRequest pageRequest) {
        CustomUserDetails self = UserUtils.getUserDetails();
        params.setTenantId(self.getTenantId());
        // ??????????????? CustomUserDetails ?????? roleId ??????
        params.setAllocateRoleId(params.getRoleId());
        params.setRoleId(null);
        return PageHelper.doPageAndSort(pageRequest, () -> userMapper.selectAllocateUsers(params));
    }

    @Override
    @ProcessLovValue
    public UserVO selectUserDetails(UserVO params) {
        Assert.notNull(params.getId(), "selectUserDetails: user id must not be null.");
        List<UserVO> users = userMapper.selectUserDetails(params);
        return CollectionUtils.isNotEmpty(users) ? users.get(0) : null;
    }

    @Override
    public UserVO selectSelf() {
        CustomUserDetails self = UserUtils.getUserDetails();

        UserVO userVO = selectSelfUser(self);

        // ??????????????????????????????
        boolean changePasswordFlag = false;
        Integer passwordUpdateRate = Optional.ofNullable(userVO.getPasswordUpdateRate()).orElse(0);
        Integer passwordReminderPeriod = Optional.ofNullable(userVO.getPasswordReminderPeriod()).orElse(0);
        if (passwordUpdateRate > 0) {
            // ??????????????????
            Date date = DateUtils.addDays(userVO.getLastPasswordUpdatedAt(), passwordUpdateRate - passwordReminderPeriod);
            changePasswordFlag = new Date().after(date);
        }
        // ?????????????????????????????????????????????
        userVO.setChangePasswordFlag(changePasswordFlag ? BaseConstants.Flag.YES : BaseConstants.Flag.NO);

        // ???????????????????????????
        userVO.setTenantId(self.getTenantId());
        // ???????????????????????????
        userVO.setCurrentRoleId(self.getRoleId());
        // ????????????
        userVO.setRoleMergeFlag(self.isRoleMergeFlag() ? BaseConstants.Flag.YES : BaseConstants.Flag.NO);
        // ??????????????????
        if (self.isRoleMergeFlag()) {
            userVO.setCurrentRoleName(RoleVO.obtainRoleName(userVO.getCurrentRoleLevel(), userVO.getCurrentRoleName(), self.getLanguage()));
        }

        try {
            redisHelper.setCurrentDatabase(HZeroService.Platform.REDIS_DB);

            // ?????? title/logo
            setupTitleLogo(userVO, self);

            // ????????????????????????????????????????????????
            setupMenuLayout(userVO, self);

            setupLayoutTheme(userVO, self);

            setupFavicon(userVO, self);
            userVO.setDataHierarchyFlag(redisHelper.hasKey(HZeroService.Platform.CODE + ":data-hierarchy:" + userVO.getTenantId())
                    ? BaseConstants.Flag.YES : BaseConstants.Flag.NO);
        } finally {
            redisHelper.clearCurrentDatabase();
        }

        if (CollectionUtils.isNotEmpty(userSelfWrappers)) {
            for (UserSelfWrapper userSelfWrapper : userSelfWrappers) {
                userSelfWrapper.wrap(userVO);
            }
        }

        return userVO;
    }

    private UserVO selectSelfUser(CustomUserDetails self) {
        UserVO params = new UserVO();
        params.setId(self.getUserId());
        params.setCurrentRoleId(self.getRoleId());
        params.setTenantId(self.getTenantId());
        params.setLanguage(self.getLanguage());
        params.setTimeRecentAccessTenant(LocalDateTime.now().plusDays(-1 * daysRecentAccessTenant));
        params.setRecentAccessTenantList(maxSize(params.getRecentAccessTenantList(), maxRecentAccessTenantCount));

        // ??????????????????
        CompletableFuture<UserVO> f1 = CompletableFuture.supplyAsync(() -> {
            SecurityTokenHelper.close();
            DetailsHelper.setCustomUserDetails(self);
            UserVO userVO = userMapper.selectSelf(params);
            SecurityTokenHelper.clear();
            return userVO;
        }, commonExecutor);

        // ?????????????????????????????????
        CompletableFuture<List<Tenant>> f2 = CompletableFuture.supplyAsync(() -> {
            SecurityTokenHelper.close();
            DetailsHelper.setCustomUserDetails(self);
            List<Tenant> recentTenants = userMapper.selectTenantAccess(params);
            SecurityTokenHelper.clear();
            return recentTenants;
        }, commonExecutor);


        CompletableFuture<UserVO> cf = f1
                .thenCombine(f2, (userVO, tenants) -> {
                    if (userVO != null) {
                        userVO.setRecentAccessTenantList(tenants);
                    }
                    return userVO;
                })
                .exceptionally((e) -> {
                    LOGGER.warn("select self user error", e);
                    return null;
                });

        UserVO userVO =  cf.join();

        if (userVO == null) {
            LOGGER.warn("User self not found. params is {}", params);
            // ??????????????????????????????????????????????????????????????????????????????
            throw new CommonException("hiam.warn.user.selfError");
        }
        return userVO;
    }

    private List<Tenant> maxSize(List<Tenant> recentAccessTenantList, int maxRecentAccessTenantCount) {
        if (!CollectionUtils.isEmpty(recentAccessTenantList) && recentAccessTenantList.size() > maxRecentAccessTenantCount) {
            return recentAccessTenantList.subList(0, maxRecentAccessTenantCount);
        }
        return recentAccessTenantList;
    }

    private void setupTitleLogo(UserVO userVO, CustomUserDetails self) {
        String title = redisHelper.strGet(UserVO.generateCacheKey(HZeroConstant.Config.CONFIG_CODE_TITLE, self.getTenantId()));
        if (StringUtils.isBlank(title)) {
            title = redisHelper.strGet(UserVO.generateCacheKey(HZeroConstant.Config.CONFIG_CODE_TITLE, BaseConstants.DEFAULT_TENANT_ID));
        }
        userVO.setTitle(title);
        String logo = redisHelper.strGet(UserVO.generateCacheKey(HZeroConstant.Config.CONFIG_CODE_LOGO, self.getTenantId()));
        if (StringUtils.isBlank(logo)) {
            logo = redisHelper.strGet(UserVO.generateCacheKey(HZeroConstant.Config.CONFIG_CODE_LOGO, BaseConstants.DEFAULT_TENANT_ID));
        }
        userVO.setLogo(logo);
    }

    private void setupMenuLayout(UserVO userVO, CustomUserDetails self) {
        UserConfig userConfig = new UserConfig();
        userConfig.setUserId(self.getUserId());
        userConfig.setTenantId(self.getTenantId());
        UserConfig dbUserConfig = userConfigMapper.selectOne(userConfig);
        if (dbUserConfig != null && dbUserConfig.getMenuLayout() != null) {
            userVO.setMenuLayout(dbUserConfig.getMenuLayout());
        } else {
            String layout = redisHelper.strGet(UserVO.generateCacheKey(Constants.Config.CONFIG_CODE_MENU_LAYOUT, self.getTenantId()));
            if (StringUtils.isBlank(layout)) {
                layout = redisHelper.strGet(UserVO.generateCacheKey(Constants.Config.CONFIG_CODE_MENU_LAYOUT, BaseConstants.DEFAULT_TENANT_ID));
            }
            userVO.setMenuLayout(layout);
        }
    }

    private void setupLayoutTheme(UserVO userVO, CustomUserDetails self) {
        String layoutTheme = redisHelper.strGet(UserVO.generateCacheKey(Constants.Config.CONFIG_CODE_MENU_LAYOUT_THEME, self.getTenantId()));
        if (StringUtils.isBlank(layoutTheme)) {
            layoutTheme = redisHelper.strGet(UserVO.generateCacheKey(Constants.Config.CONFIG_CODE_MENU_LAYOUT_THEME, BaseConstants.DEFAULT_TENANT_ID));
        }
        userVO.setMenuLayoutTheme(layoutTheme);
    }

    private void setupFavicon(UserVO userVO, CustomUserDetails self) {
        String favicon = redisHelper.strGet(UserVO.generateCacheKey(Constants.Config.CONFIG_CODE_FAVICON, self.getTenantId()));
        if (StringUtils.isBlank(favicon)) {
            favicon = redisHelper.strGet(UserVO.generateCacheKey(Constants.Config.CONFIG_CODE_FAVICON, BaseConstants.DEFAULT_TENANT_ID));
        }
        userVO.setFavicon(favicon);
    }

    @Override
    public UserVO selectSelfDetails() {
        CustomUserDetails self = UserUtils.getUserDetails();
        UserVO param = new UserVO();
        param.setId(self.getUserId());
        // ??????????????????
        param.setOrganizationId(self.getOrganizationId());
        // ??????????????????
        param.setTenantId(self.getTenantId());

        return userMapper.selectSelfDetails(param);
    }

    @Override
    public User selectSimpleUserById(Long userId) {
        return selectSimpleUserByIdAndTenantId(userId, null);
    }

    @Override
    public User selectUserPassword(Long userId) {
        User params = new User();
        params.setId(userId);

        return selectOneOptional(params, new Criteria()
                .select(
                        User.FIELD_ID,
                        User.FIELD_PASSWORD,
                        User.FIELD_OBJECT_VERSION_NUMBER
                )
                .where(User.FIELD_ID)
        );
    }

    @Override
    public User selectSimpleUserByIdAndTenantId(Long userId, Long tenantId) {
        User params = new User();
        params.setId(userId);
        params.setOrganizationId(tenantId);

        Object[] whereField = tenantId != null ?
                ArrayUtils.toArray(User.FIELD_ID, User.FIELD_ORGANIZATION_ID) : ArrayUtils.toArray(User.FIELD_ID);

        return selectOneOptional(params, new Criteria()
                .select(
                        User.FIELD_ID,
                        User.FIELD_LOGIN_NAME,
                        User.FIELD_REAL_NAME,
                        User.FIELD_ORGANIZATION_ID,
                        User.FIELD_PHONE,
                        User.FIELD_EMAIL,
                        User.FIELD_ADMIN,
                        User.FIELD_USER_TYPE,
                        User.FIELD_LANGUAGE,
                        User.FIELD_OBJECT_VERSION_NUMBER
                )
                .where(whereField)
        );
    }

    @Override
    @ProcessLovValue
    public Page<UserVO> selectRoleUsers(Long roleId, MemberRoleSearchDTO memberRoleSearchDTO, PageRequest pageRequest) {
        memberRoleSearchDTO.setRoleId(roleId);
        return PageHelper.doPage(pageRequest, () -> userMapper.selectRoleUsers(memberRoleSearchDTO));
    }

    @Override
    public int updateUserInfoByPrimaryKey(UserInfo userInfo) {
        return userInfoMapper.updateByPrimaryKey(userInfo);
    }

    @Override
    public int updateUserConfigByPrimaryKey(UserConfig userConfig) {
        return userConfigMapper.updateByPrimaryKey(userConfig);
    }

    @Override
    public UserInfo selectUserInfoByPrimaryKey(Long userId) {
        return userInfoMapper.selectByPrimaryKey(userId);
    }

    @Override
    public int insertUserInfoSelective(UserInfo userInfo) {
        return userInfoMapper.insertSelective(userInfo);
    }

    @Override
    public int insertUserConfigSelective(UserConfig userConfig) {
        return userConfigMapper.insertSelective(userConfig);
    }

    @Override
    public boolean existsUser(String loginName, String phone, String email, String userType) {
        return existsByLoginName(loginName) || existsByPhone(phone, userType) || existsByEmail(email, userType);
    }

    @Override
    public boolean existsByLoginName(String loginName) {
        return baseUserRepository.existsByLoginName(loginName);
    }

    @Override
    public boolean existsByPhone(String phone, String userType) {
        return baseUserRepository.existsByPhone(phone, UserType.ofDefault(userType));
    }

    @Override
    public boolean existsByEmail(String email, String userType) {
        return baseUserRepository.existsByEmail(email, UserType.ofDefault(userType));
    }


    @Override
    @ProcessLovValue
    public UserVO selectByLoginNameOrEmailOrPhone(UserVO params) {
        params.setUserType(UserType.ofDefault(params.getUserType()).value());
        return userMapper.selectByLoginNameOrEmailOrPhone(params);
    }

    @Override
    public List<UserVO> selectByRealNameOrEmail(UserVO params) {
        params.setUserType(UserType.ofDefault(params.getUserType()).value());
        return userMapper.selectByRealName(params);
    }

    @Override
    @ProcessLovValue(targetField = {"", UserExportDTO.FIELD_USER_AUTHORITY_LIST})
    public List<UserExportDTO> exportUserInfo(UserVO params, String authorityTypeQueryParams, PageRequest pageRequest, ExportParam exportParam) {
        List<UserExportDTO> results;

        if (exportParam.getSelection().contains(UserExportDTO.FIELD_ROLE_LIST)) {
            params.setSelectRole(true);
        }
        if (StringUtils.isNotBlank(authorityTypeQueryParams)) {
            params.setAuthorityTypeQueryParams(Arrays.asList(authorityTypeQueryParams.split(",")));
            params.setSelectAuthority(true);
        }

        if (pageRequest == null) {
            results = userMapper.selectExportUsers(params);
        } else {
            results = PageHelper.doPage(pageRequest, () -> userMapper.selectExportUsers(params));
        }

        return results;
    }

    @Override
    public CompanyVO countCompanyByName(String companyName) {
        return userMapper.countCompanyByName(companyName);
    }

    @Override
    public Page<UserVO> selectMultiTenantUsers(UserVO params, PageRequest pageRequest) {
        return PageHelper.doPage(pageRequest, () -> userMapper.selectMultiTenantUsers(params));
    }

    @Override
    public UserVO selectCompanyName(Long userId) {
        return userMapper.selectCompanyName(userId);
    }

    @Override
    public List<User> listRecentUser(Long tenantId, Date after) {
        return userMapper.selectByCondition(Condition.builder(User.class)
                .notSelect(User.FIELD_PASSWORD, User.FIELD_OBJECT_VERSION_NUMBER)
                .andWhere(Sqls.custom().andEqualTo(User.FIELD_ORGANIZATION_ID, tenantId, true)
                        .andGreaterThan(User.FIELD_LAST_UPDATE_DATE, after))
                .build());
    }

    @Override
    public User selectSimpleUserWithTenant(Long id) {
        User params = new User();
        params.setId(id);
        return userMapper.selectUserTenant(params).stream().findFirst().orElse(null);
    }

    @Override
    public List<User> selectSimpleUsersWithTenant(User params) {
        return userMapper.selectUserTenant(params);
    }

    @Override
    public int countTenantUser(Long organizationId) {
        return selectCountByCondition(Condition.builder(User.class)
                .andWhere(
                        Sqls.custom().andEqualTo(User.FIELD_ORGANIZATION_ID, organizationId)
                ).build());
    }

    @Override
    public void initUsers() {
        PageRequest pageRequest = new PageRequest(0, 600);
        Page<User> pageData = pageSimple(pageRequest);
        batchCacheUser(pageData.getContent());

        pageData.getContent().clear();

        List<AsyncTask<Integer>> tasks = IntStream.rangeClosed(1, pageData.getTotalPages()).mapToObj(page -> (AsyncTask<Integer>) () -> {
            PageRequest pr = new PageRequest(page, 600);
            Page<User> data = pageSimple(pr);
            batchCacheUser(data.getContent());

            data.getContent().clear();
            return data.getNumberOfElements();
        }).collect(Collectors.toList());

        CommonExecutor.batchExecuteAsync(tasks, commonExecutor, "BatchCacheUser");

        LOGGER.info("Finish cache user: cache size: [{}]", pageData.getTotalElements());
    }

    public Page<User> pageSimple(PageRequest pageRequest) {
        return PageHelper.doPage(pageRequest, () -> userMapper.selectCacheUseInfo(new User()));
    }

    @Override
    public Set<String> matchLoginName(Set<String> nameSet) {
        return userMapper.matchLoginName(nameSet);
    }

    @Override
    public Set<String> matchEmail(Set<String> emailSet, String userType) {
        return userMapper.matchEmail(emailSet, userType);
    }

    @Override
    public Set<String> matchPhone(Set<String> phoneSet, String userType) {
        return userMapper.matchPhone(phoneSet, userType);
    }

    @Override
    public User selectByLoginName(String loginName) {
        User param = new User();
        param.setLoginName(loginName);
        return selectOne(param);
    }

    @Override
    public Set<Long> getIdsByMatchLoginName(Set<String> nameSet) {
        return userMapper.getIdsByMatchLoginName(nameSet);
    }

    @Override
    public void disableByIdList(Set<Long> ids) {
        userMapper.disableByIdList(ids);
    }

    @Override
    public void batchCacheUser(List<User> users) {
        if (CollectionUtils.isEmpty(users)) {
            return;
        }

        Map<String, String> map = users.stream()
                .map(UserCacheVO::new)
                .collect(Collectors.toMap(u -> u.getId().toString(), u -> redisHelper.toJson(u)));
        SafeRedisHelper.execute(HZeroService.Iam.REDIS_DB, () -> {
            redisHelper.hshPutAll(HZeroCacheKey.USER, map);
        });
    }

    @Override
    public void cacheUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user = userMapper.selectCacheUseInfo(user).stream().findFirst().orElse(null);

        if (user == null) {
            LOGGER.warn("Cache user not found. userId:{}", userId);
            return;
        }

        UserCacheVO cacheVO = new UserCacheVO(user);
        SafeRedisHelper.execute(HZeroService.Iam.REDIS_DB, () -> {
            redisHelper.hshPut(HZeroCacheKey.USER, userId.toString(), redisHelper.toJson(cacheVO));
        });
    }

    @Override
    public Page<UserEmployeeAssignDTO> pageUserEmployeeAssign(PageRequest pageRequest,
                                                              UserEmployeeAssignDTO userEmployeeAssignDTO) {
        return PageHelper.doPageAndSort(pageRequest,
                () -> userMapper.selectUserEmployeeAssignList(userEmployeeAssignDTO));
    }
}
