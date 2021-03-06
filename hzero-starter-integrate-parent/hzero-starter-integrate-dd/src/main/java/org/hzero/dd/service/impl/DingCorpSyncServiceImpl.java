package org.hzero.dd.service.impl;

import org.hzero.dd.constant.DingConstans;
import org.hzero.dd.constant.DingErrorCode;
import org.hzero.dd.dto.*;
import org.hzero.dd.service.DingCorpAddressService;
import org.hzero.dd.service.DingTokenService;
import org.hzero.starter.integrate.constant.HrSyncAuthType;
import org.hzero.starter.integrate.constant.SyncType;
import org.hzero.starter.integrate.dto.SyncCorpResultDTO;
import org.hzero.starter.integrate.dto.SyncDeptDTO;
import org.hzero.starter.integrate.dto.SyncUserDTO;
import org.hzero.starter.integrate.entity.CorpHrSync;
import org.hzero.starter.integrate.service.AbstractCorpSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author J 2019/8/28
 */
@Service
public class DingCorpSyncServiceImpl implements AbstractCorpSyncService {


    private static final Logger logger = LoggerFactory.getLogger(DingCorpSyncServiceImpl.class);

    @Autowired
    private DingCorpAddressService dingCorpAddressService;
    @Autowired
    private DingTokenService dingTokenService;

    @Override
    public String corpSyncType() {
        return "DD";
    }

    @Override
    public String getAccessToken(CorpHrSync corpHrSync) {
        switch (corpHrSync.getAuthType()) {
            case HrSyncAuthType.SELF:
                TokenDTO tokenDTO =
                        dingTokenService.getTokenWithCache(corpHrSync.getAppId(), corpHrSync.getAppSecret());
                if (StringUtils.hasText(tokenDTO.getAccess_token())) {
                    return tokenDTO.getAccess_token();
                } else {
                    return null;
                }
            case HrSyncAuthType.THIRD:
                TokenDTO result = dingTokenService.getTokenFromThirdPart(corpHrSync.getAuthAddress());
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
            List<DingSyncUserDTO> insertUsers = new ArrayList<>();
            Map<String, DingSyncUserDTO> updateUsersMap = new HashMap<>(16);
            List<Long> deleteDeptIds = new ArrayList<>();
            List<DingSyncDeptDTO> insertDeptList = new ArrayList<>();
            List<DingSyncDeptDTO> updateDeptList = new ArrayList<>();

            //??????????????????
            List<DingSyncUserDTO> dingSyncUsers = new ArrayList<>();
            List<DingSyncDeptDTO> dingSyncDeptDTOList = new ArrayList<>();
            convertSyncUserList(syncUserList, dingSyncUsers);
            convertSyncDeptList(syncDeptList, dingSyncDeptDTOList);

            dingSyncUsers.forEach(dingSyncUserDTO -> {
                switch (dingSyncUserDTO.getSyncType()) {
                    case SyncType.CREATE:
                        insertUsers.add(dingSyncUserDTO);
                        break;
                    case SyncType.UPDATE:
                        updateUsersMap.put(dingSyncUserDTO.getUserid(), dingSyncUserDTO);
                        break;
                    case SyncType.DELETE:
                        deleteUserIds.add(dingSyncUserDTO.getUserid());
                        break;
                    default:
                        log.append("Employees synchronization object has no synchronization type: ")
                                .append(dingSyncUserDTO.toString()).append(org.apache.commons.lang3.StringUtils.LF);
                        break;
                }
            });
            dingSyncDeptDTOList.forEach(dingSyncDeptDTO -> {
                switch (dingSyncDeptDTO.getSyncType()) {
                    case SyncType.CREATE:
                        insertDeptList.add(dingSyncDeptDTO);
                        break;
                    case SyncType.UPDATE:
                        updateDeptList.add(dingSyncDeptDTO);
                        break;
                    case SyncType.DELETE:
                        deleteDeptIds.add(dingSyncDeptDTO.getId());
                        break;
                    default:
                        log.append("Departments synchronization object has no synchronization type: ")
                                .append(dingSyncDeptDTO.toString()).append(org.apache.commons.lang3.StringUtils.LF);
                        break;
                }
            });
            ResultCount resultCount = new ResultCount();
            logger.debug("***Start syncing DingTalk department***");
            String accessToken = getAccessToken(corpHrSync);
            if (accessToken == null) {
                log.append("error get token, sync failed!").append(org.apache.commons.lang3.StringUtils.LF);
                return syncCorpResultDTO.setLog(log.toString()).setDeptStatus(false).setEmployeeStatus(false);
            }
            // ????????????
            insertDeptList(insertDeptList, successDeptIdMap, useGeneratedDeptId, accessToken, resultCount, log);
            // ???????????????????????????????????????
            deleteDeptList(deleteDeptIds, deleteUserIds, updateUsersMap, successDeptIdMap, successUserIds,
                    accessToken, resultCount, log);
            // ????????????
            updateDeptList(updateDeptList, successDeptIdMap, accessToken, resultCount, log);

            logger.debug("***Start syncing DingTalk employees***");
            // ????????????
            deleteUsers(deleteUserIds, successUserIds, accessToken, resultCount, log);
            // ????????????
            insertUsers(insertUsers, successDeptIdMap, successUserIds, accessToken, resultCount, log);
            // ????????????
            updateUsers(updateUsersMap, successDeptIdMap, successUserIds, accessToken, resultCount, log);
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
                    .append(resultCount.getUserFailCount()).append(org.apache.commons.lang3.StringUtils.LF);
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
        DeptListDTO deptList = dingCorpAddressService.getDeptList(deptId, accessToken);
        if (deptList != null) {
            if (!CollectionUtils.isEmpty(deptList.getDepartment())) {
                List<SyncDeptDTO> result = new ArrayList<>();
                deptList.getDepartment().forEach(department -> {
                    GetDeptDTO dingDeptInfo = dingCorpAddressService.getDeptById(accessToken, Long.toString(department.getId()));
                    SyncDeptDTO deptDTO = new SyncDeptDTO();
                    BeanUtils.copyProperties(dingDeptInfo, deptDTO);
                    result.add(deptDTO);
                });
                return result;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<SyncUserDTO> listUser(Long deptId, String accessToken) {
        GetDeptUserResultDTO deptUser = dingCorpAddressService.getDeptUser(accessToken, deptId);
        if (deptUser != null) {
            if (!CollectionUtils.isEmpty(deptUser.getUserlist())) {
                List<SyncUserDTO> result = new ArrayList<>();
                deptUser.getUserlist().forEach(user -> {
                    GetUserDTO userInfo = dingCorpAddressService.getUserInfoByUserId(accessToken, user.getUserid(), null);
                    SyncUserDTO syncUserDTO = new SyncUserDTO();
                    BeanUtils.copyProperties(userInfo, syncUserDTO);
                    result.add(syncUserDTO);
                });
                return result;
            }
        }
        return Collections.emptyList();
    }

    /**
     * hzero???????????????????????????????????????????????????
     *
     * @param syncUserList     hzero????????????????????????
     * @param dingSyncUserList ????????????????????????
     */
    private void convertSyncUserList(List<SyncUserDTO> syncUserList, List<DingSyncUserDTO> dingSyncUserList) {
        syncUserList.forEach(e -> {
            DingSyncUserDTO dingSyncUserDTO = new DingSyncUserDTO();
            BeanUtils.copyProperties(e, dingSyncUserDTO);
            dingSyncUserList.add(dingSyncUserDTO);
        });
    }

    /**
     * hzero???????????????????????????????????????????????????
     *
     * @param syncDeptList     hzero??????????????????
     * @param dingSyncDeptList ????????????????????????
     */
    private void convertSyncDeptList(List<SyncDeptDTO> syncDeptList, List<DingSyncDeptDTO> dingSyncDeptList) {
        syncDeptList.forEach(e -> {
            DingSyncDeptDTO dingSyncDeptDTO = new DingSyncDeptDTO();
            BeanUtils.copyProperties(e, dingSyncDeptDTO);
            dingSyncDeptList.add(dingSyncDeptDTO);
        });
    }

    /**
     * ????????????
     *
     * @param insertDeptList     ??????????????????
     * @param successDeptIdMap   ???????????????????????????id
     * @param useGeneratedDeptId ??????????????????????????????????????????id
     * @param accessToken        token
     * @param resultCount        ??????????????????
     */
    private void insertDeptList(List<DingSyncDeptDTO> insertDeptList, Map<Long, Long> successDeptIdMap,
                                Boolean useGeneratedDeptId, String accessToken, ResultCount resultCount, StringBuilder log) {
        if (!CollectionUtils.isEmpty(insertDeptList)) {
            ArrayList<DingSyncDeptDTO> dingSyncDeptDTOList = sortDept(insertDeptList);
            for (DingSyncDeptDTO j : dingSyncDeptDTOList) {
                Long deptId = j.getId();
                // ?????????????????????id????????????null
                j.setId(null);
                boolean isParentIdMap = convertDeptParentId(j, successDeptIdMap);
                logger.debug("Create DingTalk department: {}", j.toString());
                log.append("Create DingTalk department: ").append(j.toString()).append(org.apache.commons.lang3.StringUtils.LF);
                if (!isParentIdMap) {
                    // ??????????????????id
                    logger.debug("Create failed, the parent department does not exist.");
                    log.append("Create failed,the parent department does not exist.").append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.deptFail();
                    continue;
                }
                CreateDeptResultDTO dept = dingCorpAddressService.createDept(accessToken, j);
                if (DingErrorCode.SUCCESS.equals(dept.getErrcode())) {
                    successDeptIdMap.put(deptId, dept.getId());
                    resultCount.deptSuccess();
                } else {
                    if (DingErrorCode.DEPT_EXISTS.equals(dept.getErrcode()) && !useGeneratedDeptId) {
                        // ?????????????????????????????????
                        logger.debug("Department already exists,update department");
                        log.append("Department already exists,update department").append(org.apache.commons.lang3.StringUtils.LF);
                        UpdateDeptResultDTO updateDeptResultDTO = dingCorpAddressService.updateDept(accessToken, j);
                        if (DingErrorCode.SUCCESS.equals(updateDeptResultDTO.getErrcode())) {
                            successDeptIdMap.put(deptId, deptId);
                            resultCount.deptSuccess();
                        } else {
                            resultCount.deptFail();
                            logger.debug("Update failed: {}", updateDeptResultDTO.getErrmsg());
                            log.append("Update failed: ").append(updateDeptResultDTO.getErrmsg())
                                    .append(org.apache.commons.lang3.StringUtils.LF);
                        }
                    } else {
                        resultCount.deptFail();
                        logger.debug("Create failed: {}", dept.getErrmsg());
                        log.append("Create failed: ").append(dept.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
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
     * @param updateUsersMap   ?????????????????????
     * @param successDeptIdMap ???????????????????????????id
     * @param successUserIds   ???????????????????????????id
     * @param accessToken      token
     * @param resultCount      ??????????????????
     */
    private void deleteDeptList(List<Long> deleteDeptIds, List<String> deleteUserIds,
                                Map<String, DingSyncUserDTO> updateUsersMap, Map<Long, Long> successDeptIdMap,
                                List<String> successUserIds, String accessToken, ResultCount resultCount, StringBuilder log) {
        while (!CollectionUtils.isEmpty(deleteDeptIds)) {
            Long deptId = deleteDeptIds.get(0);
            DeptListDTO deptList = dingCorpAddressService.getDeptList(deptId, accessToken, "", "true");
            Department rootDept = new Department();
            rootDept.setId(deptId);
            deptList.getDepartment().add(rootDept);
            if (!CollectionUtils.isEmpty(deptList.getDepartment())) {
                logger.debug("Delete the DingTalk department and its sub-departments id: {}", deptId);
                log.append("Delete the DingTalk department and its sub-departments id:").append(deptId)
                        .append(org.apache.commons.lang3.StringUtils.LF);
                deleteDeptIds.removeAll(
                        deptList.getDepartment().stream().map(Department::getId).collect(Collectors.toList()));
                ArrayList<Department> departments = sortDept(deptList.getDepartment(), deptId);
                for (Department k : departments) {
                    logger.debug("Delete DingTalk department???id: {}", k.getId());
                    log.append("Delete DingTalk department???id:").append(k.getId()).append(org.apache.commons.lang3.StringUtils.LF);
                    logger.debug("Change users under the department before deleting the DingTalk department");
                    log.append("Change users under the department before deleting the DingTalk department")
                            .append(org.apache.commons.lang3.StringUtils.LF);
                    GetUserListDTO userListByDeptId = dingCorpAddressService.getUserListByDeptId(accessToken, k.getId() + "");
                    List<String> userIds = userListByDeptId.getUserIds();
                    if (!CollectionUtils.isEmpty(userIds)) {
                        for (String userId : userIds) {
                            if (deleteUserIds.contains(userId)) {
                                deleteUserIds.remove(userId);
                                // ??????????????????
                                logger.debug("Delete employees under department, userid: {}", userId);
                                log.append("Delete employees under department: userid").append(userId)
                                        .append(org.apache.commons.lang3.StringUtils.LF);
                                DefaultResultDTO defaultResultDTO = dingCorpAddressService.deleteUserById(accessToken, userId);
                                if (!DingErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                                    resultCount.userFail();
                                    logger.debug("Delete failed???{}", defaultResultDTO.getErrmsg());
                                    log.append("Delete failed???").append(defaultResultDTO.getErrmsg())
                                            .append(org.apache.commons.lang3.StringUtils.LF);
                                } else {
                                    successUserIds.add(userId);
                                    resultCount.userSuccess();
                                }
                            } else if (updateUsersMap.containsKey(userId)) {
                                // ????????????
                                DingSyncUserDTO dingSyncUserDTO = updateUsersMap.get(userId);
                                updateUsersMap.remove(userId);
                                boolean isAllDepartIdsMap = convertUserDepart(dingSyncUserDTO, successDeptIdMap);
                                logger.debug("Update department DingTalk employees: {}",dingSyncUserDTO.toString());
                                log.append("Update department DingTalk employees:").append(dingSyncUserDTO.toString())
                                        .append(org.apache.commons.lang3.StringUtils.LF);
                                // ????????????????????????????????????
                                if (!isAllDepartIdsMap) {
                                    logger.debug("Update employee failed, department does not exist");
                                    log.append( "Update employee failed, department does not exist")
                                            .append(org.apache.commons.lang3.StringUtils.LF);
                                    resultCount.userFail();
                                    continue;
                                }
                                DefaultResultDTO defaultResultDTO = dingCorpAddressService.updateUser(accessToken, dingSyncUserDTO);
                                if (!DingErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                                    logger.debug("Update failed: {}", dingSyncUserDTO.toString());
                                    log.append("Update failed: ").append(dingSyncUserDTO.toString())
                                            .append(org.apache.commons.lang3.StringUtils.LF);
                                    resultCount.userFail();
                                } else {
                                    successUserIds.add(userId);
                                    resultCount.userSuccess();
                                }
                            } else {
                                GetUserDTO userInfoByUserId = dingCorpAddressService.getUserInfoByUserId(accessToken, userId + "", "");
                                logger.debug("The employee is not included in the sync employees: {}", userInfoByUserId.toString());
                                log.append("The employee is not included in the sync employees: ").append(userInfoByUserId.toString())
                                        .append(org.apache.commons.lang3.StringUtils.LF);
                            }
                        }
                    }
                    DefaultResultDTO defaultResultDTO = dingCorpAddressService.deleteDeptById(accessToken, k.getId() + "");
                    if (DingErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                        successDeptIdMap.put(k.getId(), null);
                        resultCount.deptSuccess();
                    } else {
                        logger.debug("Delete department failed: {}", defaultResultDTO.getErrmsg());
                        log.append("Delete department failed: ").append(defaultResultDTO.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                        resultCount.deptFail();
                    }
                }
            }
        }
    }

    /**
     * ????????????
     *
     * @param updateDeptList   ?????????????????????
     * @param successDeptIdMap ???????????????????????????id
     * @param accessToken      token
     * @param resultCount      ??????????????????
     */
    private void updateDeptList(List<DingSyncDeptDTO> updateDeptList, Map<Long, Long> successDeptIdMap,
                                String accessToken, ResultCount resultCount, StringBuilder log) {
        if (!CollectionUtils.isEmpty(updateDeptList)) {
            for (DingSyncDeptDTO j : updateDeptList) {
                boolean isParentIdMap = convertDeptParentId(j, successDeptIdMap);
                logger.debug("Update DingTalk department: {}", j.toString());
                log.append("Update DingTalk department:").append(j.toString()).append(org.apache.commons.lang3.StringUtils.LF);
                if (!isParentIdMap) {
                    // ???????????????id
                    resultCount.deptFail();
                    logger.debug("Update failed, parent department does not exist.");
                    log.append( "Update failed, parent department does not exist.").append(org.apache.commons.lang3.StringUtils.LF);
                    continue;
                }
                UpdateDeptResultDTO updateDeptResultDTO = dingCorpAddressService.updateDept(accessToken, j);
                if (DingErrorCode.SUCCESS.equals(updateDeptResultDTO.getErrcode())) {
                    successDeptIdMap.put(j.getId(), null);
                    resultCount.deptSuccess();
                } else {
                    logger.debug("Update failed: {}", updateDeptResultDTO.getErrmsg());
                    log.append("Update failed: ").append(updateDeptResultDTO.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.deptFail();
                }
            }
        }
    }

    /**
     * ????????????
     *
     * @param deleteUserIds  ?????????????????????
     * @param successUserIds ???????????????????????????id
     * @param accessToken    token
     * @param resultCount    ??????????????????
     */
    private void deleteUsers(List<String> deleteUserIds, List<String> successUserIds, String accessToken,
                             ResultCount resultCount, StringBuilder log) {
        if (!CollectionUtils.isEmpty(deleteUserIds)) {
            for (String j : deleteUserIds) {
                logger.debug("Delete DingTalk employees, userid: {}", j);
                log.append("Delete DingTalk employees: userid").append(j).append(org.apache.commons.lang3.StringUtils.LF);
                DefaultResultDTO defaultResultDTO = dingCorpAddressService.deleteUserById(accessToken, j);
                if (!DingErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                    logger.debug("Delete failed: {}", defaultResultDTO.getErrmsg());
                    log.append("Delete failed???").append(defaultResultDTO.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.userFail();
                } else {
                    successUserIds.add(j);
                    resultCount.userSuccess();
                }
            }
        }
    }

    /**
     * ????????????
     *
     * @param insertUsers      ?????????????????????
     * @param successDeptIdMap ???????????????????????????id
     * @param successUserIds   ???????????????????????????
     * @param accessToken      token
     * @param resultCount      ??????????????????
     */
    private void insertUsers(List<DingSyncUserDTO> insertUsers, Map<Long, Long> successDeptIdMap,
                             List<String> successUserIds, String accessToken, ResultCount resultCount, StringBuilder log) {
        if (!CollectionUtils.isEmpty(insertUsers)) {
            for (DingSyncUserDTO j : insertUsers) {
                boolean isAllDepartIdsMap = convertUserDepart(j, successDeptIdMap);
                logger.debug("Create DingTalk employees: {}", j.toString());
                log.append("Create DingTalk employees:").append(j.toString()).append(org.apache.commons.lang3.StringUtils.LF);
                // ????????????????????????????????????
                if (!isAllDepartIdsMap) {
                    logger.debug("Create employees failed, department does not exist.");
                    log.append("Create employees failed, department does not exist.").append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.userFail();
                    continue;
                }
                UserCreateResultDTO user = dingCorpAddressService.createUser(accessToken, j);
                if (DingErrorCode.SUCCESS.equals(user.getErrcode())) {
                    successUserIds.add(j.getUserid());
                    resultCount.userSuccess();
                } else {
                    if (DingErrorCode.USERID_EXISTS.equals(user.getErrcode())) {
                        // ?????????????????????????????????
                        logger.debug("Employee already exists, update employee");
                        log.append("Employee already exists, update employee").append(org.apache.commons.lang3.StringUtils.LF);
                        DefaultResultDTO defaultResultDTO = dingCorpAddressService.updateUser(accessToken, j);
                        if (DingErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                            successUserIds.add(j.getUserid());
                            resultCount.userSuccess();
                        } else {
                            resultCount.userFail();
                            logger.debug("Update failed: {}", defaultResultDTO.getErrmsg());
                            log.append("Update failed: ").append(defaultResultDTO.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                        }
                    } else {
                        resultCount.userFail();
                        logger.debug("Create failed: {}", user.getErrmsg());
                        log.append("Create failed: ").append(user.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                    }
                }
            }
        }
    }

    /**
     * ????????????
     *
     * @param updateUsersMap   ?????????????????????
     * @param successDeptIdMap ???????????????????????????id
     * @param successUserIds   ???????????????????????????
     * @param accessToken      token
     * @param resultCount      ??????????????????
     */
    private void updateUsers(Map<String, DingSyncUserDTO> updateUsersMap, Map<Long, Long> successDeptIdMap,
                             List<String> successUserIds, String accessToken, ResultCount resultCount, StringBuilder log) {
        if (!CollectionUtils.isEmpty(updateUsersMap.values())) {
            for (DingSyncUserDTO j : updateUsersMap.values()) {
                boolean isAllDepartIdsMap = convertUserDepart(j, successDeptIdMap);
                logger.debug("Update DingTalk employees: {}", j.toString());
                log.append("Update DingTalk employees: ").append(j.toString()).append(org.apache.commons.lang3.StringUtils.LF);
                // ????????????????????????????????????
                if (!isAllDepartIdsMap) {
                    logger.debug("Update employee failed, department does not exist.");
                    log.append("Update employee failed, department does not exist.").append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.userFail();
                    continue;
                }
                DefaultResultDTO defaultResultDTO = dingCorpAddressService.updateUser(accessToken, j);
                if (DingErrorCode.SUCCESS.equals(defaultResultDTO.getErrcode())) {
                    successUserIds.add(j.getUserid());
                    resultCount.userSuccess();
                } else {
                    logger.debug("Update failed: {}", defaultResultDTO.getErrmsg());
                    log.append("Update failed: ").append(defaultResultDTO.getErrmsg()).append(org.apache.commons.lang3.StringUtils.LF);
                    resultCount.userFail();
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

        void userSuccess() {
            userSuccessCount += 1;
        }

        void userFail() {
            userFailCount += 1;
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
     * @param syncDeptList ??????????????????????????????
     * @return ????????????????????????
     */
    private ArrayList<DingSyncDeptDTO> sortDept(List<DingSyncDeptDTO> syncDeptList) {
        ArrayList<DingSyncDeptDTO> sortDept = new ArrayList<>();
        List<Long> collect = syncDeptList.stream().map(DingSyncDeptDTO::getId).collect(Collectors.toList());
        boolean init = true;
        while (!CollectionUtils.isEmpty(syncDeptList)) {
            if (init) {
                List<Long> finalCollect = collect;
                List<DingSyncDeptDTO> rootDept = syncDeptList.stream()
                        .filter(e -> (DingConstans.ROOT_DEPT_ID.equals(e.getParentid()) || !finalCollect.contains(e.getParentUnitId())))
                        .collect(Collectors.toList());
                collect = rootDept.stream().map(DingSyncDeptDTO::getId).collect(Collectors.toList());
                sortDept.addAll(rootDept);
                syncDeptList.removeAll(rootDept);
                init = false;
            } else {
                List<Long> finalCollect = collect;
                List<DingSyncDeptDTO> deptList = syncDeptList.stream()
                        .filter(e -> (finalCollect.contains(e.getParentUnitId()))).collect(Collectors.toList());
                collect = deptList.stream().map(DingSyncDeptDTO::getId).collect(Collectors.toList());
                sortDept.addAll(deptList);
                syncDeptList.removeAll(deptList);
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
    private ArrayList<Department> sortDept(List<Department> departmentBeans, Long rootId) {
        List<Long> rootIds = null;
        final Long finalRootId = rootId;
        boolean init = true;
        ArrayList<Department> sortDept = new ArrayList<>();
        while (!CollectionUtils.isEmpty(departmentBeans)) {
            if (init) {
                List<Department> rootDept = departmentBeans.stream().filter(j -> j.getId() == finalRootId)
                        .collect(Collectors.toList());
                sortDept.addAll(rootDept);
                departmentBeans.removeAll(rootDept);
                rootDept = departmentBeans.stream().filter(j -> (Objects.equals(j.getParentid(), finalRootId)))
                        .collect(Collectors.toList());
                sortDept.addAll(rootDept);
                departmentBeans.removeAll(rootDept);
                rootIds = rootDept.stream().map(Department::getId).collect(Collectors.toList());
                init = false;
            } else {
                ArrayList<Department> otherDept = new ArrayList<>();
                for (Department departmentBean : departmentBeans) {
                    if (!CollectionUtils.isEmpty(rootIds) && rootIds.contains(departmentBean.getParentid())) {
                        otherDept.add(departmentBean);
                    }
                }
                sortDept.addAll(otherDept);
                departmentBeans.removeAll(otherDept);
                rootIds = otherDept.stream().map(Department::getId).collect(Collectors.toList());
            }
        }
        Collections.reverse(sortDept);
        return sortDept;
    }

    /**
     * ???hzero??????id?????????????????????id
     *
     * @param syncDeptDTO      ????????????
     * @param successDeptIdMap ????????????id???????????????id????????????
     * @return ??????id??????????????????
     */
    private boolean convertDeptParentId(DingSyncDeptDTO syncDeptDTO, Map<Long, Long> successDeptIdMap) {
        if (!DingConstans.ROOT_DEPT_ID.equals(syncDeptDTO.getParentid()) && syncDeptDTO.getParentid() == null) {
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
    private boolean convertUserDepart(DingSyncUserDTO syncUserDTO, Map<Long, Long> successDeptIdMap) {
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
}
