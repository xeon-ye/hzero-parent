package org.hzero.wechat.enterprise.service.impl;

import com.alibaba.fastjson.JSON;
import org.hzero.starter.integrate.constant.HrSyncAuthType;
import org.hzero.starter.integrate.constant.SyncType;
import org.hzero.starter.integrate.dto.SyncCorpResultDTO;
import org.hzero.starter.integrate.dto.SyncDeptDTO;
import org.hzero.starter.integrate.dto.SyncUserDTO;
import org.hzero.starter.integrate.entity.CorpHrSync;
import org.hzero.starter.integrate.service.AbstractCorpSyncService;
import org.hzero.wechat.enterprise.constant.WechatConstans;
import org.hzero.wechat.enterprise.constant.WechatErrorCode;
import org.hzero.wechat.enterprise.dto.*;
import org.hzero.wechat.enterprise.enums.SecretTypeEnum;
import org.hzero.wechat.enterprise.service.WechatCorpAddressService;
import org.hzero.wechat.enterprise.service.WechatTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author J 2019/8/28
 */
public class WechatCorpSyncServiceImpl implements AbstractCorpSyncService {


    private static final Logger logger = LoggerFactory.getLogger(WechatCorpSyncServiceImpl.class);

    @Autowired
    private WechatCorpAddressService wechatCorpAddressService;
    @Autowired
    private WechatTokenService wechatTokenService;

    @Override
    public String corpSyncType() {
        return "WX";
    }

    @Override
    public String getAccessToken(CorpHrSync corpHrSync) {
        switch (corpHrSync.getAuthType()) {
            case HrSyncAuthType.SELF:
                TokenDTO tokenDTO = wechatTokenService
                        .getTokenWithCache(corpHrSync.getAppId(), corpHrSync.getAppSecret(), SecretTypeEnum.CORP);
                if (StringUtils.hasText(tokenDTO.getAccess_token())) {
                    return tokenDTO.getAccess_token();
                } else {
                    return null;
                }
            case HrSyncAuthType.THIRD:
                TokenDTO result =
                        wechatTokenService.getTokenFromThirdPart(corpHrSync.getAuthAddress());
                if (result != null && StringUtils.hasText(result.getAccess_token())) {
                    return result.getAccess_token();
                } else {
                    return null;
                }
            default:
                return null;
        }
    }

    @Override
    public SyncCorpResultDTO syncCorp(List<SyncDeptDTO> syncDeptList, List<SyncUserDTO> syncUserList,
                                      Boolean useGeneratedDeptId, CorpHrSync corpHrSync) {
        SyncCorpResultDTO syncCorpResultDTO = new SyncCorpResultDTO().setDeptStatus(true).setEmployeeStatus(true);
        StringBuilder log = new StringBuilder();
        Map<Long, Long> successDeptIdMap = new HashMap<>(16);
        List<String> successUserIds = new ArrayList<>();
        try {
            List<String> deleteUserIds = new ArrayList<>();
            List<WechatSyncUserDTO> insertUsers = new ArrayList<>();
            Map<String, WechatSyncUserDTO> updateUsersMap = new HashMap<>(16);
            List<Long> deleteDeptIds = new ArrayList<>();
            List<WechatSyncDeptDTO> insertDeptList = new ArrayList<>();
            List<WechatSyncDeptDTO> updateDeptList = new ArrayList<>();

            List<WechatSyncUserDTO> weChatSyncUserList = new ArrayList<>();
            List<WechatSyncDeptDTO> weChatSyncDeptList = new ArrayList<>();
            convertSyncUserDTOS(syncUserList, weChatSyncUserList);
            convertSyncDeptDTOS(syncDeptList, weChatSyncDeptList);

            weChatSyncUserList.forEach(item -> {
                switch (item.getSyncType()) {
                    case SyncType.CREATE:
                        insertUsers.add(item);
                        break;
                    case SyncType.UPDATE:
                        updateUsersMap.put(item.getUserid(), item);
                        break;
                    case SyncType.DELETE:
                        deleteUserIds.add(item.getUserid());
                        break;
                    default:
                        log.append("Employees synchronization object has no synchronization type: ").append(item.toString())
                                .append(org.apache.commons.lang3.StringUtils.LF);
                        break;
                }
            });

            weChatSyncDeptList.forEach(item -> {
                switch (item.getSyncType()) {
                    case SyncType.CREATE:
                        insertDeptList.add(item);
                        break;
                    case SyncType.UPDATE:
                        updateDeptList.add(item);
                        break;
                    case SyncType.DELETE:
                        deleteDeptIds.add(item.getId());
                        break;
                    default:
                        log.append("Departments synchronization object has no synchronization type: ").append(item.toString())
                                .append(org.apache.commons.lang3.StringUtils.LF);
                        break;
                }
            });
            ResultCount resultCount = new ResultCount();
            logger.debug("***Start syncing Enterprise WeChat departments***");
            String accessToken = getAccessToken(corpHrSync);
            if (accessToken == null) {
                log.append("error get token, sync failed!").append(org.apache.commons.lang3.StringUtils.LF);
                return syncCorpResultDTO.setLog(log.toString()).setDeptStatus(false).setEmployeeStatus(false);
            }
            // ????????????
            insertDept(insertDeptList, useGeneratedDeptId, accessToken, successDeptIdMap, resultCount ,log);
            // ???????????????????????????????????????
            deleteDept(deleteDeptIds, deleteUserIds, updateUsersMap, successDeptIdMap, successUserIds,
                    accessToken, resultCount ,log);
            // ????????????
            updateDeptList(updateDeptList, successDeptIdMap, accessToken, resultCount ,log);

            logger.debug("**Start syncing Enterprise WeChat employees**");
            // ????????????
            deleteUsers(deleteUserIds, successUserIds, accessToken, resultCount ,log);
            // ????????????
            insertUsers(insertUsers, successDeptIdMap, successUserIds, accessToken, resultCount ,log);
            // ????????????
            updateUsers(updateUsersMap, successDeptIdMap, successUserIds, accessToken, resultCount ,log);
            if (resultCount.getDeptFailCount() > 0) {
                syncCorpResultDTO.setDeptStatus(false);
            }
            if (resultCount.getUserFailCount() > 0) {
                syncCorpResultDTO.setEmployeeStatus(false);
            }
            log.append("Sync is complete. Departments Synced successfully count: ")
                    .append(resultCount.getDeptSuccessCount()).append("???synced failed count: ")
                    .append(resultCount.getDeptFailCount()).append(".Employees synced successfully count: ")
                    .append(resultCount.getUserSuccessCount()).append(",synced failed count: ")
                    .append(resultCount.getUserFailCount());
            return syncCorpResultDTO.setEmployeeStatus(false).setDeptStatus(false).setLog(log.toString())
                    .setDeptIdMap(successDeptIdMap).setUserIds(successUserIds);
        } catch (Exception e) {
            log.append(e.getMessage());
            return syncCorpResultDTO.setEmployeeStatus(false).setDeptStatus(false).setLog(log.toString())
                    .setDeptIdMap(successDeptIdMap).setUserIds(successUserIds);
        }
    }

    @Override
    public List<SyncDeptDTO> listDept(Long deptId, String accessToken) {
        DeptListDTO deptList = wechatCorpAddressService.getDeptList(deptId, accessToken);
        if (deptList != null) {
            if (!CollectionUtils.isEmpty(deptList.getDepartment())) {
                List<SyncDeptDTO> result = new ArrayList<>();
                deptList.getDepartment().forEach(department -> {
                    SyncDeptDTO deptDTO = new SyncDeptDTO();
                    BeanUtils.copyProperties(department, deptDTO);
                    deptDTO.setOrder(Integer.toUnsignedLong(department.getOrder()));
                    result.add(deptDTO);
                });
                return result;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<SyncUserDTO> listUser(Long deptId, String accessToken) {
        GetUserInfoByDeptIdResultDTO wechatUserInfo = wechatCorpAddressService.getUserInfoByDeptId(accessToken, deptId, 0);
        if (wechatUserInfo != null) {
            if (!CollectionUtils.isEmpty(wechatUserInfo.getUserlist())) {
                List<SyncUserDTO> result = new ArrayList<>();
                wechatUserInfo.getUserlist().forEach(user -> {
                    SyncUserDTO userDTO = new SyncUserDTO();
                    BeanUtils.copyProperties(user, userDTO);
                    result.add(userDTO);
                });
                return result;
            }
        }
        return Collections.emptyList();
    }

    /**
     * ???hzero???????????????????????????????????????????????????
     *
     * @param syncUserList       hzero????????????????????????
     * @param weChatSyncUserList ??????????????????????????????
     */
    private void convertSyncUserDTOS(List<SyncUserDTO> syncUserList, List<WechatSyncUserDTO> weChatSyncUserList) {
        syncUserList.forEach(e -> {
            WechatSyncUserDTO weChatSyncUserDTO = new WechatSyncUserDTO();
            BeanUtils.copyProperties(e, weChatSyncUserDTO);
            weChatSyncUserList.add(weChatSyncUserDTO);
        });
    }

    private void convertSyncDeptDTOS(List<SyncDeptDTO> syncDeptList, List<WechatSyncDeptDTO> weChatSyncDeptList) {
        syncDeptList.forEach(e -> {
            WechatSyncDeptDTO weChatSyncDeptDTO = new WechatSyncDeptDTO();
            BeanUtils.copyProperties(e, weChatSyncDeptDTO);
            weChatSyncDeptList.add(weChatSyncDeptDTO);
        });
    }

    /**
     * ????????????????????????
     *
     * @param insertDeptList     ??????????????????
     * @param useGeneratedDeptId ??????????????????????????????id
     * @param accessToken        token
     * @param successDeptIdMap   ???????????????????????????
     * @param resultCount        ??????????????????
     */
    private void insertDept(List<WechatSyncDeptDTO> insertDeptList, Boolean useGeneratedDeptId, String accessToken,
                            Map<Long, Long> successDeptIdMap, ResultCount resultCount, StringBuilder log) {
        if (!CollectionUtils.isEmpty(insertDeptList)) {
            ArrayList<WechatSyncDeptDTO> syncDeptDTOList = sortDept(insertDeptList);
            for (WechatSyncDeptDTO insertDept : syncDeptDTOList) {
                Long deptId = insertDept.getId();
                if (useGeneratedDeptId) {
                    insertDept.setId(null);
                }
                boolean isParentIdMap = convertDeptParentId(insertDept, successDeptIdMap);
                logger.debug("Create WeChat department: {}", insertDept.toString());
                log.append("Create Enterprise WeChat department:").append(insertDept.toString()).append(org.apache.commons.lang3.StringUtils.LF);
                if (!isParentIdMap) {
                    // ???????????????id
                    logger.debug("Create failed, the parent department does not exist.");
                    log.append("Create failed,the parent department does not exist.").append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.deptFail();
                    continue;
                }
                if (!useGeneratedDeptId && !checkDeptId(insertDept.getId())) {
                    logger.debug("Create department failed, department id is greater than 32-bit integer value");
                    log.append("Create department failed, department id is greater than 32-bit integer value")
                            .append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.deptFail();
                    continue;
                }
                CreateDeptResultDTO dept = wechatCorpAddressService.createDept(insertDept, accessToken);
                if (WechatErrorCode.SUCCESS.equals(dept.getErrcode())) {
                    successDeptIdMap.put(deptId, dept.getId());
                    resultCount.deptSuccess();
                } else {
                    if (WechatErrorCode.DEPT_EXISTS.equals(dept.getErrcode())
                            && !useGeneratedDeptId) {
                        // ????????????????????????????????????????????????????????????id????????????????????????
                        logger.debug("Department already exists,update department");
                        log.append("Department already exists, update department").append(org.apache.commons.lang3.StringUtils.LF);
                        DefaultResultDTO defaultResultDTO = wechatCorpAddressService.updateDept(insertDept, accessToken);
                        if (WechatErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                            successDeptIdMap.put(deptId, deptId);
                            resultCount.deptSuccess();
                        } else {
                            resultCount.deptFail();
                            logger.debug("Update failed: {}", defaultResultDTO.getErrmsg());
                            log.append("Update failed???").append(defaultResultDTO.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                        }
                    } else {
                        resultCount.deptFail();
                        logger.debug("Create failed: {}", dept.getErrmsg());
                        log.append("Create failed???").append(dept.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                    }
                }
            }
        }
    }

    /**
     * ???????????????????????????????????????
     *
     
     * @param deleteDeptIds    ?????????????????????
     * @param deleteUserIds    ?????????????????????
     * @param updateUsersMap   ??????????????????
     * @param successDeptIdMap ???????????????????????????
     * @param successUserIds   ???????????????????????????
     * @param accessToken      token
     * @param resultCount      ??????????????????
     */
    private void deleteDept(List<Long> deleteDeptIds, List<String> deleteUserIds,
                            Map<String, WechatSyncUserDTO> updateUsersMap, Map<Long, Long> successDeptIdMap,
                            List<String> successUserIds, String accessToken, ResultCount resultCount, StringBuilder log) {
        while (!CollectionUtils.isEmpty(deleteDeptIds)) {
            Long deleteDeptId = deleteDeptIds.get(0);
            logger.debug("Change users in the department and its sub-departments before deleting the Enterprise WeChat department,id??? {}", deleteDeptId);
            log.append("Change users in the department and its sub-departments before deleting the Enterprise WeChat department,id???")
                    .append(deleteDeptId).append(org.apache.commons.lang3.StringUtils.LF);
            DeptUserDTO usersByDeptId = wechatCorpAddressService.getUsersByDeptId(deleteDeptId, accessToken, 1);
            List<DeptUserDTO.UserlistBean> userList = usersByDeptId.getUserlist();
            // ???????????????????????????????????????
            if (!CollectionUtils.isEmpty(userList)) {
                List<String> userIds = userList.stream().map(DeptUserDTO.UserlistBean::getUserid).collect(Collectors.toList());
                for (String userId : userIds) {
                    if (deleteUserIds.contains(userId)) {
                        deleteUserIds.remove(userId);
                        // ??????????????????
                        logger.debug("Delete Enterprise WeChat employees under the department,userid: {}", userId);
                        log.append("Delete Enterprise WeChat employees under the department,userid: ").append(userId)
                                .append(org.apache.commons.lang3.StringUtils.LF);
                        DefaultResultDTO defaultResultDTO = wechatCorpAddressService.deleteUserById(userId, accessToken);
                        if (!WechatErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                            logger.debug("Delete Failed: {}", defaultResultDTO.getErrmsg());
                            log.append("Delete failed: ").append(defaultResultDTO.getErrmsg())
                                    .append(org.apache.commons.lang3.StringUtils.LF);
                            resultCount.userFail(1);
                        } else {
                            successUserIds.add(userId);
                            resultCount.userSuccess(1);
                        }
                    } else if (updateUsersMap.containsKey(userId)) {
                        // ????????????
                        WechatSyncUserDTO syncUserDTO = updateUsersMap.get(userId);
                        updateUsersMap.remove(userId);
                        boolean isAllDepartIdsMap = convertUserDepart(syncUserDTO, successDeptIdMap);
                        logger.debug("Update Enterprise WeChat employees under the department: {}", syncUserDTO.toString());
                        log.append("Update Enterprise WeChat employees under the department:").append(syncUserDTO.toString())
                                .append(org.apache.commons.lang3.StringUtils.LF);
                        // ????????????????????????????????????
                        if (!isAllDepartIdsMap) {
                            logger.debug("Update employee failed, department does not exist.");
                            log.append("Update employee failed, department does not exist.")
                                    .append(org.apache.commons.lang3.StringUtils.LF);
                            resultCount.userFail(1);
                            continue;
                        }
                        DefaultResultDTO defaultResultDTO = wechatCorpAddressService.updateUser(syncUserDTO, accessToken);
                        if (!WechatErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                            logger.debug("Update failed: {}", defaultResultDTO.getErrmsg());
                            log.append("Update failed: ").append(defaultResultDTO.getErrmsg())
                                    .append(org.apache.commons.lang3.StringUtils.LF);
                            resultCount.userFail(1);
                        } else {
                            successUserIds.add(userId);
                            resultCount.userSuccess(1);
                        }
                    } else {
                        GetUserDTO userById = wechatCorpAddressService.getUserById(userId, accessToken);
                        logger.debug("The employee is not included in the sync employees: {}", userById.toString());
                        log.append("The employee is not included in the sync employees: ").append(userById.toString())
                                .append(org.apache.commons.lang3.StringUtils.LF);
                    }
                }
            }
            // ???????????????????????????
            DeptListDTO deptList = wechatCorpAddressService.getDeptList(deleteDeptId, accessToken);
            if (!CollectionUtils.isEmpty(deptList.getDepartment())) {
                logger.debug("Delete Enterprise WeChat department id: {},delete sub-department first", deleteDeptId);
                log.append("Delete Enterprise WeChat department id: ").append(deleteDeptId).append(",delete sub-department first")
                        .append(org.apache.commons.lang3.StringUtils.LF);
                // ???????????????
                deleteDeptIds.removeAll(
                        deptList.getDepartment().stream().map(DeptListDTO.DepartmentBean::getId).collect(Collectors.toList()));
                ArrayList<DeptListDTO.DepartmentBean> departmentBeans =
                        sortDept(deptList.getDepartment(), deleteDeptId);
                for (DeptListDTO.DepartmentBean j : departmentBeans) {
                    logger.debug("Delete Enterprise WeChat department???id: {}", j.getId());
                    log.append("Delete Enterprise WeChat department???id:").append(j.getId())
                            .append(org.apache.commons.lang3.StringUtils.LF);
                    DefaultResultDTO defaultResultDTO = wechatCorpAddressService.deleteDeptById(j.getId(), accessToken);
                    if (WechatErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                        successDeptIdMap.put(j.getId(), null);
                        resultCount.deptSuccess();
                    } else {
                        logger.debug("Delete failed: {}", defaultResultDTO.getErrmsg());
                        log.append("Delete failed: ").append(defaultResultDTO.getErrmsg())
                                .append(org.apache.commons.lang3.StringUtils.LF);
                        resultCount.deptFail();
                    }
                }
            } else {
                logger.debug("Delete Enterprise WeChat department???id: {}", deleteDeptId);
                log.append("Delete Enterprise WeChat department???id:").append(deleteDeptId)
                        .append(org.apache.commons.lang3.StringUtils.LF);
                DefaultResultDTO defaultResultDTO = wechatCorpAddressService.deleteDeptById(deleteDeptId, accessToken);
                if (WechatErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                    successDeptIdMap.put(deleteDeptId, null);
                    resultCount.deptSuccess();
                } else {
                    logger.debug("Delete failed: {}", defaultResultDTO.getErrmsg());
                    log.append("Delete failed: ").append(defaultResultDTO.getErrmsg())
                            .append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.deptFail();
                }
            }
        }
    }

    /**
     * ????????????
     *
     * @param updateDeptList   ?????????????????????
     * @param successDeptIdMap ???????????????????????????
     * @param accessToken      token
     * @param resultCount      ??????????????????
     */
    private void updateDeptList(List<WechatSyncDeptDTO> updateDeptList, Map<Long, Long> successDeptIdMap,
                                String accessToken, ResultCount resultCount, StringBuilder log) {
        for (WechatSyncDeptDTO j : updateDeptList) {
            boolean isParentIdMap = convertDeptParentId(j, successDeptIdMap);
            logger.debug("Update Enterprise WeChat department: {}", j.toString());
            log.append("Update Enterprise WeChat department:").append(j.toString()).append(org.apache.commons.lang3.StringUtils.LF);
            if (!isParentIdMap) {
                // ???????????????id
                resultCount.deptFail();
                logger.debug("Update Enterprise WeChat department failed, parent department does not exist");
                log.append("Update Enterprise WeChat department failed, parent department does not exist")
                        .append(org.apache.commons.lang3.StringUtils.LF);
                continue;
            }
            DefaultResultDTO defaultResultDTO = wechatCorpAddressService.updateDept(j, accessToken);
            if (WechatErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                successDeptIdMap.put(j.getId(), null);
                resultCount.deptSuccess();
            } else {
                logger.debug("Update failed: {}", defaultResultDTO.getErrmsg());
                log.append("Update failed: ").append(defaultResultDTO.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                resultCount.deptFail();
            }
        }
    }

    /**
     * ????????????
     *
     * @param deleteUserIds  ???????????????id
     * @param successUserIds ???????????????????????????
     * @param accessToken    token
     * @param resultCount    ??????????????????
     */
    private void deleteUsers(List<String> deleteUserIds, List<String> successUserIds, String accessToken,
                             ResultCount resultCount, StringBuilder log) {
        if (!CollectionUtils.isEmpty(deleteUserIds)) {
            logger.debug("Delete Enterprise WeChat employees in batches: {}", JSON.toJSONString(deleteUserIds));
            log.append("Delete Enterprise WeChat employees in batches:").append(JSON.toJSONString(deleteUserIds))
                    .append(org.apache.commons.lang3.StringUtils.LF);
            DefaultResultDTO defaultResultDTO = wechatCorpAddressService.batchDeleteUser(deleteUserIds, accessToken);
            if (WechatErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                logger.debug("Delete failed: {}", defaultResultDTO.getErrmsg());
                log.append("Delete failed: ").append(defaultResultDTO.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                resultCount.userFail(deleteUserIds.size());
            } else {
                successUserIds.addAll(deleteUserIds);
                resultCount.userSuccess(deleteUserIds.size());
            }
        }
    }

    /**
     * ????????????
     *
     
     * @param insertUsers      ???????????????
     * @param successDeptIdMap ???????????????????????????
     * @param successUserIds   ???????????????????????????
     * @param accessToken      token
     * @param resultCount      ??????????????????
     */
    private void insertUsers(List<WechatSyncUserDTO> insertUsers, Map<Long, Long> successDeptIdMap,
                             List<String> successUserIds, String accessToken, ResultCount resultCount, StringBuilder log) {
        if (!CollectionUtils.isEmpty(insertUsers)) {
            for (WechatSyncUserDTO j : insertUsers) {
                boolean isAllDepartIdsMap = convertUserDepart(j, successDeptIdMap);
                logger.debug("Create Enterprise WeChat employees: {}", j.toString());
                log.append("Create Enterprise WeChat employees: ").append(j.toString())
                        .append(org.apache.commons.lang3.StringUtils.LF);
                // ????????????????????????????????????
                if (!isAllDepartIdsMap) {
                    logger.debug("Create employee failed, department does not exist.");
                    log.append("Create employee failed, department does not exist.").append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.userFail(1);
                    continue;
                }
                DefaultResultDTO user = wechatCorpAddressService.createUser(j, accessToken);
                if (WechatErrorCode.SUCCESS.equals(user.getErrcode())) {
                    successUserIds.add(j.getUserid());
                    resultCount.userSuccess(1);
                } else {
                    if (WechatErrorCode.MOBILE_EXISTS.equals(user.getErrcode())
                            || WechatErrorCode.USERID_EXISTS.equals(user.getErrcode())) {
                        // ?????????????????????????????????
                        logger.debug("Employee already exists, update employee.");
                        log.append("Employee already exists, update employee.").append(org.apache.commons.lang3.StringUtils.LF);
                        DefaultResultDTO defaultResultDTO = wechatCorpAddressService.updateUser(j, accessToken);
                        if (WechatErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                            successUserIds.add(j.getUserid());
                            resultCount.userSuccess(1);
                        } else {
                            resultCount.userFail(1);
                            logger.debug("Update failed??? {}", defaultResultDTO.getErrmsg());
                            log.append("Update failed???").append(defaultResultDTO.getErrmsg())
                                    .append(org.apache.commons.lang3.StringUtils.LF);
                        }
                    } else {
                        resultCount.userFail(1);
                        logger.debug("Create failed???{}", user.getErrmsg());
                        log.append("Create failed???").append(user.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                    }
                }
            }
        }
    }

    /**
     * ????????????
     *
     
     * @param updateUsersMap   ???????????????
     * @param successDeptIdMap ???????????????????????????
     * @param successUserIds   ???????????????????????????
     * @param accessToken      token
     * @param resultCount      ??????????????????
     */
    private void updateUsers(Map<String, WechatSyncUserDTO> updateUsersMap, Map<Long, Long> successDeptIdMap,
                             List<String> successUserIds, String accessToken, ResultCount resultCount, StringBuilder log) {
        if (!CollectionUtils.isEmpty(updateUsersMap.values())) {
            for (WechatSyncUserDTO j : updateUsersMap.values()) {
                boolean isAllDepartIdsMap = convertUserDepart(j, successDeptIdMap);
                logger.debug("Update Enterprise WeChat employees: {}", j.toString());
                log.append("Update Enterprise WeChat employees: ").append(j).append(org.apache.commons.lang3.StringUtils.LF);
                // ????????????????????????????????????
                if (!isAllDepartIdsMap) {
                    logger.debug("Update employee failed, department does not exist.");
                    log.append("Update employee failed, department does not exist.").append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.userFail(1);
                    continue;
                }
                DefaultResultDTO defaultResultDTO = wechatCorpAddressService.updateUser(j, accessToken);
                if (WechatErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                    successUserIds.add(j.getUserid());
                    resultCount.userSuccess(1);
                } else {
                    logger.debug("Update failed???{}", defaultResultDTO.getErrmsg());
                    log.append("Update failed???").append(defaultResultDTO.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.userFail(1);
                }
            }
        }
    }

    public static class ResultCount {
        /**
         * ????????????????????????
         */
        private int deptSuccessCount = 0;
        /**
         * ????????????????????????
         */
        private int deptFailCount = 0;
        /**
         * ????????????????????????
         */
        private int userSuccessCount = 0;
        /**
         * ????????????????????????
         */
        private int userFailCount = 0;

        void deptSuccess() {
            deptSuccessCount += 1;
        }

        void deptFail() {
            deptFailCount += 1;
        }

        void userSuccess(int count) {
            userSuccessCount += count;
        }

        void userFail(int count) {
            userFailCount += count;
        }

        int getDeptSuccessCount() {
            return deptSuccessCount;
        }

        int getDeptFailCount() {
            return deptFailCount;
        }

        int getUserSuccessCount() {
            return userSuccessCount;
        }

        int getUserFailCount() {
            return userFailCount;
        }
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param deptDTOList ??????????????????????????????
     * @return ????????????????????????
     */
    private ArrayList<WechatSyncDeptDTO> sortDept(List<WechatSyncDeptDTO> deptDTOList) {
        ArrayList<WechatSyncDeptDTO> sortDept = new ArrayList<>();
        List<Long> collect = deptDTOList.stream().map(WechatSyncDeptDTO::getId).collect(Collectors.toList());
        boolean init = true;
        while (!CollectionUtils.isEmpty(deptDTOList)) {
            if (init) {

                List<Long> finalCollect = collect;
                List<WechatSyncDeptDTO> rootDept = deptDTOList.stream()
                        .filter(e -> (WechatConstans.ROOT_DEPT_ID.equals(e.getParentid())
                                || !finalCollect.contains(e.getParentUnitId())))
                        .collect(Collectors.toList());
                collect = rootDept.stream().map(WechatSyncDeptDTO::getId).collect(Collectors.toList());
                sortDept.addAll(rootDept);
                deptDTOList.removeAll(rootDept);
                init = false;
            } else {
                List<Long> finalCollect = collect;
                List<WechatSyncDeptDTO> depts = deptDTOList.stream().filter(e -> (finalCollect.contains(e.getParentUnitId())))
                        .collect(Collectors.toList());
                collect = depts.stream().map(WechatSyncDeptDTO::getId).collect(Collectors.toList());
                sortDept.addAll(depts);
                deptDTOList.removeAll(depts);
            }
        }
        return sortDept;
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????
     *
     * @param departmentBeans ?????????????????????????????????
     * @param rootId          ??????????????????ID
     * @return ????????????????????????
     */
    private ArrayList<DeptListDTO.DepartmentBean> sortDept(List<DeptListDTO.DepartmentBean> departmentBeans,
                                                           Long rootId) {
        List<Long> rootIds = null;
        final Long finalRootId = rootId;
        boolean init = true;
        ArrayList<DeptListDTO.DepartmentBean> sortDept = new ArrayList<>();
        while (!CollectionUtils.isEmpty(departmentBeans)) {
            if (init) {
                List<DeptListDTO.DepartmentBean> rootDept = departmentBeans.stream()
                        .filter(j -> (j.getId().equals(finalRootId))).collect(Collectors.toList());
                sortDept.addAll(rootDept);
                departmentBeans.removeAll(rootDept);
                rootDept = departmentBeans.stream().filter(j -> (Objects.equals(j.getParentid(), finalRootId)))
                        .collect(Collectors.toList());
                sortDept.addAll(rootDept);
                departmentBeans.removeAll(rootDept);
                rootIds = rootDept.stream().map(DeptListDTO.DepartmentBean::getId).collect(Collectors.toList());
                init = false;
            } else {
                ArrayList<DeptListDTO.DepartmentBean> otherDept = new ArrayList<>();
                for (DeptListDTO.DepartmentBean departmentBean : departmentBeans) {
                    if (!CollectionUtils.isEmpty(rootIds) && rootIds.contains(departmentBean.getParentid())) {
                        otherDept.add(departmentBean);
                    }
                }
                sortDept.addAll(otherDept);
                departmentBeans.removeAll(otherDept);
                rootIds = otherDept.stream().map(DeptListDTO.DepartmentBean::getId).collect(Collectors.toList());
            }
        }
        Collections.reverse(sortDept);
        return sortDept;
    }

    /**
     * ??????????????????id?????????????????????id
     *
     * @param syncDeptDTO      ????????????
     * @param successDeptIdMap ????????????id???????????????id????????????
     * @return ??????id??????????????????
     */
    private boolean convertDeptParentId(WechatSyncDeptDTO syncDeptDTO, Map<Long, Long> successDeptIdMap) {
        if (!WechatConstans.ROOT_DEPT_ID.equals(syncDeptDTO.getParentid()) && syncDeptDTO.getParentid() == null) {
            // ???????????????id???????????????id
            if (successDeptIdMap.containsKey(syncDeptDTO.getParentUnitId())) {
                syncDeptDTO.setParentid(successDeptIdMap.get(syncDeptDTO.getParentUnitId()));
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * ????????????????????????????????????id?????????????????????id
     *
     * @param syncUserDTO      ????????????
     * @param successDeptIdMap ????????????id???????????????id????????????
     * @return ????????????id???????????????????????????id
     */
    private boolean convertUserDepart(WechatSyncUserDTO syncUserDTO, Map<Long, Long> successDeptIdMap) {
        List<Long> department = syncUserDTO.getDepartment();
        List<Boolean> isDepartIdsMap = syncUserDTO.getIsDepartIdsMap();
        // ??????id??????????????????
        boolean isAllDepartIdsMap = true;
        for (int i = 0; i < isDepartIdsMap.size(); i++) {
            if (isDepartIdsMap.get(i) != null && !isDepartIdsMap.get(i)) {
                // ?????????id?????????????????????id
                Long departId = successDeptIdMap.get(department.get(i));
                if (departId != null) {
                    department.set(i, departId);
                } else {
                    isAllDepartIdsMap = false;
                }
            }
        }
        return isAllDepartIdsMap;
    }

    /**
     * ????????????????????????id????????????id???????????????????????????id?????????
     *
     * @param id ??????id
     * @return id????????????
     */
    private boolean checkDeptId(Long id) {
        return id <= WechatConstans.MAX_DEPT_ID;
    }

}
